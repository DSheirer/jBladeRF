package io.github.dsheirer.bladerf;

/**
 * Internal handles of a device's backend, {@code struct bladerf_backendinfo}.
 *
 * @param handleCount number of backend handles
 * @param handle address of the backend handle
 * @param lockCount number of backend locks
 * @param lock address of the backend lock
 */
public record BackendInfo(int handleCount, long handle, int lockCount, long lock) {
}
