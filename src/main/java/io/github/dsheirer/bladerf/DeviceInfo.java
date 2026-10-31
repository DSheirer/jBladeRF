package io.github.dsheirer.bladerf;

import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.lang.foreign.ValueLayout.JAVA_INT;

import io.github.dsheirer.bladerf.internal.Coded;
import io.github.dsheirer.bladerf.internal.Layouts;
import java.lang.foreign.GroupLayout;
import java.lang.foreign.MemorySegment;

/**
 * Identifies a device, {@code struct bladerf_devinfo}.
 *
 * @param backend backend used to reach the device
 * @param serial serial number
 * @param usbBus USB bus number
 * @param usbAddress USB device address
 * @param instance device instance
 * @param manufacturer manufacturer description
 * @param product product description
 */
public record DeviceInfo(Backend backend, String serial, int usbBus, int usbAddress, long instance,
        String manufacturer, String product) {

    static DeviceInfo read(MemorySegment info) {
        GroupLayout layout = Layouts.DEVINFO;
        return new DeviceInfo(Coded.of(Backend.class, Layouts.i32(info, layout, "backend")),
            Layouts.string(info, layout, "serial"), Layouts.u8(info, layout, "usb_bus"),
            Layouts.u8(info, layout, "usb_addr"), Layouts.u32(info, layout, "instance"),
            Layouts.string(info, layout, "manufacturer"), Layouts.string(info, layout, "product"));
    }

    void write(MemorySegment info) {
        GroupLayout layout = Layouts.DEVINFO;
        info.set(JAVA_INT, Layouts.offset(layout, "backend"), backend.code());
        Layouts.putString(info, layout, "serial", serial);
        info.set(JAVA_BYTE, Layouts.offset(layout, "usb_bus"), (byte) usbBus);
        info.set(JAVA_BYTE, Layouts.offset(layout, "usb_addr"), (byte) usbAddress);
        info.set(JAVA_INT, Layouts.offset(layout, "instance"), (int) instance);
        Layouts.putString(info, layout, "manufacturer", manufacturer);
        Layouts.putString(info, layout, "product", product);
    }
}
