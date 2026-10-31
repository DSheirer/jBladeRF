package io.github.dsheirer.bladerf;

import io.github.dsheirer.bladerf.internal.Coded;

/**
 * Format of the samples in a stream, {@code bladerf_format}. A sample is one I/Q
 * pair, or one 32-bit word for {@link #PACKET_META}.
 */
public enum SampleFormat implements Coded {
    /** Interleaved signed 16-bit I and Q, in the range [-2048, 2047]. */
    SC16_Q11(0, 4),
    /** {@link #SC16_Q11} packed into 12 bits per component. */
    SC16_Q11_PACKED(1, 3),
    /** {@link #SC16_Q11} with per-buffer timestamps and flags. */
    SC16_Q11_META(2, 4),
    /** Packets of data for FPGA images that support the packet core. */
    PACKET_META(3, 4),
    /** Interleaved signed 8-bit I and Q. */
    SC8_Q7(4, 2),
    /** {@link #SC8_Q7} with per-buffer timestamps and flags. */
    SC8_Q7_META(5, 2);

    private final int code;
    private final int bytesPerSample;

    SampleFormat(int code, int bytesPerSample) {
        this.code = code;
        this.bytesPerSample = bytesPerSample;
    }

    @Override
    public int code() {
        return code;
    }

    /** Returns the size of one sample in bytes. */
    public int bytesPerSample() {
        return bytesPerSample;
    }

    /** Returns the size in bytes of a number of samples. */
    public long byteLength(long samples) {
        return Math.multiplyExact(samples, (long) bytesPerSample);
    }
}
