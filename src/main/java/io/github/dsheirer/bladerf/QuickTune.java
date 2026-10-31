package io.github.dsheirer.bladerf;

import static java.lang.foreign.ValueLayout.JAVA_BYTE;

import io.github.dsheirer.bladerf.internal.Layouts;
import java.lang.foreign.MemorySegment;

/**
 * Saved tuning parameters that retune faster than a frequency,
 * {@code struct bladerf_quick_tune}. Get one from
 * {@link BladeRfDevice#quickTune}. Its contents depend on the board.
 */
public final class QuickTune {
    private final byte[] data;

    QuickTune(MemorySegment quickTune) {
        data = quickTune.asSlice(0, Layouts.QUICK_TUNE.byteSize()).toArray(JAVA_BYTE);
    }

    void write(MemorySegment quickTune) {
        MemorySegment.copy(data, 0, quickTune, JAVA_BYTE, 0, data.length);
    }
}
