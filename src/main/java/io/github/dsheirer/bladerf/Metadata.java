package io.github.dsheirer.bladerf;

import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;

import io.github.dsheirer.bladerf.internal.Layouts;
import java.lang.foreign.MemorySegment;

/**
 * Timing and status of a synchronous transfer, {@code struct bladerf_metadata}.
 * Set the timestamp and flags before a call, and read the status and actual
 * count afterwards. One instance can be reused for later calls.
 */
public final class Metadata {
    /** Status bit: samples were lost while receiving. */
    public static final int STATUS_OVERRUN = 1;
    /** Status bit: the device ran out of samples to transmit. */
    public static final int STATUS_UNDERRUN = 1 << 1;
    /** Flag: the first samples of a transmit burst. */
    public static final int FLAG_TX_BURST_START = 1;
    /** Flag: the last samples of a transmit burst. */
    public static final int FLAG_TX_BURST_END = 1 << 1;
    /** Flag: transmit as soon as possible, ignoring the timestamp. */
    public static final int FLAG_TX_NOW = 1 << 2;
    /** Flag: the timestamp marks where the next samples go within a burst. */
    public static final int FLAG_TX_UPDATE_TIMESTAMP = 1 << 3;
    /** Flag: receive the next available samples, ignoring the timestamp. */
    public static final int FLAG_RX_NOW = 1 << 31;
    /** Status bit set by hardware: a sample FIFO underflowed. */
    public static final int STATUS_RX_HW_UNDERFLOW = 1;
    /** Status bit set by hardware: mini expansion pin 1 is high. */
    public static final int STATUS_RX_HW_MINIEXP1 = 1 << 16;
    /** Status bit set by hardware: mini expansion pin 2 is high. */
    public static final int STATUS_RX_HW_MINIEXP2 = 1 << 17;

    private long timestamp;
    private int flags;
    private int status;
    private long actualCount;

    /** Returns the timestamp, in samples. */
    public long timestamp() {
        return timestamp;
    }

    /** Sets the timestamp, in samples. */
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    /** Returns the {@code FLAG_*} bits. */
    public int flags() {
        return flags;
    }

    /** Sets the {@code FLAG_*} bits. */
    public void setFlags(int flags) {
        this.flags = flags;
    }

    /** Returns the {@code STATUS_*} bits. */
    public int status() {
        return status;
    }

    /** Returns the number of samples actually transferred. */
    public long actualCount() {
        return actualCount;
    }

    void write(MemorySegment metadata) {
        metadata.set(JAVA_LONG, Layouts.offset(Layouts.METADATA, "timestamp"), timestamp);
        metadata.set(JAVA_INT, Layouts.offset(Layouts.METADATA, "flags"), flags);
    }

    void read(MemorySegment metadata) {
        timestamp = Layouts.i64(metadata, Layouts.METADATA, "timestamp");
        flags = Layouts.i32(metadata, Layouts.METADATA, "flags");
        status = Layouts.i32(metadata, Layouts.METADATA, "status");
        actualCount = Layouts.u32(metadata, Layouts.METADATA, "actual_count");
    }
}
