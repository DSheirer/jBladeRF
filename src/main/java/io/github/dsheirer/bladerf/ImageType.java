package io.github.dsheirer.bladerf;

import io.github.dsheirer.bladerf.internal.Coded;

/** Contents of a flash image, {@code bladerf_image_type}. */
public enum ImageType implements Coded {
    INVALID(-1),
    RAW(0),
    FIRMWARE(1),
    FPGA_40KLE(2),
    FPGA_115KLE(3),
    FPGA_A4(4),
    FPGA_A9(5),
    CALIBRATION(6),
    RX_DC_CAL(7),
    TX_DC_CAL(8),
    RX_IQ_CAL(9),
    TX_IQ_CAL(10),
    FPGA_A5(11),
    GAIN_CAL(12);

    private final int code;

    ImageType(int code) {
        this.code = code;
    }

    @Override
    public int code() {
        return code;
    }
}
