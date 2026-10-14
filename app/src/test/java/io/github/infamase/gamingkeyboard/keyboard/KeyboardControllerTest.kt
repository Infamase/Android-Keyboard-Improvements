package io.github.infamase.gamingkeyboard.keyboard

import android.view.KeyEvent.ACTION_DOWN
import android.view.KeyEvent.ACTION_UP
import android.view.KeyEvent.KEYCODE_A
import android.view.KeyEvent.KEYCODE_ALT_LEFT
import android.view.KeyEvent.KEYCODE_CAPS_LOCK
import android.view.KeyEvent.KEYCODE_D
import android.view.KeyEvent.KEYCODE_F4
import android.view.KeyEvent.KEYCODE_META_LEFT
import android.view.KeyEvent.KEYCODE_SHIFT_LEFT
import android.view.KeyEvent.KEYCODE_SPACE
import android.view.KeyEvent.KEYCODE_TAB
import android.view.KeyEvent.KEYCODE_W
import android.view.KeyEvent.META_ALT_ON
import android.view.KeyEvent.META_CAPS_LOCK_ON
import android.view.KeyEvent.META_SHIFT_ON
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class KeyboardControllerTest {

    private data class Sent(val action: Int, val keyCode: Int, val meta: Int)

    private val sent = mutableListOf<Sent>()
    private var now = 1_000L
    private val pending = mutableListOf<Pair<Long, () -> Unit>>()
    private lateinit var controller: KeyboardController

    @Before
    fun setUp() {
        val scheduler = Scheduler { delayMs, task ->
            val entry = (now + delayMs) to task
            pending += entry
            return@Scheduler { pending.remove(entry) }
        }
        controller = KeyboardController(HostKeys { a, k, m -> sent += Sent(a, k, m) }, scheduler) { now }
    }

    private fun advance(ms: Long) {
        now += ms
        pending.filter { it.first <= now }.forEach { pending.remove(it); it.second() }
    }

    private fun tap(pointer: Int, action: KeyAction, holdMs: Long = 50) {
        controller.press(pointer, action)
        advance(holdMs)
        controller.release(pointer)
    }

    private fun events() = sent.map { (if (it.action == ACTION_DOWN) "+" else "-") + it.keyCode }
    private fun down(code: Int) = "+$code"
    private fun up(code: Int) = "-$code"

    private val alt = KeyAction.Mod(Modifier.ALT)
    private val shift = KeyAction.Mod(Modifier.SHIFT)
    private val win = KeyAction.Mod(Modifier.WIN)
    private val tab = KeyAction.Press(KEYCODE_TAB)

    @Test
    fun plainKeyIsHeldUntilFingerLifts() {
        controller.press(0, KeyAction.Press(KEYCODE_A))
        assertEquals(listOf(down(KEYCODE_A)), events())
        advance(2_000)
        controller.release(0)
        assertEquals(listOf(down(KEYCODE_A), up(KEYCODE_A)), events())
    }

    @Test
    fun shortcutPressesInOrderAndReleasesInReverse() {
        tap(0, KeyAction.Press(KEYCODE_ALT_LEFT, KEYCODE_F4))
        assertEquals(
            listOf(down(KEYCODE_ALT_LEFT), down(KEYCODE_F4), up(KEYCODE_F4), up(KEYCODE_ALT_LEFT)),
            events(),
        )
        assertEquals(META_ALT_ON, sent[1].meta and META_ALT_ON)
        assertEquals(0, sent[3].meta and META_ALT_ON)
    }

    @Test
    fun multipleFingersHoldKeysIndependently() {
        controller.press(0, KeyAction.Press(KEYCODE_W))
        controller.press(1, KeyAction.Press(KEYCODE_SPACE))
        controller.release(1)
        controller.press(2, KeyAction.Press(KEYCODE_SPACE))
        controller.release(0)
        controller.release(2)
        assertEquals(
            listOf(
                down(KEYCODE_W), down(KEYCODE_SPACE), up(KEYCODE_SPACE),
                down(KEYCODE_SPACE), up(KEYCODE_W), up(KEYCODE_SPACE),
            ),
            events(),
        )
    }

    @Test
    fun tappedModifierAppliesToNextKeyOnly() {
        tap(0, win)
        assertEquals(emptyList<String>(), events())
        assertEquals(ModifierState.ONE_SHOT, controller.modifierState(Modifier.WIN))

        tap(1, KeyAction.Press(KEYCODE_D))
        assertEquals(
            listOf(down(KEYCODE_META_LEFT), down(KEYCODE_D), up(KEYCODE_D), up(KEYCODE_META_LEFT)),
            events(),
        )
        assertEquals(ModifierState.OFF, controller.modifierState(Modifier.WIN))

        sent.clear()
        tap(1, KeyAction.Press(KEYCODE_D))
        assertEquals(listOf(down(KEYCODE_D), up(KEYCODE_D)), events())
    }

    @Test
    fun heldModifierIsSentWhenAnotherKeyIsPressed() {
        controller.press(0, shift)
        controller.press(1, KeyAction.Press(KEYCODE_A))
        assertEquals(META_SHIFT_ON, sent.last().meta and META_SHIFT_ON)
        controller.release(1)
        controller.press(1, KeyAction.Press(KEYCODE_A))
        controller.release(1)
        controller.release(0)
        assertEquals(
            listOf(
                down(KEYCODE_SHIFT_LEFT), down(KEYCODE_A), up(KEYCODE_A),
                down(KEYCODE_A), up(KEYCODE_A), up(KEYCODE_SHIFT_LEFT),
            ),
            events(),
        )
        assertEquals(ModifierState.OFF, controller.modifierState(Modifier.SHIFT))
    }

    @Test
    fun modifierHeldAloneIsSentAfterHoldDelay() {
        controller.press(0, shift)
        advance(KeyboardController.HOLD_DELAY_MS - 1)
        assertEquals(emptyList<String>(), events())
        advance(1)
        assertEquals(listOf(down(KEYCODE_SHIFT_LEFT)), events())
        controller.release(0)
        assertEquals(listOf(down(KEYCODE_SHIFT_LEFT), up(KEYCODE_SHIFT_LEFT)), events())
        assertEquals(ModifierState.OFF, controller.modifierState(Modifier.SHIFT))
    }

    @Test
    fun doubleTapLocksAltSoTabCyclesWindows() {
        tap(0, alt)
        advance(100)
        tap(0, alt)
        assertEquals(ModifierState.LOCKED, controller.modifierState(Modifier.ALT))
        assertEquals(listOf(down(KEYCODE_ALT_LEFT)), events())

        repeat(3) { tap(1, tab) }
        tap(0, alt)
        assertEquals(
            listOf(down(KEYCODE_ALT_LEFT)) +
                List(3) { listOf(down(KEYCODE_TAB), up(KEYCODE_TAB)) }.flatten() +
                listOf(up(KEYCODE_ALT_LEFT)),
            events(),
        )
        assertTrue(sent.filter { it.keyCode == KEYCODE_TAB }.all { (it.meta and META_ALT_ON) != 0 })
        assertEquals(ModifierState.OFF, controller.modifierState(Modifier.ALT))
    }

    @Test
    fun slowSecondTapCancelsOneShot() {
        tap(0, alt)
        advance(KeyboardController.DOUBLE_TAP_MS + 100)
        tap(0, alt)
        assertEquals(ModifierState.OFF, controller.modifierState(Modifier.ALT))
        assertEquals(emptyList<String>(), events())
    }

    @Test
    fun heldAltAndShortcutDoNotReleaseAltEarly() {
        tap(0, alt)
        tap(0, alt) // locked
        tap(1, KeyAction.Press(KEYCODE_ALT_LEFT, KEYCODE_TAB))
        assertEquals(listOf(down(KEYCODE_ALT_LEFT), down(KEYCODE_TAB), up(KEYCODE_TAB)), events())
    }

    @Test
    fun releaseAllLiftsEverything() {
        tap(0, alt)
        tap(0, alt) // locked
        controller.press(1, KeyAction.Press(KEYCODE_W))
        controller.press(2, shift)
        controller.releaseAll()
        assertEquals(setOf(KEYCODE_ALT_LEFT, KEYCODE_W), sent.filter { it.action == ACTION_UP }.map { it.keyCode }.toSet())
        Modifier.entries.forEach { assertEquals(ModifierState.OFF, controller.modifierState(it)) }
        // Lifting fingers afterwards sends nothing more.
        sent.clear()
        controller.release(1)
        controller.release(2)
        advance(1_000)
        assertEquals(emptyList<String>(), events())
    }

    @Test
    fun nonStickyModifiersAreSentImmediately() {
        controller.stickyModifiers = false
        tap(0, shift)
        assertEquals(listOf(down(KEYCODE_SHIFT_LEFT), up(KEYCODE_SHIFT_LEFT)), events())
        assertEquals(ModifierState.OFF, controller.modifierState(Modifier.SHIFT))
    }

    @Test
    fun capsLockTogglesMetaState() {
        tap(0, KeyAction.Press(KEYCODE_CAPS_LOCK))
        tap(0, KeyAction.Press(KEYCODE_A))
        assertTrue(controller.capsLock)
        assertEquals(META_CAPS_LOCK_ON, sent.last().meta and META_CAPS_LOCK_ON)
        tap(0, KeyAction.Press(KEYCODE_CAPS_LOCK))
        assertEquals(false, controller.capsLock)
    }

    @Test
    fun everyRowFillsTheSameWidth() {
        for (layer in listOf(Layouts.main, Layouts.function)) {
            for (row in layer.rows) {
                assertEquals("${layer.name}: ${row.map { it.label }}", Layouts.ROW_UNITS, row.sumOf { it.width.toDouble() }.toFloat(), 0.001f)
            }
        }
        assertEquals(Layouts.main.rows.size, Layouts.function.rows.size)
    }
}
