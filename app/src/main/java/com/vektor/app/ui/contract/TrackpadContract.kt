package com.vektor.app.ui.contract

import androidx.compose.runtime.Immutable

// Katalog Tombol & Simbol
enum class MacroKey(
    val symbol: String,
    val title: String,
    val modifier: Byte,
    val keyCode: Byte
) {
    COPY("C", "Salin (Ctrl+C)", 0x01, 0x06),
    PASTE("V", "Tempel (Ctrl+V)", 0x01, 0x19),
    CUT("X", "Potong (Ctrl+X)", 0x01, 0x1B),
    SELECT_ALL("A", "Pilih Semua (Ctrl+A)", 0x01, 0x04),
    SAVE("S", "Simpan (Ctrl+S)", 0x01, 0x16),
    UNDO("Z", "Batal / Undo (Ctrl+Z)", 0x01, 0x1D),
    REDO("Y", "Ulangi / Redo (Ctrl+Y)", 0x01, 0x1C),
    ENTER("↵", "Enter (Baris Baru)", 0x00, 0x28),
    BACKSPACE("⌫", "Backspace (Hapus)", 0x00, 0x2A),
    TAB("⇥", "Tab (Pindah Kolom)", 0x00, 0x2B),
    ESCAPE("ESC", "Escape", 0x00, 0x29)
}

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
    // Navigasi & Gestur
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

    // Event Pengaturan & Makro
    data class MacroTriggered(val macroKey: MacroKey) : TrackpadUiEvent
    data class SlotChanged(val slotIndex: Int, val macroKey: MacroKey) : TrackpadUiEvent
    data class PointerSpeedChanged(val speed: Float) : TrackpadUiEvent
    data class ScrollSpeedChanged(val speed: Float) : TrackpadUiEvent
}
