package io.github.dsheirer.bladerf;

/**
 * A loopback mode that a device supports.
 *
 * @param name name of the mode
 * @param mode the mode
 */
public record LoopbackInfo(String name, Loopback mode) {
}
