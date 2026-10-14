package io.github.infamase.gamingkeyboard.keyboard

import android.view.KeyEvent

/** The PC modifier keys. Each maps to the Android key code Moonlight forwards to the host. */
enum class Modifier(val keyCode: Int, val metaMask: Int) {
    SHIFT(KeyEvent.KEYCODE_SHIFT_LEFT, KeyEvent.META_SHIFT_ON or KeyEvent.META_SHIFT_LEFT_ON),
    CTRL(KeyEvent.KEYCODE_CTRL_LEFT, KeyEvent.META_CTRL_ON or KeyEvent.META_CTRL_LEFT_ON),
    ALT(KeyEvent.KEYCODE_ALT_LEFT, KeyEvent.META_ALT_ON or KeyEvent.META_ALT_LEFT_ON),
    WIN(KeyEvent.KEYCODE_META_LEFT, KeyEvent.META_META_ON or KeyEvent.META_META_LEFT_ON);

    companion object {
        fun fromKeyCode(keyCode: Int): Modifier? = entries.firstOrNull { it.keyCode == keyCode }
    }
}

/** What happens when a key is touched. */
sealed interface KeyAction {
    /** Presses one or more key codes in order and releases them in reverse when the finger lifts. */
    data class Press(val keyCodes: List<Int>) : KeyAction {
        constructor(vararg keyCodes: Int) : this(keyCodes.toList())
    }

    /** A modifier: hold it, tap it for one-shot, or double-tap it to lock. */
    data class Mod(val modifier: Modifier) : KeyAction

    /** Toggles between the main and function layers. */
    data object SwitchLayer : KeyAction

    /** Hides the keyboard. */
    data object Hide : KeyAction

    /** Empty space in a row. */
    data object Spacer : KeyAction
}

/**
 * One key on the keyboard.
 *
 * @param width relative width; every row in a layer adds up to the same total.
 * @param shiftLabel label shown while Shift is active, if different.
 */
data class KeySpec(
    val label: String,
    val action: KeyAction,
    val width: Float = 1f,
    val shiftLabel: String? = null,
) {
    val isLetter: Boolean get() = label.length == 1 && label[0].isLetter()
}

data class KeyboardLayer(val name: String, val rows: List<List<KeySpec>>)
