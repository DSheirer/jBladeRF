package io.github.dsheirer.bladerf;

import static java.lang.foreign.ValueLayout.ADDRESS;

import io.github.dsheirer.bladerf.internal.Layouts;
import io.github.dsheirer.bladerf.internal.Native;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Entry point of the library: finds and opens bladeRF devices.
 *
 * <pre>{@code
 * try (BladeRfDevice device = BladeRf.open()) {
 *     device.setFrequency(Channel.RX0, 100_000_000L);
 *     device.setSampleRate(Channel.RX0, 2_000_000);
 *     device.setBandwidth(Channel.RX0, 1_500_000);
 *     try (BladeRfStream stream = device.startRx(ChannelLayout.RX_X1, SampleFormat.SC16_Q11,
 *             StreamConfig.DEFAULT, buffer -> {
 *                 process(buffer.copyShorts());
 *                 return true;
 *             })) {
 *         Thread.sleep(1000);
 *     }
 * }
 * }</pre>
 *
 * <p>The methods that enumerate and open devices run one at a time.
 */
public final class BladeRf {
    private static final Object LOCK = new Object();

    private BladeRf() {
    }

    /** Returns the version of the loaded native library. */
    public static Version libraryVersion() {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment version = arena.allocate(Layouts.VERSION);
            Native.invokeVoid(Native.VERSION, version);
            return Version.read(version);
        }
    }

    /** Sets how much the native library logs to stderr. */
    public static void setLogLevel(LogLevel level) {
        Native.invokeVoid(Native.LOG_SET_VERBOSITY, level.code());
    }

    /** Selects whether opening a device first resets its USB port. */
    public static void setUsbResetOnOpen(boolean enabled) {
        Native.invokeVoid(Native.SET_USB_RESET_ON_OPEN, enabled);
    }

    /** Returns the connected devices, or an empty list if there are none. */
    public static List<DeviceInfo> listDevices() throws BladeRfException {
        return deviceList("bladerf_get_device_list", Native.GET_DEVICE_LIST);
    }

    /** Returns the connected devices that are running their FX3 bootloader. */
    public static List<DeviceInfo> listBootloaders() throws BladeRfException {
        return deviceList("bladerf_get_bootloader_list", Native.GET_BOOTLOADER_LIST);
    }

    private static List<DeviceInfo> deviceList(String function, MethodHandle handle) throws BladeRfException {
        synchronized (LOCK) {
            try (Arena arena = Arena.ofConfined()) {
                MemorySegment pointer = arena.allocate(ADDRESS);
                int count = Native.invoke(handle, pointer);
                if (count == BladeRfError.NODEV.code()) {
                    return List.of();
                }
                check(function, count);
                long size = Layouts.DEVINFO.byteSize();
                MemorySegment devices = pointer.get(ADDRESS, 0);
                try {
                    MemorySegment array = devices.reinterpret(count * size);
                    List<DeviceInfo> result = new ArrayList<>(count);
                    for (int i = 0; i < count; i++) {
                        result.add(DeviceInfo.read(array.asSlice(i * size, size)));
                    }
                    return List.copyOf(result);
                } finally {
                    Native.invokeVoid(Native.FREE_DEVICE_LIST, devices);
                }
            }
        }
    }

    /** Opens the first available device. */
    public static BladeRfDevice open() throws BladeRfException {
        return open((String) null);
    }

    /**
     * Opens a device described by a string such as {@code *:serial=f12ce1037830}.
     *
     * @param identifier the device, or null for the first available one
     */
    public static BladeRfDevice open(String identifier) throws BladeRfException {
        synchronized (LOCK) {
            try (Arena arena = Arena.ofConfined()) {
                MemorySegment pointer = arena.allocate(ADDRESS);
                check("bladerf_open", Native.invoke(Native.OPEN, pointer, cString(identifier, arena)));
                return new BladeRfDevice(pointer.get(ADDRESS, 0));
            }
        }
    }

    /** Opens a device from {@link #listDevices()}. */
    public static BladeRfDevice open(DeviceInfo info) throws BladeRfException {
        Objects.requireNonNull(info, "info");
        synchronized (LOCK) {
            try (Arena arena = Arena.ofConfined()) {
                MemorySegment pointer = arena.allocate(ADDRESS);
                check("bladerf_open_with_devinfo",
                    Native.invoke(Native.OPEN_WITH_DEVINFO, pointer, toNative(info, arena)));
                return new BladeRfDevice(pointer.get(ADDRESS, 0));
            }
        }
    }

    /** Returns a description that matches any device. */
    public static DeviceInfo anyDevice() {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment info = arena.allocate(Layouts.DEVINFO);
            Native.invokeVoid(Native.INIT_DEVINFO, info);
            return DeviceInfo.read(info);
        }
    }

    /** Parses a device identifier string. */
    public static DeviceInfo parseDeviceString(String identifier) throws BladeRfException {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment info = arena.allocate(Layouts.DEVINFO);
            check("bladerf_get_devinfo_from_str",
                Native.invoke(Native.GET_DEVINFO_FROM_STR, arena.allocateFrom(identifier), info));
            return DeviceInfo.read(info);
        }
    }

    /** Returns true if two descriptions match, taking wildcard values into account. */
    public static boolean matches(DeviceInfo a, DeviceInfo b) {
        try (Arena arena = Arena.ofConfined()) {
            return Native.invokeBoolean(Native.DEVINFO_MATCHES, toNative(a, arena), toNative(b, arena));
        }
    }

    /** Returns true if a device identifier string matches a description. */
    public static boolean matches(String identifier, DeviceInfo info) {
        try (Arena arena = Arena.ofConfined()) {
            return Native.invokeBoolean(Native.DEVSTR_MATCHES, arena.allocateFrom(identifier), toNative(info, arena));
        }
    }

    /** Returns the library's name for a backend. */
    public static String name(Backend backend) {
        return Native.cString(Native.invokePointer(Native.BACKEND_STR, backend.code()));
    }

    /** Returns the library's name for a sample format. */
    public static String name(SampleFormat format) {
        return Native.cString(Native.invokePointer(Native.FORMAT_TO_STRING, format.code()));
    }

    /** Returns the library's name for an image type. */
    public static String name(ImageType type) {
        return Native.cString(Native.invokePointer(Native.IMAGE_TYPE_TO_STRING, type.code()));
    }

    /**
     * Loads firmware into a device that is running its FX3 bootloader. The device
     * re-enumerates afterwards, so {@link #listDevices()} finds it after a delay.
     *
     * @param device a device from {@link #listBootloaders()}
     * @param firmware firmware file
     */
    public static void loadFirmwareFromBootloader(DeviceInfo device, Path firmware) throws BladeRfException {
        try (Arena arena = Arena.ofConfined()) {
            check("bladerf_load_fw_from_bootloader", Native.invoke(Native.LOAD_FW_FROM_BOOTLOADER,
                arena.allocateFrom(device.serial()), device.backend().code(), (byte) device.usbBus(),
                (byte) device.usbAddress(), arena.allocateFrom(firmware.toString())));
        }
    }

    /**
     * Interleaves samples in place into the order the stream format needs.
     *
     * @param samples native memory holding the samples
     * @param sampleCount number of samples in the buffer
     */
    public static void interleave(ChannelLayout layout, SampleFormat format, int sampleCount, MemorySegment samples)
            throws BladeRfException {
        checkLength(samples, format, sampleCount);
        check("bladerf_interleave_stream_buffer", Native.invoke(Native.INTERLEAVE_STREAM_BUFFER, layout.code(),
            format.code(), sampleCount, samples));
    }

    /**
     * Separates interleaved samples in place into one block per channel.
     *
     * @param samples native memory holding the samples
     * @param sampleCount number of samples in the buffer
     */
    public static void deinterleave(ChannelLayout layout, SampleFormat format, int sampleCount, MemorySegment samples)
            throws BladeRfException {
        checkLength(samples, format, sampleCount);
        check("bladerf_deinterleave_stream_buffer", Native.invoke(Native.DEINTERLEAVE_STREAM_BUFFER, layout.code(),
            format.code(), sampleCount, samples));
    }

    static void checkLength(MemorySegment samples, SampleFormat format, long sampleCount) {
        if (sampleCount < 0 || format.byteLength(sampleCount) > samples.byteSize()) {
            throw new IllegalArgumentException(sampleCount + " " + format + " samples do not fit in "
                + samples.byteSize() + " bytes");
        }
    }

    private static MemorySegment toNative(DeviceInfo info, Arena arena) {
        MemorySegment segment = arena.allocate(Layouts.DEVINFO);
        info.write(segment);
        return segment;
    }

    /** Allocates a C string, or returns NULL for null. */
    static MemorySegment cString(String value, Arena arena) {
        return value == null ? MemorySegment.NULL : arena.allocateFrom(value);
    }

    static int check(String function, int result) throws BladeRfException {
        if (result < 0) {
            throw new BladeRfException(function, result);
        }
        return result;
    }
}
