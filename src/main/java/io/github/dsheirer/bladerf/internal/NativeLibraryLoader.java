package io.github.dsheirer.bladerf.internal;

import io.github.dsheirer.libusb.NativeLoader;
import java.lang.foreign.SymbolLookup;
import java.util.List;

/**
 * Loads libbladeRF through jlibusb, which supplies the libusb it depends on.
 *
 * <p>By default libbladeRF is extracted from the jar, next to jlibusb's copy of
 * libusb. Set the {@value #PATH_PROPERTY} system property to a directory to
 * load libbladeRF from there instead; set {@code jlibusb.library.path} to
 * replace libusb.
 */
public final class NativeLibraryLoader {
    /** System property naming a directory that overrides the bundled libbladeRF. */
    public static final String PATH_PROPERTY = "jbladerf.library.path";

    private static final String RESOURCE_ROOT = "/jbladerf/native/";

    private NativeLibraryLoader() {
    }

    /** File name of libbladeRF on a platform, such as {@code linux-x86_64}. */
    public static String libraryName(String platform) {
        if (platform.startsWith("linux")) {
            return "libbladeRF.so.2";
        }
        if (platform.startsWith("macos")) {
            return "libbladeRF.2.dylib";
        }
        return "bladeRF.dll";
    }

    /** Loads libusb and libbladeRF and returns a lookup for the symbols of libbladeRF. */
    static SymbolLookup load() {
        String name = libraryName(NativeLoader.platform());
        return NativeLoader.load(NativeLibraryLoader.class, RESOURCE_ROOT, List.of(name), PATH_PROPERTY);
    }
}
