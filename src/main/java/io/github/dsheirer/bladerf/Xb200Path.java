package io.github.dsheirer.bladerf;

import io.github.dsheirer.bladerf.internal.Coded;

/** XB-200 signal path, {@code bladerf_xb200_path}. */
public enum Xb200Path implements Coded {
    BYPASS(0),
    MIX(1);

    private final int code;

    Xb200Path(int code) {
        this.code = code;
    }

    @Override
    public int code() {
        return code;
    }
}
