package io.github.dsheirer.bladerf;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

/**
 * Samples delivered to a {@link SampleListener}.
 *
 * <p>{@link #samples()} points at memory owned by the native library. It may be
 * used only on the callback's thread and only until the callback returns; after
 * that, access throws {@link IllegalStateException}. Use one of the copying
 * methods, or {@link MemorySegment#copy}, to keep the data.
 *
 * <p>Samples of a stream with two channels are interleaved in pairs of channels;
 * {@link BladeRf#deinterleave} separates them.
 *
 * @param samples sample memory, in native byte order
 * @param sampleCount number of samples
 * @param format format of the samples
 */
public record SampleBuffer(MemorySegment samples, int sampleCount, SampleFormat format) {

    /** Copies 16-bit samples to a new array, with I and Q interleaved. */
    public short[] copyShorts() {
        return samples.toArray(ValueLayout.JAVA_SHORT);
    }

    /** Copies the sample memory to a new array. */
    public byte[] copyBytes() {
        return samples.toArray(ValueLayout.JAVA_BYTE);
    }
}
