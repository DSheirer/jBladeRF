package io.github.dsheirer.bladerf;

import io.github.dsheirer.bladerf.internal.Coded;

/** Source of received samples in the FPGA, {@code bladerf_rx_mux}. */
public enum RxMux implements Coded {
    INVALID(-1),
    BASEBAND(0),
    COUNTER_12BIT(1),
    COUNTER_32BIT(2),
    DIGITAL_LOOPBACK(4);

    private final int code;

    RxMux(int code) {
        this.code = code;
    }

    @Override
    public int code() {
        return code;
    }
}
