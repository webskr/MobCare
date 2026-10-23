package com.mobcare.keyboard.view

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import com.mobcare.R
import com.mobcare.keyboard.model.Key
import com.mobcare.keyboard.model.Keyboard
import com.mobcare.keyboard.model.KeyboardMode
import com.mobcare.keyboard.service.KeyActionHandler
import com.mobcare.keyboard.touch.PointerTracker

class KeyboardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    val keyboard = Keyboard(context)
    private var pointerTracker: PointerTracker? = null

    // Theme index: 0 = Dark Charcoal, 1 = Navy Blue, 2 = Emerald Green
    var currentThemeIndex = 0
        private set

    // Gboard Authentic Paints
    private val paintKeyNormal = Paint(Paint.ANTI_ALIAS_FLAG)
    private val paintKeyNormalShadow = Paint(Paint.ANTI_ALIAS_FLAG)
    private val paintKeyNormalPressed = Paint(Paint.ANTI_ALIAS_FLAG)

    private val paintKeyModifier = Paint(Paint.ANTI_ALIAS_FLAG)
    private val paintKeyModifierShadow = Paint(Paint.ANTI_ALIAS_FLAG)
    private val paintKeyModifierPressed = Paint(Paint.ANTI_ALIAS_FLAG)

    private val paintKeyAction = Paint(Paint.ANTI_ALIAS_FLAG)
    private val paintKeyActionShadow = Paint(Paint.ANTI_ALIAS_FLAG)
    private val paintKeyActionPressed = Paint(Paint.ANTI_ALIAS_FLAG)

    private val paintLabel = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    }

    private val paintHint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#9AA0A6")
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    }

    private val paintSpaceSubtitle = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#80868B")
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    }

    private val paintTrail = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#4285F4")
        style = Paint.Style.STROKE
        strokeWidth = 14f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        pathEffect = CornerPathEffect(30f)
    }

    private val paintBubbleBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#38404E")
        style = Paint.Style.FILL
        setShadowLayer(8f, 0f, 4f, Color.parseColor("#80000000"))
    }
    private val paintBubbleText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    }

    private val iconCache = mutableMapOf<Int, Drawable>()
    private val bubbleRect = RectF()
    private val trailPath = Path()

    init {
        applyTheme(0)
    }

    fun applyTheme(themeIndex: Int) {
        currentThemeIndex = themeIndex % 3
        when (currentThemeIndex) {
            0 -> { // Gboard Dark Charcoal
                paintKeyNormal.color = Color.parseColor("#2E333D")
                paintKeyNormalShadow.color = Color.parseColor("#1F232B")
                paintKeyNormalPressed.color = Color.parseColor("#404654")

                paintKeyModifier.color = Color.parseColor("#242831")
                paintKeyModifierShadow.color = Color.parseColor("#1A1D24")
                paintKeyModifierPressed.color = Color.parseColor("#343A46")

                paintKeyAction.color = Color.parseColor("#00A389")
                paintKeyActionShadow.color = Color.parseColor("#007361")
                paintKeyActionPressed.color = Color.parseColor("#008771")
            }
            1 -> { // Navy Blue Theme
                paintKeyNormal.color = Color.parseColor("#1E293B")
                paintKeyNormalShadow.color = Color.parseColor("#0F172A")
                paintKeyNormalPressed.color = Color.parseColor("#334155")

                paintKeyModifier.color = Color.parseColor("#172033")
                paintKeyModifierShadow.color = Color.parseColor("#0B132B")
                paintKeyModifierPressed.color = Color.parseColor("#283548")

                paintKeyAction.color = Color.parseColor("#2563EB")
                paintKeyActionShadow.color = Color.parseColor("#1D4ED8")
                paintKeyActionPressed.color = Color.parseColor("#1E40AF")
            }
            2 -> { // Emerald Green Theme
                paintKeyNormal.color = Color.parseColor("#163A34")
                paintKeyNormalShadow.color = Color.parseColor("#0A211D")
                paintKeyNormalPressed.color = Color.parseColor("#23544C")

                paintKeyModifier.color = Color.parseColor("#112B26")
                paintKeyModifierShadow.color = Color.parseColor("#071916")
                paintKeyModifierPressed.color = Color.parseColor("#1B423B")

                paintKeyAction.color = Color.parseColor("#10B981")
                paintKeyActionShadow.color = Color.parseColor("#059669")
                paintKeyActionPressed.color = Color.parseColor("#047857")
            }
        }
        invalidate()
    }

    fun cycleTheme(): String {
        applyTheme(currentThemeIndex + 1)
        return when (currentThemeIndex) {
            0 -> "Gboard Charcoal"
            1 -> "Navy Blue"
            else -> "Emerald Green"
        }
    }

    fun setActionHandler(handler: KeyActionHandler) {
        pointerTracker = PointerTracker(this, handler)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        keyboard.buildLayout(w, h)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val density = resources.displayMetrics.density
        val isNumpad = keyboard.mode == KeyboardMode.NUMERIC_PIN
        val shadowHeight = 2.5f * density

        for (key in keyboard.keys) {
            val bounds = key.bounds
            val cornerRadius = if (isNumpad) bounds.height() / 2f else 6f * density

            val (fillPaint, shadowPaint) = when {
                key.isAction -> {
                    paintKeyAction.color = ContextCompat.getColor(context, R.color.btn_primary_bg)
                    paintKeyActionPressed.color = ContextCompat.getColor(context, R.color.btn_primary_bg_pressed)
                    if (key.isPressed) Pair(paintKeyActionPressed, paintKeyActionShadow) else Pair(paintKeyAction, paintKeyActionShadow)
                }
                key.isModifier -> if (key.isPressed) Pair(paintKeyModifierPressed, paintKeyModifierShadow) else Pair(paintKeyModifier, paintKeyModifierShadow)
                else -> if (key.isPressed) Pair(paintKeyNormalPressed, paintKeyNormalShadow) else Pair(paintKeyNormal, paintKeyNormalShadow)
            }

            if (!key.isPressed) {
                canvas.drawRoundRect(bounds.left, bounds.top + shadowHeight, bounds.right, bounds.bottom + shadowHeight, cornerRadius, cornerRadius, shadowPaint)
            }

            val topOffset = if (key.isPressed) shadowHeight else 0f
            canvas.drawRoundRect(bounds.left, bounds.top + topOffset, bounds.right, bounds.bottom + topOffset, cornerRadius, cornerRadius, fillPaint)

            if (key.iconResId != 0) {
                drawKeyIcon(canvas, key, bounds, topOffset, density)
            } else if (key.label != null) {
                drawKeyLabel(canvas, key, bounds, topOffset, density)
            }
        }

        val tracker = pointerTracker
        if (tracker != null && tracker.isSliding && tracker.gesturePoints.size > 1) {
            trailPath.reset()
            val first = tracker.gesturePoints.first()
            trailPath.moveTo(first.x, first.y)
            for (i in 1 until tracker.gesturePoints.size) {
                val pt = tracker.gesturePoints[i]
                trailPath.lineTo(pt.x, pt.y)
            }
            canvas.drawPath(trailPath, paintTrail)
        }

        val pressedKey = tracker?.activeKey
        if (pressedKey != null && !tracker.isSliding && pressedKey.label != null && pressedKey.label!!.length == 1 && !pressedKey.isModifier) {
            drawKeyPreviewBubble(canvas, pressedKey, density)
        }
    }

    private fun drawKeyIcon(canvas: Canvas, key: Key, bounds: RectF, topOffset: Float, density: Float) {
        val icon = iconCache.getOrPut(key.iconResId) {
            ContextCompat.getDrawable(context, key.iconResId)!!.mutate()
        }
        val iconSize = (22f * density).toInt()
        val centerX = bounds.centerX().toInt()
        val centerY = (bounds.centerY() + topOffset).toInt()

        icon.setBounds(centerX - iconSize / 2, centerY - iconSize / 2, centerX + iconSize / 2, centerY + iconSize / 2)
        icon.setTint(if (key.isAction) Color.WHITE else Color.parseColor("#E2E8F0"))
        icon.draw(canvas)
    }

    private fun drawKeyLabel(canvas: Canvas, key: Key, bounds: RectF, topOffset: Float, density: Float) {
        val label = key.label ?: return
        val centerX = bounds.centerX()
        val centerY = bounds.centerY() + topOffset

        if (key.code == Key.CODE_SPACE) {
            paintSpaceSubtitle.textSize = 14f * density
            val baseline = centerY - (paintSpaceSubtitle.descent() + paintSpaceSubtitle.ascent()) / 2
            canvas.drawText(label, centerX, baseline, paintSpaceSubtitle)
            return
        }

        val textSizeSp = when {
            keyboard.mode == KeyboardMode.NUMERIC_PIN -> if (key.code == Key.CODE_MODE_CHANGE) 15f else 26f
            label.length > 2 -> 14f
            key.code in '0'.code..'9'.code -> 22f
            else -> 23f
        }
        paintLabel.textSize = textSizeSp * density

        val baseline = centerY - (paintLabel.descent() + paintLabel.ascent()) / 2
        canvas.drawText(label, centerX, baseline, paintLabel)

        if (key.hint != null) {
            paintHint.textSize = 11f * density
            val hintX = bounds.right - 8f * density
            val hintY = bounds.top + topOffset + 12f * density
            canvas.drawText(key.hint!!, hintX, hintY, paintHint)
        }
    }

    private fun drawKeyPreviewBubble(canvas: Canvas, key: Key, density: Float) {
        val bubbleWidth = key.bounds.width() * 1.3f
        val bubbleHeight = key.bounds.height() * 1.2f
        val bubbleCenterX = key.bounds.centerX()
        val bubbleBottom = key.bounds.top - (6f * density)
        val bubbleTop = bubbleBottom - bubbleHeight

        bubbleRect.set(bubbleCenterX - bubbleWidth / 2, bubbleTop, bubbleCenterX + bubbleWidth / 2, bubbleBottom)
        val bubbleCorner = 12f * density
        canvas.drawRoundRect(bubbleRect, bubbleCorner, bubbleCorner, paintBubbleBg)

        paintBubbleText.textSize = 28f * density
        val textY = bubbleRect.centerY() - (paintBubbleText.descent() + paintBubbleText.ascent()) / 2
        canvas.drawText(key.label!!, bubbleCenterX, textY, paintBubbleText)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val tracker = pointerTracker ?: return super.onTouchEvent(event)
        return tracker.onTouchEvent(event, keyboard)
    }

    fun toggleShift() {
        keyboard.mode = when (keyboard.mode) {
            KeyboardMode.QWERTY_LOWER -> KeyboardMode.QWERTY_UPPER
            KeyboardMode.QWERTY_UPPER -> KeyboardMode.QWERTY_CAPS_LOCK
            KeyboardMode.QWERTY_CAPS_LOCK -> KeyboardMode.QWERTY_LOWER
            else -> KeyboardMode.QWERTY_LOWER
        }
        keyboard.buildLayout(width, height)
        invalidate()
    }

    fun resetShiftAfterType() {
        if (keyboard.mode == KeyboardMode.QWERTY_UPPER) {
            keyboard.mode = KeyboardMode.QWERTY_LOWER
            keyboard.buildLayout(width, height)
            invalidate()
        }
    }

    fun setMode(newMode: KeyboardMode) {
        keyboard.mode = newMode
        keyboard.buildLayout(width, height)
        invalidate()
    }

    fun toggleMode() {
        keyboard.mode = when (keyboard.mode) {
            KeyboardMode.SYMBOLS, KeyboardMode.SYMBOLS_MORE, KeyboardMode.NUMERIC_PIN -> KeyboardMode.QWERTY_LOWER
            else -> KeyboardMode.SYMBOLS
        }
        keyboard.buildLayout(width, height)
        invalidate()
    }
}
