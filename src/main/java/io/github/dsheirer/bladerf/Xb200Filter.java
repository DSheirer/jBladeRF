package io.github.dsheirer.bladerf;

import io.github.dsheirer.bladerf.internal.Coded;

/** XB-200 filter bank, {@code bladerf_xb200_filter}. */
public enum Xb200Filter implements Coded {
    MHZ_50(0),
    MHZ_144(1),
    MHZ_222(2),
    CUSTOM(3),
    AUTO_1DB(4),
    AUTO_3DB(5);

    private final int code;

    Xb200Filter(int code) {
        this.code = code;
    }

    @Override
    public int code() {
        return code;
    }
}
