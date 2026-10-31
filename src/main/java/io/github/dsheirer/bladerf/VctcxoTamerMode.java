package io.github.dsheirer.bladerf;

import io.github.dsheirer.bladerf.internal.Coded;

/** Source for disciplining the VCTCXO, {@code bladerf_vctcxo_tamer_mode}. */
public enum VctcxoTamerMode implements Coded {
    INVALID(-1),
    DISABLED(0),
    PPS_1(1),
    MHZ_10(2);

    private final int code;

    VctcxoTamerMode(int code) {
        this.code = code;
    }

    @Override
    public int code() {
        return code;
    }
}
