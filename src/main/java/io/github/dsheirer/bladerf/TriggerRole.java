package io.github.dsheirer.bladerf;

import io.github.dsheirer.bladerf.internal.Coded;

/** Role of a device in a trigger chain, {@code bladerf_trigger_role}. */
public enum TriggerRole implements Coded {
    INVALID(-1),
    DISABLED(0),
    MASTER(1),
    SLAVE(2);

    private final int code;

    TriggerRole(int code) {
        this.code = code;
    }

    @Override
    public int code() {
        return code;
    }
}
