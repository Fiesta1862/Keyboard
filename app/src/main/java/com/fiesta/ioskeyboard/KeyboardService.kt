package com.fiesta.ioskeyboard

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
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

    private val panel = Color.rgb(209, 213, 220)
    private val keyText = Color.rgb(35, 37, 42)
    private val functionKey = Color.rgb(174, 181, 192)
    private val keyWhite = Color.rgb(255, 255, 255)

    override fun onCreateInputView(): View {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(3), dp(5), dp(3), dp(6))
            setBackgroundColor(panel)
        }
        refreshKeyboard()
        return root!!
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        shifted = info?.inputType?.and(android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES) != 0
        symbols = false
        refreshKeyboard()
    }

    private fun refreshKeyboard() {
        val keyboard = root ?: return
        keyboard.removeAllViews()
        addSuggestionBar(keyboard)

        val rows = if (symbols) {
            listOf(
                listOf("1","2","3","4","5","6","7","8","9","0"),
                listOf("@","#","$","&","*","-","+","(",")","/"),
                listOf("=", "%", ":", ";", "!", "?", "'", "\"", "delete")
            )
        } else {
            listOf(
                listOf("q","w","e","r","t","y","u","i","o","p"),
                listOf("a","s","d","f","g","h","j","k","l"),
                listOf("shift","z","x","c","v","b","n","m","delete")
            )
        }

        rows.forEachIndexed { index, keys -> addLetterRow(keyboard, keys, index) }
        addBottomRow(keyboard)
    }

    private fun addSuggestionBar(parent: LinearLayout) {
        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(dp(2), 0, dp(2), dp(5))
        }
        repeat(3) { index ->
            val slot = FrameLayout(this)
            if (index != 0) {
                val divider = View(this).apply {
                    setBackgroundColor(0x557E858F)
                }
                slot.addView(divider, FrameLayout.LayoutParams(dp(1), dp(22), Gravity.CENTER_VERTICAL or Gravity.LEFT))
            }
            bar.addView(slot, LinearLayout.LayoutParams(0, dp(32), 1f))
        }
        parent.addView(bar)
    }

    private fun addLetterRow(parent: LinearLayout, keys: List<String>, rowIndex: Int) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            val sideInset = if (rowIndex == 1) dp(11) else 0
            setPadding(sideInset, 0, sideInset, 0)
        }
        keys.forEach { key ->
            val special = key == "shift" || key == "delete"
            val shown = if (shifted && !symbols && key.length == 1) key.uppercase() else key
            addKey(
                row = row,
                label = shown,
                weight = if (special) 1.42f else 1f,
                style = if (special) KeyStyle.FUNCTION else KeyStyle.LETTER
            ) {
                when (key) {
                    "shift" -> {
                        shifted = !shifted
                        refreshKeyboard()
                    }
                    "delete" -> currentInputConnection?.deleteSurroundingText(1, 0)
                    else -> {
                        currentInputConnection?.commitText(
                            if (shifted && !symbols) key.uppercase() else key, 1
                        )
                        if (shifted) {
                            shifted = false
                            refreshKeyboard()
                        }
                    }
                }
            }
        }
        parent.addView(row)
    }

    private fun addBottomRow(parent: LinearLayout) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }

        addKey(row, if (symbols) "ABC" else "123", 1.28f, KeyStyle.FUNCTION) {
            symbols = !symbols
            refreshKeyboard()
        }
        addKey(row, "globe", 0.78f, KeyStyle.FUNCTION) {
            switchToNextInputMethod(false)
        }
        addKey(row, "emoji", 0.78f, KeyStyle.FUNCTION) {
            currentInputConnection?.commitText("😊", 1)
        }
        addKey(row, "space", 3.65f, KeyStyle.LETTER) {
            currentInputConnection?.commitText(" ", 1)
        }
        addKey(row, ".", 0.72f, KeyStyle.LETTER) {
            currentInputConnection?.commitText(".", 1)
        }
        addKey(row, "return", 1.35f, KeyStyle.FUNCTION) {
            sendEnter()
        }
        parent.addView(row)
    }

    private enum class KeyStyle { LETTER, FUNCTION }

    private fun addKey(
        row: LinearLayout,
        label: String,
        weight: Float,
        style: KeyStyle,
        action: () -> Unit
    ) {
        val isLetter = style == KeyStyle.LETTER
        val isSpace = label == "space"
        val face = if (isLetter) keyWhite else functionKey

        val key = FrameLayout(this).apply {
            background = GradientDrawable().apply {
                setColor(face)
                cornerRadius = dp(5).toFloat()
                if (isLetter) setStroke(dp(0.6f), 0xFFE4E6E9.toInt())
            }
            elevation = dp(if (isLetter) 1.5f else 0.5f).toFloat()
            isClickable = true
            isFocusable = true
            setOnClickListener { action() }
            setOnTouchListener { view, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> view.animate()
                        .scaleX(0.96f).scaleY(0.94f)
                        .setDuration(55)
                        .setInterpolator(DecelerateInterpolator())
                        .start()
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> view.animate()
                        .scaleX(1f).scaleY(1f)
                        .setDuration(100)
                        .setInterpolator(DecelerateInterpolator())
                        .start()
                }
                false
            }
        }

        if (label in listOf("shift", "delete", "globe", "emoji")) {
            key.addView(KeyGlyphView(label, keyText), FrameLayout.LayoutParams(dp(25), dp(25), Gravity.CENTER))
        } else {
            val text = TextView(this).apply {
                text = when (label) {
                    "space" -> ""
                    "return" -> "return"
                    else -> label
                }
                textSize = when (label) {
                    "return", "123", "ABC" -> 14f
                    "space" -> 12f
                    else -> 23f
                }
                setTextColor(keyText)
                gravity = Gravity.CENTER
                typeface = Typeface.create("sans-serif", Typeface.NORMAL)
                includeFontPadding = false
            }
            key.addView(text, FrameLayout.LayoutParams(-1, -1))
        }

        val height = if (isSpace || label in listOf("123", "ABC", "return", ".")) dp(43) else dp(44)
        row.addView(key, LinearLayout.LayoutParams(0, height, weight).apply {
            setMargins(dp(2), dp(4), dp(2), dp(4))
        })
    }

    private inner class KeyGlyphView(
        private val glyph: String,
        color: Int
    ) : View(this@KeyboardService) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.STROKE
            strokeWidth = dp(1.8f)
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val cx = width / 2f
            val cy = height / 2f
            val unit = dp(1f).toFloat()
            when (glyph) {
                "shift" -> {
                    val path = Path().apply {
                        moveTo(cx, cy - 8 * unit)
                        lineTo(cx - 8 * unit, cy)
                        lineTo(cx - 4 * unit, cy)
                        lineTo(cx - 4 * unit, cy + 7 * unit)
                        lineTo(cx + 4 * unit, cy + 7 * unit)
                        lineTo(cx + 4 * unit, cy)
                        lineTo(cx + 8 * unit, cy)
                        close()
                    }
                    paint.style = Paint.Style.FILL
                    canvas.drawPath(path, paint)
                    paint.style = Paint.Style.STROKE
                }
                "delete" -> {
                    val path = Path().apply {
                        moveTo(cx - 10 * unit, cy)
                        lineTo(cx - 4 * unit, cy - 7 * unit)
                        lineTo(cx + 10 * unit, cy - 7 * unit)
                        lineTo(cx + 10 * unit, cy + 7 * unit)
                        lineTo(cx - 4 * unit, cy + 7 * unit)
                        close()
                    }
                    canvas.drawPath(path, paint)
                    canvas.drawLine(cx - 1 * unit, cy - 3 * unit, cx + 5 * unit, cy + 3 * unit, paint)
                    canvas.drawLine(cx + 5 * unit, cy - 3 * unit, cx - 1 * unit, cy + 3 * unit, paint)
                }
                "globe" -> {
                    canvas.drawCircle(cx, cy, 9 * unit, paint)
                    canvas.drawOval(RectF(cx - 4 * unit, cy - 9 * unit, cx + 4 * unit, cy + 9 * unit), paint)
                    canvas.drawLine(cx - 8 * unit, cy, cx + 8 * unit, cy, paint)
                    canvas.drawArc(RectF(cx - 8 * unit, cy - 4 * unit, cx + 8 * unit, cy + 4 * unit), 0f, 180f, false, paint)
                }
                "emoji" -> {
                    canvas.drawCircle(cx, cy, 9 * unit, paint)
                    paint.style = Paint.Style.FILL
                    canvas.drawCircle(cx - 3 * unit, cy - 2 * unit, 1 * unit, paint)
                    canvas.drawCircle(cx + 3 * unit, cy - 2 * unit, 1 * unit, paint)
                    paint.style = Paint.Style.STROKE
                    canvas.drawArc(RectF(cx - 4 * unit, cy - 2 * unit, cx + 4 * unit, cy + 5 * unit), 15f, 150f, false, paint)
                }
            }
        }
    }

    private fun sendEnter() {
        val connection = currentInputConnection ?: return
        when (currentInputEditorInfo?.imeOptions?.and(EditorInfo.IME_MASK_ACTION)) {
            EditorInfo.IME_ACTION_GO -> connection.performEditorAction(EditorInfo.IME_ACTION_GO)
            EditorInfo.IME_ACTION_SEARCH -> connection.performEditorAction(EditorInfo.IME_ACTION_SEARCH)
            EditorInfo.IME_ACTION_SEND -> connection.performEditorAction(EditorInfo.IME_ACTION_SEND)
            EditorInfo.IME_ACTION_NEXT -> connection.performEditorAction(EditorInfo.IME_ACTION_NEXT)
            EditorInfo.IME_ACTION_DONE -> connection.performEditorAction(EditorInfo.IME_ACTION_DONE)
            else -> connection.commitText("\n", 1)
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
    private fun dp(value: Float): Int = (value * resources.displayMetrics.density).toInt()
}
