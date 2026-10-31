package io.github.dsheirer.bladerf;

import io.github.dsheirer.bladerf.internal.Coded;

/** Internal loopback path, {@code bladerf_loopback}. */
public enum Loopback implements Coded {
    NONE(0),
    FIRMWARE(1),
    BB_TXLPF_RXVGA2(2),
    BB_TXVGA1_RXVGA2(3),
    BB_TXLPF_RXLPF(4),
    BB_TXVGA1_RXLPF(5),
    RF_LNA1(6),
    RF_LNA2(7),
    RF_LNA3(8),
    RFIC_BIST(9);

    private final int code;

    Loopback(int code) {
        this.code = code;
    }

    @Override
    public int code() {
        return code;
    }
}
