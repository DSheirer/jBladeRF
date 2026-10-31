package io.github.dsheirer.bladerf;

import io.github.dsheirer.bladerf.internal.Coded;

/** Expansion board, {@code bladerf_xb}. */
public enum ExpansionBoard implements Coded {
    NONE(0),
    XB_100(1),
    XB_200(2),
    XB_300(3);

    private final int code;

    ExpansionBoard(int code) {
        this.code = code;
    }

    @Override
    public int code() {
        return code;
    }
}
