package io.github.dsheirer.bladerf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/**
 * Receive-only tests against a real device with its FPGA loaded. Enable with
 * {@code -PhardwareTest=true}.
 */
@EnabledIfSystemProperty(named = "bladerf.hardware.test", matches = "true")
class HardwareTest {
    private static final long FREQUENCY = 915_000_000L;
    private static final long SAMPLE_RATE = 2_000_000L;
    private static final int TIMEOUT_MS = 5_000;

    private static BladeRfDevice open() throws BladeRfException {
        BladeRfDevice device = BladeRf.open();
        device.setFrequency(Channel.RX0, FREQUENCY);
        device.setSampleRate(Channel.RX0, SAMPLE_RATE);
        return device;
    }

    private static BladeRfStream startRx(BladeRfDevice device, SampleListener listener) throws BladeRfException {
        return device.startRx(ChannelLayout.RX_X1, SampleFormat.SC16_Q11, new StreamConfig(16, 8192, 8), listener);
    }

    private static void awaitEnd(BladeRfStream stream) throws InterruptedException {
        for (int i = 0; i < 1000 && stream.isRunning(); i++) {
            Thread.sleep(5);
        }
        assertFalse(stream.isRunning());
    }

    @Test
    void readsDeviceInfo() throws BladeRfException {
        List<DeviceInfo> devices = BladeRf.listDevices();
        assertFalse(devices.isEmpty(), "No device found");
        try (BladeRfDevice device = BladeRf.open(devices.getFirst())) {
            assertEquals(devices.getFirst().serial(), device.serial());
            assertEquals(devices.getFirst().serial(), device.deviceInfo().serial());
            assertTrue(device.isFpgaConfigured(), "FPGA is not loaded");
            assertFalse(device.boardName().isEmpty());
            assertTrue(device.firmwareVersion().major() > 0);
            assertTrue(device.channelCount(Direction.RX) >= 1);
            assertTrue(device.frequencyRange(Channel.RX0).max() > 0);
            assertFalse(device.gainModes(Channel.RX0).isEmpty());
            assertFalse(device.gainStages(Channel.RX0).isEmpty());
            assertFalse(device.loopbackModes().isEmpty());
        }
    }

    @Test
    void tuningReadsBack() throws BladeRfException {
        try (BladeRfDevice device = open()) {
            assertEquals(SAMPLE_RATE, device.sampleRate(Channel.RX0));
            assertTrue(Math.abs(device.frequency(Channel.RX0) - FREQUENCY) < 100);
            long bandwidth = device.setBandwidth(Channel.RX0, 1_500_000);
            assertEquals(bandwidth, device.bandwidth(Channel.RX0));
        }
    }

    @Test
    void receivesSynchronously() throws BladeRfException {
        try (BladeRfDevice device = open(); Arena arena = Arena.ofConfined()) {
            device.syncConfig(ChannelLayout.RX_X1, SampleFormat.SC16_Q11, 16, 8192, 8, TIMEOUT_MS);
            device.enableChannel(Channel.RX0, true);
            try {
                MemorySegment samples = arena.allocate(SampleFormat.SC16_Q11.byteLength(8192), 2);
                device.syncRx(samples, 8192, null, TIMEOUT_MS);
                short[] array = new short[4096];
                device.syncRx(array, 2048, null, TIMEOUT_MS);
                assertAllowed(array);
            } finally {
                device.enableChannel(Channel.RX0, false);
            }
        }
    }

    @Test
    void metadataReportsTimestamps() throws BladeRfException {
        try (BladeRfDevice device = open(); Arena arena = Arena.ofConfined()) {
            device.syncConfig(ChannelLayout.RX_X1, SampleFormat.SC16_Q11_META, 16, 8192, 8, TIMEOUT_MS);
            device.enableChannel(Channel.RX0, true);
            try {
                MemorySegment samples = arena.allocate(SampleFormat.SC16_Q11_META.byteLength(8192), 2);
                Metadata metadata = new Metadata();
                metadata.setFlags(Metadata.FLAG_RX_NOW);
                device.syncRx(samples, 8192, metadata, TIMEOUT_MS);
                assertEquals(8192, metadata.actualCount());
                long first = metadata.timestamp();
                metadata.setFlags(Metadata.FLAG_RX_NOW);
                device.syncRx(samples, 8192, metadata, TIMEOUT_MS);
                assertEquals(first + 8192, metadata.timestamp());
            } finally {
                device.enableChannel(Channel.RX0, false);
            }
        }
    }

    private static void assertAllowed(short[] samples) {
        boolean nonZero = false;
        for (short sample : samples) {
            assertTrue(sample >= -2048 && sample <= 2047, "Sample out of range: " + sample);
            nonZero |= sample != 0;
        }
        assertTrue(nonZero, "Samples were all zero");
    }

    @Test
    void streamsSamplesAndStops() throws Exception {
        try (BladeRfDevice device = open()) {
            CountDownLatch received = new CountDownLatch(50);
            AtomicReference<short[]> last = new AtomicReference<>();
            BladeRfStream stream = startRx(device, buffer -> {
                assertEquals(8192, buffer.sampleCount());
                last.set(buffer.copyShorts());
                received.countDown();
                return true;
            });
            assertTrue(received.await(10, TimeUnit.SECONDS), "Samples did not arrive");
            assertTrue(stream.isRunning());
            stream.stop();
            assertFalse(stream.isRunning());
            assertNull(stream.failure());
            assertAllowed(last.get());
        }
    }

    @Test
    void canRestartAfterListenerEndedStream() throws Exception {
        try (BladeRfDevice device = open()) {
            for (int run = 0; run < 3; run++) {
                CountDownLatch received = new CountDownLatch(1);
                BladeRfStream stream = startRx(device, buffer -> {
                    received.countDown();
                    return false;
                });
                assertTrue(received.await(10, TimeUnit.SECONDS), "No samples in run " + run);
                awaitEnd(stream);
                assertNull(stream.failure());
            }
        }
    }

    @Test
    void closingDeviceStopsStream() throws Exception {
        BladeRfStream stream;
        try (BladeRfDevice device = open()) {
            CountDownLatch received = new CountDownLatch(1);
            stream = startRx(device, buffer -> {
                received.countDown();
                return true;
            });
            assertTrue(received.await(10, TimeUnit.SECONDS));
        }
        assertFalse(stream.isRunning());
    }

    @Test
    void secondStreamIsRejected() throws Exception {
        try (BladeRfDevice device = open(); BladeRfStream stream = startRx(device, buffer -> true)) {
            assertTrue(stream.isRunning());
            assertThrows(IllegalStateException.class, () -> startRx(device, buffer -> true));
        }
    }

    @Test
    void listenerExceptionStopsStreamInsteadOfCrashing() throws Exception {
        try (BladeRfDevice device = open()) {
            BladeRfStream stream = startRx(device, buffer -> {
                throw new IllegalArgumentException("expected");
            });
            awaitEnd(stream);
            assertInstanceOf(IllegalArgumentException.class, stream.failure());
        }
    }

    @Test
    void samplesCannotBeUsedAfterCallbackReturns() throws Exception {
        try (BladeRfDevice device = open()) {
            AtomicReference<MemorySegment> kept = new AtomicReference<>();
            BladeRfStream stream = startRx(device, buffer -> {
                kept.set(buffer.samples());
                return false;
            });
            awaitEnd(stream);
            assertThrows(RuntimeException.class, () -> kept.get().get(ValueLayout.JAVA_BYTE, 0));
        }
    }

    @Test
    @SuppressWarnings("try")
    void stoppingFromListenerIsRejected() throws Exception {
        try (BladeRfDevice device = open()) {
            AtomicReference<Throwable> thrown = new AtomicReference<>();
            BladeRfStream stream = startRx(device, buffer -> {
                try {
                    device.close();
                } catch (Throwable t) {
                    thrown.set(t);
                }
                return false;
            });
            awaitEnd(stream);
            assertInstanceOf(IllegalStateException.class, thrown.get());
        }
    }
}
