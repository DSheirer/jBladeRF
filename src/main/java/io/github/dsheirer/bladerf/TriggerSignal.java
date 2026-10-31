package io.github.dsheirer.bladerf;

import io.github.dsheirer.bladerf.internal.Coded;

/** Trigger pin or signal, {@code bladerf_trigger_signal}. */
public enum TriggerSignal implements Coded {
    INVALID(-1),
    J71_4(0),
    J51_1(1),
    MINI_EXP_1(2),
    USER_0(128),
    USER_1(129),
    USER_2(130),
    USER_3(131),
    USER_4(132),
    USER_5(133),
    USER_6(134),
    USER_7(135);

    private final int code;

    TriggerSignal(int code) {
        this.code = code;
    }

    @Override
    public int code() {
        return code;
    }
}
