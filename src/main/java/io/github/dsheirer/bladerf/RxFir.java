package io.github.dsheirer.bladerf;

import io.github.dsheirer.bladerf.internal.Coded;

/** bladeRF 2 RFIC receive FIR filter, {@code bladerf_rfic_rxfir}. */
public enum RxFir implements Coded {
    BYPASS(0),
    CUSTOM(1),
    DEC1(2),
    DEC2(3),
    DEC4(4);

    private final int code;

    RxFir(int code) {
        this.code = code;
    }

    @Override
    public int code() {
        return code;
    }
}
