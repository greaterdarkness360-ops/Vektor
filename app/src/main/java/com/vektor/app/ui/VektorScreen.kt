package com.vektor.app.ui

import android.bluetooth.BluetoothDevice
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vektor.app.ui.contract.ConnectionStatus
import com.vektor.app.ui.contract.TrackpadUiEvent
import com.vektor.app.ui.contract.TrackpadUiState
import com.vektor.app.ui.gesture.trackpadTouchHandler
import com.vektor.app.ui.model.MacroKey

@Composable
fun VektorScreen(
    state: TrackpadUiState,
    diagnosticText: String,
    pairedDevices: List<BluetoothDevice>,
    onReRegister: () -> Unit,
    onConnectDevice: (BluetoothDevice) -> Unit,
    onEvent: (TrackpadUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDeviceDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var editingSlotIndex by remember { mutableStateOf<Int?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0B0F19))
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // 1. Bilah Atas
        TopVektorBar(
            status = state.connectionStatus,
            diagnosticText = diagnosticText,
            onRefresh = onReRegister,
            onOpenSettings = { showSettingsDialog = true },
            onConnectClick = { showDeviceDialog = true }
        )

        // 2. Kanvas Trackpad Horizontal Luas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 12.dp, vertical = 4.dp)
                .background(Color(0xFF131722), RoundedCornerShape(12.dp))
                .border(
                    width = 1.dp,
                    color = if (state.isDragLockActive) Color(0xFF00E5FF) else Color(0xFF1F2638),
                    shape = RoundedCornerShape(12.dp)
                )
                .trackpadTouchHandler(
                    onPointerMove = { dx, dy, dt -> onEvent(TrackpadUiEvent.PointerMoved(dx, dy, dt)) },
                    onTwoFingerScroll = { dy, dt -> onEvent(TrackpadUiEvent.TwoFingerScrolled(dy, dt)) },
                    onSingleTap = { onEvent(TrackpadUiEvent.SingleTapLeftClick) },
                    onTwoFingerTap = { onEvent(TrackpadUiEvent.TwoFingerTapRightClick) },
                    onDragLockStart = { onEvent(TrackpadUiEvent.DragLockStarted) },
                    onDragLockEnd = { onEvent(TrackpadUiEvent.DragLockEnded) }
                )
        )

        Spacer(modifier = Modifier.height(6.dp))

        // 3. Bilah Bawah: 3 Tombol Makro Dinamis di Kiri, L dan R di Kanan
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(76.dp)
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 3 Slot Makro Dinamis (Tekan-tahan untuk mengganti tombol)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DynamicMacroButton(
                    macroKey = state.slot1Macro,
                    onClick = { onEvent(TrackpadUiEvent.MacroTriggered(state.slot1Macro)) },
                    onLongClick = { editingSlotIndex = 0 }
                )
                DynamicMacroButton(
                    macroKey = state.slot2Macro,
                    onClick = { onEvent(TrackpadUiEvent.MacroTriggered(state.slot2Macro)) },
                    onLongClick = { editingSlotIndex = 1 }
                )
                DynamicMacroButton(
                    macroKey = state.slot3Macro,
                    onClick = { onEvent(TrackpadUiEvent.MacroTriggered(state.slot3Macro)) },
                    onLongClick = { editingSlotIndex = 2 }
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Tombol Klik Kanan & Kiri
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF141824))
                    .border(1.dp, Color(0xFF222A3F), RoundedCornerShape(8.dp))
            ) {
                MouseButton(
                    label = "L",
                    onDown = { onEvent(TrackpadUiEvent.LeftButtonDown) },
                    onUp = { onEvent(TrackpadUiEvent.LeftButtonUp) },
                    modifier = Modifier.weight(1f)
                )
                Divider(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(1.dp),
                    color = Color(0xFF222A3F)
                )
                MouseButton(
                    label = "R",
                    onDown = { onEvent(TrackpadUiEvent.RightButtonDown) },
                    onUp = { onEvent(TrackpadUiEvent.RightButtonUp) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    // Dialog 1: Pemilih Perangkat Bluetooth
    if (showDeviceDialog) {
        AlertDialog(
            onDismissRequest = { showDeviceDialog = false },
            title = { Text("Pilih Tablet / PC", color = Color.White) },
            text = {
                Column {
                    if (pairedDevices.isEmpty()) {
                        Text("Belum ada perangkat terpasang.", color = Color.Gray)
                    } else {
                        pairedDevices.forEach { device ->
                            @Suppress("MissingPermission")
                            val name = device.name ?: device.address
                            TextButton(
                                onClick = {
                                    onConnectDevice(device)
                                    showDeviceDialog = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(name, color = Color(0xFF00E5FF), fontSize = 16.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDeviceDialog = false }) {
                    Text("Tutup", color = Color.LightGray)
                }
            },
            containerColor = Color(0xFF161C2B)
        )
    }

    // Dialog 2: Panel Pengaturan Slider Sensitivitas
    if (showSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = { Text("Pengaturan Sensitivitas", color = Color.White) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Kecepatan Kursor: ${String.format("%.1fx", state.pointerSpeed)}", color = Color.LightGray, fontSize = 13.sp)
                    Slider(
                        value = state.pointerSpeed,
                        onValueChange = { onEvent(TrackpadUiEvent.PointerSpeedChanged(it)) },
                        valueRange = 0.5f..2.5f,
                        colors = SliderDefaults.colors(thumbColor = Color(0xFF00E5FF), activeTrackColor = Color(0xFF00E5FF))
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Sensitivitas Scroll 2 Jari: ${String.format("%.2f", state.scrollSpeed)}", color = Color.LightGray, fontSize = 13.sp)
                    Slider(
                        value = state.scrollSpeed,
                        onValueChange = { onEvent(TrackpadUiEvent.ScrollSpeedChanged(it)) },
                        valueRange = 0.02f..0.15f,
                        colors = SliderDefaults.colors(thumbColor = Color(0xFF00E5FF), activeTrackColor = Color(0xFF00E5FF))
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showSettingsDialog = false }) {
                    Text("Selesai", color = Color(0xFF00E5FF))
                }
            },
            containerColor = Color(0xFF161C2B)
        )
    }

    // Dialog 3: Katalog Pemilih Tombol Makro
    editingSlotIndex?.let { slotIdx ->
        AlertDialog(
            onDismissRequest = { editingSlotIndex = null },
            title = { Text("Pilih Simbol Tombol ${slotIdx + 1}", color = Color.White) },
            text = {
                LazyColumn(modifier = Modifier.height(280.dp)) {
                    items(MacroKey.values().toList()) { macro ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onEvent(TrackpadUiEvent.SlotChanged(slotIdx, macro))
                                    editingSlotIndex = null
                                }
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color(0xFF1F293D), RoundedCornerShape(6.dp))
                                    .border(1.dp, Color(0xFF00E5FF), RoundedCornerShape(6.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(macro.symbol, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(macro.title, color = Color.LightGray, fontSize = 14.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { editingSlotIndex = null }) {
                    Text("Batal", color = Color.Gray)
                }
            },
            containerColor = Color(0xFF161C2B)
        )
    }
}

@Composable
private fun TopVektorBar(
    status: ConnectionStatus,
    diagnosticText: String,
    onRefresh: () -> Unit,
    onOpenSettings: () -> Unit,
    onConnectClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "VEKTOR",
                color = Color(0xFF00E5FF),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )
            Spacer(modifier = Modifier.width(12.dp))
            val (indicatorColor, statusText) = when (status) {
                is ConnectionStatus.Connected -> Color(0xFF4CAF50) to "Terhubung: ${status.deviceName}"
                ConnectionStatus.Connecting -> Color(0xFFFFC107) to "Menghubungkan..."
                ConnectionStatus.Disconnected -> Color(0xFF757575) to "Belum Terhubung"
            }
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(indicatorColor)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = statusText, color = Color.LightGray, fontSize = 11.sp)
            Spacer(modifier = Modifier.width(10.dp))
            Text(text = "By Natanael", color = Color(0xFF6B7280), fontSize = 10.sp)
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = diagnosticText,
                color = Color(0xFF60A5FA),
                fontSize = 10.sp,
                maxLines = 1,
                modifier = Modifier.padding(end = 6.dp)
            )
            Text(
                text = "⚙️",
                fontSize = 14.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable { onOpenSettings() }
                    .padding(4.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Refresh",
                color = Color(0xFFFFB74D),
                fontSize = 11.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable { onRefresh() }
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
            if (status !is ConnectionStatus.Connected) {
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Sambungkan",
                    color = Color(0xFF00E5FF),
                    fontSize = 11.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { onConnectClick() }
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DynamicMacroButton(
    macroKey: MacroKey,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    Box(
        modifier = Modifier
            .size(width = 58.dp, height = 58.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF141824))
            .border(1.2.dp, Color(0xFF00E5FF).copy(alpha = 0.7f), RoundedCornerShape(8.dp))
            .combinedClickable(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onClick()
                },
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = macroKey.symbol,
            color = Color(0xFFE2E8F0),
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun MouseButton(
    label: String,
    onDown: () -> Unit,
    onUp: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    Box(
        modifier = modifier
            .fillMaxHeight()
            .background(if (isPressed) Color(0xFF1F293D) else Color(0xFF141824))
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown()
                    isPressed = true
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onDown()
                    waitForUpOrCancellation()
                    isPressed = false
                    onUp()
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isPressed) Color(0xFF00E5FF) else Color(0xFF8896AB),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
