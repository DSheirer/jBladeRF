package io.github.dsheirer.bladerf;

import io.github.dsheirer.bladerf.internal.Native;

/** Thrown when a libbladeRF function reports an error. */
public class BladeRfException extends Exception {
    private static final long serialVersionUID = 1L;

    private final int code;

    BladeRfException(String function, int code) {
        super(function + " failed: " + Native.errorMessage(code) + " (" + code + ")");
        this.code = code;
    }

    /** Returns the {@code BLADERF_ERR_*} value the native function returned. */
    public int code() {
        return code;
    }

    /** Returns the error as an enum, or {@link BladeRfError#UNEXPECTED} for an unknown code. */
    public BladeRfError error() {
        return BladeRfError.fromCode(code);
    }
}
