package com.fiesta.ioskeyboard

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.inputmethod.InputMethodManager
import android.content.Context
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(28, 32, 28, 32)
            setBackgroundColor(0xFFF5F5F7.toInt())
        }

        val title = TextView(this).apply {
            text = "iOS Keyboard"
            textSize = 30f
            setTextColor(0xFF171719.toInt())
            gravity = Gravity.CENTER
        }
        val subtitle = TextView(this).apply {
            text = "An iPhone-inspired keyboard for Android"
            textSize = 15f
            setTextColor(0xFF77777D.toInt())
            gravity = Gravity.CENTER
            setPadding(0, 10, 0, 28)
        }
        root.addView(title)
        root.addView(subtitle)

        root.addView(actionButton("1. Enable keyboard") {
            startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
        })
        root.addView(actionButton("2. Select iOS Keyboard") {
            val manager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            manager.showInputMethodPicker()
        })
        root.addView(actionButton("Keyboard settings") {
            startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
        })

        val note = TextView(this).apply {
            text = "Your typing stays on your device. This starter keyboard does not collect or send what you type."
            textSize = 13f
            setTextColor(0xFF77777D.toInt())
            gravity = Gravity.CENTER
            setPadding(8, 28, 8, 0)
        }
        root.addView(note)
        setContentView(root)
    }

    private fun actionButton(label: String, action: () -> Unit) = Button(this).apply {
        text = label
        isAllCaps = false
        textSize = 15f
        setOnClickListener { action() }
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { setMargins(0, 7, 0, 7) }
    }
}
