package io.github.dsheirer.bladerf;

import io.github.dsheirer.bladerf.internal.Coded;

/** bladeRF 2 reference clock source, {@code bladerf_clock_select}. */
public enum ClockSelect implements Coded {
    ONBOARD(0),
    EXTERNAL(1);

    private final int code;

    ClockSelect(int code) {
        this.code = code;
    }

    @Override
    public int code() {
        return code;
    }
}
