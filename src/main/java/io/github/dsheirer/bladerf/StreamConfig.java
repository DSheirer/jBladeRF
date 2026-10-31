package io.github.dsheirer.bladerf;

/**
 * Buffering of an asynchronous stream.
 *
 * <p>The samples per buffer has to be a multiple of what the firmware's USB
 * transfers hold, and {@code bladerf_init_stream} rejects other values. Multiples
 * of 8192 are valid for every format.
 *
 * @param bufferCount number of buffers to allocate, at least {@code transferCount}
 * @param samplesPerBuffer samples in each buffer
 * @param transferCount number of buffers that may be in flight at once
 */
public record StreamConfig(int bufferCount, int samplesPerBuffer, int transferCount) {
    /** A configuration suitable for sample rates of up to about 40 MHz. */
    public static final StreamConfig DEFAULT = new StreamConfig(32, 32768, 16);

    /** Checks that the values are usable. */
    public StreamConfig {
        if (bufferCount <= 0 || samplesPerBuffer <= 0 || transferCount <= 0) {
            throw new IllegalArgumentException("Stream configuration values have to be positive");
        }
        if (transferCount > bufferCount) {
            throw new IllegalArgumentException("transferCount cannot be more than bufferCount");
        }
    }
}
