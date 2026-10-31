package io.github.dsheirer.bladerf;

import io.github.dsheirer.bladerf.internal.Coded;

/** USB bus speed, {@code bladerf_dev_speed}. */
public enum DeviceSpeed implements Coded {
    UNKNOWN(0),
    HIGH(1),
    SUPER(2);

    private final int code;

    DeviceSpeed(int code) {
        this.code = code;
    }

    @Override
    public int code() {
        return code;
    }
}
