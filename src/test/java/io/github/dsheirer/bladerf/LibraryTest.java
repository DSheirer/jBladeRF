package io.github.dsheirer.bladerf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.dsheirer.bladerf.internal.Coded;
import io.github.dsheirer.bladerf.internal.Layouts;
import io.github.dsheirer.bladerf.internal.Native;
import io.github.dsheirer.bladerf.internal.NativeLibraryLoader;
import io.github.dsheirer.libusb.NativeLoader;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.GroupLayout;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Tests that need the native library for the current platform but no hardware. */
class LibraryTest {
    private static void assertOffset(long expected, GroupLayout layout, String field) {
        assertEquals(expected, Layouts.offset(layout, field), field);
    }

    /** The expected values were printed by a C compiler from sizeof and offsetof. */
    @Test
    void structuresMatchCompiler() {
        assertEquals(112, Layouts.DEVINFO.byteSize());
        assertOffset(4, Layouts.DEVINFO, "serial");
        assertOffset(37, Layouts.DEVINFO, "usb_bus");
        assertOffset(38, Layouts.DEVINFO, "usb_addr");
        assertOffset(40, Layouts.DEVINFO, "instance");
        assertOffset(44, Layouts.DEVINFO, "manufacturer");
        assertOffset(77, Layouts.DEVINFO, "product");
        assertEquals(32, Layouts.BACKENDINFO.byteSize());
        assertEquals(32, Layouts.RANGE.byteSize());
        assertOffset(24, Layouts.RANGE, "scale");
        assertEquals(33, Layouts.SERIAL.byteSize());
        assertEquals(16, Layouts.VERSION.byteSize());
        assertOffset(8, Layouts.VERSION, "describe");
        assertEquals(16, Layouts.GAIN_MODES.byteSize());
        assertOffset(8, Layouts.GAIN_MODES, "mode");
        assertEquals(16, Layouts.LOOPBACK_MODES.byteSize());
        assertEquals(24, Layouts.RATIONAL_RATE.byteSize());
        assertEquals(24, Layouts.TRIGGER.byteSize());
        assertOffset(16, Layouts.TRIGGER, "options");
        assertEquals(12, Layouts.QUICK_TUNE.byteSize());
        assertEquals(56, Layouts.METADATA.byteSize());
        assertOffset(16, Layouts.METADATA, "actual_count");
        assertOffset(20, Layouts.METADATA, "reserved");
        assertEquals(248, Layouts.IMAGE.byteSize());
        assertOffset(8, Layouts.IMAGE, "checksum");
        assertOffset(40, Layouts.IMAGE, "version");
        assertOffset(56, Layouts.IMAGE, "timestamp");
        assertOffset(64, Layouts.IMAGE, "serial");
        assertOffset(98, Layouts.IMAGE, "reserved");
        assertOffset(228, Layouts.IMAGE, "type");
        assertOffset(232, Layouts.IMAGE, "address");
        assertOffset(236, Layouts.IMAGE, "length");
        assertOffset(240, Layouts.IMAGE, "data");
        assertEquals(16, Layouts.GAIN_CAL_ENTRY.byteSize());
        assertEquals(88, Layouts.GAIN_CAL_TBL.byteSize());
        assertOffset(16, Layouts.GAIN_CAL_TBL, "ch");
        assertOffset(20, Layouts.GAIN_CAL_TBL, "enabled");
        assertOffset(24, Layouts.GAIN_CAL_TBL, "n_entries");
        assertOffset(32, Layouts.GAIN_CAL_TBL, "start_freq");
        assertOffset(40, Layouts.GAIN_CAL_TBL, "stop_freq");
        assertOffset(48, Layouts.GAIN_CAL_TBL, "entries");
        assertOffset(56, Layouts.GAIN_CAL_TBL, "gain_target");
        assertOffset(64, Layouts.GAIN_CAL_TBL, "file_path_len");
        assertOffset(72, Layouts.GAIN_CAL_TBL, "file_path");
        assertOffset(80, Layouts.GAIN_CAL_TBL, "state");
        assertEquals(20, Layouts.LMS_DC_CALS.byteSize());
        assertEquals(32, Layouts.RF_SWITCH_CONFIG.byteSize());
    }

    @Test
    void loadsLibraryAndResolvesEveryFunction() {
        Version version = BladeRf.libraryVersion();
        assertTrue(version.major() >= 2, version.toString());
        assertTrue(version.describe().startsWith(version.toString()), version.describe());
        assertNotNull(Native.SYNC_RX);
    }

    @Test
    void translatesNativeErrors() {
        assertEquals("No devices available", Native.errorMessage(BladeRfError.NODEV.code()));
        assertEquals(BladeRfError.TIMEOUT, BladeRfError.fromCode(-6));
        assertEquals(BladeRfError.UNEXPECTED, BladeRfError.fromCode(-12345));
    }

    @Test
    void namesComeFromLibrary() {
        assertEquals("libusb", BladeRf.name(Backend.LIBUSB));
        for (SampleFormat format : SampleFormat.values()) {
            assertFalse(BladeRf.name(format).isEmpty());
        }
        for (ImageType type : ImageType.values()) {
            assertFalse(BladeRf.name(type).isEmpty());
        }
    }

    @Test
    void parsesAndMatchesDeviceStrings() throws BladeRfException {
        DeviceInfo info = BladeRf.parseDeviceString("libusb:instance=3");
        assertEquals(Backend.LIBUSB, info.backend());
        assertEquals(3, info.instance());
        assertTrue(BladeRf.matches(BladeRf.anyDevice(), info));
        assertTrue(BladeRf.matches("*:instance=3", info));
        assertFalse(BladeRf.matches("*:instance=4", info));
        BladeRfException invalid = assertThrows(BladeRfException.class,
            () -> BladeRf.parseDeviceString("unknown-backend:"));
        assertEquals(BladeRfError.INVAL, invalid.error());
    }

    @Test
    void enumeratesWithoutFailing() {
        // CI machines may have no USB bus, which the library reports as an error.
        try {
            assertNotNull(BladeRf.listDevices());
        } catch (BladeRfException e) {
            assertTrue(Set.of(BladeRfError.IO, BladeRfError.UNEXPECTED).contains(e.error()), e.getMessage());
        }
    }

    @Test
    void openingNoDeviceReportsIt() {
        // No machine running these tests has a device with this serial number.
        BladeRfException e = assertThrows(BladeRfException.class,
            () -> BladeRf.open("*:serial=00000000000000000000000000000001"));
        assertTrue(Set.of(BladeRfError.NODEV, BladeRfError.IO, BladeRfError.UNEXPECTED).contains(e.error()),
            e.getMessage());
    }

    @Test
    void interleavesTwoChannels() throws BladeRfException {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment samples = arena.allocateFrom(ValueLayout.JAVA_SHORT,
                (short) 1, (short) 2, (short) 3, (short) 4, (short) 5, (short) 6, (short) 7, (short) 8);
            BladeRf.deinterleave(ChannelLayout.RX_X2, SampleFormat.SC16_Q11, 4, samples);
            assertEquals(List.of((short) 1, (short) 2, (short) 5, (short) 6),
                List.of(samples.getAtIndex(ValueLayout.JAVA_SHORT, 0), samples.getAtIndex(ValueLayout.JAVA_SHORT, 1),
                    samples.getAtIndex(ValueLayout.JAVA_SHORT, 2), samples.getAtIndex(ValueLayout.JAVA_SHORT, 3)));
            BladeRf.interleave(ChannelLayout.TX_X2, SampleFormat.SC16_Q11, 4, samples);
            assertEquals((short) 3, samples.getAtIndex(ValueLayout.JAVA_SHORT, 2));
        }
    }

    @Test
    void rejectsBuffersThatAreTooSmall() {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment samples = arena.allocate(7);
            assertThrows(IllegalArgumentException.class,
                () -> BladeRf.deinterleave(ChannelLayout.RX_X2, SampleFormat.SC16_Q11, 2, samples));
        }
    }

    @Test
    void sampleFormatsHaveTheirSizes() {
        assertEquals(400, SampleFormat.SC16_Q11.byteLength(100));
        assertEquals(400, SampleFormat.SC16_Q11_META.byteLength(100));
        assertEquals(300, SampleFormat.SC16_Q11_PACKED.byteLength(100));
        assertEquals(200, SampleFormat.SC8_Q7.byteLength(100));
        assertEquals(400, SampleFormat.PACKET_META.byteLength(100));
    }

    @Test
    void channelsEncodeDirectionAndIndex() {
        assertEquals(2, Channel.RX1.code());
        assertEquals(1, Channel.RX1.index());
        assertEquals(1, Channel.TX0.code());
        assertEquals(Direction.TX, Channel.TX1.direction());
        assertEquals(List.of(Channel.TX0, Channel.TX1), BladeRfStream.channels(ChannelLayout.TX_X2));
    }

    @Test
    void enumsDecodeFromNativeValues() {
        assertEquals(FpgaSize.A9, Coded.of(FpgaSize.class, 301));
        assertEquals(RxMux.DIGITAL_LOOPBACK, Coded.of(RxMux.class, 4));
        assertThrows(IllegalArgumentException.class, () -> Coded.of(RxMux.class, 3));
    }

    @Test
    void streamConfigurationIsChecked() {
        assertThrows(IllegalArgumentException.class, () -> new StreamConfig(4, 8192, 8));
        assertThrows(IllegalArgumentException.class, () -> new StreamConfig(0, 8192, 0));
    }

    @Test
    void namesLibraries() {
        assertEquals("libbladeRF.so.2", NativeLibraryLoader.libraryName("linux-aarch64"));
        assertEquals("libbladeRF.2.dylib", NativeLibraryLoader.libraryName("macos-x86_64"));
        assertEquals("bladeRF.dll", NativeLibraryLoader.libraryName("windows-aarch64"));
    }

    @Test
    void sharesLibusbWithJlibusb() throws Throwable {
        // Loading libbladeRF loads jlibusb's libusb, so both sit in its directory.
        assertNotNull(BladeRf.libraryVersion());
        Path directory = NativeLoader.directory();
        String platform = NativeLoader.platform();
        assertTrue(Files.isRegularFile(directory.resolve(NativeLoader.libusbName(platform))));
        assertTrue(Files.isRegularFile(directory.resolve(NativeLibraryLoader.libraryName(platform))));
        if (platform.startsWith("linux")) {
            // One copy of libusb in the process, the one jlibusb extracted.
            List<String> mapped = Files.readAllLines(Path.of("/proc/self/maps")).stream()
                .filter(line -> line.contains("libusb-1.0"))
                .map(line -> line.substring(line.indexOf('/')))
                .distinct()
                .toList();
            assertEquals(List.of(directory.resolve("libusb-1.0.so.0").toString()), mapped);
        } else if (platform.startsWith("macos")) {
            // dyld must reuse the libusb jlibusb loaded when libbladeRF asks for
            // @loader_path/libusb-1.0.0.dylib, even through the /var symlink.
            List<String> images = dyldImages().stream().filter(name -> name.contains("libusb-1.0")).toList();
            assertEquals(1, images.size(), images.toString());
        }
    }

    /** Paths of the images dyld has loaded into this process. */
    private static List<String> dyldImages() throws Throwable {
        Linker linker = Linker.nativeLinker();
        SymbolLookup system = linker.defaultLookup();
        MethodHandle count = linker.downcallHandle(system.find("_dyld_image_count").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.JAVA_INT));
        MethodHandle name = linker.downcallHandle(system.find("_dyld_get_image_name").orElseThrow(),
            FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.JAVA_INT));
        int images = (int) count.invokeExact();
        List<String> names = new ArrayList<>();
        for (int i = 0; i < images; i++) {
            MemorySegment pointer = (MemorySegment) name.invokeExact(i);
            if (!pointer.equals(MemorySegment.NULL)) {
                names.add(pointer.reinterpret(Long.MAX_VALUE).getString(0));
            }
        }
        return names;
    }
}
