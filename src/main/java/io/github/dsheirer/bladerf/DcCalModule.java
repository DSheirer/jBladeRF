package io.github.dsheirer.bladerf;

import io.github.dsheirer.bladerf.internal.Coded;

/** bladeRF 1 DC calibration module, {@code bladerf_cal_module}. */
public enum DcCalModule implements Coded {
    INVALID(-1),
    LPF_TUNING(0),
    TX_LPF(1),
    RX_LPF(2),
    RXVGA2(3);

    private final int code;

    DcCalModule(int code) {
        this.code = code;
    }

    @Override
    public int code() {
        return code;
    }
}
