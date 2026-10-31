package io.github.dsheirer.bladerf;

/** Error codes of the {@code BLADERF_ERR_*} constants. */
public enum BladeRfError {
    UNEXPECTED(-1),
    RANGE(-2),
    INVAL(-3),
    MEM(-4),
    IO(-5),
    TIMEOUT(-6),
    NODEV(-7),
    UNSUPPORTED(-8),
    MISALIGNED(-9),
    CHECKSUM(-10),
    NO_FILE(-11),
    UPDATE_FPGA(-12),
    UPDATE_FW(-13),
    TIME_PAST(-14),
    QUEUE_FULL(-15),
    FPGA_OP(-16),
    PERMISSION(-17),
    WOULD_BLOCK(-18),
    NOT_INIT(-19);

    private final int code;

    BladeRfError(int code) {
        this.code = code;
    }

    /** Returns the native error code. */
    public int code() {
        return code;
    }

    /** Returns the error for a native code, or {@link #UNEXPECTED} if it is unknown. */
    public static BladeRfError fromCode(int code) {
        for (BladeRfError error : values()) {
            if (error.code == code) {
                return error;
            }
        }
        return UNEXPECTED;
    }
}
