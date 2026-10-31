package io.github.dsheirer.bladerf;

/** One radio channel, as built by the {@code BLADERF_CHANNEL_RX} and {@code BLADERF_CHANNEL_TX} macros. */
public enum Channel {
    RX0(0, Direction.RX),
    TX0(1, Direction.TX),
    RX1(2, Direction.RX),
    TX1(3, Direction.TX);

    private final int code;
    private final Direction direction;

    Channel(int code, Direction direction) {
        this.code = code;
        this.direction = direction;
    }

    /** Returns the native {@code bladerf_channel} value. */
    public int code() {
        return code;
    }

    /** Returns whether this channel receives or transmits. */
    public Direction direction() {
        return direction;
    }

    /** Returns the channel's index within its direction, starting from 0. */
    public int index() {
        return code >> 1;
    }
}
