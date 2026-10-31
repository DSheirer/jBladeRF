package io.github.dsheirer.bladerf;

import io.github.dsheirer.bladerf.internal.Coded;

/** Automatic gain control mode, {@code bladerf_gain_mode}. */
public enum GainMode implements Coded {
    DEFAULT(0),
    MANUAL(1),
    FAST_ATTACK_AGC(2),
    SLOW_ATTACK_AGC(3),
    HYBRID_AGC(4);

    private final int code;

    GainMode(int code) {
        this.code = code;
    }

    @Override
    public int code() {
        return code;
    }
}
