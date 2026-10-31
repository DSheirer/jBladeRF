package io.github.dsheirer.bladerf;

import static java.lang.foreign.ValueLayout.JAVA_DOUBLE;
import static java.lang.foreign.ValueLayout.JAVA_LONG;

import io.github.dsheirer.bladerf.internal.Layouts;
import io.github.dsheirer.bladerf.internal.Native;
import java.lang.foreign.GroupLayout;
import java.lang.foreign.MemorySegment;
import java.util.ArrayList;
import java.util.List;

/**
 * A copy of a channel's loaded gain calibration table, {@code struct bladerf_gain_cal_tbl}.
 *
 * @param version table format version
 * @param enabled true if gain calibration is enabled
 * @param startFrequency start frequency in Hz
 * @param stopFrequency stop frequency in Hz
 * @param gainTarget compensated gain
 * @param filePath file the table was loaded from
 * @param entries calibration entries sorted by frequency
 */
public record GainCalibration(Version version, boolean enabled, long startFrequency, long stopFrequency,
        int gainTarget, String filePath, List<Entry> entries) {

    /**
     * One point of a table, {@code struct bladerf_gain_cal_entry}.
     *
     * @param frequency frequency in Hz
     * @param correction gain correction factor
     */
    public record Entry(long frequency, double correction) {
    }

    static GainCalibration read(MemorySegment table) {
        GroupLayout layout = Layouts.GAIN_CAL_TBL;
        int count = Math.toIntExact(Layouts.u32(table, layout, "n_entries"));
        long entrySize = Layouts.GAIN_CAL_ENTRY.byteSize();
        MemorySegment nativeEntries = Layouts.pointer(table, layout, "entries").reinterpret(count * entrySize);
        List<Entry> entries = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            entries.add(new Entry(
                nativeEntries.get(JAVA_LONG, i * entrySize + Layouts.offset(Layouts.GAIN_CAL_ENTRY, "freq")),
                nativeEntries.get(JAVA_DOUBLE, i * entrySize + Layouts.offset(Layouts.GAIN_CAL_ENTRY, "gain_corr"))));
        }
        return new GainCalibration(Version.read(Layouts.slice(table, layout, "version")),
            Layouts.u8(table, layout, "enabled") != 0, Layouts.i64(table, layout, "start_freq"),
            Layouts.i64(table, layout, "stop_freq"), Layouts.i32(table, layout, "gain_target"),
            Native.cString(Layouts.pointer(table, layout, "file_path")), List.copyOf(entries));
    }
}
