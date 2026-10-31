package io.github.dsheirer.bladerf;

import static io.github.dsheirer.bladerf.BladeRf.check;
import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_BOOLEAN;
import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.lang.foreign.ValueLayout.JAVA_FLOAT;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;
import static java.lang.foreign.ValueLayout.JAVA_SHORT;

import io.github.dsheirer.bladerf.internal.Coded;
import io.github.dsheirer.bladerf.internal.Layouts;
import io.github.dsheirer.bladerf.internal.Native;
import java.lang.foreign.Arena;
import java.lang.foreign.GroupLayout;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * An open bladeRF device. Close it when finished, which also stops its streams.
 *
 * <p>Methods labelled bladeRF 1 or bladeRF 2 fail with
 * {@link BladeRfError#UNSUPPORTED} on the other board. Methods may be called from
 * any thread, except that {@link #close()} must not be called from a stream
 * callback or while other threads are still using the device.
 *
 * <p>Samples move in one of two ways. The synchronous interface is simpler:
 * call {@link #syncConfig}, then {@link #enableChannel}, then {@link #syncRx} or
 * {@link #syncTx} in a loop. The asynchronous interface of {@link #startRx} and
 * {@link #startTx} delivers samples through a callback and enables the channels
 * by itself. Only one of them can be used per direction at a time.
 *
 * <p>A device needs its FPGA to be loaded before it can stream: see
 * {@link #isFpgaConfigured()} and {@link #loadFpga}.
 */
public final class BladeRfDevice implements AutoCloseable {
    /** Timestamp for {@link #scheduleRetune} that retunes immediately. */
    public static final long RETUNE_NOW = 0;
    /** Largest number of names that {@link #gainStages} and {@link #rfPorts} ask for. */
    private static final int MAX_NAMES = 64;

    private final MemorySegment handle;
    private final Object lifecycleLock = new Object();
    private boolean closed;
    private BladeRfStream rxStream;
    private BladeRfStream txStream;
    private volatile SampleFormat rxFormat;
    private volatile SampleFormat txFormat;

    BladeRfDevice(MemorySegment handle) {
        this.handle = handle;
    }

    // ---- Information ----

    /** Returns the description of this device. */
    public DeviceInfo deviceInfo() throws BladeRfException {
        return read("bladerf_get_devinfo", Native.GET_DEVINFO, Layouts.DEVINFO, DeviceInfo::read);
    }

    /** Returns the internal handles of this device's backend. */
    public BackendInfo backendInfo() throws BladeRfException {
        GroupLayout layout = Layouts.BACKENDINFO;
        return read("bladerf_get_backendinfo", Native.GET_BACKENDINFO, layout, s -> new BackendInfo(
            Layouts.i32(s, layout, "handle_count"), Layouts.pointer(s, layout, "handle").address(),
            Layouts.i32(s, layout, "lock_count"), Layouts.pointer(s, layout, "lock").address()));
    }

    /** Returns the serial number. */
    public String serial() throws BladeRfException {
        return read("bladerf_get_serial_struct", Native.GET_SERIAL_STRUCT, Layouts.SERIAL,
            s -> Layouts.string(s, Layouts.SERIAL, "serial"));
    }

    /** Returns the name of the board, such as {@code bladerf2}. */
    public String boardName() {
        return Native.cString(Native.invokePointer(Native.GET_BOARD_NAME, handle()));
    }

    /** Returns the USB bus speed. */
    public DeviceSpeed speed() {
        return Coded.of(DeviceSpeed.class, Native.invoke(Native.DEVICE_SPEED, handle()));
    }

    /** Returns the number of channels in a direction. */
    public int channelCount(Direction direction) {
        return Math.toIntExact(Native.invokeLong(Native.GET_CHANNEL_COUNT, handle(), direction.code()));
    }

    /** Returns the size of the SPI flash. */
    public FlashSize flashSize() throws BladeRfException {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment size = arena.allocate(JAVA_INT);
            MemorySegment guessed = arena.allocate(JAVA_BOOLEAN);
            call("bladerf_get_flash_size", Native.GET_FLASH_SIZE, size, guessed);
            return new FlashSize(Integer.toUnsignedLong(size.get(JAVA_INT, 0)), guessed.get(JAVA_BOOLEAN, 0));
        }
    }

    /** Returns the firmware version. */
    public Version firmwareVersion() throws BladeRfException {
        return readVersion("bladerf_fw_version", Native.FW_VERSION);
    }

    /** Returns the version of the loaded FPGA image. */
    public Version fpgaVersion() throws BladeRfException {
        return readVersion("bladerf_fpga_version", Native.FPGA_VERSION);
    }

    /** Returns true if the FPGA has been loaded, which streaming requires. */
    public boolean isFpgaConfigured() throws BladeRfException {
        return call("bladerf_is_fpga_configured", Native.IS_FPGA_CONFIGURED) != 0;
    }

    /** Returns the size of the FPGA. */
    public FpgaSize fpgaSize() throws BladeRfException {
        return readEnum(FpgaSize.class, "bladerf_get_fpga_size", Native.GET_FPGA_SIZE);
    }

    /** Returns the size in bytes of an FPGA image for this device. */
    public long fpgaBytes() throws BladeRfException {
        return readLong("bladerf_get_fpga_bytes", Native.GET_FPGA_BYTES);
    }

    /** Returns where the FPGA was loaded from. */
    public FpgaSource fpgaSource() throws BladeRfException {
        return readEnum(FpgaSource.class, "bladerf_get_fpga_source", Native.GET_FPGA_SOURCE);
    }

    // ---- Tuning ----

    /** Tunes a channel to a frequency in Hz. */
    public void setFrequency(Channel channel, long hertz) throws BladeRfException {
        call("bladerf_set_frequency", Native.SET_FREQUENCY, channel.code(), hertz);
    }

    /** Returns the frequency of a channel in Hz. */
    public long frequency(Channel channel) throws BladeRfException {
        return readLong("bladerf_get_frequency", Native.GET_FREQUENCY, channel.code());
    }

    /** Returns the frequencies a channel supports, in Hz. */
    public Range frequencyRange(Channel channel) throws BladeRfException {
        return readRange("bladerf_get_frequency_range", Native.GET_FREQUENCY_RANGE, channel.code());
    }

    /** Selects the band for a frequency in Hz without tuning to it. */
    public void selectBand(Channel channel, long hertz) throws BladeRfException {
        call("bladerf_select_band", Native.SELECT_BAND, channel.code(), hertz);
    }

    /**
     * Sets a channel's sample rate.
     *
     * @param hertz requested rate in Hz
     * @return the rate that was set, in Hz
     */
    public long setSampleRate(Channel channel, long hertz) throws BladeRfException {
        return readUnsigned("bladerf_set_sample_rate", Native.SET_SAMPLE_RATE, channel.code(), u32(hertz));
    }

    /**
     * Sets a channel's bandwidth.
     *
     * @param hertz requested bandwidth in Hz
     * @return the bandwidth that was set, in Hz
     */
    public long setBandwidth(Channel channel, long hertz) throws BladeRfException {
        return readUnsigned("bladerf_set_bandwidth", Native.SET_BANDWIDTH, channel.code(), u32(hertz));
    }

    /**
     * Sets a channel's sample rate exactly.
     *
     * @return the rate that was set
     */
    public RationalRate setSampleRate(Channel channel, RationalRate rate) throws BladeRfException {
        return rational("bladerf_set_rational_sample_rate", Native.SET_RATIONAL_SAMPLE_RATE, rate, channel.code());
    }

    private RationalRate rational(String function, MethodHandle method, RationalRate rate, Object... arguments)
            throws BladeRfException {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment requested = arena.allocate(Layouts.RATIONAL_RATE);
            rate.write(requested);
            Object[] all = new Object[arguments.length + 1];
            System.arraycopy(arguments, 0, all, 0, arguments.length);
            all[arguments.length] = requested;
            return read(function, method, Layouts.RATIONAL_RATE, RationalRate::read, all);
        }
    }

    /** Returns a channel's exact sample rate. */
    public RationalRate rationalSampleRate(Channel channel) throws BladeRfException {
        return read("bladerf_get_rational_sample_rate", Native.GET_RATIONAL_SAMPLE_RATE, Layouts.RATIONAL_RATE,
            RationalRate::read, channel.code());
    }

    /** Returns a channel's sample rate in Hz. */
    public long sampleRate(Channel channel) throws BladeRfException {
        return readUnsigned("bladerf_get_sample_rate", Native.GET_SAMPLE_RATE, channel.code());
    }

    /** Returns the sample rates a channel supports, in Hz. */
    public Range sampleRateRange(Channel channel) throws BladeRfException {
        return readRange("bladerf_get_sample_rate_range", Native.GET_SAMPLE_RATE_RANGE, channel.code());
    }

    /** Returns a channel's bandwidth in Hz. */
    public long bandwidth(Channel channel) throws BladeRfException {
        return readUnsigned("bladerf_get_bandwidth", Native.GET_BANDWIDTH, channel.code());
    }

    /** Returns the bandwidths a channel supports, in Hz. */
    public Range bandwidthRange(Channel channel) throws BladeRfException {
        return readRange("bladerf_get_bandwidth_range", Native.GET_BANDWIDTH_RANGE, channel.code());
    }

    /** Selects which tuning calculations the host or the FPGA does. */
    public void setTuningMode(TuningMode mode) throws BladeRfException {
        call("bladerf_set_tuning_mode", Native.SET_TUNING_MODE, mode.code());
    }

    /** Returns the tuning mode. */
    public TuningMode tuningMode() throws BladeRfException {
        return readEnum(TuningMode.class, "bladerf_get_tuning_mode", Native.GET_TUNING_MODE);
    }

    /**
     * Schedules a retune, for use while streaming with metadata.
     *
     * @param timestamp sample time to retune at, or {@link #RETUNE_NOW}
     * @param hertz frequency in Hz, ignored if {@code quickTune} is given
     * @param quickTune saved settings from {@link #quickTune}, or null
     */
    public void scheduleRetune(Channel channel, long timestamp, long hertz, QuickTune quickTune)
            throws BladeRfException {
        try (Arena arena = Arena.ofConfined()) {
            call("bladerf_schedule_retune", Native.SCHEDULE_RETUNE, channel.code(), timestamp, hertz,
                quickTune(quickTune, arena));
        }
    }

    private static MemorySegment quickTune(QuickTune quickTune, Arena arena) {
        if (quickTune == null) {
            return MemorySegment.NULL;
        }
        MemorySegment segment = arena.allocate(Layouts.QUICK_TUNE);
        quickTune.write(segment);
        return segment;
    }

    /** Returns settings that retune to the channel's current frequency faster. */
    public QuickTune quickTune(Channel channel) throws BladeRfException {
        return read("bladerf_get_quick_tune", Native.GET_QUICK_TUNE, Layouts.QUICK_TUNE, QuickTune::new,
            channel.code());
    }

    /** Prints quick tune settings to stdout. */
    public void printQuickTune(QuickTune quickTune) throws BladeRfException {
        Objects.requireNonNull(quickTune, "quickTune");
        try (Arena arena = Arena.ofConfined()) {
            call("bladerf_print_quick_tune", Native.PRINT_QUICK_TUNE, quickTune(quickTune, arena));
        }
    }

    /** Cancels the scheduled retunes of a channel. */
    public void cancelScheduledRetunes(Channel channel) throws BladeRfException {
        call("bladerf_cancel_scheduled_retunes", Native.CANCEL_SCHEDULED_RETUNES, channel.code());
    }

    // ---- Gain and RF ----

    /** Sets the overall gain of a channel in dB. */
    public void setGain(Channel channel, int gain) throws BladeRfException {
        call("bladerf_set_gain", Native.SET_GAIN, channel.code(), gain);
    }

    /** Returns the overall gain of a channel in dB. */
    public int gain(Channel channel) throws BladeRfException {
        return readInt("bladerf_get_gain", Native.GET_GAIN, channel.code());
    }

    /** Returns the overall gains a channel supports. */
    public Range gainRange(Channel channel) throws BladeRfException {
        return readRange("bladerf_get_gain_range", Native.GET_GAIN_RANGE, channel.code());
    }

    /** Selects a channel's gain mode. */
    public void setGainMode(Channel channel, GainMode mode) throws BladeRfException {
        call("bladerf_set_gain_mode", Native.SET_GAIN_MODE, channel.code(), mode.code());
    }

    /** Returns a channel's gain mode. */
    public GainMode gainMode(Channel channel) throws BladeRfException {
        return readEnum(GainMode.class, "bladerf_get_gain_mode", Native.GET_GAIN_MODE, channel.code());
    }

    /** Returns the gain modes that a channel supports. */
    public List<GainModeInfo> gainModes(Channel channel) throws BladeRfException {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment pointer = arena.allocate(ADDRESS);
            int count = readTable("bladerf_get_gain_modes", Native.GET_GAIN_MODES, pointer, channel.code());
            long size = Layouts.GAIN_MODES.byteSize();
            MemorySegment table = pointer.get(ADDRESS, 0).reinterpret(count * size);
            List<GainModeInfo> modes = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                MemorySegment entry = table.asSlice(i * size, size);
                modes.add(new GainModeInfo(Native.cString(Layouts.pointer(entry, Layouts.GAIN_MODES, "name")),
                    Coded.of(GainMode.class, Layouts.i32(entry, Layouts.GAIN_MODES, "mode"))));
            }
            return List.copyOf(modes);
        }
    }

    /** Returns the names of a channel's gain stages. */
    public List<String> gainStages(Channel channel) throws BladeRfException {
        return names("bladerf_get_gain_stages", Native.GET_GAIN_STAGES, channel, true);
    }

    /** Sets a gain stage from {@link #gainStages} in dB. */
    public void setGainStage(Channel channel, String stage, int gain) throws BladeRfException {
        try (Arena arena = Arena.ofConfined()) {
            call("bladerf_set_gain_stage", Native.SET_GAIN_STAGE, channel.code(), arena.allocateFrom(stage), gain);
        }
    }

    /** Returns a gain stage's gain in dB. */
    public int gainStage(Channel channel, String stage) throws BladeRfException {
        try (Arena arena = Arena.ofConfined()) {
            return readInt("bladerf_get_gain_stage", Native.GET_GAIN_STAGE, channel.code(), arena.allocateFrom(stage));
        }
    }

    /** Returns the gains that a gain stage supports. */
    public Range gainStageRange(Channel channel, String stage) throws BladeRfException {
        try (Arena arena = Arena.ofConfined()) {
            return readRange("bladerf_get_gain_stage_range", Native.GET_GAIN_STAGE_RANGE, channel.code(),
                arena.allocateFrom(stage));
        }
    }

    /** Returns the names of a channel's RF ports. */
    public List<String> rfPorts(Channel channel) throws BladeRfException {
        return names("bladerf_get_rf_ports", Native.GET_RF_PORTS, channel, false);
    }

    /** Selects an RF port from {@link #rfPorts}. */
    public void setRfPort(Channel channel, String port) throws BladeRfException {
        try (Arena arena = Arena.ofConfined()) {
            call("bladerf_set_rf_port", Native.SET_RF_PORT, channel.code(), arena.allocateFrom(port));
        }
    }

    /** Sets an I/Q correction. */
    public void setCorrection(Channel channel, Correction correction, short value) throws BladeRfException {
        call("bladerf_set_correction", Native.SET_CORRECTION, channel.code(), correction.code(), value);
    }

    /** Returns an I/Q correction. */
    public short correction(Channel channel, Correction correction) throws BladeRfException {
        return read("bladerf_get_correction", Native.GET_CORRECTION, JAVA_SHORT, s -> s.get(JAVA_SHORT, 0),
            channel.code(), correction.code());
    }

    /** Returns the loopback modes that this device supports. */
    public List<LoopbackInfo> loopbackModes() throws BladeRfException {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment pointer = arena.allocate(ADDRESS);
            int count = readTable("bladerf_get_loopback_modes", Native.GET_LOOPBACK_MODES, pointer);
            long size = Layouts.LOOPBACK_MODES.byteSize();
            MemorySegment table = pointer.get(ADDRESS, 0).reinterpret(count * size);
            List<LoopbackInfo> modes = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                MemorySegment entry = table.asSlice(i * size, size);
                modes.add(new LoopbackInfo(Native.cString(Layouts.pointer(entry, Layouts.LOOPBACK_MODES, "name")),
                    Coded.of(Loopback.class, Layouts.i32(entry, Layouts.LOOPBACK_MODES, "mode"))));
            }
            return List.copyOf(modes);
        }
    }

    /** Returns true if this device supports a loopback mode. */
    public boolean supports(Loopback mode) {
        return Native.invokeBoolean(Native.IS_LOOPBACK_MODE_SUPPORTED, handle(), mode.code());
    }

    /** Enables or disables a feature. */
    public void enableFeature(Feature feature, boolean enabled) throws BladeRfException {
        call("bladerf_enable_feature", Native.ENABLE_FEATURE, feature.code(), enabled);
    }

    /** Returns a channel's RF port. */
    public String rfPort(Channel channel) throws BladeRfException {
        return readString("bladerf_get_rf_port", Native.GET_RF_PORT, channel.code());
    }

    /** Selects a loopback mode. */
    public void setLoopback(Loopback mode) throws BladeRfException {
        call("bladerf_set_loopback", Native.SET_LOOPBACK, mode.code());
    }

    /** Returns the loopback mode. */
    public Loopback loopback() throws BladeRfException {
        return readEnum(Loopback.class, "bladerf_get_loopback", Native.GET_LOOPBACK);
    }

    /** Selects where received samples come from. */
    public void setRxMux(RxMux mux) throws BladeRfException {
        call("bladerf_set_rx_mux", Native.SET_RX_MUX, mux.code());
    }

    /** Returns where received samples come from. */
    public RxMux rxMux() throws BladeRfException {
        return readEnum(RxMux.class, "bladerf_get_rx_mux", Native.GET_RX_MUX);
    }

    /** Returns the enabled feature. */
    public Feature feature() throws BladeRfException {
        return readEnum(Feature.class, "bladerf_get_feature", Native.GET_FEATURE);
    }

    // ---- Gain calibration ----

    /** Loads a bladeRF 2 gain calibration table from a file. */
    public void loadGainCalibration(Channel channel, Path file) throws BladeRfException {
        try (Arena arena = Arena.ofConfined()) {
            call("bladerf_load_gain_calibration", Native.LOAD_GAIN_CALIBRATION, channel.code(),
                arena.allocateFrom(file.toString()));
        }
    }

    /** Prints a channel's gain calibration to stdout. */
    public void printGainCalibration(Channel channel, boolean withEntries) throws BladeRfException {
        call("bladerf_print_gain_calibration", Native.PRINT_GAIN_CALIBRATION, channel.code(), withEntries);
    }

    /** Enables or disables a channel's gain calibration. */
    public void enableGainCalibration(Channel channel, boolean enabled) throws BladeRfException {
        call("bladerf_enable_gain_calibration", Native.ENABLE_GAIN_CALIBRATION, channel.code(), enabled);
    }

    /** Returns a copy of a channel's gain calibration table. */
    public GainCalibration gainCalibration(Channel channel) throws BladeRfException {
        return read("bladerf_get_gain_calibration", Native.GET_GAIN_CALIBRATION, ADDRESS,
            s -> GainCalibration.read(s.get(ADDRESS, 0).reinterpret(Layouts.GAIN_CAL_TBL.byteSize())),
            channel.code());
    }

    /** Returns the gain that a channel's calibration compensates towards. */
    public int gainTarget(Channel channel) throws BladeRfException {
        return readInt("bladerf_get_gain_target", Native.GET_GAIN_TARGET, channel.code());
    }

    // ---- Synchronous streaming ----

    /** Enables or disables a channel. */
    public void enableChannel(Channel channel, boolean enabled) throws BladeRfException {
        call("bladerf_enable_module", Native.ENABLE_MODULE, channel.code(), enabled);
    }

    /**
     * Configures the synchronous interface for a direction.
     *
     * @param bufferCount number of buffers, more than {@code transferCount}
     * @param bufferSize samples per buffer, a multiple of 1024
     * @param transferCount number of buffers that may be in flight at once
     * @param timeoutMs timeout of the underlying stream in milliseconds
     */
    public void syncConfig(ChannelLayout layout, SampleFormat format, int bufferCount, int bufferSize,
            int transferCount, int timeoutMs) throws BladeRfException {
        call("bladerf_sync_config", Native.SYNC_CONFIG, layout.code(), format.code(), bufferCount, bufferSize,
            transferCount, timeoutMs);
        if (BladeRfStream.channels(layout).getFirst().direction() == Direction.RX) {
            rxFormat = format;
        } else {
            txFormat = format;
        }
    }

    /**
     * Receives samples, blocking until they have arrived. This is the method to
     * use for a sustained stream: it does not allocate buffers.
     *
     * @param samples native memory to fill, in native byte order
     * @param sampleCount number of samples to receive; for two channels, the total
     * @param metadata timing to use and status to update, or null
     * @param timeoutMs timeout in milliseconds, or 0 for the stream's timeout
     */
    public void syncRx(MemorySegment samples, int sampleCount, Metadata metadata, int timeoutMs)
            throws BladeRfException {
        sync(true, rxFormat, samples, sampleCount, metadata, timeoutMs);
    }

    /**
     * Transmits samples, blocking until they have been queued.
     *
     * @param samples native memory holding samples, in native byte order
     * @param sampleCount number of samples to send; for two channels, the total
     * @param metadata timing to use and status to update, or null
     * @param timeoutMs timeout in milliseconds, or 0 for the stream's timeout
     */
    public void syncTx(MemorySegment samples, int sampleCount, Metadata metadata, int timeoutMs)
            throws BladeRfException {
        sync(false, txFormat, samples, sampleCount, metadata, timeoutMs);
    }

    /**
     * Receives 16-bit samples into an array, with I and Q interleaved. This copies
     * through native memory; prefer {@link #syncRx(MemorySegment, int, Metadata, int)}
     * for sustained streaming.
     */
    public void syncRx(short[] samples, int sampleCount, Metadata metadata, int timeoutMs) throws BladeRfException {
        long bytes = shortLength(rxFormat, samples, sampleCount);
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment memory = arena.allocate(bytes, JAVA_SHORT.byteAlignment());
            syncRx(memory, sampleCount, metadata, timeoutMs);
            MemorySegment.copy(memory, JAVA_SHORT, 0, samples, 0, sampleCount * 2);
        }
    }

    /**
     * Transmits 16-bit samples from an array, with I and Q interleaved. This copies
     * through native memory; prefer {@link #syncTx(MemorySegment, int, Metadata, int)}
     * for sustained streaming.
     */
    public void syncTx(short[] samples, int sampleCount, Metadata metadata, int timeoutMs) throws BladeRfException {
        long bytes = shortLength(txFormat, samples, sampleCount);
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment memory = arena.allocate(bytes, JAVA_SHORT.byteAlignment());
            MemorySegment.copy(samples, 0, memory, JAVA_SHORT, 0, sampleCount * 2);
            syncTx(memory, sampleCount, metadata, timeoutMs);
        }
    }

    private static long shortLength(SampleFormat format, short[] samples, int sampleCount) {
        if (format == null) {
            throw new IllegalStateException("syncConfig has not been called for this direction");
        }
        if (format.bytesPerSample() != 2 * Short.BYTES) {
            throw new IllegalArgumentException(format + " samples do not fit in a short array");
        }
        if (sampleCount < 0 || sampleCount * 2L > samples.length) {
            throw new IllegalArgumentException(sampleCount + " samples do not fit in an array of " + samples.length);
        }
        return format.byteLength(sampleCount);
    }

    private void sync(boolean receive, SampleFormat format, MemorySegment samples, int sampleCount, Metadata metadata,
            int timeoutMs) throws BladeRfException {
        if (format == null) {
            throw new IllegalStateException("syncConfig has not been called for this direction");
        }
        BladeRf.checkLength(samples, format, sampleCount);
        MemorySegment device = handle();
        if (metadata == null) {
            check(receive ? "bladerf_sync_rx" : "bladerf_sync_tx", receive
                ? Native.syncRx(device, samples, sampleCount, MemorySegment.NULL, timeoutMs)
                : Native.syncTx(device, samples, sampleCount, MemorySegment.NULL, timeoutMs));
            return;
        }
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment nativeMetadata = arena.allocate(Layouts.METADATA);
            metadata.write(nativeMetadata);
            check(receive ? "bladerf_sync_rx" : "bladerf_sync_tx", receive
                ? Native.syncRx(device, samples, sampleCount, nativeMetadata, timeoutMs)
                : Native.syncTx(device, samples, sampleCount, nativeMetadata, timeoutMs));
            metadata.read(nativeMetadata);
        }
    }

    /** Sets the timeout of a direction's synchronous stream in milliseconds. */
    public void setStreamTimeout(Direction direction, long milliseconds) throws BladeRfException {
        call("bladerf_set_stream_timeout", Native.SET_STREAM_TIMEOUT, direction.code(), u32(milliseconds));
    }

    /** Returns the timeout of a direction's synchronous stream in milliseconds. */
    public long streamTimeout(Direction direction) throws BladeRfException {
        return readUnsigned("bladerf_get_stream_timeout", Native.GET_STREAM_TIMEOUT, direction.code());
    }

    /** Returns the current sample time of a direction. */
    public long timestamp(Direction direction) throws BladeRfException {
        return readLong("bladerf_get_timestamp", Native.GET_TIMESTAMP, direction.code());
    }

    // ---- Asynchronous streaming ----

    /**
     * Enables the channels of a layout and starts delivering their samples to a
     * listener. The channels are disabled when the stream ends.
     *
     * @param layout {@link ChannelLayout#RX_X1} or {@link ChannelLayout#RX_X2}
     * @return the running stream
     * @throws IllegalStateException if a receive stream is already running
     */
    public BladeRfStream startRx(ChannelLayout layout, SampleFormat format, StreamConfig config,
            SampleListener listener) throws BladeRfException {
        Objects.requireNonNull(listener, "listener");
        checkDirection(layout, Direction.RX);
        synchronized (lifecycleLock) {
            if (rxStream != null && rxStream.isRunning()) {
                throw new IllegalStateException("A receive stream is already running");
            }
            rxStream = BladeRfStream.start(this, handle(), layout, format, config, listener, null);
            return rxStream;
        }
    }

    /**
     * Enables the channels of a layout and starts transmitting the samples of a
     * producer. The channels are disabled when the stream ends.
     *
     * @param layout {@link ChannelLayout#TX_X1} or {@link ChannelLayout#TX_X2}
     * @param format any format except {@link SampleFormat#PACKET_META}, whose
     *     buffers have sizes that a {@link SampleProducer} cannot report
     * @return the running stream
     * @throws IllegalStateException if a transmit stream is already running
     */
    public BladeRfStream startTx(ChannelLayout layout, SampleFormat format, StreamConfig config,
            SampleProducer producer) throws BladeRfException {
        Objects.requireNonNull(producer, "producer");
        checkDirection(layout, Direction.TX);
        if (format == SampleFormat.PACKET_META) {
            throw new IllegalArgumentException("PACKET_META is not supported for transmit streams");
        }
        synchronized (lifecycleLock) {
            if (txStream != null && txStream.isRunning()) {
                throw new IllegalStateException("A transmit stream is already running");
            }
            txStream = BladeRfStream.start(this, handle(), layout, format, config, null, producer);
            return txStream;
        }
    }

    private static void checkDirection(ChannelLayout layout, Direction direction) {
        if (BladeRfStream.channels(layout).getFirst().direction() != direction) {
            throw new IllegalArgumentException(layout + " is not a " + direction + " layout");
        }
    }

    // ---- Triggers ----

    /** Returns a disabled trigger for a channel and signal; see {@link Trigger#withRole}. */
    public Trigger initTrigger(Channel channel, TriggerSignal signal) throws BladeRfException {
        return read("bladerf_trigger_init", Native.TRIGGER_INIT, Layouts.TRIGGER, Trigger::read, channel.code(),
            signal.code());
    }

    private static MemorySegment trigger(Trigger trigger, Arena arena) {
        MemorySegment segment = arena.allocate(Layouts.TRIGGER);
        trigger.write(segment);
        return segment;
    }

    /** Arms or disarms a trigger. */
    public void armTrigger(Trigger trigger, boolean arm) throws BladeRfException {
        try (Arena arena = Arena.ofConfined()) {
            call("bladerf_trigger_arm", Native.TRIGGER_ARM, trigger(trigger, arena), arm, 0L, 0L);
        }
    }

    /** Fires a master trigger. */
    public void fireTrigger(Trigger trigger) throws BladeRfException {
        try (Arena arena = Arena.ofConfined()) {
            call("bladerf_trigger_fire", Native.TRIGGER_FIRE, trigger(trigger, arena));
        }
    }

    /** Returns the state of a trigger. */
    public TriggerState triggerState(Trigger trigger) throws BladeRfException {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment armed = arena.allocate(JAVA_BOOLEAN);
            MemorySegment fired = arena.allocate(JAVA_BOOLEAN);
            MemorySegment requested = arena.allocate(JAVA_BOOLEAN);
            call("bladerf_trigger_state", Native.TRIGGER_STATE, trigger(trigger, arena), armed, fired, requested,
                MemorySegment.NULL, MemorySegment.NULL);
            return new TriggerState(armed.get(JAVA_BOOLEAN, 0), fired.get(JAVA_BOOLEAN, 0),
                requested.get(JAVA_BOOLEAN, 0));
        }
    }

    /** Reads a channel's trigger control register. */
    public int readTrigger(Channel channel, TriggerSignal signal) throws BladeRfException {
        return readU8("bladerf_read_trigger", Native.READ_TRIGGER, channel.code(), signal.code());
    }

    /** Writes a channel's trigger control register. */
    public void writeTrigger(Channel channel, TriggerSignal signal, int value) throws BladeRfException {
        call("bladerf_write_trigger", Native.WRITE_TRIGGER, channel.code(), signal.code(), u8(value));
    }

    // ---- Firmware, FPGA and flash ----

    /** Writes firmware to flash. It runs after the device is power cycled or reset. */
    public void flashFirmware(Path file) throws BladeRfException {
        file("bladerf_flash_firmware", Native.FLASH_FIRMWARE, file);
    }

    /** Loads an FPGA bitstream, which lasts until the device loses power. */
    public void loadFpga(Path file) throws BladeRfException {
        file("bladerf_load_fpga", Native.LOAD_FPGA, file);
    }

    /** Writes an FPGA bitstream to flash, so that the device loads it at power on. */
    public void flashFpga(Path file) throws BladeRfException {
        file("bladerf_flash_fpga", Native.FLASH_FPGA, file);
    }

    /**
     * Saves the firmware log to a file.
     *
     * @param file file to write, or null to print to stdout
     */
    public void saveFirmwareLog(Path file) throws BladeRfException {
        try (Arena arena = Arena.ofConfined()) {
            String name = file == null ? null : file.toString();
            call("bladerf_get_fw_log", Native.GET_FW_LOG, BladeRf.cString(name, arena));
        }
    }

    private void file(String function, MethodHandle method, Path file) throws BladeRfException {
        try (Arena arena = Arena.ofConfined()) {
            call(function, method, arena.allocateFrom(file.toString()));
        }
    }

    /** Erases the FPGA bitstream stored in flash. */
    public void eraseStoredFpga() throws BladeRfException {
        call("bladerf_erase_stored_fpga", Native.ERASE_STORED_FPGA);
    }

    /** Resets the device. It has to be opened again afterwards. */
    public void reset() throws BladeRfException {
        call("bladerf_device_reset", Native.DEVICE_RESET);
    }

    /** Makes the device run its bootloader. It has to be found again afterwards. */
    public void jumpToBootloader() throws BladeRfException {
        call("bladerf_jump_to_bootloader", Native.JUMP_TO_BOOTLOADER);
    }

    /** Erases flash blocks. */
    public void eraseFlash(long block, long count) throws BladeRfException {
        call("bladerf_erase_flash", Native.ERASE_FLASH, u32(block), u32(count));
    }

    /** Erases flash by address and length, which have to be aligned to erase blocks. */
    public void eraseFlashBytes(long address, long length) throws BladeRfException {
        call("bladerf_erase_flash_bytes", Native.ERASE_FLASH_BYTES, u32(address), u32(length));
    }

    /** Reads flash, with an address and length that are aligned to flash pages. */
    public byte[] readFlashBytes(long address, int length) throws BladeRfException {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment data = arena.allocate(length);
            call("bladerf_read_flash_bytes", Native.READ_FLASH_BYTES, data, u32(address), length);
            return data.toArray(JAVA_BYTE);
        }
    }

    /** Writes flash, with an address and length that are aligned to flash pages. */
    public void writeFlashBytes(long address, byte[] data) throws BladeRfException {
        try (Arena arena = Arena.ofConfined()) {
            call("bladerf_write_flash_bytes", Native.WRITE_FLASH_BYTES, bytes(data, arena), u32(address),
                data.length);
        }
    }

    /** Reads the first page of the one-time-programmable area of the flash. */
    public byte[] readOtp() throws BladeRfException {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment data = arena.allocate(Layouts.OTP_LENGTH);
            call("bladerf_read_otp", Native.READ_OTP, data);
            return data.toArray(JAVA_BYTE);
        }
    }

    /** Writes the first page of the one-time-programmable area. This cannot be undone. */
    public void writeOtp(byte[] data) throws BladeRfException {
        if (data.length != Layouts.OTP_LENGTH) {
            throw new IllegalArgumentException("OTP data has to be " + Layouts.OTP_LENGTH + " bytes");
        }
        try (Arena arena = Arena.ofConfined()) {
            call("bladerf_write_otp", Native.WRITE_OTP, bytes(data, arena));
        }
    }

    /** Locks the one-time-programmable area against writing. This cannot be undone. */
    public void lockOtp() throws BladeRfException {
        call("bladerf_lock_otp", Native.LOCK_OTP);
    }

    /** Allocates a flash image. */
    public BladeRfImage allocateImage(ImageType type, long address, long length) throws BladeRfException {
        return image("bladerf_alloc_image", Native.invokePointer(Native.ALLOC_IMAGE, handle(), type.code(),
            u32(address), u32(length)));
    }

    /** Allocates a calibration image holding an FPGA size and VCTCXO trim. */
    public BladeRfImage allocateCalibrationImage(FpgaSize fpgaSize, int vctcxoTrim) throws BladeRfException {
        return image("bladerf_alloc_cal_image", Native.invokePointer(Native.ALLOC_CAL_IMAGE, handle(), fpgaSize.code(),
            u16(vctcxoTrim)));
    }

    private static BladeRfImage image(String function, MemorySegment image) throws BladeRfException {
        if (image.equals(MemorySegment.NULL)) {
            throw new BladeRfException(function, BladeRfError.MEM.code());
        }
        return new BladeRfImage(image);
    }

    /** Writes an image to a file. */
    public void writeImage(BladeRfImage image, Path file) throws BladeRfException {
        try (Arena arena = Arena.ofConfined()) {
            call("bladerf_image_write", Native.IMAGE_WRITE, image.handle(), arena.allocateFrom(file.toString()));
        }
    }

    // ---- Clocks and low-level access ----

    /** Selects what disciplines the VCTCXO. */
    public void setVctcxoTamerMode(VctcxoTamerMode mode) throws BladeRfException {
        call("bladerf_set_vctcxo_tamer_mode", Native.SET_VCTCXO_TAMER_MODE, mode.code());
    }

    /** Returns what disciplines the VCTCXO. */
    public VctcxoTamerMode vctcxoTamerMode() throws BladeRfException {
        return readEnum(VctcxoTamerMode.class, "bladerf_get_vctcxo_tamer_mode", Native.GET_VCTCXO_TAMER_MODE);
    }

    /** Returns the VCTCXO trim stored in flash. */
    public int vctcxoTrim() throws BladeRfException {
        return readU16("bladerf_get_vctcxo_trim", Native.GET_VCTCXO_TRIM);
    }

    /** Writes the VCTCXO trim DAC. */
    public void writeTrimDac(int value) throws BladeRfException {
        call("bladerf_trim_dac_write", Native.TRIM_DAC_WRITE, u16(value));
    }

    /** Reads the VCTCXO trim DAC. */
    public int readTrimDac() throws BladeRfException {
        return readU16("bladerf_trim_dac_read", Native.TRIM_DAC_READ);
    }

    /** Reads the FPGA configuration GPIO register. */
    public long readConfigGpio() throws BladeRfException {
        return readUnsigned("bladerf_config_gpio_read", Native.CONFIG_GPIO_READ);
    }

    /** Writes the FPGA configuration GPIO register. */
    public void writeConfigGpio(long value) throws BladeRfException {
        call("bladerf_config_gpio_write", Native.CONFIG_GPIO_WRITE, u32(value));
    }

    /** Reads a Wishbone master register of the FPGA. */
    public long readWishbone(long address) throws BladeRfException {
        return readUnsigned("bladerf_wishbone_master_read", Native.WISHBONE_MASTER_READ, u32(address));
    }

    /** Writes a Wishbone master register of the FPGA. */
    public void writeWishbone(long address, long value) throws BladeRfException {
        call("bladerf_wishbone_master_write", Native.WISHBONE_MASTER_WRITE, u32(address), u32(value));
    }

    // ---- Expansion boards ----

    /** Attaches an expansion board. This cannot be undone before the device is closed. */
    public void attachExpansionBoard(ExpansionBoard board) throws BladeRfException {
        call("bladerf_expansion_attach", Native.EXPANSION_ATTACH, board.code());
    }

    /** Returns the attached expansion board. */
    public ExpansionBoard expansionBoard() throws BladeRfException {
        return readEnum(ExpansionBoard.class, "bladerf_expansion_get_attached", Native.EXPANSION_GET_ATTACHED);
    }

    /** Reads the expansion GPIO pins. */
    public long readExpansionGpio() throws BladeRfException {
        return readUnsigned("bladerf_expansion_gpio_read", Native.EXPANSION_GPIO_READ);
    }

    /** Writes the expansion GPIO pins. */
    public void writeExpansionGpio(long value) throws BladeRfException {
        call("bladerf_expansion_gpio_write", Native.EXPANSION_GPIO_WRITE, u32(value));
    }

    /** Writes the expansion GPIO pins selected by a mask. */
    public void writeExpansionGpio(long mask, long value) throws BladeRfException {
        call("bladerf_expansion_gpio_masked_write", Native.EXPANSION_GPIO_MASKED_WRITE, u32(mask), u32(value));
    }

    /** Reads the expansion GPIO directions, where a set bit is an output. */
    public long readExpansionGpioDirection() throws BladeRfException {
        return readUnsigned("bladerf_expansion_gpio_dir_read", Native.EXPANSION_GPIO_DIR_READ);
    }

    /** Writes the expansion GPIO directions, where a set bit is an output. */
    public void writeExpansionGpioDirection(long outputs) throws BladeRfException {
        call("bladerf_expansion_gpio_dir_write", Native.EXPANSION_GPIO_DIR_WRITE, u32(outputs));
    }

    /** Writes the expansion GPIO directions selected by a mask. */
    public void writeExpansionGpioDirection(long mask, long outputs) throws BladeRfException {
        call("bladerf_expansion_gpio_dir_masked_write",
            Native.EXPANSION_GPIO_DIR_MASKED_WRITE, u32(mask), u32(outputs));
    }

    /** Selects an XB-200 filter bank. */
    public void setXb200FilterBank(Channel channel, Xb200Filter filter) throws BladeRfException {
        call("bladerf_xb200_set_filterbank", Native.XB200_SET_FILTERBANK, channel.code(), filter.code());
    }

    /** Returns the XB-200 filter bank. */
    public Xb200Filter xb200FilterBank(Channel channel) throws BladeRfException {
        return readEnum(Xb200Filter.class, "bladerf_xb200_get_filterbank", Native.XB200_GET_FILTERBANK, channel.code());
    }

    /** Selects the XB-200 signal path. */
    public void setXb200Path(Channel channel, Xb200Path path) throws BladeRfException {
        call("bladerf_xb200_set_path", Native.XB200_SET_PATH, channel.code(), path.code());
    }

    /** Returns the XB-200 signal path. */
    public Xb200Path xb200Path(Channel channel) throws BladeRfException {
        return readEnum(Xb200Path.class, "bladerf_xb200_get_path", Native.XB200_GET_PATH, channel.code());
    }

    /** Selects the XB-300 TRX antenna mode. */
    public void setXb300Trx(Xb300Trx trx) throws BladeRfException {
        call("bladerf_xb300_set_trx", Native.XB300_SET_TRX, trx.code());
    }

    /** Returns the XB-300 TRX antenna mode. */
    public Xb300Trx xb300Trx() throws BladeRfException {
        return readEnum(Xb300Trx.class, "bladerf_xb300_get_trx", Native.XB300_GET_TRX);
    }

    /** Enables or disables an XB-300 amplifier. */
    public void setXb300AmplifierEnabled(Xb300Amplifier amplifier, boolean enabled) throws BladeRfException {
        call("bladerf_xb300_set_amplifier_enable", Native.XB300_SET_AMPLIFIER_ENABLE, amplifier.code(), enabled);
    }

    /** Returns true if an XB-300 amplifier is enabled. */
    public boolean isXb300AmplifierEnabled(Xb300Amplifier amplifier) throws BladeRfException {
        return readBoolean("bladerf_xb300_get_amplifier_enable", Native.XB300_GET_AMPLIFIER_ENABLE, amplifier.code());
    }

    /** Returns the XB-300 output power in dBm. */
    public float xb300OutputPower() throws BladeRfException {
        return readFloat("bladerf_xb300_get_output_power", Native.XB300_GET_OUTPUT_POWER);
    }

    /** Writes a 32-bit word over the SPI bus of an expansion board. */
    public void writeExpansionSpi(long value) throws BladeRfException {
        call("bladerf_xb_spi_write", Native.XB_SPI_WRITE, u32(value));
    }

    // ---- bladeRF 1 ----

    /** Selects the ADC sampling input. */
    public void setSampling(Sampling sampling) throws BladeRfException {
        call("bladerf_set_sampling", Native.SET_SAMPLING, sampling.code());
    }

    /** Returns the ADC sampling input. */
    public Sampling sampling() throws BladeRfException {
        return readEnum(Sampling.class, "bladerf_get_sampling", Native.GET_SAMPLING);
    }

    /** Selects a channel's low-pass filter mode. */
    public void setLpfMode(Channel channel, LpfMode mode) throws BladeRfException {
        call("bladerf_set_lpf_mode", Native.SET_LPF_MODE, channel.code(), mode.code());
    }

    /** Returns a channel's low-pass filter mode. */
    public LpfMode lpfMode(Channel channel) throws BladeRfException {
        return readEnum(LpfMode.class, "bladerf_get_lpf_mode", Native.GET_LPF_MODE, channel.code());
    }

    /** Selects the mode of the SMB clock port. */
    public void setSmbMode(SmbMode mode) throws BladeRfException {
        call("bladerf_set_smb_mode", Native.SET_SMB_MODE, mode.code());
    }

    /** Returns the mode of the SMB clock port. */
    public SmbMode smbMode() throws BladeRfException {
        return readEnum(SmbMode.class, "bladerf_get_smb_mode", Native.GET_SMB_MODE);
    }

    /**
     * Sets the SMB clock port's frequency.
     *
     * @param hertz requested frequency in Hz
     * @return the frequency that was set, in Hz
     */
    public long setSmbFrequency(long hertz) throws BladeRfException {
        return readUnsigned("bladerf_set_smb_frequency", Native.SET_SMB_FREQUENCY, u32(hertz));
    }

    /**
     * Sets the SMB clock port's frequency exactly.
     *
     * @return the frequency that was set
     */
    public RationalRate setSmbFrequency(RationalRate rate) throws BladeRfException {
        return rational("bladerf_set_rational_smb_frequency", Native.SET_RATIONAL_SMB_FREQUENCY, rate);
    }

    /** Returns the SMB clock port's exact frequency. */
    public RationalRate rationalSmbFrequency() throws BladeRfException {
        return read("bladerf_get_rational_smb_frequency", Native.GET_RATIONAL_SMB_FREQUENCY, Layouts.RATIONAL_RATE,
            RationalRate::read);
    }

    /** Applies LMS6002D DC calibration values. */
    public void setLmsDcCals(LmsDcCals cals) throws BladeRfException {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment segment = arena.allocate(Layouts.LMS_DC_CALS);
            cals.write(segment);
            call("bladerf_lms_set_dc_cals", Native.LMS_SET_DC_CALS, segment);
        }
    }

    /** Returns the LMS6002D DC calibration values. */
    public LmsDcCals lmsDcCals() throws BladeRfException {
        return read("bladerf_lms_get_dc_cals", Native.LMS_GET_DC_CALS, Layouts.LMS_DC_CALS, LmsDcCals::read);
    }

    /** Returns the SMB clock port's frequency in Hz. */
    public long smbFrequency() throws BladeRfException {
        return readUnsigned("bladerf_get_smb_frequency", Native.GET_SMB_FREQUENCY);
    }

    /** Runs DC offset calibration of an LMS6002D module. */
    public void calibrateDc(DcCalModule module) throws BladeRfException {
        call("bladerf_calibrate_dc", Native.CALIBRATE_DC, module.code());
    }

    /** Reads a Si5338 clock generator register. */
    public int readSi5338(int address) throws BladeRfException {
        return readU8("bladerf_si5338_read", Native.SI5338_READ, u8(address));
    }

    /** Writes a Si5338 clock generator register. */
    public void writeSi5338(int address, int value) throws BladeRfException {
        call("bladerf_si5338_write", Native.SI5338_WRITE, u8(address), u8(value));
    }

    /** Reads an LMS6002D transceiver register. */
    public int readLms(int address) throws BladeRfException {
        return readU8("bladerf_lms_read", Native.LMS_READ, u8(address));
    }

    /** Writes an LMS6002D transceiver register. */
    public void writeLms(int address, int value) throws BladeRfException {
        call("bladerf_lms_write", Native.LMS_WRITE, u8(address), u8(value));
    }

    // ---- bladeRF 2 ----

    /**
     * Switches a channel's bias tee. This puts DC power on the antenna connector,
     * which can damage equipment not built for it.
     */
    public void setBiasTee(Channel channel, boolean enabled) throws BladeRfException {
        call("bladerf_set_bias_tee", Native.SET_BIAS_TEE, channel.code(), enabled);
    }

    /** Reads the received signal strength of a channel. */
    public Rssi rfRssi(Channel channel) throws BladeRfException {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment preamble = arena.allocate(JAVA_INT);
            MemorySegment symbol = arena.allocate(JAVA_INT);
            call("bladerf_get_rfic_rssi", Native.GET_RFIC_RSSI, channel.code(), preamble, symbol);
            return new Rssi(preamble.get(JAVA_INT, 0), symbol.get(JAVA_INT, 0));
        }
    }

    /** Reads a power monitor register. */
    public double pmicRegister(PmicRegister register) throws BladeRfException {
        if (register.floatingPoint()) {
            return readFloat("bladerf_get_pmic_register", Native.GET_PMIC_REGISTER, register.code());
        }
        return readU16("bladerf_get_pmic_register", Native.GET_PMIC_REGISTER, register.code());
    }

    /** Returns the settings of the RF switches. */
    public RfSwitchConfig rfSwitchConfig() throws BladeRfException {
        return read("bladerf_get_rf_switch_config", Native.GET_RF_SWITCH_CONFIG, Layouts.RF_SWITCH_CONFIG,
            RfSwitchConfig::read);
    }

    /** Returns true if a channel's bias tee is on. */
    public boolean biasTee(Channel channel) throws BladeRfException {
        return readBoolean("bladerf_get_bias_tee", Native.GET_BIAS_TEE, channel.code());
    }

    /** Reads an RFIC register. */
    public int readRfic(int address) throws BladeRfException {
        return readU8("bladerf_get_rfic_register", Native.GET_RFIC_REGISTER, u16(address));
    }

    /** Writes an RFIC register. */
    public void writeRfic(int address, int value) throws BladeRfException {
        call("bladerf_set_rfic_register", Native.SET_RFIC_REGISTER, u16(address), u8(value));
    }

    /** Returns the RFIC temperature in degrees Celsius. */
    public float rfTemperature() throws BladeRfException {
        return readFloat("bladerf_get_rfic_temperature", Native.GET_RFIC_TEMPERATURE);
    }

    /** Reads the RFIC control output pins. */
    public int rfControlOutput() throws BladeRfException {
        return readU8("bladerf_get_rfic_ctrl_out", Native.GET_RFIC_CTRL_OUT);
    }

    /** Returns the RFIC receive FIR filter. */
    public RxFir rxFir() throws BladeRfException {
        return readEnum(RxFir.class, "bladerf_get_rfic_rx_fir", Native.GET_RFIC_RX_FIR);
    }

    /** Selects the RFIC receive FIR filter. */
    public void setRxFir(RxFir fir) throws BladeRfException {
        call("bladerf_set_rfic_rx_fir", Native.SET_RFIC_RX_FIR, fir.code());
    }

    /** Returns the RFIC transmit FIR filter. */
    public TxFir txFir() throws BladeRfException {
        return readEnum(TxFir.class, "bladerf_get_rfic_tx_fir", Native.GET_RFIC_TX_FIR);
    }

    /** Selects the RFIC transmit FIR filter. */
    public void setTxFir(TxFir fir) throws BladeRfException {
        call("bladerf_set_rfic_tx_fir", Native.SET_RFIC_TX_FIR, fir.code());
    }

    /** Returns true if the clock PLL is locked. */
    public boolean isPllLocked() throws BladeRfException {
        return readBoolean("bladerf_get_pll_lock_state", Native.GET_PLL_LOCK_STATE);
    }

    /** Returns true if the clock PLL is enabled. */
    public boolean isPllEnabled() throws BladeRfException {
        return readBoolean("bladerf_get_pll_enable", Native.GET_PLL_ENABLE);
    }

    /** Enables or disables the clock PLL. */
    public void setPllEnabled(boolean enabled) throws BladeRfException {
        call("bladerf_set_pll_enable", Native.SET_PLL_ENABLE, enabled);
    }

    /** Returns the PLL reference frequencies the device supports, in Hz. */
    public Range pllReferenceRange() throws BladeRfException {
        return readRange("bladerf_get_pll_refclk_range", Native.GET_PLL_REFCLK_RANGE);
    }

    /** Returns the PLL reference frequency in Hz. */
    public long pllReference() throws BladeRfException {
        return readLong("bladerf_get_pll_refclk", Native.GET_PLL_REFCLK);
    }

    /** Sets the PLL reference frequency in Hz. */
    public void setPllReference(long hertz) throws BladeRfException {
        call("bladerf_set_pll_refclk", Native.SET_PLL_REFCLK, hertz);
    }

    /** Reads a PLL register. */
    public long readPll(int address) throws BladeRfException {
        return readUnsigned("bladerf_get_pll_register", Native.GET_PLL_REGISTER, u8(address));
    }

    /** Writes a PLL register. */
    public void writePll(int address, long value) throws BladeRfException {
        call("bladerf_set_pll_register", Native.SET_PLL_REGISTER, u8(address), u32(value));
    }

    /** Returns what is powering the board. */
    public PowerSource powerSource() throws BladeRfException {
        return readEnum(PowerSource.class, "bladerf_get_power_source", Native.GET_POWER_SOURCE);
    }

    /** Returns the reference clock source. */
    public ClockSelect clockSelect() throws BladeRfException {
        return readEnum(ClockSelect.class, "bladerf_get_clock_select", Native.GET_CLOCK_SELECT);
    }

    /** Selects the reference clock source. */
    public void setClockSelect(ClockSelect source) throws BladeRfException {
        call("bladerf_set_clock_select", Native.SET_CLOCK_SELECT, source.code());
    }

    /** Returns true if the clock output is on. */
    public boolean clockOutput() throws BladeRfException {
        return readBoolean("bladerf_get_clock_output", Native.GET_CLOCK_OUTPUT);
    }

    /** Switches the clock output. */
    public void setClockOutput(boolean enabled) throws BladeRfException {
        call("bladerf_set_clock_output", Native.SET_CLOCK_OUTPUT, enabled);
    }

    // ---- Lifecycle ----

    /**
     * Stops the streams and releases the device. Closing again does nothing.
     *
     * @throws IllegalStateException if called from a stream callback
     */
    @Override
    public void close() {
        BladeRfStream.rejectCallbackThread("close");
        synchronized (lifecycleLock) {
            if (closed) {
                return;
            }
            if (rxStream != null) {
                rxStream.stop();
            }
            if (txStream != null) {
                txStream.stop();
            }
            closed = true;
            Native.invokeVoid(Native.CLOSE, handle);
        }
    }

    /** Returns the handle for a stream's thread, which ends before the device closes. */
    MemorySegment nativeHandle() {
        return handle;
    }

    private MemorySegment handle() {
        if (closed) {
            throw new IllegalStateException("Device is closed");
        }
        return handle;
    }

    /** Calls a function that returns a status and takes the device first. */
    private int call(String function, MethodHandle method, Object... arguments) throws BladeRfException {
        return check(function, Native.invoke(method, withDevice(arguments)));
    }

    /**
     * Calls a function whose last parameter points to a result.
     *
     * @param layout layout of the result
     * @param decoder converts the result before its memory is released
     * @param arguments the parameters between the device and the result
     */
    private <T> T read(String function, MethodHandle method, MemoryLayout layout, Function<MemorySegment, T> decoder,
            Object... arguments) throws BladeRfException {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment result = arena.allocate(layout);
            Object[] all = new Object[arguments.length + 1];
            System.arraycopy(arguments, 0, all, 0, arguments.length);
            all[arguments.length] = result;
            call(function, method, all);
            return decoder.apply(result);
        }
    }

    private int readInt(String function, MethodHandle method, Object... arguments) throws BladeRfException {
        return read(function, method, JAVA_INT, s -> s.get(JAVA_INT, 0), arguments);
    }

    private long readUnsigned(String function, MethodHandle method, Object... arguments) throws BladeRfException {
        return Integer.toUnsignedLong(readInt(function, method, arguments));
    }

    private long readLong(String function, MethodHandle method, Object... arguments) throws BladeRfException {
        return read(function, method, JAVA_LONG, s -> s.get(JAVA_LONG, 0), arguments);
    }

    private int readU16(String function, MethodHandle method, Object... arguments) throws BladeRfException {
        return read(function, method, JAVA_SHORT, s -> Short.toUnsignedInt(s.get(JAVA_SHORT, 0)), arguments);
    }

    private int readU8(String function, MethodHandle method, Object... arguments) throws BladeRfException {
        return read(function, method, JAVA_BYTE, s -> Byte.toUnsignedInt(s.get(JAVA_BYTE, 0)), arguments);
    }

    private float readFloat(String function, MethodHandle method, Object... arguments) throws BladeRfException {
        return read(function, method, JAVA_FLOAT, s -> s.get(JAVA_FLOAT, 0), arguments);
    }

    private boolean readBoolean(String function, MethodHandle method, Object... arguments) throws BladeRfException {
        return read(function, method, JAVA_BOOLEAN, s -> s.get(JAVA_BOOLEAN, 0), arguments);
    }

    private String readString(String function, MethodHandle method, Object... arguments) throws BladeRfException {
        return read(function, method, ADDRESS, s -> Native.cString(s.get(ADDRESS, 0)), arguments);
    }

    private <E extends Enum<E> & Coded> E readEnum(Class<E> type, String function, MethodHandle method,
            Object... arguments) throws BladeRfException {
        return Coded.of(type, readInt(function, method, arguments));
    }

    private Range readRange(String function, MethodHandle method, Object... arguments) throws BladeRfException {
        return read(function, method, ADDRESS,
            s -> Range.read(s.get(ADDRESS, 0).reinterpret(Layouts.RANGE.byteSize())), arguments);
    }

    private Version readVersion(String function, MethodHandle method) throws BladeRfException {
        return read(function, method, Layouts.VERSION, Version::read);
    }

    /** Reads a function's table of named modes, whose size is the return value. */
    private int readTable(String function, MethodHandle method, MemorySegment pointer, Object... arguments)
            throws BladeRfException {
        Object[] all = new Object[arguments.length + 1];
        System.arraycopy(arguments, 0, all, 0, arguments.length);
        all[arguments.length] = pointer;
        return call(function, method, all);
    }

    private List<String> names(String function, MethodHandle method, Channel channel, boolean sizeIsLong)
            throws BladeRfException {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment names = arena.allocate(ADDRESS, MAX_NAMES);
            Object capacity = sizeIsLong ? (Object) (long) MAX_NAMES : (Object) MAX_NAMES;
            int count = Math.min(MAX_NAMES, call(function, method, channel.code(), names, capacity));
            List<String> result = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                result.add(Native.cString(names.getAtIndex(ADDRESS, i)));
            }
            return List.copyOf(result);
        }
    }

    private static MemorySegment bytes(byte[] data, Arena arena) {
        return arena.allocateFrom(JAVA_BYTE, data);
    }

    private Object[] withDevice(Object[] arguments) {
        Object[] all = new Object[arguments.length + 1];
        all[0] = handle();
        System.arraycopy(arguments, 0, all, 1, arguments.length);
        return all;
    }

    private static byte u8(int value) {
        if (value < 0 || value > 0xFF) {
            throw new IllegalArgumentException("Value out of 8-bit range: " + value);
        }
        return (byte) value;
    }

    private static short u16(int value) {
        if (value < 0 || value > 0xFFFF) {
            throw new IllegalArgumentException("Value out of 16-bit range: " + value);
        }
        return (short) value;
    }

    private static int u32(long value) {
        if (value < 0 || value > 0xFFFF_FFFFL) {
            throw new IllegalArgumentException("Value out of 32-bit range: " + value);
        }
        return (int) value;
    }
}
