package com.vektor.app.ui.contract

import androidx.compose.runtime.Immutable
import com.vektor.app.ui.model.MacroKey

@Immutable
data class TrackpadUiState(
    val connectionStatus: ConnectionStatus = ConnectionStatus.Disconnected,
    val isDragLockActive: Boolean = false,
    val slot1Macro: MacroKey = MacroKey.COPY,
    val slot2Macro: MacroKey = MacroKey.PASTE,
    val slot3Macro: MacroKey = MacroKey.UNDO,
    val pointerSpeed: Float = 1.0f,
    val scrollSpeed: Float = 0.06f
)

sealed interface ConnectionStatus {
    object Disconnected : ConnectionStatus
    object Connecting : ConnectionStatus
    data class Connected(val deviceName: String) : ConnectionStatus
}

sealed interface TrackpadUiEvent {
    data class PointerMoved(val deltaX: Float, val deltaY: Float, val dtMillis: Long) : TrackpadUiEvent
    data class TwoFingerScrolled(val deltaY: Float, val dtMillis: Long) : TrackpadUiEvent
    object SingleTapLeftClick : TrackpadUiEvent
    object TwoFingerTapRightClick : TrackpadUiEvent
    object DragLockStarted : TrackpadUiEvent
    object DragLockEnded : TrackpadUiEvent

    object LeftButtonDown : TrackpadUiEvent
    object LeftButtonUp : TrackpadUiEvent
    object RightButtonDown : TrackpadUiEvent
    object RightButtonUp : TrackpadUiEvent

    // Event Pengaturan & Makro
    data class MacroTriggered(val macroKey: MacroKey) : TrackpadUiEvent
    data class SlotChanged(val slotIndex: Int, val macroKey: MacroKey) : TrackpadUiEvent
    data class PointerSpeedChanged(val speed: Float) : TrackpadUiEvent
    data class ScrollSpeedChanged(val speed: Float) : TrackpadUiEvent
}
