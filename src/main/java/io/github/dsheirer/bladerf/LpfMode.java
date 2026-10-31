package io.github.dsheirer.bladerf;

import io.github.dsheirer.bladerf.internal.Coded;

/** bladeRF 1 low-pass filter mode, {@code bladerf_lpf_mode}. */
public enum LpfMode implements Coded {
    NORMAL(0),
    BYPASSED(1),
    DISABLED(2);

    private final int code;

    LpfMode(int code) {
        this.code = code;
    }

    @Override
    public int code() {
        return code;
    }
}
