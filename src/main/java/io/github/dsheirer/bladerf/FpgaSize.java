package io.github.dsheirer.bladerf;

import io.github.dsheirer.bladerf.internal.Coded;

/** FPGA size, {@code bladerf_fpga_size}. */
public enum FpgaSize implements Coded {
    UNKNOWN(0),
    KLE_40(40),
    KLE_115(115),
    A4(49),
    A5(77),
    A9(301);

    private final int code;

    FpgaSize(int code) {
        this.code = code;
    }

    @Override
    public int code() {
        return code;
    }
}
