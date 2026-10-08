package com.vektor.app.ui.model

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
