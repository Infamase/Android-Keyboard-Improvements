package io.github.infamase.gamingkeyboard.keyboard

import android.view.KeyEvent
import java.util.EnumMap

/** Runs a task later. Returns a function that cancels it. */
fun interface Scheduler {
    fun postDelayed(delayMs: Long, task: () -> Unit): () -> Unit
}

/** How a modifier key currently looks and behaves. */
enum class ModifierState { OFF, HELD, ONE_SHOT, LOCKED }

/**
 * Turns touches on keys into key events, independent of how the keyboard is drawn.
 *
 * Every touch is identified by its pointer id, so several fingers can hold different keys at
 * once (e.g. W + Shift + Space), and each key is held on the host for exactly as long as the
 * finger stays on it.
 *
 * With [stickyModifiers] on, a modifier key works three ways:
 *  - **Hold** it and press other keys: it is held on the host for as long as the finger stays.
 *    Holding it alone for [HOLD_DELAY_MS] also sends it, so games see a held Shift/Ctrl.
 *  - **Tap** it: nothing is sent yet; it applies to the next key (one-shot), so a tap on Win
 *    followed by D sends Win+D without opening the Start menu first.
 *  - **Double-tap** it: it locks down on the host until tapped again. Locking Alt and tapping Tab
 *    repeatedly cycles through windows, exactly like holding Alt on a PC keyboard.
 *
 * With [stickyModifiers] off, modifiers are plain keys: down on touch, up on release.
 */
class KeyboardController(
    private val host: HostKeys,
    private val scheduler: Scheduler,
    private val clock: () -> Long,
) {
    var onStateChanged: () -> Unit = {}

    var stickyModifiers = true
        set(value) {
            if (field != value) {
                releaseAll()
                field = value
            }
        }

    private class ModifierTouch {
        var fingers = 0
        var sentDown = false
        var unlocking = false
        var cancelHoldTimer: (() -> Unit)? = null
    }

    private sealed interface Touch {
        class OnModifier(val modifier: Modifier) : Touch
        class OnKeys(val keyCodes: List<Int>, val oneShots: List<Modifier>) : Touch
    }

    private val latched = EnumMap<Modifier, ModifierState>(Modifier::class.java)
    private val lastTapAt = EnumMap<Modifier, Long>(Modifier::class.java)
    private val modifierTouches = EnumMap<Modifier, ModifierTouch>(Modifier::class.java).apply {
        Modifier.entries.forEach { put(it, ModifierTouch()) }
    }
    private val touches = HashMap<Int, Touch>()

    val capsLock: Boolean get() = host.capsLock

    fun modifierState(modifier: Modifier): ModifierState {
        val latchState = latched[modifier] ?: ModifierState.OFF
        if (latchState != ModifierState.OFF) return latchState
        return if (modifierTouches.getValue(modifier).fingers > 0) ModifierState.HELD else ModifierState.OFF
    }

    /** True when letters should show (and, on the host, produce) their shifted form. */
    val shiftActive: Boolean get() = modifierState(Modifier.SHIFT) != ModifierState.OFF

    /** A finger with id [pointerId] went down on a key with [action]. */
    fun press(pointerId: Int, action: KeyAction) {
        if (touches.containsKey(pointerId)) release(pointerId)
        when (action) {
            is KeyAction.Mod -> {
                touches[pointerId] = Touch.OnModifier(action.modifier)
                pressModifier(action.modifier)
            }
            is KeyAction.Press -> touches[pointerId] = pressKeys(action.keyCodes)
            else -> return
        }
        onStateChanged()
    }

    /** The finger with id [pointerId] lifted. */
    fun release(pointerId: Int) {
        when (val touch = touches.remove(pointerId) ?: return) {
            is Touch.OnModifier -> releaseModifier(touch.modifier)
            is Touch.OnKeys -> {
                touch.keyCodes.asReversed().forEach(host::release)
                touch.oneShots.forEach { host.release(it.keyCode) }
            }
        }
        onStateChanged()
    }

    /** Lifts every key and clears latched modifiers, e.g. when the keyboard is hidden. */
    fun releaseAll() {
        for (touch in modifierTouches.values) {
            touch.cancelHoldTimer?.invoke()
            touch.cancelHoldTimer = null
            touch.fingers = 0
            touch.sentDown = false
            touch.unlocking = false
        }
        touches.clear()
        latched.clear()
        host.releaseAll()
        onStateChanged()
    }

    private fun pressModifier(modifier: Modifier) {
        val touch = modifierTouches.getValue(modifier)
        if (++touch.fingers > 1) return

        if (!stickyModifiers) {
            host.acquire(modifier.keyCode)
            touch.sentDown = true
            return
        }
        if (latched[modifier] == ModifierState.LOCKED) {
            touch.unlocking = true
            return
        }
        touch.sentDown = false
        touch.cancelHoldTimer = scheduler.postDelayed(HOLD_DELAY_MS) {
            touch.cancelHoldTimer = null
            if (touch.fingers > 0 && !touch.sentDown) {
                host.acquire(modifier.keyCode)
                touch.sentDown = true
                onStateChanged()
            }
        }
    }

    private fun releaseModifier(modifier: Modifier) {
        val touch = modifierTouches.getValue(modifier)
        if (touch.fingers == 0 || --touch.fingers > 0) return
        touch.cancelHoldTimer?.invoke()
        touch.cancelHoldTimer = null

        when {
            touch.unlocking -> {
                touch.unlocking = false
                latched.remove(modifier)
                host.release(modifier.keyCode)
            }
            touch.sentDown -> {
                // Used as a held modifier.
                touch.sentDown = false
                host.release(modifier.keyCode)
                if (stickyModifiers) latched.remove(modifier)
            }
            else -> {
                // A quick tap: latch it instead of sending anything.
                val now = clock()
                val tappedRecently = now - (lastTapAt[modifier] ?: Long.MIN_VALUE / 2) <= DOUBLE_TAP_MS
                when (latched[modifier]) {
                    ModifierState.ONE_SHOT -> if (tappedRecently) {
                        latched[modifier] = ModifierState.LOCKED
                        host.acquire(modifier.keyCode)
                    } else {
                        latched.remove(modifier)
                    }
                    else -> latched[modifier] = ModifierState.ONE_SHOT
                }
                lastTapAt[modifier] = now
            }
        }
    }

    private fun pressKeys(keyCodes: List<Int>): Touch.OnKeys {
        // Modifiers whose fingers are down but haven't been sent yet apply to this key.
        for ((modifier, touch) in modifierTouches) {
            if (touch.fingers > 0 && !touch.sentDown && !touch.unlocking) {
                touch.cancelHoldTimer?.invoke()
                touch.cancelHoldTimer = null
                host.acquire(modifier.keyCode)
                touch.sentDown = true
            }
        }

        // One-shot modifiers apply to this key only, and are released together with it.
        val oneShots = latched.filterValues { it == ModifierState.ONE_SHOT }.keys.toList()
        for (modifier in oneShots) {
            latched.remove(modifier)
            host.acquire(modifier.keyCode)
        }

        if (KeyEvent.KEYCODE_CAPS_LOCK in keyCodes) host.capsLock = !host.capsLock
        keyCodes.forEach(host::acquire)
        return Touch.OnKeys(keyCodes, oneShots)
    }

    companion object {
        const val HOLD_DELAY_MS = 250L
        const val DOUBLE_TAP_MS = 400L
    }
}
