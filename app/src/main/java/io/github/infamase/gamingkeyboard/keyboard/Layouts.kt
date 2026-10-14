package io.github.infamase.gamingkeyboard.keyboard

import android.view.KeyEvent.*

/** Built-in layouts. Every row of a layer adds up to [ROW_UNITS] width units. */
object Layouts {
    const val ROW_UNITS = 15f

    private fun key(label: String, code: Int, width: Float = 1f, shift: String? = null) =
        KeySpec(label, KeyAction.Press(code), width, shift)

    private fun mod(label: String, modifier: Modifier, width: Float = 1f) =
        KeySpec(label, KeyAction.Mod(modifier), width)

    private fun combo(label: String, vararg codes: Int, width: Float = 1f) =
        KeySpec(label, KeyAction.Press(*codes), width)

    private fun spacer(width: Float) = KeySpec("", KeyAction.Spacer, width)

    private fun letters(chars: String) = chars.map { c ->
        key(c.toString(), KEYCODE_A + (c - 'a'))
    }

    /** Always-visible row of one-touch shortcuts. Holding a shortcut holds all of its keys. */
    val shortcuts: List<KeySpec> = listOf(
        key("Esc", KEYCODE_ESCAPE),
        combo("Alt+Tab", KEYCODE_ALT_LEFT, KEYCODE_TAB, width = 1.3f),
        combo("Alt+F4", KEYCODE_ALT_LEFT, KEYCODE_F4, width = 1.3f),
        key("Win", KEYCODE_META_LEFT),
        combo("Win+Tab", KEYCODE_META_LEFT, KEYCODE_TAB, width = 1.3f),
        combo("Win+D", KEYCODE_META_LEFT, KEYCODE_D, width = 1.2f),
        combo("Task Mgr", KEYCODE_CTRL_LEFT, KEYCODE_SHIFT_LEFT, KEYCODE_ESCAPE, width = 1.4f),
        KeySpec("Hide", KeyAction.Hide, 1f),
    )

    val main = KeyboardLayer(
        "main",
        listOf(
            listOf(
                key("`", KEYCODE_GRAVE, shift = "~"),
                key("1", KEYCODE_1, shift = "!"),
                key("2", KEYCODE_2, shift = "@"),
                key("3", KEYCODE_3, shift = "#"),
                key("4", KEYCODE_4, shift = "$"),
                key("5", KEYCODE_5, shift = "%"),
                key("6", KEYCODE_6, shift = "^"),
                key("7", KEYCODE_7, shift = "&"),
                key("8", KEYCODE_8, shift = "*"),
                key("9", KEYCODE_9, shift = "("),
                key("0", KEYCODE_0, shift = ")"),
                key("-", KEYCODE_MINUS, shift = "_"),
                key("=", KEYCODE_EQUALS, shift = "+"),
                key("⌫", KEYCODE_DEL, 2f),
            ),
            listOf(key("Tab", KEYCODE_TAB, 1.5f)) + letters("qwertyuiop") + listOf(
                key("[", KEYCODE_LEFT_BRACKET, shift = "{"),
                key("]", KEYCODE_RIGHT_BRACKET, shift = "}"),
                key("\\", KEYCODE_BACKSLASH, 1.5f, shift = "|"),
            ),
            listOf(key("Caps", KEYCODE_CAPS_LOCK, 1.75f)) + letters("asdfghjkl") + listOf(
                key(";", KEYCODE_SEMICOLON, shift = ":"),
                key("'", KEYCODE_APOSTROPHE, shift = "\""),
                key("Enter", KEYCODE_ENTER, 2.25f),
            ),
            listOf(mod("Shift", Modifier.SHIFT, 2.25f)) + letters("zxcvbnm") + listOf(
                key(",", KEYCODE_COMMA, shift = "<"),
                key(".", KEYCODE_PERIOD, shift = ">"),
                key("/", KEYCODE_SLASH, shift = "?"),
                key("↑", KEYCODE_DPAD_UP),
                mod("Shift", Modifier.SHIFT, 1.75f),
            ),
            listOf(
                mod("Ctrl", Modifier.CTRL, 1.5f),
                mod("Win", Modifier.WIN, 1.25f),
                mod("Alt", Modifier.ALT, 1.25f),
                key("Space", KEYCODE_SPACE, 5.5f),
                KeySpec("Fn", KeyAction.SwitchLayer, 1.5f),
                key("Del", KEYCODE_FORWARD_DEL),
                key("←", KEYCODE_DPAD_LEFT),
                key("↓", KEYCODE_DPAD_DOWN),
                key("→", KEYCODE_DPAD_RIGHT),
            ),
        ),
    )

    val function = KeyboardLayer(
        "function",
        listOf(
            listOf(key("Esc", KEYCODE_ESCAPE, 1.5f)) +
                (1..12).map { key("F$it", KEYCODE_F1 + it - 1) } +
                listOf(key("PrtSc", KEYCODE_SYSRQ, 1.5f)),
            listOf(
                key("Ins", KEYCODE_INSERT, 1.5f),
                key("Home", KEYCODE_MOVE_HOME, 1.5f),
                key("PgUp", KEYCODE_PAGE_UP, 1.5f),
                key("ScrLk", KEYCODE_SCROLL_LOCK, 1.5f),
                key("Pause", KEYCODE_BREAK, 1.5f),
                key("7", KEYCODE_NUMPAD_7, 1.5f),
                key("8", KEYCODE_NUMPAD_8, 1.5f),
                key("9", KEYCODE_NUMPAD_9, 1.5f),
                key("−", KEYCODE_NUMPAD_SUBTRACT, 1.5f),
                key("NumLk", KEYCODE_NUM_LOCK, 1.5f),
            ),
            listOf(
                key("Del", KEYCODE_FORWARD_DEL, 1.5f),
                key("End", KEYCODE_MOVE_END, 1.5f),
                key("PgDn", KEYCODE_PAGE_DOWN, 1.5f),
                key("Menu", KEYCODE_MENU, 1.5f),
                key("Tab", KEYCODE_TAB, 1.5f),
                key("4", KEYCODE_NUMPAD_4, 1.5f),
                key("5", KEYCODE_NUMPAD_5, 1.5f),
                key("6", KEYCODE_NUMPAD_6, 1.5f),
                key("+", KEYCODE_NUMPAD_ADD, 1.5f),
                key("*", KEYCODE_NUMPAD_MULTIPLY, 1.5f),
            ),
            listOf(
                mod("Shift", Modifier.SHIFT, 3f),
                spacer(1.5f),
                key("↑", KEYCODE_DPAD_UP, 1.5f),
                spacer(1.5f),
                key("1", KEYCODE_NUMPAD_1, 1.5f),
                key("2", KEYCODE_NUMPAD_2, 1.5f),
                key("3", KEYCODE_NUMPAD_3, 1.5f),
                key("/", KEYCODE_NUMPAD_DIVIDE, 1.5f),
                key("Enter", KEYCODE_NUMPAD_ENTER, 1.5f),
            ),
            listOf(
                mod("Ctrl", Modifier.CTRL, 1.5f),
                mod("Alt", Modifier.ALT, 1.5f),
                key("←", KEYCODE_DPAD_LEFT, 1.5f),
                key("↓", KEYCODE_DPAD_DOWN, 1.5f),
                key("→", KEYCODE_DPAD_RIGHT, 1.5f),
                key("0", KEYCODE_NUMPAD_0, 3f),
                key(".", KEYCODE_NUMPAD_DOT, 1.5f),
                KeySpec("ABC", KeyAction.SwitchLayer, 3f),
            ),
        ),
    )
}
