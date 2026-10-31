package io.github.dsheirer.bladerf;

import java.util.List;

/** Command line tool that prints the library version and the connected devices. */
public final class BladeRfInfo {
    private BladeRfInfo() {
    }

    /** Runs the tool. */
    public static void main(String[] args) {
        System.out.println("libbladeRF " + BladeRf.libraryVersion().describe());
        List<DeviceInfo> devices;
        try {
            devices = BladeRf.listDevices();
        } catch (BladeRfException e) {
            // Machines without USB access, such as containers, end up here.
            System.out.println("Unable to list devices: " + e.getMessage());
            return;
        }
        System.out.println("Devices found: " + devices.size());
        for (DeviceInfo info : devices) {
            System.out.println("Serial " + info.serial());
            try (BladeRfDevice device = BladeRf.open(info)) {
                System.out.println("  Board:     " + device.boardName());
                System.out.println("  Speed:     " + device.speed());
                System.out.println("  Firmware:  " + device.firmwareVersion().describe());
                if (device.isFpgaConfigured()) {
                    System.out.println("  FPGA:      " + device.fpgaVersion().describe());
                    System.out.println("  RX tuning: " + device.frequencyRange(Channel.RX0));
                } else {
                    System.out.println("  FPGA:      not loaded");
                }
            } catch (BladeRfException e) {
                System.out.println("  Unable to open: " + e.getMessage());
            }
        }
    }
}
