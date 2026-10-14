package io.github.infamase.gamingkeyboard.ime

import android.inputmethodservice.InputMethodService
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.InputDevice
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import io.github.infamase.gamingkeyboard.Settings
import io.github.infamase.gamingkeyboard.keyboard.HostKeys
import io.github.infamase.gamingkeyboard.keyboard.KeyboardController
import io.github.infamase.gamingkeyboard.keyboard.KeyboardView
import io.github.infamase.gamingkeyboard.keyboard.Scheduler

/**
 * The input method. Every key is sent as a raw key event (never as text), the same way a USB
 * keyboard would deliver it, so apps like Moonlight can forward each press and release to the PC.
 */
class GamingKeyboardService : InputMethodService() {

    private val handler = Handler(Looper.getMainLooper())
    private val downTimes = HashMap<Int, Long>()
    private lateinit var settings: Settings
    private lateinit var controller: KeyboardController
    private var keyboardView: KeyboardView? = null

    override fun onCreate() {
        super.onCreate()
        settings = Settings(this)
        val scheduler = Scheduler { delayMs, task ->
            val runnable = Runnable { task() }
            handler.postDelayed(runnable, delayMs)
            return@Scheduler { handler.removeCallbacks(runnable) }
        }
        controller = KeyboardController(HostKeys(::sendKey), scheduler, SystemClock::uptimeMillis)
    }

    override fun onCreateInputView(): View =
        KeyboardView(this, controller, onHide = { requestHideSelf(0) }).also { keyboardView = it }

    // Never take over the whole screen in landscape; the game has to stay visible.
    override fun onEvaluateFullscreenMode(): Boolean = false

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        controller.stickyModifiers = settings.stickyModifiers
        keyboardView?.apply {
            setKeyHeightDp(settings.keyHeightDp)
            hapticsEnabled = settings.haptics
        }
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        releaseEverything()
        super.onFinishInputView(finishingInput)
    }

    override fun onWindowHidden() {
        releaseEverything()
        super.onWindowHidden()
    }

    override fun onFinishInput() {
        releaseEverything()
        super.onFinishInput()
    }

    /** Never leave a key stuck down on the PC when the keyboard goes away. */
    private fun releaseEverything() {
        keyboardView?.resetTouches()
        controller.releaseAll()
    }

    private fun sendKey(action: Int, keyCode: Int, metaState: Int) {
        val connection = currentInputConnection ?: return
        val now = SystemClock.uptimeMillis()
        val downTime = if (action == KeyEvent.ACTION_DOWN) {
            downTimes[keyCode] = now
            now
        } else {
            downTimes.remove(keyCode) ?: now
        }
        connection.sendKeyEvent(
            KeyEvent(
                downTime, now, action, keyCode, 0, metaState,
                KeyCharacterMap.VIRTUAL_KEYBOARD, 0,
                KeyEvent.FLAG_SOFT_KEYBOARD or KeyEvent.FLAG_KEEP_TOUCH_MODE,
                InputDevice.SOURCE_KEYBOARD,
            ),
        )
    }
}
