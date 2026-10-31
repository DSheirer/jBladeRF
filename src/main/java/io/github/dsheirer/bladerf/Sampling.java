package io.github.dsheirer.bladerf;

import io.github.dsheirer.bladerf.internal.Coded;

/** bladeRF 1 ADC sampling input, {@code bladerf_sampling}. */
public enum Sampling implements Coded {
    UNKNOWN(0),
    INTERNAL(1),
    EXTERNAL(2);

    private final int code;

    Sampling(int code) {
        this.code = code;
    }

    @Override
    public int code() {
        return code;
    }
}
