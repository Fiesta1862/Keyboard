package com.fiesta.ioskeyboard

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.inputmethodservice.InputMethodService
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.inputmethod.EditorInfo
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView

class KeyboardService : InputMethodService() {
    private var shifted = false
    private var symbols = false
    private var root: LinearLayout? = null
    private val board = 0xFFD3D6DC.toInt()
    private val ink = 0xFF25272D.toInt()

    override fun onCreateInputView(): View = buildKeyboard()

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        shifted = info?.inputType?.and(android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES) != 0
        refreshKeyboard()
    }

    private fun buildKeyboard(): View {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(3), dp(5), dp(3), dp(7))
            setBackgroundColor(board)
        }
        refreshKeyboard()
        return root!!
    }

    private fun refreshKeyboard() {
        val layout = root ?: return
        layout.removeAllViews()

        val suggestions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(dp(3), 0, dp(3), dp(5))
        }
        listOf("✧", "", "").forEachIndexed { i, value ->
            val cell = TextView(this).apply {
                text = value
                textSize = 17f
                gravity = Gravity.CENTER
                setTextColor(0xFF626873.toInt())
                if (i > 0) background = GradientDrawable().apply {
                    setColor(Color.TRANSPARENT)
                    setStroke(dp(1), 0x33777D87)
                    cornerRadius = dp(1).toFloat()
                }
            }
            suggestions.addView(cell, LinearLayout.LayoutParams(0, dp(32), 1f))
        }
        layout.addView(suggestions)

        val rows = if (symbols) listOf(
            listOf("1","2","3","4","5","6","7","8","9","0"),
            listOf("@","#","$","&","*","-","+","(",")","/"),
            listOf("=", "%", "“", "”", ":", ";", "!", "?", "⌫")
        ) else listOf(
            listOf("q","w","e","r","t","y","u","i","o","p"),
            listOf("a","s","d","f","g","h","j","k","l"),
            listOf("⇧","z","x","c","v","b","n","m","⌫")
        )
        rows.forEachIndexed { index, keys -> addKeyRow(layout, keys, index) }

        val bottom = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        addKey(bottom, if (symbols) "ABC" else "?123", 1.42f, Style.FUNCTION) {
            symbols = !symbols; refreshKeyboard()
        }
        addKey(bottom, "🌐", 0.82f, Style.FUNCTION) { switchToNextInputMethod(false) }
        addKey(bottom, "☺", 0.82f, Style.FUNCTION) { currentInputConnection?.commitText("😊", 1) }
        addKey(bottom, "space", 4.25f, Style.LETTER) { currentInputConnection?.commitText(" ", 1) }
        addKey(bottom, "return", 1.55f, Style.RETURN) { sendEnter() }
        layout.addView(bottom)
    }

    private fun addKeyRow(parent: LinearLayout, keys: List<String>, rowIndex: Int) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            val inset = if (rowIndex == 1) dp(12) else 0
            setPadding(inset, 0, inset, 0)
        }
        keys.forEach { key ->
            val special = key == "⇧" || key == "⌫"
            val shown = if (shifted && !symbols && key.length == 1) key.uppercase() else key
            addKey(row, shown, if (special) 1.42f else 1f,
                if (special) Style.FUNCTION else Style.LETTER) {
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

    private enum class Style { LETTER, FUNCTION, RETURN }

    private fun addKey(row: LinearLayout, label: String, weight: Float, style: Style, action: () -> Unit) {
        val face = when (style) {
            Style.LETTER -> Color.WHITE
            Style.FUNCTION -> 0xFFB8BEC8.toInt()
            Style.RETURN -> 0xFF929BA9.toInt()
        }
        val frame = FrameLayout(this).apply {
            background = GradientDrawable().apply {
                setColor(face)
                cornerRadius = dp(if (style == Style.LETTER) 5 else 6).toFloat()
                if (style == Style.LETTER) setStroke(dp(1), 0xFFE6E7EA.toInt())
            }
            elevation = dp(if (style == Style.LETTER) 2 else 1).toFloat()
            isClickable = true
            isFocusable = true
            setOnClickListener { action() }
            setOnTouchListener { view, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> view.animate().scaleX(.94f).scaleY(.94f)
                        .setDuration(65).setInterpolator(DecelerateInterpolator()).start()
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL ->
                        view.animate().scaleX(1f).scaleY(1f).setDuration(95)
                            .setInterpolator(DecelerateInterpolator()).start()
                }
                false
            }
        }
        val main = TextView(this).apply {
            text = when (label) { "space" -> ""; else -> label }
            textSize = when (label) {
                "space" -> 13f
                "return", "?123", "ABC" -> 13f
                "⇧", "⌫", "🌐", "☺" -> 20f
                else -> 22f
            }
            setTextColor(if (style == Style.RETURN) Color.WHITE else ink)
            gravity = Gravity.CENTER
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        }
        frame.addView(main, FrameLayout.LayoutParams(-1, -1))

        if (style == Style.LETTER && label.length == 1 && label[0].isLetter()) {
            val legend = TextView(this).apply {
                text = when (label.lowercase()) {
                    "q" -> "1"; "w" -> "2"; "e" -> "3"; "r" -> "4"; "t" -> "5"
                    "y" -> "6"; "u" -> "7"; "i" -> "8"; "o" -> "9"; "p" -> "0"
                    else -> ""
                }
                textSize = 8f
                setTextColor(0xFF9A9DA3.toInt())
                gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                setPadding(0, dp(3), 0, 0)
            }
            frame.addView(legend, FrameLayout.LayoutParams(-1, dp(15), Gravity.TOP))
        }

        val height = if (label in listOf("space", "return", "?123", "ABC")) dp(44) else dp(48)
        row.addView(frame, LinearLayout.LayoutParams(0, height, weight).apply {
            setMargins(dp(2), dp(4), dp(2), dp(4))
        })
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

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
    private fun dp(value: Float): Int = (value * resources.displayMetrics.density).toInt()
}
