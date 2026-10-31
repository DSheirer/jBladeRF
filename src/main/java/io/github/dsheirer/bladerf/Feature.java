package io.github.dsheirer.bladerf;

import io.github.dsheirer.bladerf.internal.Coded;

/** Optional device feature, {@code bladerf_feature}. */
public enum Feature implements Coded {
    DEFAULT(0),
    OVERSAMPLE(1);

    private final int code;

    Feature(int code) {
        this.code = code;
    }

    @Override
    public int code() {
        return code;
    }
}
