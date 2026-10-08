package com.vektor.app.ui.contract

import androidx.compose.runtime.Immutable

@Immutable
data class TrackpadUiState(
    val connectionStatus: ConnectionStatus = ConnectionStatus.Disconnected,
    val isDragLockActive: Boolean = false
)

sealed interface ConnectionStatus {
    object Disconnected : ConnectionStatus
    object Connecting : ConnectionStatus
    data class Connected(val deviceName: String) : ConnectionStatus
}

sealed interface TrackpadUiEvent {
    // Navigasi & Gestur Laptop
    data class PointerMoved(val deltaX: Float, val deltaY: Float, val dtMillis: Long) : TrackpadUiEvent
    data class TwoFingerScrolled(val deltaY: Float, val dtMillis: Long) : TrackpadUiEvent
    object SingleTapLeftClick : TrackpadUiEvent
    object TwoFingerTapRightClick : TrackpadUiEvent
    object DragLockStarted : TrackpadUiEvent
    object DragLockEnded : TrackpadUiEvent

    // Tombol Fisik Virtual
    object LeftButtonDown : TrackpadUiEvent
    object LeftButtonUp : TrackpadUiEvent
    object RightButtonDown : TrackpadUiEvent
    object RightButtonUp : TrackpadUiEvent

    // Makro Pintasan
    object CopyTriggered : TrackpadUiEvent
    object PasteTriggered : TrackpadUiEvent
    object UndoTriggered : TrackpadUiEvent
}
