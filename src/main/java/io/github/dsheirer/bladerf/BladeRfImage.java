package io.github.dsheirer.bladerf;

import io.github.dsheirer.bladerf.internal.Coded;
import io.github.dsheirer.bladerf.internal.Layouts;
import io.github.dsheirer.bladerf.internal.Native;
import java.lang.foreign.Arena;
import java.lang.foreign.GroupLayout;
import java.lang.foreign.MemorySegment;
import java.nio.file.Path;

/**
 * A flash image held in native memory, {@code struct bladerf_image}. Create one
 * with {@link BladeRfDevice#allocateImage} or
 * {@link BladeRfDevice#allocateCalibrationImage}, and close it when finished.
 * Images are not safe to use from several threads at once.
 */
public final class BladeRfImage implements AutoCloseable {
    private static final GroupLayout LAYOUT = Layouts.IMAGE;

    private final MemorySegment image;
    private Arena dataArena = Arena.ofShared();
    private boolean closed;

    BladeRfImage(MemorySegment pointer) {
        image = pointer.reinterpret(LAYOUT.byteSize());
    }

    /** Replaces the contents of this image with a file that was checked against its checksum. */
    public void read(Path file) throws BladeRfException {
        MemorySegment pointer = handle();
        // The library frees the old data, so views of it have to end first.
        dataArena.close();
        dataArena = Arena.ofShared();
        try (Arena arena = Arena.ofConfined()) {
            BladeRf.check("bladerf_image_read",
                Native.invoke(Native.IMAGE_READ, pointer, arena.allocateFrom(file.toString())));
        }
    }

    /** Prints the image's metadata to stdout. */
    public void printMetadata() throws BladeRfException {
        BladeRf.check("bladerf_image_print_metadata", Native.invoke(Native.IMAGE_PRINT_METADATA, handle()));
    }

    /** Returns the version of the image's contents. */
    public Version version() {
        return Version.read(Layouts.slice(handle(), LAYOUT, "version"));
    }

    /** Returns the image's creation time, in seconds since the Unix epoch. */
    public long timestamp() {
        return Layouts.i64(handle(), LAYOUT, "timestamp");
    }

    /** Returns the serial number of the device the image was made for. */
    public String serial() {
        return Layouts.string(handle(), LAYOUT, "serial");
    }

    /** Returns what the image contains. */
    public ImageType type() {
        return Coded.of(ImageType.class, Layouts.i32(handle(), LAYOUT, "type"));
    }

    /** Returns the flash address of the image's contents. */
    public long address() {
        return Layouts.u32(handle(), LAYOUT, "address");
    }

    /** Returns the size of the image's contents in bytes. */
    public long length() {
        return Layouts.u32(handle(), LAYOUT, "length");
    }

    /** Returns the image's contents, which can be read or written until the next {@link #read} or {@link #close}. */
    public MemorySegment data() {
        MemorySegment pointer = Layouts.pointer(handle(), LAYOUT, "data");
        return pointer.reinterpret(length(), dataArena, null);
    }

    /** Returns the native {@code struct bladerf_image}. */
    MemorySegment handle() {
        if (closed) {
            throw new IllegalStateException("Image is closed");
        }
        return image;
    }

    /** Frees the image. Closing again does nothing. */
    @Override
    public void close() {
        if (!closed) {
            closed = true;
            dataArena.close();
            Native.invokeVoid(Native.FREE_IMAGE, image);
        }
    }
}
