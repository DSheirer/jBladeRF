package io.github.dsheirer.bladerf;

/**
 * Receives samples from a stream started with {@link BladeRfDevice#startRx}.
 *
 * <p>Runs on a callback from the driver, which times out if it is held up, so it
 * should return quickly: copy the samples and hand them to another thread for
 * slow work. It must not call {@link BladeRfStream#stop()},
 * {@link BladeRfStream#close()} or {@link BladeRfDevice#close()}; return false to
 * stop instead. If it throws, streaming stops and the exception is available from
 * {@link BladeRfStream#failure()}.
 *
 * <p>While both directions are streaming, it may run on the transmit stream's
 * thread.
 */
@FunctionalInterface
public interface SampleListener {
    /**
     * Handles a block of samples.
     *
     * @return true to keep streaming, false to stop
     */
    boolean onSamples(SampleBuffer buffer);
}
