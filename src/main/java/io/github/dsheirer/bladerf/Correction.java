package io.github.dsheirer.bladerf;

import io.github.dsheirer.bladerf.internal.Coded;

/** I/Q correction, {@code bladerf_correction}. */
public enum Correction implements Coded {
    DC_OFFSET_I(0),
    DC_OFFSET_Q(1),
    PHASE(2),
    GAIN(3);

    private final int code;

    Correction(int code) {
        this.code = code;
    }

    @Override
    public int code() {
        return code;
    }
}
