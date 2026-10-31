package io.github.dsheirer.bladerf;

import io.github.dsheirer.bladerf.internal.Coded;

/** XB-300 amplifier, {@code bladerf_xb300_amplifier}. */
public enum Xb300Amplifier implements Coded {
    INVALID(-1),
    PA(0),
    LNA(1),
    PA_AUX(2);

    private final int code;

    Xb300Amplifier(int code) {
        this.code = code;
    }

    @Override
    public int code() {
        return code;
    }
}
