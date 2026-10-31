package io.github.dsheirer.bladerf.internal;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_BOOLEAN;
import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;
import static java.lang.foreign.ValueLayout.JAVA_SHORT;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.invoke.MethodHandle;

/**
 * Downcall handles for the public functions of libbladeRF.
 *
 * <p>Deprecated upstream functions are intentionally absent: each has a
 * replacement here. {@code native/check-bindings.sh} compares this list against
 * the libbladeRF headers.
 *
 * <p>All of the handles assume a 64-bit platform, which is all that jBladeRF
 * supports, so {@code size_t} is {@link java.lang.foreign.ValueLayout#JAVA_LONG}.
 * The API has no {@code long} parameters, the one C type whose size differs
 * between the supported platforms.
 */
public final class Native {
    private static final Linker LINKER = Linker.nativeLinker();
    private static final SymbolLookup LOOKUP = NativeLibraryLoader.load();

    public static final MethodHandle OPEN = call("bladerf_open", ADDRESS, ADDRESS);
    public static final MethodHandle CLOSE = voidCall("bladerf_close", ADDRESS);
    public static final MethodHandle OPEN_WITH_DEVINFO = call("bladerf_open_with_devinfo", ADDRESS, ADDRESS);
    public static final MethodHandle GET_DEVICE_LIST = call("bladerf_get_device_list", ADDRESS);
    public static final MethodHandle FREE_DEVICE_LIST = voidCall("bladerf_free_device_list", ADDRESS);
    public static final MethodHandle INIT_DEVINFO = voidCall("bladerf_init_devinfo", ADDRESS);
    public static final MethodHandle GET_DEVINFO = call("bladerf_get_devinfo", ADDRESS, ADDRESS);
    public static final MethodHandle GET_BACKENDINFO = call("bladerf_get_backendinfo", ADDRESS, ADDRESS);
    public static final MethodHandle GET_DEVINFO_FROM_STR = call("bladerf_get_devinfo_from_str", ADDRESS, ADDRESS);
    public static final MethodHandle DEVINFO_MATCHES = call(JAVA_BOOLEAN, "bladerf_devinfo_matches", ADDRESS, ADDRESS);
    public static final MethodHandle DEVSTR_MATCHES = call(JAVA_BOOLEAN, "bladerf_devstr_matches", ADDRESS, ADDRESS);
    public static final MethodHandle BACKEND_STR = call(ADDRESS, "bladerf_backend_str", JAVA_INT);
    public static final MethodHandle SET_USB_RESET_ON_OPEN = voidCall("bladerf_set_usb_reset_on_open", JAVA_BOOLEAN);
    public static final MethodHandle GET_SERIAL_STRUCT = call("bladerf_get_serial_struct", ADDRESS, ADDRESS);
    public static final MethodHandle GET_FPGA_SIZE = call("bladerf_get_fpga_size", ADDRESS, ADDRESS);
    public static final MethodHandle GET_FPGA_BYTES = call("bladerf_get_fpga_bytes", ADDRESS, ADDRESS);
    public static final MethodHandle GET_FLASH_SIZE = call("bladerf_get_flash_size", ADDRESS, ADDRESS, ADDRESS);
    public static final MethodHandle FW_VERSION = call("bladerf_fw_version", ADDRESS, ADDRESS);
    public static final MethodHandle IS_FPGA_CONFIGURED = call("bladerf_is_fpga_configured", ADDRESS);
    public static final MethodHandle FPGA_VERSION = call("bladerf_fpga_version", ADDRESS, ADDRESS);
    public static final MethodHandle GET_FPGA_SOURCE = call("bladerf_get_fpga_source", ADDRESS, ADDRESS);
    public static final MethodHandle DEVICE_SPEED = call("bladerf_device_speed", ADDRESS);
    public static final MethodHandle GET_BOARD_NAME = call(ADDRESS, "bladerf_get_board_name", ADDRESS);
    public static final MethodHandle GET_CHANNEL_COUNT =
        call(JAVA_LONG, "bladerf_get_channel_count", ADDRESS, JAVA_INT);
    public static final MethodHandle SET_GAIN = call("bladerf_set_gain", ADDRESS, JAVA_INT, JAVA_INT);
    public static final MethodHandle GET_GAIN = call("bladerf_get_gain", ADDRESS, JAVA_INT, ADDRESS);
    public static final MethodHandle SET_GAIN_MODE = call("bladerf_set_gain_mode", ADDRESS, JAVA_INT, JAVA_INT);
    public static final MethodHandle GET_GAIN_MODE = call("bladerf_get_gain_mode", ADDRESS, JAVA_INT, ADDRESS);
    public static final MethodHandle GET_GAIN_MODES = call("bladerf_get_gain_modes", ADDRESS, JAVA_INT, ADDRESS);
    public static final MethodHandle GET_GAIN_RANGE = call("bladerf_get_gain_range", ADDRESS, JAVA_INT, ADDRESS);
    public static final MethodHandle SET_GAIN_STAGE =
        call("bladerf_set_gain_stage", ADDRESS, JAVA_INT, ADDRESS, JAVA_INT);
    public static final MethodHandle GET_GAIN_STAGE =
        call("bladerf_get_gain_stage", ADDRESS, JAVA_INT, ADDRESS, ADDRESS);
    public static final MethodHandle GET_GAIN_STAGE_RANGE =
        call("bladerf_get_gain_stage_range", ADDRESS, JAVA_INT, ADDRESS, ADDRESS);
    public static final MethodHandle GET_GAIN_STAGES =
        call("bladerf_get_gain_stages", ADDRESS, JAVA_INT, ADDRESS, JAVA_LONG);
    public static final MethodHandle SET_SAMPLE_RATE =
        call("bladerf_set_sample_rate", ADDRESS, JAVA_INT, JAVA_INT, ADDRESS);
    public static final MethodHandle SET_RATIONAL_SAMPLE_RATE =
        call("bladerf_set_rational_sample_rate", ADDRESS, JAVA_INT, ADDRESS, ADDRESS);
    public static final MethodHandle GET_SAMPLE_RATE = call("bladerf_get_sample_rate", ADDRESS, JAVA_INT, ADDRESS);
    public static final MethodHandle GET_SAMPLE_RATE_RANGE =
        call("bladerf_get_sample_rate_range", ADDRESS, JAVA_INT, ADDRESS);
    public static final MethodHandle GET_RATIONAL_SAMPLE_RATE =
        call("bladerf_get_rational_sample_rate", ADDRESS, JAVA_INT, ADDRESS);
    public static final MethodHandle SET_BANDWIDTH =
        call("bladerf_set_bandwidth", ADDRESS, JAVA_INT, JAVA_INT, ADDRESS);
    public static final MethodHandle GET_BANDWIDTH = call("bladerf_get_bandwidth", ADDRESS, JAVA_INT, ADDRESS);
    public static final MethodHandle GET_BANDWIDTH_RANGE =
        call("bladerf_get_bandwidth_range", ADDRESS, JAVA_INT, ADDRESS);
    public static final MethodHandle SELECT_BAND = call("bladerf_select_band", ADDRESS, JAVA_INT, JAVA_LONG);
    public static final MethodHandle SET_FREQUENCY = call("bladerf_set_frequency", ADDRESS, JAVA_INT, JAVA_LONG);
    public static final MethodHandle GET_FREQUENCY = call("bladerf_get_frequency", ADDRESS, JAVA_INT, ADDRESS);
    public static final MethodHandle GET_FREQUENCY_RANGE =
        call("bladerf_get_frequency_range", ADDRESS, JAVA_INT, ADDRESS);
    public static final MethodHandle GET_LOOPBACK_MODES = call("bladerf_get_loopback_modes", ADDRESS, ADDRESS);
    public static final MethodHandle IS_LOOPBACK_MODE_SUPPORTED =
        call(JAVA_BOOLEAN, "bladerf_is_loopback_mode_supported", ADDRESS, JAVA_INT);
    public static final MethodHandle SET_LOOPBACK = call("bladerf_set_loopback", ADDRESS, JAVA_INT);
    public static final MethodHandle GET_LOOPBACK = call("bladerf_get_loopback", ADDRESS, ADDRESS);
    public static final MethodHandle TRIGGER_INIT = call("bladerf_trigger_init", ADDRESS, JAVA_INT, JAVA_INT, ADDRESS);
    public static final MethodHandle TRIGGER_ARM =
        call("bladerf_trigger_arm", ADDRESS, ADDRESS, JAVA_BOOLEAN, JAVA_LONG, JAVA_LONG);
    public static final MethodHandle TRIGGER_FIRE = call("bladerf_trigger_fire", ADDRESS, ADDRESS);
    public static final MethodHandle TRIGGER_STATE =
        call("bladerf_trigger_state", ADDRESS, ADDRESS, ADDRESS, ADDRESS, ADDRESS, ADDRESS, ADDRESS);
    public static final MethodHandle SET_RX_MUX = call("bladerf_set_rx_mux", ADDRESS, JAVA_INT);
    public static final MethodHandle GET_RX_MUX = call("bladerf_get_rx_mux", ADDRESS, ADDRESS);
    public static final MethodHandle SCHEDULE_RETUNE =
        call("bladerf_schedule_retune", ADDRESS, JAVA_INT, JAVA_LONG, JAVA_LONG, ADDRESS);
    public static final MethodHandle CANCEL_SCHEDULED_RETUNES =
        call("bladerf_cancel_scheduled_retunes", ADDRESS, JAVA_INT);
    public static final MethodHandle GET_QUICK_TUNE = call("bladerf_get_quick_tune", ADDRESS, JAVA_INT, ADDRESS);
    public static final MethodHandle PRINT_QUICK_TUNE = call("bladerf_print_quick_tune", ADDRESS, ADDRESS);
    public static final MethodHandle SET_CORRECTION =
        call("bladerf_set_correction", ADDRESS, JAVA_INT, JAVA_INT, JAVA_SHORT);
    public static final MethodHandle GET_CORRECTION =
        call("bladerf_get_correction", ADDRESS, JAVA_INT, JAVA_INT, ADDRESS);
    public static final MethodHandle FORMAT_TO_STRING = call(ADDRESS, "bladerf_format_to_string", JAVA_INT);
    public static final MethodHandle INTERLEAVE_STREAM_BUFFER =
        call("bladerf_interleave_stream_buffer", JAVA_INT, JAVA_INT, JAVA_INT, ADDRESS);
    public static final MethodHandle DEINTERLEAVE_STREAM_BUFFER =
        call("bladerf_deinterleave_stream_buffer", JAVA_INT, JAVA_INT, JAVA_INT, ADDRESS);
    public static final MethodHandle ENABLE_MODULE = call("bladerf_enable_module", ADDRESS, JAVA_INT, JAVA_BOOLEAN);
    public static final MethodHandle GET_TIMESTAMP = call("bladerf_get_timestamp", ADDRESS, JAVA_INT, ADDRESS);
    public static final MethodHandle SYNC_CONFIG =
        call("bladerf_sync_config", ADDRESS, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT);
    public static final MethodHandle SYNC_TX = call("bladerf_sync_tx", ADDRESS, ADDRESS, JAVA_INT, ADDRESS, JAVA_INT);
    public static final MethodHandle SYNC_RX = call("bladerf_sync_rx", ADDRESS, ADDRESS, JAVA_INT, ADDRESS, JAVA_INT);
    public static final MethodHandle INIT_STREAM =
        call("bladerf_init_stream", ADDRESS, ADDRESS, ADDRESS, ADDRESS, JAVA_LONG, JAVA_INT, JAVA_LONG, JAVA_LONG,
            ADDRESS);
    public static final MethodHandle STREAM = call("bladerf_stream", ADDRESS, JAVA_INT);
    public static final MethodHandle SUBMIT_STREAM_BUFFER =
        call("bladerf_submit_stream_buffer", ADDRESS, ADDRESS, JAVA_INT);
    public static final MethodHandle SUBMIT_STREAM_BUFFER_NB =
        call("bladerf_submit_stream_buffer_nb", ADDRESS, ADDRESS);
    public static final MethodHandle DEINIT_STREAM = voidCall("bladerf_deinit_stream", ADDRESS);
    public static final MethodHandle SET_STREAM_TIMEOUT =
        call("bladerf_set_stream_timeout", ADDRESS, JAVA_INT, JAVA_INT);
    public static final MethodHandle GET_STREAM_TIMEOUT =
        call("bladerf_get_stream_timeout", ADDRESS, JAVA_INT, ADDRESS);
    public static final MethodHandle FLASH_FIRMWARE = call("bladerf_flash_firmware", ADDRESS, ADDRESS);
    public static final MethodHandle LOAD_FPGA = call("bladerf_load_fpga", ADDRESS, ADDRESS);
    public static final MethodHandle FLASH_FPGA = call("bladerf_flash_fpga", ADDRESS, ADDRESS);
    public static final MethodHandle ERASE_STORED_FPGA = call("bladerf_erase_stored_fpga", ADDRESS);
    public static final MethodHandle DEVICE_RESET = call("bladerf_device_reset", ADDRESS);
    public static final MethodHandle GET_FW_LOG = call("bladerf_get_fw_log", ADDRESS, ADDRESS);
    public static final MethodHandle JUMP_TO_BOOTLOADER = call("bladerf_jump_to_bootloader", ADDRESS);
    public static final MethodHandle GET_BOOTLOADER_LIST = call("bladerf_get_bootloader_list", ADDRESS);
    public static final MethodHandle LOAD_FW_FROM_BOOTLOADER =
        call("bladerf_load_fw_from_bootloader", ADDRESS, JAVA_INT, JAVA_BYTE, JAVA_BYTE, ADDRESS);
    public static final MethodHandle ALLOC_IMAGE =
        call(ADDRESS, "bladerf_alloc_image", ADDRESS, JAVA_INT, JAVA_INT, JAVA_INT);
    public static final MethodHandle ALLOC_CAL_IMAGE =
        call(ADDRESS, "bladerf_alloc_cal_image", ADDRESS, JAVA_INT, JAVA_SHORT);
    public static final MethodHandle FREE_IMAGE = voidCall("bladerf_free_image", ADDRESS);
    public static final MethodHandle IMAGE_PRINT_METADATA = call("bladerf_image_print_metadata", ADDRESS);
    public static final MethodHandle IMAGE_TYPE_TO_STRING = call(ADDRESS, "bladerf_image_type_to_string", JAVA_INT);
    public static final MethodHandle IMAGE_WRITE = call("bladerf_image_write", ADDRESS, ADDRESS, ADDRESS);
    public static final MethodHandle IMAGE_READ = call("bladerf_image_read", ADDRESS, ADDRESS);
    public static final MethodHandle SET_VCTCXO_TAMER_MODE = call("bladerf_set_vctcxo_tamer_mode", ADDRESS, JAVA_INT);
    public static final MethodHandle GET_VCTCXO_TAMER_MODE = call("bladerf_get_vctcxo_tamer_mode", ADDRESS, ADDRESS);
    public static final MethodHandle GET_VCTCXO_TRIM = call("bladerf_get_vctcxo_trim", ADDRESS, ADDRESS);
    public static final MethodHandle TRIM_DAC_WRITE = call("bladerf_trim_dac_write", ADDRESS, JAVA_SHORT);
    public static final MethodHandle TRIM_DAC_READ = call("bladerf_trim_dac_read", ADDRESS, ADDRESS);
    public static final MethodHandle SET_TUNING_MODE = call("bladerf_set_tuning_mode", ADDRESS, JAVA_INT);
    public static final MethodHandle GET_TUNING_MODE = call("bladerf_get_tuning_mode", ADDRESS, ADDRESS);
    public static final MethodHandle READ_TRIGGER = call("bladerf_read_trigger", ADDRESS, JAVA_INT, JAVA_INT, ADDRESS);
    public static final MethodHandle WRITE_TRIGGER =
        call("bladerf_write_trigger", ADDRESS, JAVA_INT, JAVA_INT, JAVA_BYTE);
    public static final MethodHandle WISHBONE_MASTER_READ =
        call("bladerf_wishbone_master_read", ADDRESS, JAVA_INT, ADDRESS);
    public static final MethodHandle WISHBONE_MASTER_WRITE =
        call("bladerf_wishbone_master_write", ADDRESS, JAVA_INT, JAVA_INT);
    public static final MethodHandle CONFIG_GPIO_READ = call("bladerf_config_gpio_read", ADDRESS, ADDRESS);
    public static final MethodHandle CONFIG_GPIO_WRITE = call("bladerf_config_gpio_write", ADDRESS, JAVA_INT);
    public static final MethodHandle ERASE_FLASH = call("bladerf_erase_flash", ADDRESS, JAVA_INT, JAVA_INT);
    public static final MethodHandle ERASE_FLASH_BYTES = call("bladerf_erase_flash_bytes", ADDRESS, JAVA_INT, JAVA_INT);
    public static final MethodHandle READ_FLASH = call("bladerf_read_flash", ADDRESS, ADDRESS, JAVA_INT, JAVA_INT);
    public static final MethodHandle READ_FLASH_BYTES =
        call("bladerf_read_flash_bytes", ADDRESS, ADDRESS, JAVA_INT, JAVA_INT);
    public static final MethodHandle WRITE_FLASH = call("bladerf_write_flash", ADDRESS, ADDRESS, JAVA_INT, JAVA_INT);
    public static final MethodHandle WRITE_FLASH_BYTES =
        call("bladerf_write_flash_bytes", ADDRESS, ADDRESS, JAVA_INT, JAVA_INT);
    public static final MethodHandle LOCK_OTP = call("bladerf_lock_otp", ADDRESS);
    public static final MethodHandle READ_OTP = call("bladerf_read_otp", ADDRESS, ADDRESS);
    public static final MethodHandle WRITE_OTP = call("bladerf_write_otp", ADDRESS, ADDRESS);
    public static final MethodHandle SET_RF_PORT = call("bladerf_set_rf_port", ADDRESS, JAVA_INT, ADDRESS);
    public static final MethodHandle GET_RF_PORT = call("bladerf_get_rf_port", ADDRESS, JAVA_INT, ADDRESS);
    public static final MethodHandle GET_RF_PORTS = call("bladerf_get_rf_ports", ADDRESS, JAVA_INT, ADDRESS, JAVA_INT);
    public static final MethodHandle ENABLE_FEATURE = call("bladerf_enable_feature", ADDRESS, JAVA_INT, JAVA_BOOLEAN);
    public static final MethodHandle GET_FEATURE = call("bladerf_get_feature", ADDRESS, ADDRESS);
    public static final MethodHandle LOAD_GAIN_CALIBRATION =
        call("bladerf_load_gain_calibration", ADDRESS, JAVA_INT, ADDRESS);
    public static final MethodHandle PRINT_GAIN_CALIBRATION =
        call("bladerf_print_gain_calibration", ADDRESS, JAVA_INT, JAVA_BOOLEAN);
    public static final MethodHandle ENABLE_GAIN_CALIBRATION =
        call("bladerf_enable_gain_calibration", ADDRESS, JAVA_INT, JAVA_BOOLEAN);
    public static final MethodHandle GET_GAIN_CALIBRATION =
        call("bladerf_get_gain_calibration", ADDRESS, JAVA_INT, ADDRESS);
    public static final MethodHandle GET_GAIN_TARGET = call("bladerf_get_gain_target", ADDRESS, JAVA_INT, ADDRESS);
    public static final MethodHandle EXPANSION_ATTACH = call("bladerf_expansion_attach", ADDRESS, JAVA_INT);
    public static final MethodHandle EXPANSION_GET_ATTACHED = call("bladerf_expansion_get_attached", ADDRESS, ADDRESS);
    public static final MethodHandle LOG_SET_VERBOSITY = voidCall("bladerf_log_set_verbosity", JAVA_INT);
    public static final MethodHandle VERSION = voidCall("bladerf_version", ADDRESS);
    public static final MethodHandle STRERROR = call(ADDRESS, "bladerf_strerror", JAVA_INT);
    public static final MethodHandle SET_SAMPLING = call("bladerf_set_sampling", ADDRESS, JAVA_INT);
    public static final MethodHandle GET_SAMPLING = call("bladerf_get_sampling", ADDRESS, ADDRESS);
    public static final MethodHandle SET_LPF_MODE = call("bladerf_set_lpf_mode", ADDRESS, JAVA_INT, JAVA_INT);
    public static final MethodHandle GET_LPF_MODE = call("bladerf_get_lpf_mode", ADDRESS, JAVA_INT, ADDRESS);
    public static final MethodHandle SET_SMB_MODE = call("bladerf_set_smb_mode", ADDRESS, JAVA_INT);
    public static final MethodHandle GET_SMB_MODE = call("bladerf_get_smb_mode", ADDRESS, ADDRESS);
    public static final MethodHandle SET_RATIONAL_SMB_FREQUENCY =
        call("bladerf_set_rational_smb_frequency", ADDRESS, ADDRESS, ADDRESS);
    public static final MethodHandle SET_SMB_FREQUENCY = call("bladerf_set_smb_frequency", ADDRESS, JAVA_INT, ADDRESS);
    public static final MethodHandle GET_RATIONAL_SMB_FREQUENCY =
        call("bladerf_get_rational_smb_frequency", ADDRESS, ADDRESS);
    public static final MethodHandle GET_SMB_FREQUENCY = call("bladerf_get_smb_frequency", ADDRESS, ADDRESS);
    public static final MethodHandle EXPANSION_GPIO_READ = call("bladerf_expansion_gpio_read", ADDRESS, ADDRESS);
    public static final MethodHandle EXPANSION_GPIO_WRITE = call("bladerf_expansion_gpio_write", ADDRESS, JAVA_INT);
    public static final MethodHandle EXPANSION_GPIO_MASKED_WRITE =
        call("bladerf_expansion_gpio_masked_write", ADDRESS, JAVA_INT, JAVA_INT);
    public static final MethodHandle EXPANSION_GPIO_DIR_READ =
        call("bladerf_expansion_gpio_dir_read", ADDRESS, ADDRESS);
    public static final MethodHandle EXPANSION_GPIO_DIR_WRITE =
        call("bladerf_expansion_gpio_dir_write", ADDRESS, JAVA_INT);
    public static final MethodHandle EXPANSION_GPIO_DIR_MASKED_WRITE =
        call("bladerf_expansion_gpio_dir_masked_write", ADDRESS, JAVA_INT, JAVA_INT);
    public static final MethodHandle XB200_SET_FILTERBANK =
        call("bladerf_xb200_set_filterbank", ADDRESS, JAVA_INT, JAVA_INT);
    public static final MethodHandle XB200_GET_FILTERBANK =
        call("bladerf_xb200_get_filterbank", ADDRESS, JAVA_INT, ADDRESS);
    public static final MethodHandle XB200_SET_PATH = call("bladerf_xb200_set_path", ADDRESS, JAVA_INT, JAVA_INT);
    public static final MethodHandle XB200_GET_PATH = call("bladerf_xb200_get_path", ADDRESS, JAVA_INT, ADDRESS);
    public static final MethodHandle XB300_SET_TRX = call("bladerf_xb300_set_trx", ADDRESS, JAVA_INT);
    public static final MethodHandle XB300_GET_TRX = call("bladerf_xb300_get_trx", ADDRESS, ADDRESS);
    public static final MethodHandle XB300_SET_AMPLIFIER_ENABLE =
        call("bladerf_xb300_set_amplifier_enable", ADDRESS, JAVA_INT, JAVA_BOOLEAN);
    public static final MethodHandle XB300_GET_AMPLIFIER_ENABLE =
        call("bladerf_xb300_get_amplifier_enable", ADDRESS, JAVA_INT, ADDRESS);
    public static final MethodHandle XB300_GET_OUTPUT_POWER = call("bladerf_xb300_get_output_power", ADDRESS, ADDRESS);
    public static final MethodHandle CALIBRATE_DC = call("bladerf_calibrate_dc", ADDRESS, JAVA_INT);
    public static final MethodHandle SI5338_READ = call("bladerf_si5338_read", ADDRESS, JAVA_BYTE, ADDRESS);
    public static final MethodHandle SI5338_WRITE = call("bladerf_si5338_write", ADDRESS, JAVA_BYTE, JAVA_BYTE);
    public static final MethodHandle LMS_READ = call("bladerf_lms_read", ADDRESS, JAVA_BYTE, ADDRESS);
    public static final MethodHandle LMS_WRITE = call("bladerf_lms_write", ADDRESS, JAVA_BYTE, JAVA_BYTE);
    public static final MethodHandle LMS_SET_DC_CALS = call("bladerf_lms_set_dc_cals", ADDRESS, ADDRESS);
    public static final MethodHandle LMS_GET_DC_CALS = call("bladerf_lms_get_dc_cals", ADDRESS, ADDRESS);
    public static final MethodHandle XB_SPI_WRITE = call("bladerf_xb_spi_write", ADDRESS, JAVA_INT);
    public static final MethodHandle GET_BIAS_TEE = call("bladerf_get_bias_tee", ADDRESS, JAVA_INT, ADDRESS);
    public static final MethodHandle SET_BIAS_TEE = call("bladerf_set_bias_tee", ADDRESS, JAVA_INT, JAVA_BOOLEAN);
    public static final MethodHandle GET_RFIC_REGISTER =
        call("bladerf_get_rfic_register", ADDRESS, JAVA_SHORT, ADDRESS);
    public static final MethodHandle SET_RFIC_REGISTER =
        call("bladerf_set_rfic_register", ADDRESS, JAVA_SHORT, JAVA_BYTE);
    public static final MethodHandle GET_RFIC_TEMPERATURE = call("bladerf_get_rfic_temperature", ADDRESS, ADDRESS);
    public static final MethodHandle GET_RFIC_RSSI = call("bladerf_get_rfic_rssi", ADDRESS, JAVA_INT, ADDRESS, ADDRESS);
    public static final MethodHandle GET_RFIC_CTRL_OUT = call("bladerf_get_rfic_ctrl_out", ADDRESS, ADDRESS);
    public static final MethodHandle GET_RFIC_RX_FIR = call("bladerf_get_rfic_rx_fir", ADDRESS, ADDRESS);
    public static final MethodHandle SET_RFIC_RX_FIR = call("bladerf_set_rfic_rx_fir", ADDRESS, JAVA_INT);
    public static final MethodHandle GET_RFIC_TX_FIR = call("bladerf_get_rfic_tx_fir", ADDRESS, ADDRESS);
    public static final MethodHandle SET_RFIC_TX_FIR = call("bladerf_set_rfic_tx_fir", ADDRESS, JAVA_INT);
    public static final MethodHandle GET_PLL_LOCK_STATE = call("bladerf_get_pll_lock_state", ADDRESS, ADDRESS);
    public static final MethodHandle GET_PLL_ENABLE = call("bladerf_get_pll_enable", ADDRESS, ADDRESS);
    public static final MethodHandle SET_PLL_ENABLE = call("bladerf_set_pll_enable", ADDRESS, JAVA_BOOLEAN);
    public static final MethodHandle GET_PLL_REFCLK_RANGE = call("bladerf_get_pll_refclk_range", ADDRESS, ADDRESS);
    public static final MethodHandle GET_PLL_REFCLK = call("bladerf_get_pll_refclk", ADDRESS, ADDRESS);
    public static final MethodHandle SET_PLL_REFCLK = call("bladerf_set_pll_refclk", ADDRESS, JAVA_LONG);
    public static final MethodHandle GET_PLL_REGISTER = call("bladerf_get_pll_register", ADDRESS, JAVA_BYTE, ADDRESS);
    public static final MethodHandle SET_PLL_REGISTER = call("bladerf_set_pll_register", ADDRESS, JAVA_BYTE, JAVA_INT);
    public static final MethodHandle GET_POWER_SOURCE = call("bladerf_get_power_source", ADDRESS, ADDRESS);
    public static final MethodHandle GET_CLOCK_SELECT = call("bladerf_get_clock_select", ADDRESS, ADDRESS);
    public static final MethodHandle SET_CLOCK_SELECT = call("bladerf_set_clock_select", ADDRESS, JAVA_INT);
    public static final MethodHandle GET_CLOCK_OUTPUT = call("bladerf_get_clock_output", ADDRESS, ADDRESS);
    public static final MethodHandle SET_CLOCK_OUTPUT = call("bladerf_set_clock_output", ADDRESS, JAVA_BOOLEAN);
    public static final MethodHandle GET_PMIC_REGISTER = call("bladerf_get_pmic_register", ADDRESS, JAVA_INT, ADDRESS);
    public static final MethodHandle GET_RF_SWITCH_CONFIG = call("bladerf_get_rf_switch_config", ADDRESS, ADDRESS);

    private Native() {
    }

    private static MemorySegment symbol(String name) {
        return LOOKUP.find(name).orElseThrow(() -> new UnsatisfiedLinkError("libbladeRF has no symbol " + name));
    }

    private static MethodHandle call(String name, MemoryLayout... arguments) {
        return call(JAVA_INT, name, arguments);
    }

    private static MethodHandle call(MemoryLayout result, String name, MemoryLayout... arguments) {
        return LINKER.downcallHandle(symbol(name), FunctionDescriptor.of(result, arguments));
    }

    private static MethodHandle voidCall(String name, MemoryLayout... arguments) {
        return LINKER.downcallHandle(symbol(name), FunctionDescriptor.ofVoid(arguments));
    }

    /**
     * Calls a native function returning {@code int}. This boxes its arguments,
     * which is fine for the control paths using it. {@link #syncRx} and
     * {@link #syncTx} are the sample paths that avoid it.
     */
    public static int invoke(MethodHandle function, Object... arguments) {
        return (int) invokeObject(function, arguments);
    }

    /** Calls a native function that returns nothing. */
    public static void invokeVoid(MethodHandle function, Object... arguments) {
        invokeObject(function, arguments);
    }

    /** Calls a native function returning a C {@code bool}. */
    public static boolean invokeBoolean(MethodHandle function, Object... arguments) {
        return (boolean) invokeObject(function, arguments);
    }

    /** Calls a native function returning {@code size_t}. */
    public static long invokeLong(MethodHandle function, Object... arguments) {
        return (long) invokeObject(function, arguments);
    }

    /** Calls a native function returning a pointer. */
    public static MemorySegment invokePointer(MethodHandle function, Object... arguments) {
        return (MemorySegment) invokeObject(function, arguments);
    }

    private static Object invokeObject(MethodHandle function, Object... arguments) {
        try {
            return function.invokeWithArguments(arguments);
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable t) {
            throw new IllegalStateException("Native call failed", t);
        }
    }

    /** Calls {@code bladerf_sync_rx} without boxing. */
    public static int syncRx(MemorySegment device, MemorySegment samples, int count, MemorySegment metadata,
            int timeoutMs) {
        try {
            return (int) SYNC_RX.invokeExact(device, samples, count, metadata, timeoutMs);
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable t) {
            throw new IllegalStateException("Native call failed", t);
        }
    }

    /** Calls {@code bladerf_sync_tx} without boxing. */
    public static int syncTx(MemorySegment device, MemorySegment samples, int count, MemorySegment metadata,
            int timeoutMs) {
        try {
            return (int) SYNC_TX.invokeExact(device, samples, count, metadata, timeoutMs);
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable t) {
            throw new IllegalStateException("Native call failed", t);
        }
    }

    /** Returns the description of an error code. */
    public static String errorMessage(int code) {
        return cString(invokePointer(STRERROR, code));
    }

    /** Reads a NUL-terminated string that the library owns. */
    public static String cString(MemorySegment pointer) {
        return pointer.equals(MemorySegment.NULL) ? null : pointer.reinterpret(Long.MAX_VALUE).getString(0);
    }

    /** Creates an upcall stub for {@code bladerf_stream_cb}. */
    public static MemorySegment streamCallback(MethodHandle target, Arena arena) {
        return LINKER.upcallStub(target,
            FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS, ADDRESS, ADDRESS, JAVA_LONG, ADDRESS), arena);
    }
}
