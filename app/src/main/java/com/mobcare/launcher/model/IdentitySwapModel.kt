package com.mobcare.launcher.model

import org.json.JSONObject

/**
 * Represents an Identity Swap rule where MobCare disguises itself as another app
 * or custom identity on the Home Launcher screen.
 */
data class IdentitySwapModel(
    val targetPackageName: String,
    val originalLabel: String,
    val customLabel: String,
    val iconType: String = ICON_TYPE_CLONED,
    val customIconFileName: String? = null,
    val isOriginalHidden: Boolean = true,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val ICON_TYPE_CLONED = "CLONED"
        const val ICON_TYPE_PRESET = "PRESET"
        const val ICON_TYPE_CUSTOM = "CUSTOM_UPLOAD"

        fun fromJson(json: JSONObject): IdentitySwapModel {
            return IdentitySwapModel(
                targetPackageName = json.optString("targetPackageName", ""),
                originalLabel = json.optString("originalLabel", ""),
                customLabel = json.optString("customLabel", ""),
                iconType = json.optString("iconType", ICON_TYPE_CLONED),
                customIconFileName = json.optString("customIconFileName", null.takeIf { json.isNull("customIconFileName") }),
                isOriginalHidden = json.optBoolean("isOriginalHidden", true),
                isActive = json.optBoolean("isActive", true),
                createdAt = json.optLong("createdAt", System.currentTimeMillis())
            )
        }
    }

    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("targetPackageName", targetPackageName)
            put("originalLabel", originalLabel)
            put("customLabel", customLabel)
            put("iconType", iconType)
            put("customIconFileName", customIconFileName ?: JSONObject.NULL)
            put("isOriginalHidden", isOriginalHidden)
            put("isActive", isActive)
            put("createdAt", createdAt)
        }
    }
}
