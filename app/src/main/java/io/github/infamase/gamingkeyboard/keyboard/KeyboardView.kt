package io.github.infamase.gamingkeyboard.keyboard

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.WindowInsets

/**
 * Draws the shortcut row plus the current layer, and turns touches into [KeyboardController] calls.
 *
 * Keys stay pressed until the finger that pressed them lifts, even if it slides off the key,
 * which is what games expect for held keys.
 */
@SuppressLint("ViewConstructor")
class KeyboardView(
    context: Context,
    private val controller: KeyboardController,
    private val onHide: () -> Unit,
) : View(context) {

    private class PlacedKey(val spec: KeySpec, val bounds: RectF, val isShortcut: Boolean)

    private val density = resources.displayMetrics.density
    private var keyHeightPx = 36 * density
    private val gapPx = 3 * density
    private val cornerPx = 5 * density

    var hapticsEnabled = true

    private var layer = Layouts.main
    private var placedKeys: List<PlacedKey> = emptyList()
    private val pressedByPointer = HashMap<Int, KeySpec>()

    private val backgroundColor = Color.rgb(0x16, 0x18, 0x1d)
    private val keyColor = Color.rgb(0x2c, 0x30, 0x38)
    private val specialKeyColor = Color.rgb(0x22, 0x25, 0x2b)
    private val shortcutColor = Color.rgb(0x1f, 0x3a, 0x5c)
    private val pressedColor = Color.rgb(0x55, 0x5d, 0x6b)
    private val accentColor = Color.rgb(0x3d, 0x9b, 0xff)

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2 * density
        color = accentColor
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
    }

    init {
        setBackgroundColor(backgroundColor)
        isHapticFeedbackEnabled = true
        controller.onStateChanged = { invalidate() }
    }

    fun setKeyHeightDp(dp: Int) {
        val px = dp * density
        if (px != keyHeightPx) {
            keyHeightPx = px
            requestLayout()
        }
    }

    private val shortcutRowHeight get() = keyHeightPx * 0.9f
    private val rowCount get() = layer.rows.size

    override fun onApplyWindowInsets(insets: WindowInsets): WindowInsets {
        // Keep keys clear of the navigation bar / gesture area.
        val bottom = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            insets.getInsets(WindowInsets.Type.navigationBars()).bottom
        } else {
            @Suppress("DEPRECATION")
            insets.systemWindowInsetBottom
        }
        if (paddingBottom != bottom) setPadding(paddingLeft, paddingTop, paddingRight, bottom)
        return insets
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = gapPx + shortcutRowHeight + rowCount * keyHeightPx + paddingTop + paddingBottom
        setMeasuredDimension(width, height.toInt())
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        placeKeys()
    }

    private fun placeKeys() {
        val placed = ArrayList<PlacedKey>()
        val usableWidth = (width - paddingLeft - paddingRight).toFloat()
        var y = paddingTop + gapPx / 2

        fun placeRow(row: List<KeySpec>, rowHeight: Float, isShortcut: Boolean = false) {
            val unitWidth = usableWidth / row.sumOf { it.width.toDouble() }.toFloat()
            var x = paddingLeft.toFloat()
            for (spec in row) {
                val w = spec.width * unitWidth
                placed += PlacedKey(spec, RectF(x, y, x + w, y + rowHeight), isShortcut)
                x += w
            }
            y += rowHeight
        }

        placeRow(Layouts.shortcuts, shortcutRowHeight, isShortcut = true)
        y += gapPx / 2
        layer.rows.forEach { placeRow(it, keyHeightPx) }
        placedKeys = placed
    }

    private fun keyAt(x: Float, y: Float): KeySpec? {
        placedKeys.firstOrNull { it.bounds.contains(x, y) }?.let { return it.spec }
        // Touches in the gaps or padding go to the nearest key.
        return placedKeys.minByOrNull {
            val dx = maxOf(it.bounds.left - x, 0f, x - it.bounds.right)
            val dy = maxOf(it.bounds.top - y, 0f, y - it.bounds.bottom)
            dx * dx + dy * dy
        }?.spec
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val index = event.actionIndex
                val spec = keyAt(event.getX(index), event.getY(index)) ?: return true
                onKeyDown(event.getPointerId(index), spec)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                onKeyUp(event.getPointerId(event.actionIndex))
            }
            MotionEvent.ACTION_CANCEL -> {
                pressedByPointer.keys.toList().forEach(::onKeyUp)
            }
        }
        return true
    }

    private fun onKeyDown(pointerId: Int, spec: KeySpec) {
        when (spec.action) {
            KeyAction.Spacer -> return
            KeyAction.Hide -> onHide()
            KeyAction.SwitchLayer -> {
                layer = if (layer === Layouts.main) Layouts.function else Layouts.main
                requestLayout()
            }
            else -> controller.press(pointerId, spec.action)
        }
        pressedByPointer[pointerId] = spec
        if (hapticsEnabled) performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        invalidate()
    }

    private fun onKeyUp(pointerId: Int) {
        if (pressedByPointer.remove(pointerId) == null) return
        controller.release(pointerId)
        invalidate()
    }

    /** Lifts every finger, e.g. when the keyboard is hidden mid-press. */
    fun resetTouches() {
        pressedByPointer.clear()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val shift = controller.shiftActive
        for (key in placedKeys) {
            val spec = key.spec
            if (spec.action == KeyAction.Spacer) continue

            val rect = RectF(key.bounds).apply { inset(gapPx / 2, gapPx / 2) }
            val modifierState = (spec.action as? KeyAction.Mod)?.let { controller.modifierState(it.modifier) }
            val isPressed = pressedByPointer.values.any { it === spec } || modifierState == ModifierState.HELD
            val isCapsKey = spec.action == CAPS_LOCK

            fillPaint.color = when {
                isPressed -> pressedColor
                modifierState == ModifierState.LOCKED || (isCapsKey && controller.capsLock) -> accentColor
                key.isShortcut -> shortcutColor
                spec.label.length == 1 || spec.action == SPACE -> keyColor
                else -> specialKeyColor
            }
            canvas.drawRoundRect(rect, cornerPx, cornerPx, fillPaint)
            if (modifierState == ModifierState.ONE_SHOT) {
                canvas.drawRoundRect(rect, cornerPx, cornerPx, outlinePaint)
            }

            val label = when {
                spec.isLetter -> if (shift != controller.capsLock) spec.label.uppercase() else spec.label
                shift && spec.shiftLabel != null -> spec.shiftLabel
                else -> spec.label
            }
            drawLabel(canvas, label, rect)
        }
    }

    private fun drawLabel(canvas: Canvas, label: String, rect: RectF) {
        textPaint.textSize = if (label.length <= 1) keyHeightPx * 0.45f else keyHeightPx * 0.32f
        val maxWidth = rect.width() - 4 * density
        val measured = textPaint.measureText(label)
        if (measured > maxWidth) textPaint.textSize *= maxWidth / measured
        val baseline = rect.centerY() - (textPaint.descent() + textPaint.ascent()) / 2
        canvas.drawText(label, rect.centerX(), baseline, textPaint)
    }

    private companion object {
        val CAPS_LOCK = KeyAction.Press(KeyEvent.KEYCODE_CAPS_LOCK)
        val SPACE = KeyAction.Press(KeyEvent.KEYCODE_SPACE)
    }
}
