package io.github.dsheirer.bladerf;

/**
 * A gain mode that a channel supports.
 *
 * @param name name of the mode
 * @param mode the mode
 */
public record GainModeInfo(String name, GainMode mode) {
}
