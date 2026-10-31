package io.github.dsheirer.bladerf;

import io.github.dsheirer.bladerf.internal.Coded;

/** Verbosity of the library's logging to stderr, {@code bladerf_log_level}. */
public enum LogLevel implements Coded {
    VERBOSE(0),
    DEBUG(1),
    INFO(2),
    WARNING(3),
    ERROR(4),
    CRITICAL(5),
    SILENT(6);

    private final int code;

    LogLevel(int code) {
        this.code = code;
    }

    @Override
    public int code() {
        return code;
    }
}
