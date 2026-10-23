package com.mobcare.keyboard.model

import android.graphics.RectF
import androidx.annotation.DrawableRes

data class Key(
    val code: Int,
    var label: String? = null,
    var hint: String? = null,
    @DrawableRes var iconResId: Int = 0,
    val bounds: RectF = RectF(),
    val hitArea: RectF = RectF(),
    val isModifier: Boolean = false,
    val isAction: Boolean = false,
    var isPressed: Boolean = false,
    val weight: Float = 1.0f
) {
    companion object {
        const val CODE_SHIFT = -1
        const val CODE_BACKSPACE = -2
        const val CODE_MODE_CHANGE = -3
        const val CODE_ENTER = -4
        const val CODE_SPACE = 32
        const val CODE_EMOJI = -5
        const val CODE_NUMERIC_PAD = -6
    }

    fun contains(x: Float, y: Float): Boolean {
        return hitArea.contains(x, y)
    }
}
