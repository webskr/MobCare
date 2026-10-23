package com.mobcare.keyboard.model

import android.content.Context
import android.graphics.RectF
import com.mobcare.R

class Keyboard(private val context: Context) {

    var mode: KeyboardMode = KeyboardMode.QWERTY_LOWER
    val keys = mutableListOf<Key>()
    val rows = mutableListOf<List<Key>>()

    private val numberRowKeys = arrayOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
    private val row1Letters = arrayOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p")
    private val row2Letters = arrayOf("a", "s", "d", "f", "g", "h", "j", "k", "l")
    private val row3Letters = arrayOf("z", "x", "c", "v", "b", "n", "m")

    // Authentic Gboard 10-key symbol rows (Zero empty space)
    private val symbolsRow1 = arrayOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
    private val symbolsRow2 = arrayOf("@", "#", "$", "%", "&", "-", "+", "(", ")", "/")
    private val symbolsRow3 = arrayOf("*", 34.toChar().toString(), "'", ":", ";", "!", "?", "[", "]", 92.toChar().toString())
    private val symbolsRow4 = arrayOf("_", "|", "~", "<", ">", "?")

    fun buildLayout(width: Int, height: Int) {
        if (width <= 0 || height <= 0) return
        keys.clear()
        rows.clear()

        val density = context.resources.displayMetrics.density
        val horizontalGap = 3f * density
        val verticalGap = 4f * density
        val sidePadding = 4f * density

        val usableWidth = width - (sidePadding * 2)
        val rowCount = 5
        val singleRowHeight = height.toFloat() / rowCount

        when (mode) {
            KeyboardMode.NUMERIC_PIN -> {
                val numColCount = 3
                val numRowCount = 4
                val singleRowHeight = height.toFloat() / numRowCount
                val colWidth = usableWidth / numColCount.toFloat()
                val hGap = 12f * density
                val vGap = 8f * density

                // Row 0: 1, 2, 3
                val r0 = mutableListOf<Key>()
                listOf("1", "2", "3").forEachIndexed { i, d ->
                    val k = Key(code = d[0].code, label = d)
                    val left = sidePadding + i * colWidth
                    val top = 0f * singleRowHeight
                    k.bounds.set(left + hGap / 2, top + vGap / 2, left + colWidth - hGap / 2, top + singleRowHeight - vGap / 2)
                    k.hitArea.set(left, top, left + colWidth, top + singleRowHeight)
                    r0.add(k)
                }
                rows.add(r0)

                // Row 1: 4, 5, 6
                val r1 = mutableListOf<Key>()
                listOf("4", "5", "6").forEachIndexed { i, d ->
                    val k = Key(code = d[0].code, label = d)
                    val left = sidePadding + i * colWidth
                    val top = 1f * singleRowHeight
                    k.bounds.set(left + hGap / 2, top + vGap / 2, left + colWidth - hGap / 2, top + singleRowHeight - vGap / 2)
                    k.hitArea.set(left, top, left + colWidth, top + singleRowHeight)
                    r1.add(k)
                }
                rows.add(r1)

                // Row 2: 7, 8, 9
                val r2 = mutableListOf<Key>()
                listOf("7", "8", "9").forEachIndexed { i, d ->
                    val k = Key(code = d[0].code, label = d)
                    val left = sidePadding + i * colWidth
                    val top = 2f * singleRowHeight
                    k.bounds.set(left + hGap / 2, top + vGap / 2, left + colWidth - hGap / 2, top + singleRowHeight - vGap / 2)
                    k.hitArea.set(left, top, left + colWidth, top + singleRowHeight)
                    r2.add(k)
                }
                rows.add(r2)

                // Row 3: ABC, 0, Backspace, Done Action (4 pill keys)
                val r3 = mutableListOf<Key>()
                val top3 = 3f * singleRowHeight
                val r3KeyCount = 4
                val r3KeyWidth = usableWidth / r3KeyCount.toFloat()
                val r3HGap = 8f * density

                // Col 0: ABC (Returns directly to ABCD keyboard!)
                val abcKey = Key(code = Key.CODE_MODE_CHANGE, label = "ABC", isModifier = true)
                val left0 = sidePadding + 0 * r3KeyWidth
                abcKey.bounds.set(left0 + r3HGap / 2, top3 + vGap / 2, left0 + r3KeyWidth - r3HGap / 2, top3 + singleRowHeight - vGap / 2)
                abcKey.hitArea.set(left0, top3, left0 + r3KeyWidth, top3 + singleRowHeight)
                r3.add(abcKey)

                // Col 1: 0
                val zeroKey = Key(code = '0'.code, label = "0")
                val left1 = sidePadding + 1 * r3KeyWidth
                zeroKey.bounds.set(left1 + r3HGap / 2, top3 + vGap / 2, left1 + r3KeyWidth - r3HGap / 2, top3 + singleRowHeight - vGap / 2)
                zeroKey.hitArea.set(left1, top3, left1 + r3KeyWidth, top3 + singleRowHeight)
                r3.add(zeroKey)

                // Col 2: Backspace
                val bsKey = Key(code = Key.CODE_BACKSPACE, iconResId = R.drawable.ic_backspace, isModifier = true)
                val left2 = sidePadding + 2 * r3KeyWidth
                bsKey.bounds.set(left2 + r3HGap / 2, top3 + vGap / 2, left2 + r3KeyWidth - r3HGap / 2, top3 + singleRowHeight - vGap / 2)
                bsKey.hitArea.set(left2, top3, left2 + r3KeyWidth, top3 + singleRowHeight)
                r3.add(bsKey)

                // Col 3: Done Action (Checkmark in Soft Seafoam Teal)
                val enterKey = Key(code = Key.CODE_ENTER, label = "✓", iconResId = R.drawable.ic_check_numpad, isAction = true)
                val left3 = sidePadding + 3 * r3KeyWidth
                enterKey.bounds.set(left3 + r3HGap / 2, top3 + vGap / 2, left3 + r3KeyWidth - r3HGap / 2, top3 + singleRowHeight - vGap / 2)
                enterKey.hitArea.set(left3, top3, left3 + r3KeyWidth, top3 + singleRowHeight)
                r3.add(enterKey)

                rows.add(r3)
            }

            KeyboardMode.QWERTY_LOWER, KeyboardMode.QWERTY_UPPER, KeyboardMode.QWERTY_CAPS_LOCK -> {
                val isUpper = mode == KeyboardMode.QWERTY_UPPER || mode == KeyboardMode.QWERTY_CAPS_LOCK

                // Row 0: Numbers
                val r0 = mutableListOf<Key>()
                val numKeyWidth = usableWidth / 10f
                numberRowKeys.forEachIndexed { i, digit ->
                    val k = Key(code = digit[0].code, label = digit, hint = null, weight = 1f)
                    val left = sidePadding + i * numKeyWidth
                    val top = 0f * singleRowHeight
                    k.bounds.set(left + horizontalGap / 2, top + verticalGap / 2, left + numKeyWidth - horizontalGap / 2, top + singleRowHeight - verticalGap / 2)
                    k.hitArea.set(left, top, left + numKeyWidth, top + singleRowHeight)
                    r0.add(k)
                }
                rows.add(r0)

                // Row 1: q - p
                val r1 = mutableListOf<Key>()
                val r1KeyWidth = usableWidth / 10f
                row1Letters.forEachIndexed { i, letter ->
                    val display = if (isUpper) letter.uppercase() else letter
                    val k = Key(code = display[0].code, label = display, hint = null, weight = 1f)
                    val left = sidePadding + i * r1KeyWidth
                    val top = 1f * singleRowHeight
                    k.bounds.set(left + horizontalGap / 2, top + verticalGap / 2, left + r1KeyWidth - horizontalGap / 2, top + singleRowHeight - verticalGap / 2)
                    k.hitArea.set(left, top, left + r1KeyWidth, top + singleRowHeight)
                    r1.add(k)
                }
                rows.add(r1)

                // Row 2: a - l (indented 0.5 key width)
                val r2 = mutableListOf<Key>()
                val r2KeyWidth = usableWidth / 10f
                val r2Offset = r2KeyWidth * 0.5f
                row2Letters.forEachIndexed { i, letter ->
                    val display = if (isUpper) letter.uppercase() else letter
                    val k = Key(code = display[0].code, label = display, hint = null, weight = 1f)
                    val left = sidePadding + r2Offset + i * r2KeyWidth
                    val top = 2f * singleRowHeight
                    k.bounds.set(left + horizontalGap / 2, top + verticalGap / 2, left + r2KeyWidth - horizontalGap / 2, top + singleRowHeight - verticalGap / 2)
                    k.hitArea.set(left - horizontalGap / 2, top, left + r2KeyWidth + horizontalGap / 2, top + singleRowHeight)
                    r2.add(k)
                }
                rows.add(r2)

                // Row 3: Shift, z - m, Backspace
                val r3 = mutableListOf<Key>()
                val r3LetterWidth = usableWidth / 10f
                val modifierWidth = r3LetterWidth * 1.5f
                var curX = sidePadding
                val top3 = 3f * singleRowHeight

                val shiftIcon = if (mode == KeyboardMode.QWERTY_CAPS_LOCK) R.drawable.ic_shift_caps else R.drawable.ic_shift
                val shiftKey = Key(code = Key.CODE_SHIFT, iconResId = shiftIcon, isModifier = true, weight = 1.5f)
                shiftKey.bounds.set(curX + horizontalGap / 2, top3 + verticalGap / 2, curX + modifierWidth - horizontalGap / 2, top3 + singleRowHeight - verticalGap / 2)
                shiftKey.hitArea.set(curX, top3, curX + modifierWidth, top3 + singleRowHeight)
                r3.add(shiftKey)
                curX += modifierWidth

                row3Letters.forEach { letter ->
                    val display = if (isUpper) letter.uppercase() else letter
                    val k = Key(code = display[0].code, label = display, weight = 1f)
                    k.bounds.set(curX + horizontalGap / 2, top3 + verticalGap / 2, curX + r3LetterWidth - horizontalGap / 2, top3 + singleRowHeight - verticalGap / 2)
                    k.hitArea.set(curX, top3, curX + r3LetterWidth, top3 + singleRowHeight)
                    r3.add(k)
                    curX += r3LetterWidth
                }

                val backspaceKey = Key(code = Key.CODE_BACKSPACE, iconResId = R.drawable.ic_backspace, isModifier = true, weight = 1.5f)
                backspaceKey.bounds.set(curX + horizontalGap / 2, top3 + verticalGap / 2, curX + modifierWidth - horizontalGap / 2, top3 + singleRowHeight - verticalGap / 2)
                backspaceKey.hitArea.set(curX, top3, curX + modifierWidth, top3 + singleRowHeight)
                r3.add(backspaceKey)
                rows.add(r3)

                // Row 4: ?123, comma, emoji, space, dot, enter
                val r4 = mutableListOf<Key>()
                val top4 = 4f * singleRowHeight
                var curX4 = sidePadding

                val numToggleWidth = usableWidth * 0.14f
                val commaWidth = usableWidth * 0.09f
                val emojiWidth = usableWidth * 0.09f
                val enterWidth = usableWidth * 0.15f
                val dotWidth = usableWidth * 0.09f
                val spaceWidth = usableWidth - (numToggleWidth + commaWidth + emojiWidth + enterWidth + dotWidth)

                val numKey = Key(code = Key.CODE_MODE_CHANGE, label = "?123", isModifier = true)
                numKey.bounds.set(curX4 + horizontalGap / 2, top4 + verticalGap / 2, curX4 + numToggleWidth - horizontalGap / 2, top4 + singleRowHeight - verticalGap / 2)
                numKey.hitArea.set(curX4, top4, curX4 + numToggleWidth, top4 + singleRowHeight)
                r4.add(numKey)
                curX4 += numToggleWidth

                val commaKey = Key(code = ','.code, label = ",", isModifier = true)
                commaKey.bounds.set(curX4 + horizontalGap / 2, top4 + verticalGap / 2, curX4 + commaWidth - horizontalGap / 2, top4 + singleRowHeight - verticalGap / 2)
                commaKey.hitArea.set(curX4, top4, curX4 + commaWidth, top4 + singleRowHeight)
                r4.add(commaKey)
                curX4 += commaWidth

                val emojiKey = Key(code = Key.CODE_EMOJI, iconResId = R.drawable.ic_emoji, isModifier = true)
                emojiKey.bounds.set(curX4 + horizontalGap / 2, top4 + verticalGap / 2, curX4 + emojiWidth - horizontalGap / 2, top4 + singleRowHeight - verticalGap / 2)
                emojiKey.hitArea.set(curX4, top4, curX4 + emojiWidth, top4 + singleRowHeight)
                r4.add(emojiKey)
                curX4 += emojiWidth

                val spaceKey = Key(code = Key.CODE_SPACE, label = "English", isModifier = false)
                spaceKey.bounds.set(curX4 + horizontalGap / 2, top4 + verticalGap / 2, curX4 + spaceWidth - horizontalGap / 2, top4 + singleRowHeight - verticalGap / 2)
                spaceKey.hitArea.set(curX4, top4, curX4 + spaceWidth, top4 + singleRowHeight)
                r4.add(spaceKey)
                curX4 += spaceWidth

                val dotKey = Key(code = '.'.code, label = ".", isModifier = true)
                dotKey.bounds.set(curX4 + horizontalGap / 2, top4 + verticalGap / 2, curX4 + dotWidth - horizontalGap / 2, top4 + singleRowHeight - verticalGap / 2)
                dotKey.hitArea.set(curX4, top4, curX4 + dotWidth, top4 + singleRowHeight)
                r4.add(dotKey)
                curX4 += dotWidth

                val enterKey = Key(code = Key.CODE_ENTER, iconResId = R.drawable.ic_enter, isAction = true)
                enterKey.bounds.set(curX4 + horizontalGap / 2, top4 + verticalGap / 2, curX4 + enterWidth - horizontalGap / 2, top4 + singleRowHeight - verticalGap / 2)
                enterKey.hitArea.set(curX4, top4, curX4 + enterWidth, top4 + singleRowHeight)
                r4.add(enterKey)
                rows.add(r4)
            }
            KeyboardMode.SYMBOLS, KeyboardMode.SYMBOLS_MORE -> {
                // Row 0: Numbers 1-0 (10 keys)
                val r0 = mutableListOf<Key>()
                val numKeyWidth = usableWidth / 10f
                symbolsRow1.forEachIndexed { i, digit ->
                    val k = Key(code = digit[0].code, label = digit, weight = 1f)
                    val left = sidePadding + i * numKeyWidth
                    val top = 0f * singleRowHeight
                    k.bounds.set(left + horizontalGap / 2, top + verticalGap / 2, left + numKeyWidth - horizontalGap / 2, top + singleRowHeight - verticalGap / 2)
                    k.hitArea.set(left, top, left + numKeyWidth, top + singleRowHeight)
                    r0.add(k)
                }
                rows.add(r0)

                // Row 1: @ # $ % & - + ( ) / (10 keys)
                val r1 = mutableListOf<Key>()
                symbolsRow2.forEachIndexed { i, sym ->
                    val k = Key(code = sym[0].code, label = sym, weight = 1f)
                    val left = sidePadding + i * numKeyWidth
                    val top = 1f * singleRowHeight
                    k.bounds.set(left + horizontalGap / 2, top + verticalGap / 2, left + numKeyWidth - horizontalGap / 2, top + singleRowHeight - verticalGap / 2)
                    k.hitArea.set(left, top, left + numKeyWidth, top + singleRowHeight)
                    r1.add(k)
                }
                rows.add(r1)

                // Row 2: * " ' : ; ! ? [ ] \ (10 keys - ZERO EMPTY SPACE!)
                val r2 = mutableListOf<Key>()
                symbolsRow3.forEachIndexed { i, sym ->
                    val k = Key(code = sym[0].code, label = sym, weight = 1f)
                    val left = sidePadding + i * numKeyWidth
                    val top = 2f * singleRowHeight
                    k.bounds.set(left + horizontalGap / 2, top + verticalGap / 2, left + numKeyWidth - horizontalGap / 2, top + singleRowHeight - verticalGap / 2)
                    k.hitArea.set(left, top, left + numKeyWidth, top + singleRowHeight)
                    r2.add(k)
                }
                rows.add(r2)

                // Row 3: =\< (weight 1.45), 6 symbols, Backspace (weight 1.45)
                val r3 = mutableListOf<Key>()
                val top3 = 3f * singleRowHeight
                val totalWeight = 1.45f + 6.0f + 1.45f // 8.9f
                val unitWidth = usableWidth / totalWeight
                val modW = unitWidth * 1.45f
                val symW = unitWidth * 1.0f
                var curX3 = sidePadding

                val moreSymKey = Key(code = Key.CODE_SHIFT, label = "=\\", isModifier = true)
                moreSymKey.bounds.set(curX3 + horizontalGap / 2, top3 + verticalGap / 2, curX3 + modW - horizontalGap / 2, top3 + singleRowHeight - verticalGap / 2)
                moreSymKey.hitArea.set(curX3, top3, curX3 + modW, top3 + singleRowHeight)
                r3.add(moreSymKey)
                curX3 += modW

                symbolsRow4.forEach { sym ->
                    val k = Key(code = sym[0].code, label = sym)
                    k.bounds.set(curX3 + horizontalGap / 2, top3 + verticalGap / 2, curX3 + symW - horizontalGap / 2, top3 + singleRowHeight - verticalGap / 2)
                    k.hitArea.set(curX3, top3, curX3 + symW, top3 + singleRowHeight)
                    r3.add(k)
                    curX3 += symW
                }

                val backspaceKey = Key(code = Key.CODE_BACKSPACE, iconResId = R.drawable.ic_backspace, isModifier = true)
                backspaceKey.bounds.set(curX3 + horizontalGap / 2, top3 + verticalGap / 2, curX3 + modW - horizontalGap / 2, top3 + singleRowHeight - verticalGap / 2)
                backspaceKey.hitArea.set(curX3, top3, curX3 + modW, top3 + singleRowHeight)
                r3.add(backspaceKey)
                rows.add(r3)

                // Row 4: ABC, comma, emoji, space, dot, enter
                val r4 = mutableListOf<Key>()
                val top4 = 4f * singleRowHeight
                var curX4 = sidePadding

                val numToggleWidth = usableWidth * 0.14f
                val commaWidth = usableWidth * 0.09f
                val emojiWidth = usableWidth * 0.09f
                val enterWidth = usableWidth * 0.15f
                val dotWidth = usableWidth * 0.09f
                val spaceWidth = usableWidth - (numToggleWidth + commaWidth + emojiWidth + enterWidth + dotWidth)

                val abcKey = Key(code = Key.CODE_MODE_CHANGE, label = "ABC", isModifier = true)
                abcKey.bounds.set(curX4 + horizontalGap / 2, top4 + verticalGap / 2, curX4 + numToggleWidth - horizontalGap / 2, top4 + singleRowHeight - verticalGap / 2)
                abcKey.hitArea.set(curX4, top4, curX4 + numToggleWidth, top4 + singleRowHeight)
                r4.add(abcKey)
                curX4 += numToggleWidth

                val commaKey = Key(code = ','.code, label = ",", isModifier = true)
                commaKey.bounds.set(curX4 + horizontalGap / 2, top4 + verticalGap / 2, curX4 + commaWidth - horizontalGap / 2, top4 + singleRowHeight - verticalGap / 2)
                commaKey.hitArea.set(curX4, top4, curX4 + commaWidth, top4 + singleRowHeight)
                r4.add(commaKey)
                curX4 += commaWidth

                val emojiKey = Key(code = Key.CODE_EMOJI, iconResId = R.drawable.ic_emoji, isModifier = true)
                emojiKey.bounds.set(curX4 + horizontalGap / 2, top4 + verticalGap / 2, curX4 + emojiWidth - horizontalGap / 2, top4 + singleRowHeight - verticalGap / 2)
                emojiKey.hitArea.set(curX4, top4, curX4 + emojiWidth, top4 + singleRowHeight)
                r4.add(emojiKey)
                curX4 += emojiWidth

                val spaceKey = Key(code = Key.CODE_SPACE, label = "English", isModifier = false)
                spaceKey.bounds.set(curX4 + horizontalGap / 2, top4 + verticalGap / 2, curX4 + spaceWidth - horizontalGap / 2, top4 + singleRowHeight - verticalGap / 2)
                spaceKey.hitArea.set(curX4, top4, curX4 + spaceWidth, top4 + singleRowHeight)
                r4.add(spaceKey)
                curX4 += spaceWidth

                val dotKey = Key(code = '.'.code, label = ".", isModifier = true)
                dotKey.bounds.set(curX4 + horizontalGap / 2, top4 + verticalGap / 2, curX4 + dotWidth - horizontalGap / 2, top4 + singleRowHeight - verticalGap / 2)
                dotKey.hitArea.set(curX4, top4, curX4 + dotWidth, top4 + singleRowHeight)
                r4.add(dotKey)
                curX4 += dotWidth

                val enterKey = Key(code = Key.CODE_ENTER, iconResId = R.drawable.ic_enter, isAction = true)
                enterKey.bounds.set(curX4 + horizontalGap / 2, top4 + verticalGap / 2, curX4 + enterWidth - horizontalGap / 2, top4 + singleRowHeight - verticalGap / 2)
                enterKey.hitArea.set(curX4, top4, curX4 + enterWidth, top4 + singleRowHeight)
                r4.add(enterKey)
                rows.add(r4)
            }
        }

        rows.forEach { keys.addAll(it) }
    }

    fun findKeyAt(x: Float, y: Float): Key? {
        for (i in keys.indices.reversed()) {
            val key = keys[i]
            if (key.contains(x, y)) {
                return key
            }
        }
        return null
    }
}
