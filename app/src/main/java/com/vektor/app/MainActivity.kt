package com.vektor.app

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.SharedPreferences
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
import com.vektor.app.ui.contract.*
import kotlin.math.hypot

class MainActivity : ComponentActivity() {

    private lateinit var hidManager: HidDeviceManager
    private lateinit var prefsManager: VektorPreferences
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

        prefsManager = VektorPreferences(this)
        hidManager = HidDeviceManager(this)
        checkAndRequestPermissions()

        setContent {
            val connectionStatus by hidManager.connectionStatus.collectAsState()
            val diagnosticText by hidManager.diagnosticText.collectAsState()
            var isDragLockActive by remember { mutableStateOf(false) }

            var slot1 by remember { mutableStateOf(prefsManager.getSlotMacro(0)) }
            var slot2 by remember { mutableStateOf(prefsManager.getSlotMacro(1)) }
            var slot3 by remember { mutableStateOf(prefsManager.getSlotMacro(2)) }
            var pointerSpeed by remember { mutableStateOf(prefsManager.pointerSpeed) }
            var scrollSpeed by remember { mutableStateOf(prefsManager.scrollSpeed) }

            val pairedDevices = remember { getPairedDevicesList() }

            val uiState = TrackpadUiState(
                connectionStatus = connectionStatus,
                isDragLockActive = isDragLockActive,
                slot1Macro = slot1,
                slot2Macro = slot2,
                slot3Macro = slot3,
                pointerSpeed = pointerSpeed,
                scrollSpeed = scrollSpeed
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
                            val (scaledDx, scaledDy) = calculateSmoothPointerDelta(event.deltaX, event.deltaY, event.dtMillis, pointerSpeed)
                            if (scaledDx != 0.toByte() || scaledDy != 0.toByte()) {
                                hidManager.sendMouseInput(currentButtonMask, scaledDx, scaledDy, 0)
                            }
                        }
                        is TrackpadUiEvent.TwoFingerScrolled -> {
                            val scrollStep = calculateSmoothScrollDelta(event.deltaY, event.dtMillis, scrollSpeed)
                            if (scrollStep != 0.toByte()) {
                                hidManager.sendMouseInput(currentButtonMask, 0, 0, scrollStep)
                            }
                        }
                        is TrackpadUiEvent.SingleTapLeftClick -> {
                            hidManager.sendMouseInput(HidReportDescriptor.MOUSE_BTN_LEFT, 0, 0, 0)
                            hidManager.sendMouseInput(HidReportDescriptor.MOUSE_BTN_NONE, 0, 0, 0)
                        }
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
                        is TrackpadUiEvent.MacroTriggered -> {
                            hidManager.sendMacro(event.macroKey.modifier, event.macroKey.keyCode)
                        }
                        is TrackpadUiEvent.SlotChanged -> {
                            prefsManager.setSlotMacro(event.slotIndex, event.macroKey)
                            when (event.slotIndex) {
                                0 -> slot1 = event.macroKey
                                1 -> slot2 = event.macroKey
                                2 -> slot3 = event.macroKey
                            }
                        }
                        is TrackpadUiEvent.PointerSpeedChanged -> {
                            pointerSpeed = event.speed
                            prefsManager.pointerSpeed = event.speed
                        }
                        is TrackpadUiEvent.ScrollSpeedChanged -> {
                            scrollSpeed = event.speed
                            prefsManager.scrollSpeed = event.speed
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

    private fun calculateSmoothPointerDelta(rawDx: Float, rawDy: Float, dtMillis: Long, userSpeed: Float): Pair<Byte, Byte> {
        if (dtMillis <= 0L) return Pair(0, 0)
        val distance = hypot(rawDx, rawDy)
        val velocity = distance / dtMillis

        val baseAccel = when {
            velocity < 0.15f -> 0.95f
            velocity < 0.6f -> 1.20f
            else -> (1.20f + (velocity - 0.6f) * 0.75f).coerceAtMost(2.5f)
        }
        val accelFactor = baseAccel * userSpeed

        val targetDx = rawDx * accelFactor + remainderX
        val targetDy = rawDy * accelFactor + remainderY

        val stepX = targetDx.toInt().coerceIn(-127, 127)
        val stepY = targetDy.toInt().coerceIn(-127, 127)

        remainderX = targetDx - stepX
        remainderY = targetDy - stepY

        return Pair(stepX.toByte(), stepY.toByte())
    }

    private fun calculateSmoothScrollDelta(rawDy: Float, dtMillis: Long, userScrollFactor: Float): Byte {
        if (dtMillis <= 0L) return 0
        val targetScroll = (rawDy * userScrollFactor) + scrollRemainder
        val stepScroll = targetScroll.toInt().coerceIn(-3, 3)
        scrollRemainder = targetScroll - stepScroll
        return stepScroll.toByte()
    }

    override fun onDestroy() {
        super.onDestroy()
        hidManager.release()
    }
}

// Penyimpan Pengaturan Lokal Tanpa Perlu File Eksternal
private class VektorPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("vektor_prefs", Context.MODE_PRIVATE)

    var pointerSpeed: Float
        get() = prefs.getFloat("pointer_speed", 1.0f)
        set(value) = prefs.edit().putFloat("pointer_speed", value).apply()

    var scrollSpeed: Float
        get() = prefs.getFloat("scroll_speed", 0.06f)
        set(value) = prefs.edit().putFloat("scroll_speed", value).apply()

    fun getSlotMacro(slotIndex: Int): MacroKey {
        val defaultKey = when (slotIndex) {
            0 -> MacroKey.COPY
            1 -> MacroKey.PASTE
            else -> MacroKey.UNDO
        }
        val name = prefs.getString("slot_${slotIndex}_macro", defaultKey.name) ?: defaultKey.name
        return try {
            MacroKey.valueOf(name)
        } catch (_: Exception) {
            defaultKey
        }
    }

    fun setSlotMacro(slotIndex: Int, key: MacroKey) {
        prefs.edit().putString("slot_${slotIndex}_macro", key.name).apply()
    }
}
