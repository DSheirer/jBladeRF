package io.github.dsheirer.bladerf;

import io.github.dsheirer.bladerf.internal.Layouts;
import io.github.dsheirer.bladerf.internal.Native;
import java.lang.foreign.GroupLayout;
import java.lang.foreign.MemorySegment;

/**
 * Version of the library, firmware or FPGA, {@code struct bladerf_version}.
 *
 * @param major major version
 * @param minor minor version
 * @param patch patch version
 * @param describe version with any additional suffix, such as {@code 2.6.0-git-abc123}
 */
public record Version(int major, int minor, int patch, String describe) {
    static Version read(MemorySegment version) {
        GroupLayout layout = Layouts.VERSION;
        return new Version(Layouts.u16(version, layout, "major"), Layouts.u16(version, layout, "minor"),
            Layouts.u16(version, layout, "patch"), Native.cString(Layouts.pointer(version, layout, "describe")));
    }

    @Override
    public String toString() {
        return major + "." + minor + "." + patch;
    }
}
