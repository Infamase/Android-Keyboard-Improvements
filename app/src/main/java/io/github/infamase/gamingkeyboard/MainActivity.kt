package io.github.infamase.gamingkeyboard

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings.ACTION_INPUT_METHOD_SETTINGS
import android.provider.Settings.Secure
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import io.github.infamase.gamingkeyboard.ime.GamingKeyboardService

/** Setup steps, settings, and a text field for trying the keyboard. */
class MainActivity : Activity() {

    private lateinit var settings: Settings
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        settings = Settings(this)
        status = findViewById(R.id.status)

        findViewById<Button>(R.id.enable_button).setOnClickListener {
            startActivity(Intent(ACTION_INPUT_METHOD_SETTINGS))
        }
        findViewById<Button>(R.id.choose_button).setOnClickListener {
            getSystemService(InputMethodManager::class.java).showInputMethodPicker()
        }

        val heightLabel = findViewById<TextView>(R.id.key_height_label)
        findViewById<SeekBar>(R.id.key_height).apply {
            max = Settings.MAX_KEY_HEIGHT_DP - Settings.MIN_KEY_HEIGHT_DP
            progress = settings.keyHeightDp - Settings.MIN_KEY_HEIGHT_DP
            heightLabel.text = getString(R.string.key_height, settings.keyHeightDp)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    val dp = Settings.MIN_KEY_HEIGHT_DP + progress
                    settings.keyHeightDp = dp
                    heightLabel.text = getString(R.string.key_height, dp)
                }

                override fun onStartTrackingTouch(seekBar: SeekBar) = Unit
                override fun onStopTrackingTouch(seekBar: SeekBar) = Unit
            })
        }

        findViewById<Switch>(R.id.haptics).apply {
            isChecked = settings.haptics
            setOnCheckedChangeListener { _, checked -> settings.haptics = checked }
        }
        findViewById<Switch>(R.id.sticky_modifiers).apply {
            isChecked = settings.stickyModifiers
            setOnCheckedChangeListener { _, checked -> settings.stickyModifiers = checked }
        }
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }

    // The keyboard picker is a dialog, so closing it doesn't resume the activity.
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) updateStatus()
    }

    private fun updateStatus() {
        val component = ComponentName(this, GamingKeyboardService::class.java)
        val enabled = getSystemService(InputMethodManager::class.java).enabledInputMethodList
            .any { it.component == component }
        val selected = Secure.getString(contentResolver, Secure.DEFAULT_INPUT_METHOD)
            ?.let { ComponentName.unflattenFromString(it) } == component
        status.setText(
            when {
                !enabled -> R.string.status_not_enabled
                !selected -> R.string.status_not_selected
                else -> R.string.status_ready
            },
        )
    }
}
