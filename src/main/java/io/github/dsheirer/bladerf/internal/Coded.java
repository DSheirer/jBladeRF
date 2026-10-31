package io.github.dsheirer.bladerf.internal;

/** An enum whose constants have values assigned by a C enumeration. */
public interface Coded {
    /** Returns the native value. */
    int code();

    /**
     * Returns the constant of an enum for a native value.
     *
     * @throws IllegalArgumentException if no constant has that value, which
     *     means that the library is newer than these bindings
     */
    static <E extends Enum<E> & Coded> E of(Class<E> type, int code) {
        for (E constant : type.getEnumConstants()) {
            if (constant.code() == code) {
                return constant;
            }
        }
        throw new IllegalArgumentException("Unknown " + type.getSimpleName() + " value: " + code);
    }
}
