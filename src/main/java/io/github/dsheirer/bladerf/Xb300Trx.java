package io.github.dsheirer.bladerf;

import io.github.dsheirer.bladerf.internal.Coded;

/** XB-300 TRX antenna mode, {@code bladerf_xb300_trx}. */
public enum Xb300Trx implements Coded {
    INVALID(-1),
    TX(0),
    RX(1),
    UNSET(2);

    private final int code;

    Xb300Trx(int code) {
        this.code = code;
    }

    @Override
    public int code() {
        return code;
    }
}
