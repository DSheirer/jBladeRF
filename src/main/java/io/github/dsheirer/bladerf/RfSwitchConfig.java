package io.github.dsheirer.bladerf;

import io.github.dsheirer.bladerf.internal.Layouts;
import java.lang.foreign.GroupLayout;
import java.lang.foreign.MemorySegment;

/**
 * bladeRF 2 RF switch settings, {@code bladerf_rf_switch_config}.
 *
 * @param tx1RficPort active TX1 output from the RFIC
 * @param tx1SpdtPort RF switch configuration for the TX1 path
 * @param tx2RficPort active TX2 output from the RFIC
 * @param tx2SpdtPort RF switch configuration for the TX2 path
 * @param rx1RficPort active RX1 input to the RFIC
 * @param rx1SpdtPort RF switch configuration for the RX1 path
 * @param rx2RficPort active RX2 input to the RFIC
 * @param rx2SpdtPort RF switch configuration for the RX2 path
 */
public record RfSwitchConfig(long tx1RficPort, long tx1SpdtPort, long tx2RficPort, long tx2SpdtPort,
        long rx1RficPort, long rx1SpdtPort, long rx2RficPort, long rx2SpdtPort) {

    static RfSwitchConfig read(MemorySegment config) {
        GroupLayout layout = Layouts.RF_SWITCH_CONFIG;
        return new RfSwitchConfig(Layouts.u32(config, layout, "tx1_rfic_port"),
            Layouts.u32(config, layout, "tx1_spdt_port"), Layouts.u32(config, layout, "tx2_rfic_port"),
            Layouts.u32(config, layout, "tx2_spdt_port"), Layouts.u32(config, layout, "rx1_rfic_port"),
            Layouts.u32(config, layout, "rx1_spdt_port"), Layouts.u32(config, layout, "rx2_rfic_port"),
            Layouts.u32(config, layout, "rx2_spdt_port"));
    }
}
