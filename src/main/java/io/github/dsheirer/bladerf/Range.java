package io.github.dsheirer.bladerf;

import io.github.dsheirer.bladerf.internal.Layouts;
import java.lang.foreign.MemorySegment;

/**
 * Allowed values of a setting, {@code struct bladerf_range}. Multiply a value by
 * {@code scale} to get the value in its natural unit, such as dB.
 *
 * @param min minimum value
 * @param max maximum value
 * @param step distance between values
 * @param scale unit scale
 */
public record Range(long min, long max, long step, float scale) {
    static Range read(MemorySegment range) {
        return new Range(Layouts.i64(range, Layouts.RANGE, "min"), Layouts.i64(range, Layouts.RANGE, "max"),
            Layouts.i64(range, Layouts.RANGE, "step"), Layouts.f32(range, Layouts.RANGE, "scale"));
    }
}
