package io.github.infamase.gamingkeyboard.keyboard

import android.view.KeyEvent

/** Delivers a raw key event to whatever app currently has focus (e.g. Moonlight). */
fun interface KeySink {
    fun send(action: Int, keyCode: Int, metaState: Int)
}

/**
 * Tracks which keys the receiving app (and, through Moonlight, the PC) believes are held down.
 *
 * Several touches can hold the same key at once, e.g. holding the Alt key while also pressing
 * the "Alt+Tab" shortcut. Each holder [acquire]s the key; the key-down event is sent on the first
 * acquire and the key-up only when the last holder [release]s it, so the host never sees a key
 * released while something on screen still holds it.
 */
class HostKeys(private val sink: KeySink) {
    private val holds = LinkedHashMap<Int, Int>()

    var capsLock = false

    fun isDown(keyCode: Int): Boolean = (holds[keyCode] ?: 0) > 0

    fun acquire(keyCode: Int) {
        val count = holds[keyCode] ?: 0
        holds[keyCode] = count + 1
        if (count == 0) sink.send(KeyEvent.ACTION_DOWN, keyCode, metaState())
    }

    fun release(keyCode: Int) {
        val count = holds[keyCode] ?: return
        if (count > 1) {
            holds[keyCode] = count - 1
        } else {
            holds.remove(keyCode)
            sink.send(KeyEvent.ACTION_UP, keyCode, metaState())
        }
    }

    /** Sends a key-down and key-up without holding the key. */
    fun tap(keyCode: Int) {
        acquire(keyCode)
        release(keyCode)
    }

    /** Releases every held key, most recently pressed first. */
    fun releaseAll() {
        for (keyCode in holds.keys.reversed()) {
            holds.remove(keyCode)
            sink.send(KeyEvent.ACTION_UP, keyCode, metaState())
        }
    }

    /** Android meta state matching the modifiers currently held, as a hardware keyboard would report. */
    fun metaState(): Int {
        var meta = if (capsLock) KeyEvent.META_CAPS_LOCK_ON else 0
        for (modifier in Modifier.entries) {
            if (isDown(modifier.keyCode)) meta = meta or modifier.metaMask
        }
        return meta
    }
}
