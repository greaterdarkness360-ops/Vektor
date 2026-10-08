package com.vektor.app.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.vektor.app.ui.contract.ConnectionStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.Executors

@SuppressLint("MissingPermission")
class HidDeviceManager(private val context: Context) {

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
    private var hidDevice: BluetoothHidDevice? = null
    private var connectedHost: BluetoothDevice? = null

    private val _connectionStatus = MutableStateFlow<ConnectionStatus>(ConnectionStatus.Disconnected)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus

    val diagnosticText = MutableStateFlow("Menyiapkan Vektor Trackpad...")

    private val executor = Executors.newSingleThreadExecutor()

    private val bluetoothStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                if (state == BluetoothAdapter.STATE_ON) {
                    diagnosticText.value = "Bluetooth aktif. Mendaftarkan ulang Vektor..."
                    init()
                } else if (state == BluetoothAdapter.STATE_TURNING_OFF || state == BluetoothAdapter.STATE_OFF) {
                    diagnosticText.value = "Bluetooth HP nonaktif."
                }
            }
        }
    }

    private val profileServiceListener = object : BluetoothProfile.ServiceListener {
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
            if (profile == BluetoothProfile.HID_DEVICE) {
                hidDevice = proxy as BluetoothHidDevice
                registerHidApp()
            }
        }

        override fun onServiceDisconnected(profile: Int) {
            if (profile == BluetoothProfile.HID_DEVICE) {
                hidDevice = null
                _connectionStatus.value = ConnectionStatus.Disconnected
                diagnosticText.value = "Layanan HID terlepas."
            }
        }
    }

    private val hidCallback = object : BluetoothHidDevice.Callback() {
        override fun onConnectionStateChanged(device: BluetoothDevice, state: Int) {
            when (state) {
                BluetoothProfile.STATE_CONNECTED -> {
                    connectedHost = device
                    _connectionStatus.value = ConnectionStatus.Connected(device.name ?: device.address)
                    diagnosticText.value = "TERHUBUNG ke ${device.name ?: device.address}"
                }
                BluetoothProfile.STATE_CONNECTING -> {
                    _connectionStatus.value = ConnectionStatus.Connecting
                    diagnosticText.value = "Menyambungkan..."
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    connectedHost = null
                    _connectionStatus.value = ConnectionStatus.Disconnected
                    diagnosticText.value = "HID Siap: Menunggu sambungan dari MatePad/Laptop."
                }
            }
        }

        override fun onAppStatusChanged(pluggedDevice: BluetoothDevice?, registered: Boolean) {
            if (registered) {
                diagnosticText.value = "BERHASIL: Vektor siap digunakan!"
                if (pluggedDevice != null) {
                    hidDevice?.connect(pluggedDevice)
                }
            } else {
                diagnosticText.value = "Status: Belum terdaftar (Tekan 'Refresh')."
            }
        }

        override fun onGetReport(device: BluetoothDevice?, type: Byte, id: Byte, bufferSize: Int) {
            if (device != null) {
                hidDevice?.reportError(device, BluetoothHidDevice.ERROR_RSP_SUCCESS)
            }
        }

        override fun onSetReport(device: BluetoothDevice?, type: Byte, id: Byte, data: ByteArray?) {
            if (device != null) {
                hidDevice?.reportError(device, BluetoothHidDevice.ERROR_RSP_SUCCESS)
            }
        }
    }

    fun init() {
        val adapter = bluetoothAdapter ?: return
        if (!adapter.isEnabled) {
            diagnosticText.value = "Bluetooth HP belum aktif."
            return
        }

        try {
            context.registerReceiver(
                bluetoothStateReceiver,
                IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
            )
        } catch (_: Exception) {}

        adapter.getProfileProxy(context.applicationContext, profileServiceListener, BluetoothProfile.HID_DEVICE)
    }

    fun registerHidApp() {
        val sdpSettings = BluetoothHidDeviceAppSdpSettings(
            "Vektor Trackpad",
            "Precision Touchpad & Keyboard Shortcuts",
            "Dimensional",
            BluetoothHidDevice.SUBCLASS1_MOUSE,
            HidReportDescriptor.COMPOSITE_DESCRIPTOR
        )
        hidDevice?.registerApp(sdpSettings, null, null, executor, hidCallback)
    }

    fun reRegister() {
        if (hidDevice != null) {
            registerHidApp()
        } else {
            init()
        }
    }

    fun connectToDevice(device: BluetoothDevice): Boolean {
        diagnosticText.value = "Menghubungkan ke ${device.name}..."
        return hidDevice?.connect(device) ?: false
    }

    fun sendMouseInput(buttonMask: Byte, deltaX: Byte, deltaY: Byte, wheel: Byte = 0) {
        val host = connectedHost ?: return
        val report = byteArrayOf(buttonMask, deltaX, deltaY, wheel)
        hidDevice?.sendReport(host, HidReportDescriptor.REPORT_ID_MOUSE, report)
    }

    // Eksekutor Makro Keyboard Biner
    private fun sendKeyCombination(modifier: Byte, keycode: Byte) {
        val host = connectedHost ?: return
        val keyDown = byteArrayOf(modifier, 0x00.toByte(), keycode, 0x00, 0x00, 0x00, 0x00, 0x00)
        hidDevice?.sendReport(host, HidReportDescriptor.REPORT_ID_KEYBOARD, keyDown)

        val keyUp = ByteArray(8) { 0x00 }
        hidDevice?.sendReport(host, HidReportDescriptor.REPORT_ID_KEYBOARD, keyUp)
    }

    fun sendCopyMacro() = sendKeyCombination(HidReportDescriptor.KEY_MOD_LCTRL, HidReportDescriptor.KEY_C)
    fun sendPasteMacro() = sendKeyCombination(HidReportDescriptor.KEY_MOD_LCTRL, HidReportDescriptor.KEY_V)
    fun sendUndoMacro() = sendKeyCombination(HidReportDescriptor.KEY_MOD_LCTRL, HidReportDescriptor.KEY_Z)

    fun release() {
        try {
            context.unregisterReceiver(bluetoothStateReceiver)
        } catch (_: Exception) {}
        bluetoothAdapter?.closeProfileProxy(BluetoothProfile.HID_DEVICE, hidDevice)
    }
}
