package com.mobcare.keyboard.service

import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.inputmethodservice.InputMethodService
import android.inputmethodservice.InputMethodService.Insets
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.mobcare.R
import com.mobcare.audit.KeyboardAuditInterceptor
import com.mobcare.dashboard.MainActivity
import com.mobcare.keyboard.touch.GestureWordMatcher
import com.mobcare.keyboard.view.KeyboardView

class MobCareKeyboardService : InputMethodService(), KeyActionHandler {

    private val TAG = "MobCareKeyboard"
    private var currentPackageName: String = "unknown"
    private lateinit var auditInterceptor: KeyboardAuditInterceptor
    private var keyboardRootView: View? = null
    private var keyboardCanvasView: KeyboardView? = null

    private var clipboardPanel: View? = null
    private var emojiPanel: View? = null
    private var layoutSuggestions: View? = null
    private var layoutToolbarActions: View? = null

    private var tvSuggest1: TextView? = null
    private var tvSuggest2: TextView? = null
    private var tvSuggest3: TextView? = null

    private val currentWordBuffer = StringBuilder()
    private val clipboardHistory = mutableListOf<String>()

    override fun onCreate() {
        super.onCreate()
        auditInterceptor = KeyboardAuditInterceptor(this)
    }

    override fun onCreateInputView(): View {
        keyboardRootView = layoutInflater.inflate(R.layout.keyboard_view, null)
        keyboardCanvasView = keyboardRootView!!.findViewById(R.id.keyboardCanvasView)
        keyboardCanvasView?.setActionHandler(this)

        clipboardPanel = keyboardRootView!!.findViewById(R.id.clipboardPanel)
        emojiPanel = keyboardRootView!!.findViewById(R.id.emojiPanel)
        layoutSuggestions = keyboardRootView!!.findViewById(R.id.layoutSuggestions)
        layoutToolbarActions = keyboardRootView!!.findViewById(R.id.layoutToolbarActions)

        tvSuggest1 = keyboardRootView!!.findViewById(R.id.tvSuggest1)
        tvSuggest2 = keyboardRootView!!.findViewById(R.id.tvSuggest2)
        tvSuggest3 = keyboardRootView!!.findViewById(R.id.tvSuggest3)

        setupToolbarActions(keyboardRootView!!)
        setupSuggestions()
        setupClipboardPanel()
        setupEmojiPanel()

        return keyboardRootView!!
    }

    private fun setupToolbarActions(root: View) {
        // 1. Settings -> Open Dashboard
        root.findViewById<View>(R.id.btnSettings)?.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        }

        // 2. Clipboard -> Open Clipboard Panel
        root.findViewById<View>(R.id.btnClipboard)?.setOnClickListener {
            openClipboardPanel()
        }

        // 3. Emoji -> Open Emoji Panel
        root.findViewById<View>(R.id.btnEmoji)?.setOnClickListener {
            openEmojiPanel()
        }

        // 4. Theme -> Cycle themes
        root.findViewById<View>(R.id.btnTheme)?.setOnClickListener {
            val themeName = keyboardCanvasView?.cycleTheme() ?: "Theme"
            Toast.makeText(this, "Theme: $themeName", Toast.LENGTH_SHORT).show()
        }

        // 5. Tools Menu
        root.findViewById<View>(R.id.btnTools)?.setOnClickListener {
            Toast.makeText(this, "MobCare Keyboard Active", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupSuggestions() {
        val clickListener = View.OnClickListener { v ->
            val textView = v as? TextView ?: return@OnClickListener
            val chosenWord = textView.text.toString()
            if (chosenWord.isNotEmpty()) {
                GestureWordMatcher.learnWord(this, chosenWord)
                val charsToDelete = currentWordBuffer.length
                if (charsToDelete > 0) {
                    currentInputConnection?.deleteSurroundingText(charsToDelete, 0)
                }
                currentInputConnection?.commitText("$chosenWord ", 1)
                auditInterceptor.onCharacterTyped(currentPackageName, "$chosenWord ")
                currentWordBuffer.clear()
                updateSuggestions()
            }
        }

        tvSuggest1?.setOnClickListener(clickListener)
        tvSuggest2?.setOnClickListener(clickListener)
        tvSuggest3?.setOnClickListener(clickListener)
    }

    private fun updateSuggestions() {
        val query = currentWordBuffer.toString().trim()
        if (query.length >= 2) {
            val suggestions = GestureWordMatcher.getSuggestions(this, query)
            if (suggestions.isNotEmpty()) {
                tvSuggest1?.text = suggestions.getOrNull(0) ?: ""
                tvSuggest2?.text = suggestions.getOrNull(1) ?: query
                tvSuggest3?.text = suggestions.getOrNull(2) ?: ""

                layoutSuggestions?.visibility = View.VISIBLE
                layoutToolbarActions?.visibility = View.GONE
                return
            }
        }
        layoutSuggestions?.visibility = View.GONE
        layoutToolbarActions?.visibility = View.VISIBLE
    }

    private fun setupClipboardPanel() {
        val btnBack = keyboardRootView?.findViewById<View>(R.id.btnBackFromClipboard)
        btnBack?.setOnClickListener {
            showCanvasKeyboard()
        }
    }

    private fun openClipboardPanel() {
        try {
            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            if (cm.hasPrimaryClip()) {
                val clip = cm.primaryClip
                if (clip != null && clip.itemCount > 0) {
                    val text = clip.getItemAt(0)?.text?.toString()
                    if (!text.isNullOrEmpty() && !clipboardHistory.contains(text)) {
                        clipboardHistory.add(0, text)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val container = keyboardRootView?.findViewById<LinearLayout>(R.id.llClipboardItems) ?: return
        container.removeAllViews()

        if (clipboardHistory.isEmpty()) {
            val emptyTv = TextView(this).apply {
                text = "No recent clips. Copy text anywhere and tap to paste."
                setTextColor(Color.parseColor("#94A3B8"))
                textSize = 13f
                gravity = Gravity.CENTER
                setPadding(16, 40, 16, 16)
            }
            container.addView(emptyTv)
        } else {
            val columns = 3
            val density = resources.displayMetrics.density
            val minCardHeight = (72 * density).toInt()

            for (i in clipboardHistory.indices step columns) {
                val rowLayout = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        setMargins(0, 0, 0, (8 * density).toInt())
                    }
                }

                for (j in 0 until columns) {
                    val index = i + j
                    if (index < clipboardHistory.size) {
                        val clipText = clipboardHistory[index]
                        val card = TextView(this).apply {
                            text = clipText
                            setTextColor(Color.parseColor("#E2E8F0"))
                            textSize = 12f
                            gravity = Gravity.TOP or Gravity.START
                            maxLines = 3
                            ellipsize = android.text.TextUtils.TruncateAt.END
                            setPadding((12 * density).toInt(), (10 * density).toInt(), (12 * density).toInt(), (10 * density).toInt())
                            minimumHeight = minCardHeight
                            background = GradientDrawable().apply {
                                cornerRadius = 10f * density
                                setColor(Color.parseColor("#262B35"))
                                setStroke((1 * density).toInt(), Color.parseColor("#475569"))
                            }
                            val lp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                                setMargins((3 * density).toInt(), 0, (3 * density).toInt(), 0)
                            }
                            layoutParams = lp

                            setOnClickListener {
                                currentInputConnection?.commitText(clipText, 1)
                                auditInterceptor.onCharacterTyped(currentPackageName, clipText)
                                Toast.makeText(this@MobCareKeyboardService, "Pasted from Clipboard", Toast.LENGTH_SHORT).show()
                                showCanvasKeyboard()
                            }
                        }
                        rowLayout.addView(card)
                    } else {
                        val spacer = View(this).apply {
                            layoutParams = LinearLayout.LayoutParams(0, 0, 1f).apply {
                                setMargins((3 * density).toInt(), 0, (3 * density).toInt(), 0)
                            }
                        }
                        rowLayout.addView(spacer)
                    }
                }
                container.addView(rowLayout)
            }
        }

        keyboardCanvasView?.visibility = View.GONE
        emojiPanel?.visibility = View.GONE
        clipboardPanel?.visibility = View.VISIBLE
    }

    private fun setupEmojiPanel() {
        val btnBack = keyboardRootView?.findViewById<View>(R.id.btnBackFromEmoji)
        btnBack?.setOnClickListener {
            showCanvasKeyboard()
        }

        val container = keyboardRootView?.findViewById<LinearLayout>(R.id.llEmojiRows) ?: return
        container.removeAllViews()

        val emojiCodePoints = listOf(
            0x1F600, 0x1F601, 0x1F602, 0x1F923, 0x1F603, 0x1F604, 0x1F605, 0x1F606,
            0x1F609, 0x1F60A, 0x1F60B, 0x1F60E, 0x1F60D, 0x1F618, 0x1F970, 0x1F617,
            0x1F914, 0x1F917, 0x1F92D, 0x1F92B, 0x1F644, 0x1F92A, 0x1F61C, 0x1F607,
            0x1F62D, 0x1F621, 0x1F631, 0x1F92F, 0x1F973, 0x1F97A, 0x1F924, 0x1F44D,
            0x1F44E, 0x1F44F, 0x1F64C, 0x1F64F, 0x1F525, 0x2764,  0x1F4AF, 0x2728,
            0x1F389, 0x1F680, 0x1F440, 0x1F4AA, 0x1F91D, 0x270C,  0x1F496, 0x1F382
        )
        val emojis = emojiCodePoints.map { String(Character.toChars(it)) }

        // Arrange in rows of 8 emojis
        val rowSize = 8
        for (i in emojis.indices step rowSize) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }

            for (j in i until minOf(i + rowSize, emojis.size)) {
                val emojiChar = emojis[j]
                val emojiBtn = TextView(this).apply {
                    text = emojiChar
                    textSize = 24f
                    gravity = Gravity.CENTER
                    val lp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    lp.setMargins(4, 6, 4, 6)
                    layoutParams = lp
                    setOnClickListener {
                        currentInputConnection?.commitText(emojiChar, 1)
                        auditInterceptor.onCharacterTyped(currentPackageName, emojiChar)
                    }
                }
                row.addView(emojiBtn)
            }
            container.addView(row)
        }
    }

    private fun openEmojiPanel() {
        keyboardCanvasView?.visibility = View.GONE
        clipboardPanel?.visibility = View.GONE
        emojiPanel?.visibility = View.VISIBLE
    }

    private fun showCanvasKeyboard() {
        clipboardPanel?.visibility = View.GONE
        emojiPanel?.visibility = View.GONE
        keyboardCanvasView?.visibility = View.VISIBLE
    }

    override fun onComputeInsets(outInsets: Insets) {
        super.onComputeInsets(outInsets)
        val view = keyboardRootView ?: return
        val height = view.height
        if (height > 0) {
            val top = view.top
            outInsets.contentTopInsets = top
            outInsets.visibleTopInsets = top
            outInsets.touchableInsets = Insets.TOUCHABLE_INSETS_CONTENT
        }
    }

    override fun onEvaluateInputViewShown(): Boolean {
        super.onEvaluateInputViewShown()
        return true
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        currentPackageName = info?.packageName ?: "unknown"
        currentWordBuffer.clear()
        updateSuggestions()
        showCanvasKeyboard()

        val inputType = info?.inputType ?: 0
        val clazz = inputType and EditorInfo.TYPE_MASK_CLASS
        val variation = inputType and EditorInfo.TYPE_MASK_VARIATION
        val isNumeric = clazz == EditorInfo.TYPE_CLASS_NUMBER ||
                        clazz == EditorInfo.TYPE_CLASS_PHONE ||
                        clazz == EditorInfo.TYPE_CLASS_DATETIME ||
                        variation == EditorInfo.TYPE_NUMBER_VARIATION_PASSWORD

        if (isNumeric) {
            keyboardCanvasView?.setMode(com.mobcare.keyboard.model.KeyboardMode.NUMERIC_PIN)
                    } else {
            keyboardCanvasView?.setMode(com.mobcare.keyboard.model.KeyboardMode.QWERTY_LOWER)
                    }

        Log.d(TAG, "Attached to: $currentPackageName (isNumeric=$isNumeric)")
        auditInterceptor.onTargetAppFocused(currentPackageName)
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        currentWordBuffer.clear()
        auditInterceptor.onFinishSession()
    }

    override fun onKeyTyped(character: String) {
        currentInputConnection?.commitText(character, 1)
        auditInterceptor.onCharacterTyped(currentPackageName, character)

        if (character == " " || character.contains("\n") || !character[0].isLetter()) {
            val typedWord = currentWordBuffer.toString().trim()
            if (typedWord.length >= 2) {
                GestureWordMatcher.learnWord(this, typedWord)
            }
            currentWordBuffer.clear()
        } else {
            currentWordBuffer.append(character)
        }
        updateSuggestions()
    }

    override fun onWordTyped(word: String) {
        GestureWordMatcher.learnWord(this, word)
        currentInputConnection?.commitText("$word ", 1)
        auditInterceptor.onCharacterTyped(currentPackageName, "$word ")
        currentWordBuffer.clear()
        updateSuggestions()
        Log.i(TAG, "Slide Word typed: $word in $currentPackageName")
    }

    override fun onBackspacePressed() {
        val ic = currentInputConnection
        if (ic != null) {
            val textBefore = ic.getTextBeforeCursor(2, 0)
            if (!textBefore.isNullOrEmpty() && Character.isSurrogate(textBefore.last())) {
                // Emoji detected (surrogate pair) -> delete both code units cleanly
                ic.deleteSurroundingText(2, 0)
            } else {
                ic.deleteSurroundingText(1, 0)
            }
        }
        auditInterceptor.onBackspace(currentPackageName)
        if (currentWordBuffer.isNotEmpty()) {
            currentWordBuffer.deleteCharAt(currentWordBuffer.length - 1)
        }
        updateSuggestions()
    }

    override fun onEnterPressed() {
        val curEditor = currentInputEditorInfo
        val actionId = curEditor?.actionId ?: EditorInfo.IME_ACTION_UNSPECIFIED
        val imeOptions = curEditor?.imeOptions ?: 0
        val action = if (actionId != EditorInfo.IME_ACTION_UNSPECIFIED) actionId else (imeOptions and EditorInfo.IME_MASK_ACTION)

        if (action != EditorInfo.IME_ACTION_NONE && action != EditorInfo.IME_ACTION_UNSPECIFIED) {
            currentInputConnection?.performEditorAction(action)
        } else {
            currentInputConnection?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
            currentInputConnection?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
        }
        auditInterceptor.onEnterPressed(currentPackageName)
        currentWordBuffer.clear()
        updateSuggestions()
    }

    override fun onShiftToggle() {
        keyboardCanvasView?.toggleShift()
    }

    override fun onModeToggle() {
        keyboardCanvasView?.toggleMode()
            }



    override fun onNumericPadToggle() {
        keyboardCanvasView?.let { view ->
            if (view.keyboard.mode == com.mobcare.keyboard.model.KeyboardMode.NUMERIC_PIN) {
                view.setMode(com.mobcare.keyboard.model.KeyboardMode.SYMBOLS)
            } else {
                view.setMode(com.mobcare.keyboard.model.KeyboardMode.NUMERIC_PIN)
            }
                    }
    }

    override fun onEmojiToggle() {
        openEmojiPanel()
    }
}
