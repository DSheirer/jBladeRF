package io.github.dsheirer.bladerf;

import static java.lang.foreign.ValueLayout.JAVA_SHORT;

import io.github.dsheirer.bladerf.internal.Layouts;
import java.lang.foreign.GroupLayout;
import java.lang.foreign.MemorySegment;

/**
 * bladeRF 1 LMS6002D DC calibration values, {@code struct bladerf_lms_dc_cals}.
 * With {@link BladeRfDevice#setLmsDcCals}, a negative value leaves that module
 * unchanged.
 *
 * @param lpfTuning LPF tuning module
 * @param txLpfI TX LPF I filter
 * @param txLpfQ TX LPF Q filter
 * @param rxLpfI RX LPF I filter
 * @param rxLpfQ RX LPF Q filter
 * @param dcRef RX VGA2 DC reference module
 * @param rxvga2aI RX VGA2, I channel of the first gain stage
 * @param rxvga2aQ RX VGA2, Q channel of the first gain stage
 * @param rxvga2bI RX VGA2, I channel of the second gain stage
 * @param rxvga2bQ RX VGA2, Q channel of the second gain stage
 */
public record LmsDcCals(short lpfTuning, short txLpfI, short txLpfQ, short rxLpfI, short rxLpfQ, short dcRef,
        short rxvga2aI, short rxvga2aQ, short rxvga2bI, short rxvga2bQ) {

    static LmsDcCals read(MemorySegment cals) {
        GroupLayout layout = Layouts.LMS_DC_CALS;
        return new LmsDcCals(Layouts.i16(cals, layout, "lpf_tuning"), Layouts.i16(cals, layout, "tx_lpf_i"),
            Layouts.i16(cals, layout, "tx_lpf_q"), Layouts.i16(cals, layout, "rx_lpf_i"),
            Layouts.i16(cals, layout, "rx_lpf_q"), Layouts.i16(cals, layout, "dc_ref"),
            Layouts.i16(cals, layout, "rxvga2a_i"), Layouts.i16(cals, layout, "rxvga2a_q"),
            Layouts.i16(cals, layout, "rxvga2b_i"), Layouts.i16(cals, layout, "rxvga2b_q"));
    }

    void write(MemorySegment cals) {
        short[] values = {lpfTuning, txLpfI, txLpfQ, rxLpfI, rxLpfQ, dcRef, rxvga2aI, rxvga2aQ, rxvga2bI, rxvga2bQ};
        MemorySegment.copy(values, 0, cals, JAVA_SHORT, 0, values.length);
    }
}
