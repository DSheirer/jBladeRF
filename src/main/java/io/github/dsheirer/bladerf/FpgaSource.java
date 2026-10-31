package io.github.dsheirer.bladerf;

import io.github.dsheirer.bladerf.internal.Coded;

/** Where the FPGA was loaded from, {@code bladerf_fpga_source}. */
public enum FpgaSource implements Coded {
    UNKNOWN(0),
    FLASH(1),
    HOST(2);

    private final int code;

    FpgaSource(int code) {
        this.code = code;
    }

    @Override
    public int code() {
        return code;
    }
}
