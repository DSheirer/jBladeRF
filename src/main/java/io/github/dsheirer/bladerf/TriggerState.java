package io.github.dsheirer.bladerf;

/**
 * State of a trigger.
 *
 * @param armed true if the trigger is armed
 * @param fired true if the trigger has fired
 * @param fireRequested true if a master has been asked to fire
 */
public record TriggerState(boolean armed, boolean fired, boolean fireRequested) {
}
