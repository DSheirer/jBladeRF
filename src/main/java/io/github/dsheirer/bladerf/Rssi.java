package io.github.dsheirer.bladerf;

/**
 * Received signal strength from the bladeRF 2 RFIC.
 *
 * @param preamble preamble RSSI in dB
 * @param symbol symbol RSSI in dB
 */
public record Rssi(int preamble, int symbol) {
}
