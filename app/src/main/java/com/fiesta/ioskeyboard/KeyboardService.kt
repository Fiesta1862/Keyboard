package com.fiesta.ioskeyboard

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.inputmethodservice.InputMethodService
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.animation.OvershootInterpolator
import android.view.inputmethod.EditorInfo
import android.widget.LinearLayout
import android.widget.TextView

class KeyboardService : InputMethodService() {
    private var shifted = false
    private var symbols = false
    private var root: LinearLayout? = null

    private val ink = Color.rgb(35, 38, 44)
    private val keyText = Color.rgb(38, 40, 45)
    private val keyboardBg = Color.rgb(211, 214, 220)

    override fun onCreateInputView(): View = buildKeyboard()

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        val type = info?.inputType ?: InputType.TYPE_CLASS_TEXT
        shifted = type and InputType.TYPE_TEXT_FLAG_CAP_SENTENCES != 0 ||
            type and InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS != 0
        refreshKeyboard()
    }

    private fun buildKeyboard(): View {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(4), dp(5), dp(4), dp(7))
            background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(Color.rgb(218, 221, 226), keyboardBg)
            )
        }
        refreshKeyboard()
        return root!!
    }

    private fun refreshKeyboard() {
        val layout = root ?: return
        layout.removeAllViews()

        // Predictive-text strip, separated into three native-looking suggestion cells.
        val suggestions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(dp(3), 0, dp(3), dp(4))
        }
        listOf("✧", "", "").forEachIndexed { index, label ->
            val cell = TextView(this).apply {
                text = label
                textSize = 17f
                gravity = Gravity.CENTER
                setTextColor(Color.rgb(78, 81, 88))
            }
            suggestions.addView(cell, LinearLayout.LayoutParams(0, dp(29), 1f))
            if (index < 2) {
                val divider = View(this).apply { setBackgroundColor(0xFFB9BDC5.toInt()) }
                suggestions.addView(divider, LinearLayout.LayoutParams(dp(1), dp(19)))
            }
        }
        layout.addView(suggestions)

        val rows = if (symbols) {
            listOf(
                listOf("1","2","3","4","5","6","7","8","9","0"),
                listOf("@","#","$","&","*","-","+","(",")","/"),
                listOf("=","%","“","”",":",";","!","?","⌫")
            )
        } else {
            listOf(
                listOf("q","w","e","r","t","y","u","i","o","p"),
                listOf("a","s","d","f","g","h","j","k","l"),
                listOf("⇧","z","x","c","v","b","n","m","⌫")
            )
        }
        rows.forEachIndexed { index, keys -> addKeyRow(layout, keys, index) }

        val bottom = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        addKey(bottom, if (symbols) "ABC" else "?123", 1.42f, KeyStyle.SPECIAL, 46) {
            symbols = !symbols
            refreshKeyboard()
        }
        addKey(bottom, ",", 0.82f, KeyStyle.SPECIAL, 46) {
            currentInputConnection?.commitText(",", 1)
        }
        addKey(bottom, "☺", 0.82f, KeyStyle.SPECIAL, 46) {
            currentInputConnection?.commitText("😊", 1)
        }
        addKey(bottom, "English (US)", 3.75f, KeyStyle.LETTER, 46) {
            currentInputConnection?.commitText(" ", 1)
        }
        addKey(bottom, ".", 0.82f, KeyStyle.SPECIAL, 46) {
            currentInputConnection?.commitText(".", 1)
        }
        addKey(bottom, "return", 1.48f, KeyStyle.ACTION, 46) { sendEnter() }
        layout.addView(bottom)
    }

    private fun addKeyRow(parent: LinearLayout, keys: List<String>, rowIndex: Int) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            if (rowIndex == 1) setPadding(dp(12), 0, dp(12), 0)
        }
        keys.forEach { key ->
            val style = when (key) {
                "⇧", "⌫" -> KeyStyle.SPECIAL
                else -> KeyStyle.LETTER
            }
            val weight = if (key == "⇧" || key == "⌫") 1.42f else 1f
            val shown = if (shifted && !symbols && key.length == 1) key.uppercase() else key
            addKey(row, shown, weight, style, 48) {
                when (key) {
                    "⇧" -> {
                        shifted = !shifted
                        refreshKeyboard()
                    }
                    "⌫" -> currentInputConnection?.deleteSurroundingText(1, 0)
                    else -> {
                        val value = if (shifted && !symbols) key.uppercase() else key
                        currentInputConnection?.commitText(value, 1)
                        if (shifted && !symbols) {
                            shifted = false
                            refreshKeyboard()
                        }
                    }
                }
            }
        }
        parent.addView(row)
    }

    private enum class KeyStyle { LETTER, SPECIAL, ACTION }

    private fun addKey(
        row: LinearLayout,
        label: String,
        weight: Float,
        style: KeyStyle,
        height: Int,
        action: () -> Unit
    ) {
        val isLetter = style == KeyStyle.LETTER
        val key = TextView(this).apply {
            text = label
            textSize = when (label) {
                "English (US)" -> 11f
                "return", "?123", "ABC" -> 13f
                else -> 19f
            }
            setTextColor(if (style == KeyStyle.ACTION) Color.WHITE else keyText)
            gravity = Gravity.CENTER
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            background = keyBackground(style)
            elevation = dp(if (isLetter) 1 else 0).toFloat()
            isClickable = true
            isFocusable = true
            stateListAnimator = null
            setOnClickListener {
                animate().scaleX(0.94f).scaleY(0.92f).setDuration(65).withEndAction {
                    animate().scaleX(1f).scaleY(1f).setDuration(100)
                        .setInterpolator(OvershootInterpolator()).start()
                }.start()
                action()
            }
        }
        val params = LinearLayout.LayoutParams(0, dp(height), weight).apply {
            setMargins(dp(2), dp(4), dp(2), dp(4))
        }
        row.addView(key, params)
    }

    private fun keyBackground(style: KeyStyle): GradientDrawable {
        val colors = when (style) {
            KeyStyle.LETTER -> intArrayOf(Color.WHITE, 0xFFFDFDFE.toInt())
            KeyStyle.SPECIAL -> intArrayOf(0xFFE6E8EC.toInt(), 0xFFD9DCE2.toInt())
            KeyStyle.ACTION -> intArrayOf(0xFF8E98A8.toInt(), 0xFF7E899A.toInt())
        }
        return GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, colors).apply {
            cornerRadius = dp(6).toFloat()
            setStroke(dp(1), if (style == KeyStyle.LETTER) 0xFFE2E4E8.toInt() else 0xFFBEC3CC.toInt())
        }
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

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}
