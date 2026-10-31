package io.github.dsheirer.bladerf;

import io.github.dsheirer.bladerf.internal.Coded;

/** bladeRF 1 SMB clock port mode, {@code bladerf_smb_mode}. */
public enum SmbMode implements Coded {
    INVALID(-1),
    DISABLED(0),
    OUTPUT(1),
    INPUT(2),
    UNAVAILABLE(3);

    private final int code;

    SmbMode(int code) {
        this.code = code;
    }

    @Override
    public int code() {
        return code;
    }
}
