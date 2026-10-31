package io.github.dsheirer.bladerf;

import io.github.dsheirer.bladerf.internal.Coded;

/** bladeRF 2 power monitor register, {@code bladerf_pmic_register}. */
public enum PmicRegister implements Coded {
    CONFIGURATION(0, false),
    VOLTAGE_SHUNT(1, true),
    VOLTAGE_BUS(2, true),
    POWER(3, true),
    CURRENT(4, true),
    CALIBRATION(5, false);

    private final int code;
    private final boolean floatingPoint;

    PmicRegister(int code, boolean floatingPoint) {
        this.code = code;
        this.floatingPoint = floatingPoint;
    }

    @Override
    public int code() {
        return code;
    }

    /** Returns true if the library reports this register as a float, not a {@code uint16_t}. */
    boolean floatingPoint() {
        return floatingPoint;
    }
}
