package com.vektor.app.ui

import android.bluetooth.BluetoothDevice
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0B0F19))
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // 1. Bilah Atas: Branding "VEKTOR", Status Koneksi, "By Natanael"
        TopVektorBar(
            status = state.connectionStatus,
            diagnosticText = diagnosticText,
            onRefresh = onReRegister,
            onConnectClick = { showDeviceDialog = true }
        )

        // 2. Kanvas Trackpad Horizontal Luas (Mendukung Tap-to-Click & Scroll 2 Jari)
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

        // 3. Bilah Bawah (Bottom Dock): [ C ] [ V ] [ RE ] di Kiri, [ L ] [ R ] di Kanan
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(76.dp)
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Tombol Makro Kiri: Copy, Paste, Undo
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MacroButton(label = "C", onClick = { onEvent(TrackpadUiEvent.CopyTriggered) })
                MacroButton(label = "V", onClick = { onEvent(TrackpadUiEvent.PasteTriggered) })
                MacroButton(label = "RE", onClick = { onEvent(TrackpadUiEvent.UndoTriggered) })
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Tombol Fisik Virtual Kanan: L dan R (50:50)
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

    // Dialog Pemilih Perangkat Tablet / PC
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
}

@Composable
private fun TopVektorBar(
    status: ConnectionStatus,
    diagnosticText: String,
    onRefresh: () -> Unit,
    onConnectClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Bagian Kiri: Nama Brand, Status Lampu, dan "By Natanael"
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

        // Bagian Kanan: Teks Diagnostik, Tombol Refresh, dan Tombol Sambungkan
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = diagnosticText,
                color = Color(0xFF60A5FA),
                fontSize = 10.sp,
                maxLines = 1,
                modifier = Modifier.padding(end = 8.dp)
            )
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

@Composable
private fun MacroButton(
    label: String,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    Box(
        modifier = Modifier
            .size(width = 58.dp, height = 58.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF141824))
            .border(1.2.dp, Color(0xFF00E5FF).copy(alpha = 0.7f), RoundedCornerShape(8.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onClick()
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = Color(0xFFE2E8F0),
            fontSize = 16.sp,
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
