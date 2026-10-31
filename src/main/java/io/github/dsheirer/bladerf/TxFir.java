package io.github.dsheirer.bladerf;

import io.github.dsheirer.bladerf.internal.Coded;

/** bladeRF 2 RFIC transmit FIR filter, {@code bladerf_rfic_txfir}. */
public enum TxFir implements Coded {
    BYPASS(0),
    CUSTOM(1),
    INT1(2),
    INT2(3),
    INT4(4);

    private final int code;

    TxFir(int code) {
        this.code = code;
    }

    @Override
    public int code() {
        return code;
    }
}
