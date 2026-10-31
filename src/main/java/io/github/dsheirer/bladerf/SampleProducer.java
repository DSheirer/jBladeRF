package io.github.dsheirer.bladerf;

import java.lang.foreign.MemorySegment;

/**
 * Provides samples for a stream started with {@link BladeRfDevice#startTx}.
 *
 * <p>The rules of {@link SampleListener} apply here too: it should return quickly
 * and it must not stop the stream except by returning false.
 */
@FunctionalInterface
public interface SampleProducer {
    /**
     * Fills a block of samples. The memory may be used only until this method
     * returns.
     *
     * @param buffer memory to fill completely, in native byte order
     * @param sampleCount number of samples that fit in the buffer
     * @return true to send this buffer and carry on, false to discard it and stop
     */
    boolean fill(MemorySegment buffer, int sampleCount);
}
