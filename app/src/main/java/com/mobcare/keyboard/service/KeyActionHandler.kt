package com.mobcare.keyboard.service

interface KeyActionHandler {
    fun onKeyTyped(character: String)
    fun onWordTyped(word: String)
    fun onBackspacePressed()
    fun onEnterPressed()
    fun onShiftToggle()
    fun onModeToggle()
    fun onEmojiToggle()
    fun onNumericPadToggle()
}
