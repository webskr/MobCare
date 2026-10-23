package com.mobcare.keyboard.touch

import android.graphics.PointF
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import com.mobcare.keyboard.model.Key
import com.mobcare.keyboard.model.Keyboard
import com.mobcare.keyboard.model.KeyboardMode
import com.mobcare.keyboard.service.KeyActionHandler
import com.mobcare.keyboard.view.KeyboardView
import kotlin.math.hypot

class PointerTracker(
    private val keyboardView: KeyboardView,
    private val actionHandler: KeyActionHandler
) {
    private var activePointerId = -1
    private var downX = 0f
    private var downY = 0f
    private var downTime = 0L

    var activeKey: Key? = null
        private set

    var isSliding = false
        private set

    val gesturePoints = mutableListOf<PointF>()
    private val traversedLetters = mutableListOf<Char>()

    private val repeatHandler = Handler(Looper.getMainLooper())
    private var isRepeatingBackspace = false
    private var repeatCount = 0

    private val backspaceRepeatRunnable = object : Runnable {
        override fun run() {
            if (activeKey?.code == Key.CODE_BACKSPACE) {
                isRepeatingBackspace = true
                actionHandler.onBackspacePressed()
                keyboardView.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                repeatCount++
                val interval = if (repeatCount > 8) 35L else 55L
                repeatHandler.postDelayed(this, interval)
            }
        }
    }

    private fun startBackspaceRepeat() {
        repeatCount = 0
        isRepeatingBackspace = false
        repeatHandler.removeCallbacks(backspaceRepeatRunnable)
        repeatHandler.postDelayed(backspaceRepeatRunnable, 350L)
    }

    private fun stopBackspaceRepeat() {
        repeatHandler.removeCallbacks(backspaceRepeatRunnable)
        isRepeatingBackspace = false
        repeatCount = 0
    }

    fun onTouchEvent(event: MotionEvent, keyboard: Keyboard): Boolean {
        val action = event.actionMasked
        val pointerIndex = event.actionIndex

        when (action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val pointerId = event.getPointerId(pointerIndex)
                if (activePointerId == -1) {
                    activePointerId = pointerId
                    downX = event.getX(pointerIndex)
                    downY = event.getY(pointerIndex)
                    downTime = SystemClock.uptimeMillis()
                    isSliding = false
                    gesturePoints.clear()
                    traversedLetters.clear()

                    val key = keyboard.findKeyAt(downX, downY)
                    onKeyDown(key, downX, downY)
                }
            }

            MotionEvent.ACTION_MOVE -> {
                if (activePointerId != -1) {
                    val pIdx = event.findPointerIndex(activePointerId)
                    if (pIdx != -1) {
                        val x = event.getX(pIdx)
                        val y = event.getY(pIdx)
                        val dist = hypot(x - downX, y - downY)

                        if (!isSliding && dist > 40f) {
                            isSliding = true
                            stopBackspaceRepeat()
                        }

                        if (isSliding) {
                            gesturePoints.add(PointF(x, y))
                            val currentKey = keyboard.findKeyAt(x, y)
                            if (currentKey != null && currentKey.label != null && currentKey.label!!.length == 1) {
                                val ch = currentKey.label!![0]
                                if (traversedLetters.isEmpty() || traversedLetters.last() != ch) {
                                    traversedLetters.add(ch)
                                    keyboardView.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                }
                            }
                            keyboardView.invalidate()
                        } else {
                            val newKey = keyboard.findKeyAt(x, y)
                            if (newKey != activeKey) {
                                if (activeKey?.code == Key.CODE_BACKSPACE) {
                                    stopBackspaceRepeat()
                                }
                                activeKey?.isPressed = false
                                activeKey = newKey
                                activeKey?.isPressed = true
                                if (newKey?.code == Key.CODE_BACKSPACE) {
                                    startBackspaceRepeat()
                                }
                                keyboardView.invalidate()
                            }
                        }
                    }
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                val pointerId = event.getPointerId(pointerIndex)
                if (pointerId == activePointerId) {
                    val pIdx = event.findPointerIndex(pointerId)
                    val upX = if (pIdx != -1) event.getX(pIdx) else downX
                    val upY = if (pIdx != -1) event.getY(pIdx) else downY

                    onKeyUp(keyboard, upX, upY)
                    activePointerId = -1
                }
            }

            MotionEvent.ACTION_CANCEL -> {
                stopBackspaceRepeat()
                activeKey?.isPressed = false
                activeKey = null
                isSliding = false
                gesturePoints.clear()
                traversedLetters.clear()
                activePointerId = -1
                keyboardView.invalidate()
            }
        }
        return true
    }

    private fun onKeyDown(key: Key?, x: Float, y: Float) {
        activeKey = key
        key?.isPressed = true
        keyboardView.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)

        if (key?.code == Key.CODE_BACKSPACE) {
            actionHandler.onBackspacePressed()
            startBackspaceRepeat()
        }

        keyboardView.invalidate()
    }

    private fun onKeyUp(keyboard: Keyboard, upX: Float, upY: Float) {
        val key = activeKey
        stopBackspaceRepeat()

        activeKey?.isPressed = false
        activeKey = null

        if (isSliding && traversedLetters.size >= 2) {
            val matchedWord = GestureWordMatcher.findBestMatch(traversedLetters)
            if (matchedWord != null && matchedWord.isNotEmpty()) {
                actionHandler.onWordTyped(matchedWord)
            }
        } else if (key != null) {
            if (key.code != Key.CODE_BACKSPACE) {
                handleKeyAction(key, keyboard)
            }
        }

        isSliding = false
        gesturePoints.clear()
        traversedLetters.clear()
        keyboardView.invalidate()
    }

    private fun handleKeyAction(key: Key, keyboard: Keyboard) {
        when (key.code) {
            Key.CODE_SHIFT -> actionHandler.onShiftToggle()
            Key.CODE_MODE_CHANGE -> actionHandler.onModeToggle()
            Key.CODE_ENTER -> actionHandler.onEnterPressed()
            Key.CODE_SPACE -> actionHandler.onKeyTyped(" ")
            Key.CODE_EMOJI -> actionHandler.onEmojiToggle()
            Key.CODE_NUMERIC_PAD -> actionHandler.onNumericPadToggle()
            else -> {
                val char = key.label ?: (key.code.toChar().toString())
                actionHandler.onKeyTyped(char)
                if (keyboard.mode == KeyboardMode.QWERTY_UPPER) {
                    keyboardView.resetShiftAfterType()
                }
            }
        }
    }
}
