package io.github.dsheirer.bladerf;

import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;

import io.github.dsheirer.bladerf.internal.Coded;
import io.github.dsheirer.bladerf.internal.Layouts;
import java.lang.foreign.GroupLayout;
import java.lang.foreign.MemorySegment;

/**
 * Trigger configuration, {@code struct bladerf_trigger}. Get one from
 * {@link BladeRfDevice#initTrigger}, and then choose its role with
 * {@link #withRole}.
 *
 * @param channel channel associated with the trigger
 * @param role role of the device in the trigger chain
 * @param signal pin or signal being used
 * @param options reserved
 */
public record Trigger(Channel channel, TriggerRole role, TriggerSignal signal, long options) {
    private static final Channel[] CHANNELS = Channel.values();

    /** Returns a copy of this trigger with a different role. */
    public Trigger withRole(TriggerRole newRole) {
        return new Trigger(channel, newRole, signal, options);
    }

    static Trigger read(MemorySegment trigger) {
        GroupLayout layout = Layouts.TRIGGER;
        return new Trigger(CHANNELS[Layouts.i32(trigger, layout, "channel")],
            Coded.of(TriggerRole.class, Layouts.i32(trigger, layout, "role")),
            Coded.of(TriggerSignal.class, Layouts.i32(trigger, layout, "signal")),
            Layouts.i64(trigger, layout, "options"));
    }

    void write(MemorySegment trigger) {
        GroupLayout layout = Layouts.TRIGGER;
        trigger.set(JAVA_INT, Layouts.offset(layout, "channel"), channel.code());
        trigger.set(JAVA_INT, Layouts.offset(layout, "role"), role.code());
        trigger.set(JAVA_INT, Layouts.offset(layout, "signal"), signal.code());
        trigger.set(JAVA_LONG, Layouts.offset(layout, "options"), options);
    }
}
