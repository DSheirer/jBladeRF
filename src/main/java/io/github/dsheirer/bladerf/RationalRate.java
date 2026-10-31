package io.github.dsheirer.bladerf;

import io.github.dsheirer.bladerf.internal.Layouts;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

/**
 * A rate of {@code integer + numerator / denominator}, {@code struct bladerf_rational_rate}.
 *
 * @param integer integer part
 * @param numerator numerator of the fractional part
 * @param denominator denominator of the fractional part, which must be more than 0
 */
public record RationalRate(long integer, long numerator, long denominator) {
    static RationalRate read(MemorySegment rate) {
        return new RationalRate(Layouts.i64(rate, Layouts.RATIONAL_RATE, "integer"),
            Layouts.i64(rate, Layouts.RATIONAL_RATE, "num"), Layouts.i64(rate, Layouts.RATIONAL_RATE, "den"));
    }

    void write(MemorySegment rate) {
        rate.set(ValueLayout.JAVA_LONG, Layouts.offset(Layouts.RATIONAL_RATE, "integer"), integer);
        rate.set(ValueLayout.JAVA_LONG, Layouts.offset(Layouts.RATIONAL_RATE, "num"), numerator);
        rate.set(ValueLayout.JAVA_LONG, Layouts.offset(Layouts.RATIONAL_RATE, "den"), denominator);
    }
}
