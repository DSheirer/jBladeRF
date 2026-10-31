package io.github.dsheirer.bladerf;

import io.github.dsheirer.bladerf.internal.Coded;

/** Where tuning is calculated, {@code bladerf_tuning_mode}. */
public enum TuningMode implements Coded {
    INVALID(-1),
    HOST(0),
    FPGA(1);

    private final int code;

    TuningMode(int code) {
        this.code = code;
    }

    @Override
    public int code() {
        return code;
    }
}
