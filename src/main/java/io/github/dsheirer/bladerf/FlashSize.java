package io.github.dsheirer.bladerf;

/**
 * Size of a device's SPI flash.
 *
 * @param bytes size in bytes
 * @param guessed true if the library could not read the size and assumed it
 */
public record FlashSize(long bytes, boolean guessed) {
}
