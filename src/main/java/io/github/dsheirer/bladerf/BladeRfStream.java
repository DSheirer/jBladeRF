package io.github.dsheirer.bladerf;

import static java.lang.foreign.ValueLayout.ADDRESS;

import io.github.dsheirer.bladerf.internal.Native;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.List;

/**
 * A running asynchronous stream, started by {@link BladeRfDevice#startRx} or
 * {@link BladeRfDevice#startTx}. The stream runs on its own daemon thread, which
 * disables the stream's channels and frees its native resources when it ends.
 * Closing the stream stops it.
 *
 * <p>The stream hands its buffers out in rotation, so a buffer must not be used
 * after the callback that received it returns.
 */
public final class BladeRfStream implements AutoCloseable {
    /**
     * True on a thread running a stream callback. This is per thread and not per
     * stream because libbladeRF can run one stream's callback on the thread of
     * the opposite direction's stream.
     */
    private static final ThreadLocal<Boolean> IN_CALLBACK = new ThreadLocal<>();
    private static final MethodHandle ON_BUFFER;

    static {
        try {
            ON_BUFFER = MethodHandles.lookup().findVirtual(BladeRfStream.class, "onBuffer",
                MethodType.methodType(MemorySegment.class, MemorySegment.class, MemorySegment.class,
                    MemorySegment.class, MemorySegment.class, long.class, MemorySegment.class));
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private final BladeRfDevice device;
    private final ChannelLayout layout;
    private final SampleFormat format;
    private final StreamConfig config;
    private final SampleListener listener;
    private final SampleProducer producer;
    private final List<Channel> channels;
    private final Arena arena = Arena.ofShared();
    private final Thread thread;

    private MemorySegment stream;
    private MemorySegment[] buffers;
    // Only one callback of a stream runs at a time, but not always on one thread.
    private volatile int nextBuffer;
    private volatile boolean stopRequested;
    private volatile Throwable failure;

    private BladeRfStream(BladeRfDevice device, ChannelLayout layout, SampleFormat format, StreamConfig config,
            SampleListener listener, SampleProducer producer) {
        this.device = device;
        this.layout = layout;
        this.format = format;
        this.config = config;
        this.listener = listener;
        this.producer = producer;
        this.channels = channels(layout);
        this.thread = new Thread(this::run, "jbladerf-" + layout);
        thread.setDaemon(true);
        thread.setPriority(Thread.MAX_PRIORITY);
    }

    /** Returns the channels of a layout. */
    static List<Channel> channels(ChannelLayout layout) {
        return switch (layout) {
            case RX_X1 -> List.of(Channel.RX0);
            case TX_X1 -> List.of(Channel.TX0);
            case RX_X2 -> List.of(Channel.RX0, Channel.RX1);
            case TX_X2 -> List.of(Channel.TX0, Channel.TX1);
        };
    }

    /** Initializes a stream, enables its channels and starts its thread. */
    static BladeRfStream start(BladeRfDevice device, MemorySegment handle, ChannelLayout layout, SampleFormat format,
            StreamConfig config, SampleListener listener, SampleProducer producer) throws BladeRfException {
        BladeRfStream result = new BladeRfStream(device, layout, format, config, listener, producer);
        result.start(handle);
        return result;
    }

    private void start(MemorySegment handle) throws BladeRfException {
        try {
            init(handle);
        } catch (BladeRfException | RuntimeException | Error e) {
            arena.close();
            throw e;
        }
        int enabled = 0;
        try {
            for (Channel channel : channels) {
                BladeRf.check("bladerf_enable_module",
                    Native.invoke(Native.ENABLE_MODULE, handle, channel.code(), true));
                enabled++;
            }
            thread.start();
        } catch (BladeRfException | RuntimeException | Error e) {
            for (Channel channel : channels.subList(0, enabled)) {
                Native.invoke(Native.ENABLE_MODULE, handle, channel.code(), false);
            }
            release();
            throw e;
        }
    }

    private void init(MemorySegment handle) throws BladeRfException {
        MemorySegment streamPointer = arena.allocate(ADDRESS);
        MemorySegment buffersPointer = arena.allocate(ADDRESS);
        MemorySegment callback = Native.streamCallback(ON_BUFFER.bindTo(this), arena);
        BladeRf.check("bladerf_init_stream", Native.invoke(Native.INIT_STREAM, streamPointer, handle, callback,
            buffersPointer, (long) config.bufferCount(), format.code(), (long) config.samplesPerBuffer(),
            (long) config.transferCount(), MemorySegment.NULL));
        stream = streamPointer.get(ADDRESS, 0);
        MemorySegment array = buffersPointer.get(ADDRESS, 0).reinterpret(config.bufferCount() * ADDRESS.byteSize());
        buffers = new MemorySegment[config.bufferCount()];
        for (int i = 0; i < buffers.length; i++) {
            buffers[i] = array.getAtIndex(ADDRESS, i);
        }
        // The library starts receiving into the first transferCount buffers by
        // itself, while transmitting asks for every buffer from the start.
        nextBuffer = listener != null ? config.transferCount() % buffers.length : 0;
    }

    /** Body of the stream's thread. */
    private void run() {
        try {
            int status = Native.invoke(Native.STREAM, stream, layout.code());
            if (status < 0 && failure == null) {
                failure = new BladeRfException("bladerf_stream", status);
            }
        } catch (Throwable t) {
            recordFailure(t);
        }
        MemorySegment handle = device.nativeHandle();
        for (Channel channel : channels) {
            try {
                BladeRf.check("bladerf_enable_module",
                    Native.invoke(Native.ENABLE_MODULE, handle, channel.code(), false));
            } catch (Throwable t) {
                recordFailure(t);
            }
        }
        release();
    }

    private void recordFailure(Throwable t) {
        if (failure == null) {
            failure = t;
        }
    }

    private void release() {
        Native.invokeVoid(Native.DEINIT_STREAM, stream);
        arena.close();
    }

    /**
     * Called through the upcall stub for every finished transfer, and with NULL
     * samples for each transmit transfer that is starting up. An exception that
     * escaped from here would terminate the JVM, so all of them are caught.
     */
    @SuppressWarnings("unused")
    private MemorySegment onBuffer(MemorySegment deviceHandle, MemorySegment streamHandle, MemorySegment metadata,
            MemorySegment samples, long sampleCount, MemorySegment userData) {
        if (stopRequested) {
            return MemorySegment.NULL;
        }
        IN_CALLBACK.set(Boolean.TRUE);
        // Closing this arena ends the segment's lifetime, so that a listener that
        // keeps it gets an exception and not memory that is being reused.
        try (Arena scope = Arena.ofConfined()) {
            int index = nextBuffer;
            nextBuffer = (index + 1) % buffers.length;
            MemorySegment next = buffers[index];
            boolean proceed;
            if (listener != null) {
                MemorySegment memory = samples.reinterpret(format.byteLength(sampleCount), scope, null);
                proceed = listener.onSamples(new SampleBuffer(memory, Math.toIntExact(sampleCount), format));
            } else {
                MemorySegment memory = next.reinterpret(format.byteLength(config.samplesPerBuffer()), scope, null);
                proceed = producer.fill(memory, config.samplesPerBuffer());
            }
            return proceed ? next : MemorySegment.NULL;
        } catch (Throwable t) {
            recordFailure(t);
            return MemorySegment.NULL;
        } finally {
            IN_CALLBACK.remove();
        }
    }

    /** Returns true until the stream has ended and released its resources. */
    public boolean isRunning() {
        return thread.isAlive();
    }

    /**
     * Returns the error or exception that ended the stream, or null if there was
     * none. An exception from a listener takes priority over a library error.
     */
    public Throwable failure() {
        return failure;
    }

    /**
     * Stops the stream and waits for it to end. Does nothing if it has ended.
     *
     * @throws IllegalStateException if called from a {@link SampleListener} or
     *     {@link SampleProducer}
     */
    public void stop() {
        rejectCallbackThread("stop");
        stopRequested = true;
        boolean interrupted = false;
        // Native memory is released at the end of the thread, so a caller that
        // needs the device must not carry on before that.
        while (thread.isAlive()) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                interrupted = true;
            }
        }
        if (interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    /** Stops the stream. */
    @Override
    public void close() {
        stop();
    }

    /** Throws if the current thread is running a stream callback. */
    static void rejectCallbackThread(String method) {
        if (IN_CALLBACK.get() != null) {
            throw new IllegalStateException(method + " cannot be called from a stream callback");
        }
    }
}
