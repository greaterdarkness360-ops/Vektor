package com.vektor.app.data

import android.content.Context
import android.content.SharedPreferences
import com.vektor.app.ui.model.MacroKey

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("vektor_prefs", Context.MODE_PRIVATE)

    var pointerSpeed: Float
        get() = prefs.getFloat("pointer_speed", 1.0f)
        set(value) = prefs.edit().putFloat("pointer_speed", value).apply()

    var scrollSpeed: Float
        get() = prefs.getFloat("scroll_speed", 0.06f)
        set(value) = prefs.edit().putFloat("scroll_speed", value).apply()

    fun getSlotMacro(slotIndex: Int): MacroKey {
        val defaultName = when (slotIndex) {
            0 -> MacroKey.COPY.name
            1 -> MacroKey.PASTE.name
            else -> MacroKey.UNDO.name
        }
        val name = prefs.getString("slot_${slotIndex}_macro", defaultName) ?: defaultName
        return try {
            MacroKey.valueOf(name)
        } catch (_: Exception) {
            MacroKey.COPY
        }
    }

    fun setSlotMacro(slotIndex: Int, key: MacroKey) {
        prefs.edit().putString("slot_${slotIndex}_macro", key.name).apply()
    }
}
