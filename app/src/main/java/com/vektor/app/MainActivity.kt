package com.vektor.app

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import com.vektor.app.bluetooth.HidDeviceManager
import com.vektor.app.bluetooth.HidReportDescriptor
import com.vektor.app.ui.VektorScreen
import com.vektor.app.ui.contract.TrackpadUiEvent
import com.vektor.app.ui.contract.TrackpadUiState
import kotlin.math.hypot

class MainActivity : ComponentActivity() {

    private lateinit var hidManager: HidDeviceManager
    private var currentButtonMask: Byte = HidReportDescriptor.MOUSE_BTN_NONE

    private var remainderX = 0f
    private var remainderY = 0f
    private var scrollRemainder = 0f

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val isGranted = permissions.values.all { it }
        if (isGranted) {
            hidManager.init()
        } else {
            Toast.makeText(this, "Izin Bluetooth diperlukan", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        hidManager = HidDeviceManager(this)
        checkAndRequestPermissions()

        setContent {
            val connectionStatus by hidManager.connectionStatus.collectAsState()
            val diagnosticText by hidManager.diagnosticText.collectAsState()
            var isDragLockActive by remember { mutableStateOf(false) }

            val pairedDevices = remember { getPairedDevicesList() }

            val uiState = TrackpadUiState(
                connectionStatus = connectionStatus,
                isDragLockActive = isDragLockActive
            )

            VektorScreen(
                state = uiState,
                diagnosticText = diagnosticText,
                pairedDevices = pairedDevices,
                onReRegister = {
                    hidManager.reRegister()
                },
                onConnectDevice = { targetDevice ->
                    hidManager.connectToDevice(targetDevice)
                },
                onEvent = { event ->
                    when (event) {
                        is TrackpadUiEvent.PointerMoved -> {
                            val (scaledDx, scaledDy) = calculateSmoothPointerDelta(event.deltaX, event.deltaY, event.dtMillis)
                            if (scaledDx != 0.toByte() || scaledDy != 0.toByte()) {
                                hidManager.sendMouseInput(currentButtonMask, scaledDx, scaledDy, 0)
                            }
                        }
                        is TrackpadUiEvent.TwoFingerScrolled -> {
                            val scrollStep = calculateSmoothScrollDelta(event.deltaY, event.dtMillis)
                            if (scrollStep != 0.toByte()) {
                                hidManager.sendMouseInput(currentButtonMask, 0, 0, scrollStep)
                            }
                        }
                        // GESTUR LAPTOP: Ketuk 1 Jari = Klik Kiri Instan
                        is TrackpadUiEvent.SingleTapLeftClick -> {
                            hidManager.sendMouseInput(HidReportDescriptor.MOUSE_BTN_LEFT, 0, 0, 0)
                            hidManager.sendMouseInput(HidReportDescriptor.MOUSE_BTN_NONE, 0, 0, 0)
                        }
                        // GESTUR LAPTOP: Ketuk 2 Jari = Klik Kanan Instan
                        is TrackpadUiEvent.TwoFingerTapRightClick -> {
                            hidManager.sendMouseInput(HidReportDescriptor.MOUSE_BTN_RIGHT, 0, 0, 0)
                            hidManager.sendMouseInput(HidReportDescriptor.MOUSE_BTN_NONE, 0, 0, 0)
                        }
                        is TrackpadUiEvent.DragLockStarted -> {
                            isDragLockActive = true
                            currentButtonMask = HidReportDescriptor.MOUSE_BTN_LEFT
                            hidManager.sendMouseInput(currentButtonMask, 0, 0, 0)
                        }
                        is TrackpadUiEvent.DragLockEnded -> {
                            isDragLockActive = false
                            currentButtonMask = HidReportDescriptor.MOUSE_BTN_NONE
                            hidManager.sendMouseInput(currentButtonMask, 0, 0, 0)
                        }
                        is TrackpadUiEvent.LeftButtonDown -> {
                            currentButtonMask = (currentButtonMask.toInt() or HidReportDescriptor.MOUSE_BTN_LEFT.toInt()).toByte()
                            hidManager.sendMouseInput(currentButtonMask, 0, 0, 0)
                        }
                        is TrackpadUiEvent.LeftButtonUp -> {
                            currentButtonMask = (currentButtonMask.toInt() and HidReportDescriptor.MOUSE_BTN_LEFT.toInt().inv()).toByte()
                            hidManager.sendMouseInput(currentButtonMask, 0, 0, 0)
                        }
                        is TrackpadUiEvent.RightButtonDown -> {
                            currentButtonMask = (currentButtonMask.toInt() or HidReportDescriptor.MOUSE_BTN_RIGHT.toInt()).toByte()
                            hidManager.sendMouseInput(currentButtonMask, 0, 0, 0)
                        }
                        is TrackpadUiEvent.RightButtonUp -> {
                            currentButtonMask = (currentButtonMask.toInt() and HidReportDescriptor.MOUSE_BTN_RIGHT.toInt().inv()).toByte()
                            hidManager.sendMouseInput(currentButtonMask, 0, 0, 0)
                        }
                        is TrackpadUiEvent.CopyTriggered -> {
                            hidManager.sendCopyMacro()
                        }
                        is TrackpadUiEvent.PasteTriggered -> {
                            hidManager.sendPasteMacro()
                        }
                        is TrackpadUiEvent.UndoTriggered -> {
                            hidManager.sendUndoMacro()
                        }
                    }
                }
            )
        }
    }

    override fun onResume() {
        super.onResume()
        hidManager.reRegister()
    }

    @SuppressLint("MissingPermission")
    private fun getPairedDevicesList(): List<BluetoothDevice> {
        val adapter = hidManager.bluetoothAdapter ?: return emptyList()
        return adapter.bondedDevices.toList()
    }

    private fun checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val permissions = arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_ADVERTISE
            )
            val allGranted = permissions.all {
                ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
            }
            if (allGranted) {
                hidManager.init()
            } else {
                permissionLauncher.launch(permissions)
            }
        } else {
            hidManager.init()
        }
    }

    private fun calculateSmoothPointerDelta(rawDx: Float, rawDy: Float, dtMillis: Long): Pair<Byte, Byte> {
        if (dtMillis <= 0L) return Pair(0, 0)
        val distance = hypot(rawDx, rawDy)
        val velocity = distance / dtMillis

        val accelFactor = when {
            velocity < 0.15f -> 0.95f
            velocity < 0.6f -> 1.20f
            else -> (1.20f + (velocity - 0.6f) * 0.75f).coerceAtMost(2.5f)
        }

        val targetDx = rawDx * accelFactor + remainderX
        val targetDy = rawDy * accelFactor + remainderY

        val stepX = targetDx.toInt().coerceIn(-127, 127)
        val stepY = targetDy.toInt().coerceIn(-127, 127)

        remainderX = targetDx - stepX
        remainderY = targetDy - stepY

        return Pair(stepX.toByte(), stepY.toByte())
    }

    private fun calculateSmoothScrollDelta(rawDy: Float, dtMillis: Long): Byte {
        if (dtMillis <= 0L) return 0
        val scrollSpeedFactor = 0.06f
        val targetScroll = (rawDy * scrollSpeedFactor) + scrollRemainder
        val stepScroll = targetScroll.toInt().coerceIn(-3, 3)
        scrollRemainder = targetScroll - stepScroll
        return stepScroll.toByte()
    }

    override fun onDestroy() {
        super.onDestroy()
        hidManager.release()
    }
}
