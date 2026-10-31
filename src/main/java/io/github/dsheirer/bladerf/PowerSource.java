package io.github.dsheirer.bladerf;

import io.github.dsheirer.bladerf.internal.Coded;

/** bladeRF 2 power source, {@code bladerf_power_sources}. */
public enum PowerSource implements Coded {
    UNKNOWN(0),
    DC(1),
    USB_VBUS(2);

    private final int code;

    PowerSource(int code) {
        this.code = code;
    }

    @Override
    public int code() {
        return code;
    }
}
