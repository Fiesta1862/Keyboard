package com.fiesta.ioskeyboard

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.inputmethodservice.InputMethodService
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.LinearLayout
import android.widget.TextView

class KeyboardService : InputMethodService() {
    private var shifted = false
    private var symbols = false
    private var root: LinearLayout? = null

    override fun onCreateInputView(): View {
        return buildKeyboard()
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        shifted = info?.inputType?.and(android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES) != 0
        refreshKeyboard()
    }

    private fun buildKeyboard(): View {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(4), dp(7), dp(4), dp(8))
            setBackgroundColor(Color.rgb(207, 211, 218))
        }
        root = layout
        refreshKeyboard()
        return layout
    }

    private fun refreshKeyboard() {
        val layout = root ?: return
        layout.removeAllViews()

        val suggestions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(7))
        }
        listOf("☷", " ", " ", " ").forEachIndexed { index, label ->
            val text = TextView(this).apply {
                this.text = if (index == 0) "✧" else label
                textSize = 18f
                gravity = Gravity.CENTER
                setTextColor(Color.rgb(65, 68, 75))
            }
            suggestions.addView(text, LinearLayout.LayoutParams(0, dp(30), 1f))
        }
        layout.addView(suggestions)

        val letters = if (symbols) {
            listOf(
                listOf("1","2","3","4","5","6","7","8","9","0"),
                listOf("@","#","$","&","*","-","+","(",")","/"),
                listOf("=", "%", "“", "”", ":", ";", "!", "?", "⌫")
            )
        } else {
            listOf(
                listOf("q","w","e","r","t","y","u","i","o","p"),
                listOf("a","s","d","f","g","h","j","k","l"),
                listOf("⇧","z","x","c","v","b","n","m","⌫")
            )
        }
        letters.forEachIndexed { index, row ->
            addKeyRow(layout, row, index)
        }

        val bottom = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        addKey(bottom, if (symbols) "ABC" else "?123", 1.35f, special = true) {
            symbols = !symbols
            refreshKeyboard()
        }
        addKey(bottom, "🌐", 0.9f, special = true) { switchToNextInputMethod(false) }
        addKey(bottom, "😊", 0.9f, special = true) { currentInputConnection?.commitText("😊", 1) }
        addKey(bottom, "space", 4.4f, special = false) { currentInputConnection?.commitText(" ", 1) }
        addKey(bottom, "return", 1.65f, special = true) { sendEnter() }
        layout.addView(bottom)
    }

    private fun addKeyRow(parent: LinearLayout, keys: List<String>, rowIndex: Int) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        val sideInset = if (rowIndex == 1) dp(13) else 0
        row.setPadding(sideInset, 0, sideInset, 0)
        keys.forEach { key ->
            val weight = when (key) {
                "⇧", "⌫" -> 1.45f
                else -> 1f
            }
            addKey(row, if (shifted && !symbols && key.length == 1) key.uppercase() else key,
                weight, special = key == "⇧" || key == "⌫") {
                when (key) {
                    "⇧" -> { shifted = !shifted; refreshKeyboard() }
                    "⌫" -> currentInputConnection?.deleteSurroundingText(1, 0)
                    else -> {
                        currentInputConnection?.commitText(
                            if (shifted && !symbols) key.uppercase() else key, 1
                        )
                        if (shifted) { shifted = false; refreshKeyboard() }
                    }
                }
            }
        }
        parent.addView(row)
    }

    private fun addKey(
        row: LinearLayout,
        label: String,
        weight: Float,
        special: Boolean,
        action: () -> Unit
    ) {
        val key = TextView(this).apply {
            text = label
            textSize = when (label) { "space" -> 14f; "return" -> 14f; else -> 20f }
            setTextColor(if (special) Color.rgb(35, 38, 43) else Color.rgb(25, 27, 31))
            gravity = Gravity.CENTER
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            background = rounded(if (special) 0xFFADB4BE.toInt() else Color.WHITE, 6)
            elevation = dp(1).toFloat()
            isClickable = true
            isFocusable = true
            setOnClickListener { action() }
        }
        val params = LinearLayout.LayoutParams(0, dp(47), weight).apply {
            setMargins(dp(2), dp(4), dp(2), dp(4))
        }
        row.addView(key, params)
    }

    private fun sendEnter() {
        val ic = currentInputConnection ?: return
        when (currentInputEditorInfo?.imeOptions?.and(EditorInfo.IME_MASK_ACTION)) {
            EditorInfo.IME_ACTION_GO -> ic.performEditorAction(EditorInfo.IME_ACTION_GO)
            EditorInfo.IME_ACTION_SEARCH -> ic.performEditorAction(EditorInfo.IME_ACTION_SEARCH)
            EditorInfo.IME_ACTION_SEND -> ic.performEditorAction(EditorInfo.IME_ACTION_SEND)
            EditorInfo.IME_ACTION_NEXT -> ic.performEditorAction(EditorInfo.IME_ACTION_NEXT)
            EditorInfo.IME_ACTION_DONE -> ic.performEditorAction(EditorInfo.IME_ACTION_DONE)
            else -> ic.commitText("\n", 1)
        }
    }

    private fun rounded(color: Int, radiusDp: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radiusDp).toFloat()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
