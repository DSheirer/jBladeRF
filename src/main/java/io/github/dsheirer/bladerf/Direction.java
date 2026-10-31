package io.github.dsheirer.bladerf;

/** Stream direction, {@code bladerf_direction}. */
public enum Direction {
    RX,
    TX;

    int code() {
        return ordinal();
    }
}
