package io.github.dsheirer.bladerf;

import io.github.dsheirer.bladerf.internal.Coded;

/** Stream direction and channel count, {@code bladerf_channel_layout}. */
public enum ChannelLayout implements Coded {
    RX_X1(0),
    TX_X1(1),
    RX_X2(2),
    TX_X2(3);

    private final int code;

    ChannelLayout(int code) {
        this.code = code;
    }

    @Override
    public int code() {
        return code;
    }
}
