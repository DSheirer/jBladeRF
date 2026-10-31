package io.github.dsheirer.bladerf;

import io.github.dsheirer.bladerf.internal.Coded;

/** Device backend, {@code bladerf_backend}. */
public enum Backend implements Coded {
    ANY(0),
    LINUX(1),
    LIBUSB(2),
    CYPRESS(3),
    DUMMY(100);

    private final int code;

    Backend(int code) {
        this.code = code;
    }

    @Override
    public int code() {
        return code;
    }
}
