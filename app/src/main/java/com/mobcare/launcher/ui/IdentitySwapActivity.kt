package com.mobcare.launcher.ui

import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.mobcare.R
import com.mobcare.launcher.data.IdentitySwapManager
import com.mobcare.launcher.model.IdentitySwapModel

/**
 * Identity Camouflage Activity
 *
 * Provides INDEPENDENT control over:
 * 1. App Icon (Logo): Can be chosen from asset presets OR uploaded from custom gallery photos!
 * 2. App Name: Can be chosen from presets OR custom typed independently!
 *
 * Preset icons & names switch the real system App Drawer & Home Screen icon/name via Activity Aliases.
 * Custom uploaded photos pin a disguised shortcut directly to the Home Screen.
 */
class IdentitySwapActivity : AppCompatActivity() {

    companion object {
        private const val SHORTCUT_ID = "mobcare_camouflage_shortcut"
        private const val ICON_SAVE_FILENAME = "camouflage_icon.png"
    }

    // State
    private var selectedIconKey: String = "default"
    private var selectedNameKey: String = "default"
    private var galleryBitmap: Bitmap? = null
    private var isCustomUploadSelected: Boolean = false

    // UI elements
    private lateinit var ivCurrentDisguiseIcon: ImageView
    private lateinit var tvCurrentDisguiseName: TextView
    private lateinit var tvCurrentDisguiseDetails: TextView
    private lateinit var tvCurrentDisguiseBadge: TextView

    private lateinit var presetIconsRow: LinearLayout
    private lateinit var ivSelectedIconPreview: ImageView
    private lateinit var tvSelectedIconLabel: TextView

    private lateinit var presetNamesRow: LinearLayout
    private lateinit var etCustomName: EditText

    private val dp by lazy { resources.displayMetrics.density }

    // Robust Photo Picker using modern ActivityResultContracts
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            handlePickedImageUri(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        selectedIconKey = IdentitySwapManager.getCurrentIconKey(this)
        selectedNameKey = IdentitySwapManager.getCurrentNameKey(this)

        // Check if an existing custom uploaded image is saved
        val existingCustomBmp = IdentitySwapManager.getCustomIconBitmap(this, ICON_SAVE_FILENAME)
        if (existingCustomBmp != null && IdentitySwapManager.getIndependentIconFileName(this) == ICON_SAVE_FILENAME) {
            galleryBitmap = existingCustomBmp
            isCustomUploadSelected = true
        }

        val scroll = ScrollView(this).apply {
            setBackgroundColor(Color.parseColor("#0A0B0E"))
        }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, (32 * dp).toInt())
        }
        scroll.addView(root)

        root.addView(buildHeader())
        root.addView(buildStatusOverviewCard())
        root.addView(buildIconSection())
        root.addView(buildNameSection())
        root.addView(buildShortcutSection())
        root.addView(buildResetAllSection())

        setContentView(scroll)

        refreshStatusCard()
        refreshIconSelectionUI()
        refreshNameSelectionUI()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Header
    // ─────────────────────────────────────────────────────────────────────────

    private fun buildHeader(): View {
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding((16 * dp).toInt(), (18 * dp).toInt(), (16 * dp).toInt(), (14 * dp).toInt())
            setBackgroundColor(Color.parseColor("#121318"))
        }

        val back = ImageView(this).apply {
            setImageResource(R.drawable.ic_arrow_right)
            rotation = 180f
            setColorFilter(Color.parseColor("#ECEEF4"))
            layoutParams = LinearLayout.LayoutParams((32 * dp).toInt(), (32 * dp).toInt())
            setPadding((4 * dp).toInt(), (4 * dp).toInt(), (4 * dp).toInt(), (4 * dp).toInt())
            setOnClickListener { finish() }
        }
        header.addView(back)

        val titleCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = (12 * dp).toInt()
            }
        }
        titleCol.addView(TextView(this).apply {
            text = "Identity Camouflage"
            setTextColor(Color.parseColor("#ECEEF4"))
            textSize = 18f
            setTypeface(null, Typeface.BOLD)
        })
        titleCol.addView(TextView(this).apply {
            text = "Disguise App Icon & Name Independently"
            setTextColor(Color.parseColor("#7A7F8E"))
            textSize = 12f
        })
        header.addView(titleCol)

        return header
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Top Overview Status Card
    // ─────────────────────────────────────────────────────────────────────────

    private fun buildStatusOverviewCard(): View {
        val card = buildCard()

        val headerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, (14 * dp).toInt())
        }
        headerRow.addView(sectionLabel("CURRENT SYSTEM IDENTITY").apply {
            setPadding(0, 0, 0, 0)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        })

        tvCurrentDisguiseBadge = TextView(this).apply {
            textSize = 10f
            setTypeface(null, Typeface.BOLD)
            setPadding((10 * dp).toInt(), (4 * dp).toInt(), (10 * dp).toInt(), (4 * dp).toInt())
        }
        headerRow.addView(tvCurrentDisguiseBadge)
        card.addView(headerRow)

        val previewRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 14f * dp
                setColor(Color.parseColor("#14151B"))
                setStroke((1 * dp).toInt(), Color.parseColor("#262934"))
            }
            setPadding((16 * dp).toInt(), (14 * dp).toInt(), (16 * dp).toInt(), (14 * dp).toInt())
        }

        ivCurrentDisguiseIcon = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams((54 * dp).toInt(), (54 * dp).toInt())
            scaleType = ImageView.ScaleType.CENTER_CROP
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 12f * dp
                setColor(Color.parseColor("#1E1F24"))
            }
        }
        previewRow.addView(ivCurrentDisguiseIcon)

        val infoCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = (14 * dp).toInt()
            }
        }
        tvCurrentDisguiseName = TextView(this).apply {
            text = "MobCare"
            setTextColor(Color.parseColor("#FFFFFF"))
            textSize = 16f
            setTypeface(null, Typeface.BOLD)
        }
        tvCurrentDisguiseDetails = TextView(this).apply {
            text = "Logo: Default • Label: MobCare"
            setTextColor(Color.parseColor("#9AA0A6"))
            textSize = 12f
            setPadding(0, (2 * dp).toInt(), 0, 0)
        }
        infoCol.addView(tvCurrentDisguiseName)
        infoCol.addView(tvCurrentDisguiseDetails)
        previewRow.addView(infoCol)

        card.addView(previewRow)

        val hintTv = TextView(this).apply {
            text = "⚡ System-level disguise updates your phone's App Drawer and Home Screen instantly."
            setTextColor(Color.parseColor("#8AB4F8"))
            textSize = 11f
            setPadding(0, (12 * dp).toInt(), 0, 0)
        }
        card.addView(hintTv)

        return card
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Section 1: Logo (App Icon)
    // ─────────────────────────────────────────────────────────────────────────

    private fun buildIconSection(): View {
        val card = buildCard()
        card.addView(sectionLabel("SECTION 1 — APP LOGO (INDIVIDUAL)"))

        card.addView(TextView(this).apply {
            text = "Choose an icon from assets OR upload your own custom photo. Applying logo changes ONLY the app icon; your app name stays untouched."
            setTextColor(Color.parseColor("#9AA0A6"))
            textSize = 12f
            setPadding(0, 0, 0, (12 * dp).toInt())
        })

        // Upload Custom Photo Button
        val btnUploadPhoto = Button(this).apply {
            text = "📁 Upload Custom Picture from Gallery"
            textSize = 13f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#8AB4F8"))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 12f * dp
                setColor(Color.parseColor("#182335"))
                setStroke((1.5f * dp).toInt(), Color.parseColor("#38598A"))
            }
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (48 * dp).toInt()).apply {
                bottomMargin = (14 * dp).toInt()
            }
            setOnClickListener { openGalleryPicker() }
        }
        card.addView(btnUploadPhoto)

        // Subhead for Presets
        card.addView(TextView(this).apply {
            text = "Or Pick From Preset Disguise Icons:"
            setTextColor(Color.parseColor("#8AB4F8"))
            textSize = 11f
            setTypeface(null, Typeface.BOLD)
            setPadding(0, 0, 0, (8 * dp).toInt())
        })

        // Preset icons row
        val hScroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        presetIconsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        hScroll.addView(presetIconsRow)
        card.addView(hScroll)
        loadPresetIconsRow()

        // Preview of selected icon
        val previewRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, (14 * dp).toInt(), 0, 0)
        }
        ivSelectedIconPreview = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams((48 * dp).toInt(), (48 * dp).toInt())
            scaleType = ImageView.ScaleType.CENTER_CROP
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 10f * dp
                setColor(Color.parseColor("#252834"))
            }
        }
        previewRow.addView(ivSelectedIconPreview)

        val selInfo = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = (12 * dp).toInt()
            }
        }
        tvSelectedIconLabel = TextView(this).apply {
            text = "Selected Logo: MobCare"
            setTextColor(Color.parseColor("#ECEEF4"))
            textSize = 13f
            setTypeface(null, Typeface.BOLD)
        }
        selInfo.addView(tvSelectedIconLabel)
        selInfo.addView(TextView(this).apply {
            text = "Tap 'Apply Logo Only' below to confirm"
            setTextColor(Color.parseColor("#7A7F8E"))
            textSize = 11f
        })
        previewRow.addView(selInfo)
        card.addView(previewRow)

        // Button row for Logo
        val btnRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = (14 * dp).toInt()
            }
        }

        val btnResetLogo = Button(this).apply {
            text = "Reset Logo"
            textSize = 12f
            setTextColor(Color.parseColor("#F28B82"))
            setBackgroundColor(Color.TRANSPARENT)
            setOnClickListener {
                galleryBitmap = null
                isCustomUploadSelected = false
                IdentitySwapManager.deleteCustomIcon(this@IdentitySwapActivity, ICON_SAVE_FILENAME)
                val ok = IdentitySwapManager.resetIconOnly(this@IdentitySwapActivity)
                if (ok) {
                    selectedIconKey = "default"
                    refreshStatusCard()
                    refreshIconSelectionUI()
                    Toast.makeText(this@IdentitySwapActivity, "✓ Logo reset to default MobCare", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@IdentitySwapActivity, "Failed to reset logo", Toast.LENGTH_SHORT).show()
                }
            }
        }
        btnRow.addView(btnResetLogo)

        val btnApplyLogo = Button(this).apply {
            text = "✓ Apply Logo Only"
            textSize = 12f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(0, (44 * dp).toInt(), 1f).apply {
                marginStart = (8 * dp).toInt()
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 10f * dp
                setColor(Color.parseColor("#4CAF50"))
            }
            setOnClickListener {
                if (isCustomUploadSelected && galleryBitmap != null) {
                    // Apply custom uploaded photo
                    applyCustomPhotoDisguise(galleryBitmap!!)
                } else {
                    // Apply preset icon via Activity Alias
                    val ok = IdentitySwapManager.updateIconOnly(this@IdentitySwapActivity, selectedIconKey)
                    if (ok) {
                        refreshStatusCard()
                        refreshIconSelectionUI()
                        val disp = IdentitySwapManager.PRESET_ITEMS.firstOrNull { it.first == selectedIconKey }?.second ?: selectedIconKey
                        Toast.makeText(this@IdentitySwapActivity, "✓ App Logo updated to '$disp' in App Drawer!", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this@IdentitySwapActivity, "Failed to apply logo disguise", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
        btnRow.addView(btnApplyLogo)
        card.addView(btnRow)

        return card
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Section 2: Name (App Label)
    // ─────────────────────────────────────────────────────────────────────────

    private fun buildNameSection(): View {
        val card = buildCard()
        card.addView(sectionLabel("SECTION 2 — APP NAME (INDIVIDUAL)"))

        card.addView(TextView(this).apply {
            text = "Choose a disguise name below or type custom. Applying name changes ONLY the app name; your app logo stays untouched."
            setTextColor(Color.parseColor("#9AA0A6"))
            textSize = 12f
            setPadding(0, 0, 0, (14 * dp).toInt())
        })

        // Preset names scroll chips
        val hScroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        presetNamesRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        hScroll.addView(presetNamesRow)
        card.addView(hScroll)
        loadPresetNamesRow()

        // Custom name input field
        card.addView(TextView(this).apply {
            text = "Selected / Custom Name:"
            setTextColor(Color.parseColor("#8AB4F8"))
            textSize = 11f
            setTypeface(null, Typeface.BOLD)
            setPadding(0, (14 * dp).toInt(), 0, (6 * dp).toInt())
        })

        etCustomName = EditText(this).apply {
            hint = "e.g. Settings, Calculator, Clock…"
            setHintTextColor(Color.parseColor("#5F6368"))
            setTextColor(Color.parseColor("#ECEEF4"))
            textSize = 15f
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 10f * dp
                setColor(Color.parseColor("#16181F"))
                setStroke((1 * dp).toInt(), Color.parseColor("#2B2C33"))
            }
            setPadding((14 * dp).toInt(), (12 * dp).toInt(), (14 * dp).toInt(), (12 * dp).toInt())
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            isSingleLine = true
        }
        card.addView(etCustomName)

        // Buttons for Name
        val btnRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = (14 * dp).toInt()
            }
        }

        val btnResetName = Button(this).apply {
            text = "Reset Name"
            textSize = 12f
            setTextColor(Color.parseColor("#F28B82"))
            setBackgroundColor(Color.TRANSPARENT)
            setOnClickListener {
                val ok = IdentitySwapManager.resetNameOnly(this@IdentitySwapActivity)
                if (ok) {
                    selectedNameKey = "default"
                    etCustomName.setText("MobCare")
                    refreshStatusCard()
                    refreshNameSelectionUI()
                    Toast.makeText(this@IdentitySwapActivity, "✓ Name reset to MobCare", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@IdentitySwapActivity, "Failed to reset name", Toast.LENGTH_SHORT).show()
                }
            }
        }
        btnRow.addView(btnResetName)

        val btnApplyName = Button(this).apply {
            text = "✓ Apply Name Only"
            textSize = 12f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#0E0F13"))
            layoutParams = LinearLayout.LayoutParams(0, (44 * dp).toInt(), 1f).apply {
                marginStart = (8 * dp).toInt()
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 10f * dp
                setColor(Color.parseColor("#8AB4F8"))
            }
            setOnClickListener {
                val typed = etCustomName.text.toString().trim()
                val key = if (typed.isNotEmpty()) IdentitySwapManager.nameToKey(typed) else selectedNameKey
                val ok = IdentitySwapManager.updateNameOnly(this@IdentitySwapActivity, key)
                if (ok) {
                    selectedNameKey = key
                    refreshStatusCard()
                    refreshNameSelectionUI()
                    val disp = if (typed.isNotEmpty()) typed else IdentitySwapManager.getDisplayNameForKey(key)
                    Toast.makeText(this@IdentitySwapActivity, "✓ App Name updated to '$disp'!", Toast.LENGTH_LONG).show()

                    // If a custom photo shortcut was active, re-pin shortcut with new name
                    if (isCustomUploadSelected && galleryBitmap != null) {
                        pinShortcutWithBitmap(galleryBitmap!!, disp)
                    }
                } else {
                    Toast.makeText(this@IdentitySwapActivity, "Failed to apply name disguise", Toast.LENGTH_SHORT).show()
                }
            }
        }
        btnRow.addView(btnApplyName)
        card.addView(btnRow)

        return card
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Section 3: Desktop Pinned Shortcut (Optional)
    // ─────────────────────────────────────────────────────────────────────────

    private fun buildShortcutSection(): View {
        val card = buildCard()
        card.addView(sectionLabel("SECTION 3 — HOME SCREEN PINNED SHORTCUT"))

        card.addView(TextView(this).apply {
            text = "You can also pin a disguised shortcut directly to your Home Screen with the currently selected logo and name."
            setTextColor(Color.parseColor("#9AA0A6"))
            textSize = 12f
            setPadding(0, 0, 0, (12 * dp).toInt())
        })

        val btnPin = Button(this).apply {
            text = "📌 Pin Disguised Shortcut to Home Screen"
            textSize = 12f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 10f * dp
                setColor(Color.parseColor("#2D313E"))
                setStroke((1 * dp).toInt(), Color.parseColor("#434758"))
            }
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (44 * dp).toInt())
            setOnClickListener {
                val name = etCustomName.text.toString().trim().ifEmpty {
                    IdentitySwapManager.getDisplayNameForKey(selectedNameKey)
                }
                val iconBmp: Bitmap = galleryBitmap ?: run {
                    val assetFile = IdentitySwapManager.getAssetFileForIconKey(selectedIconKey)
                    try {
                        assets.open(assetFile).use { BitmapFactory.decodeStream(it) }
                    } catch (_: Exception) {
                        null
                    }
                } ?: BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher)

                pinShortcutWithBitmap(iconBmp, name)
            }
        }
        card.addView(btnPin)

        return card
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Reset Everything
    // ─────────────────────────────────────────────────────────────────────────

    private fun buildResetAllSection(): View {
        val card = buildCard()

        val btnResetAll = Button(this).apply {
            text = "↺ Reset Everything to Default MobCare"
            textSize = 13f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#F28B82"))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 10f * dp
                setColor(Color.parseColor("#261717"))
                setStroke((1 * dp).toInt(), Color.parseColor("#4A2323"))
            }
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (46 * dp).toInt())
            setOnClickListener {
                IdentitySwapManager.resetAllDisguise(this@IdentitySwapActivity)
                IdentitySwapManager.deleteCustomIcon(this@IdentitySwapActivity, ICON_SAVE_FILENAME)
                galleryBitmap = null
                isCustomUploadSelected = false
                selectedIconKey = "default"
                selectedNameKey = "default"
                etCustomName.setText("MobCare")
                refreshStatusCard()
                refreshIconSelectionUI()
                refreshNameSelectionUI()
                Toast.makeText(this@IdentitySwapActivity, "✓ All disguise reset. Identity reverted to MobCare.", Toast.LENGTH_LONG).show()
            }
        }
        card.addView(btnResetAll)
        return card
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Gallery Image Picker Logic (Android 8 to Android 16 compatible)
    // ─────────────────────────────────────────────────────────────────────────

    private fun openGalleryPicker() {
        try {
            pickImageLauncher.launch("image/*")
        } catch (e: Exception) {
            try {
                val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                    type = "image/*"
                    addCategory(Intent.CATEGORY_OPENABLE)
                }
                @Suppress("DEPRECATION")
                startActivityForResult(intent, 999)
            } catch (_: Exception) {
                Toast.makeText(this, "Could not open photo picker", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 999 && resultCode == RESULT_OK && data?.data != null) {
            handlePickedImageUri(data.data!!)
        }
    }

    private fun handlePickedImageUri(uri: Uri) {
        try {
            val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(contentResolver, uri)
                ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    decoder.isMutableRequired = true
                }
            } else {
                contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream)
                }
            }

            if (bitmap != null) {
                val scaled = Bitmap.createScaledBitmap(bitmap, 512, 512, true)
                galleryBitmap = scaled
                isCustomUploadSelected = true

                // Save to private internal storage
                IdentitySwapManager.saveCustomIconBitmap(this, scaled, ICON_SAVE_FILENAME)
                IdentitySwapManager.saveIndependentIcon(this, ICON_SAVE_FILENAME)

                ivSelectedIconPreview.setImageBitmap(scaled)
                tvSelectedIconLabel.text = "Selected Logo: Custom Photo"

                Toast.makeText(this, "✓ Image uploaded! Tap 'Apply Logo Only' to set as app icon.", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "Could not decode chosen image", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to load image: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun applyCustomPhotoDisguise(bitmap: Bitmap) {
        val name = etCustomName.text.toString().trim().ifEmpty {
            IdentitySwapManager.getDisplayNameForKey(selectedNameKey)
        }

        // Save active swap model
        IdentitySwapManager.setActiveSwap(this, IdentitySwapModel(
            targetPackageName  = "custom_upload",
            originalLabel      = "MobCare",
            customLabel        = name,
            iconType           = IdentitySwapModel.ICON_TYPE_CUSTOM,
            customIconFileName = ICON_SAVE_FILENAME,
            isOriginalHidden   = true,
            isActive           = true
        ))

        // Pin desktop shortcut with custom photo
        val success = pinShortcutWithBitmap(bitmap, name)

        // Update UI
        ivCurrentDisguiseIcon.setImageBitmap(bitmap)
        tvCurrentDisguiseName.text = name
        tvCurrentDisguiseDetails.text = "Logo: Custom Photo • Label: $name"
        tvCurrentDisguiseBadge.text = "CUSTOM PHOTO"
        tvCurrentDisguiseBadge.setTextColor(Color.parseColor("#81C995"))

        if (success) {
            Toast.makeText(this, "✓ Custom photo applied! Accept shortcut on Home Screen.", Toast.LENGTH_LONG).show()
        }
    }

    private fun pinShortcutWithBitmap(bitmap: Bitmap, label: String): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            Toast.makeText(this, "Pinned shortcuts require Android 8.0+", Toast.LENGTH_SHORT).show()
            return false
        }
        val sm = getSystemService(ShortcutManager::class.java)
        if (sm == null || !sm.isRequestPinShortcutSupported) {
            Toast.makeText(this, "Launcher does not support pinned shortcuts", Toast.LENGTH_LONG).show()
            return false
        }

        val shortcutIcon = Icon.createWithAdaptiveBitmap(Bitmap.createScaledBitmap(bitmap, 512, 512, true))
        val intent = Intent(this, CamouflageGateActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            putExtra(CamouflageGateActivity.EXTRA_DISGUISE_LABEL, label)
            putExtra(CamouflageGateActivity.EXTRA_TARGET_PACKAGE, "")
        }

        val shortcut = ShortcutInfo.Builder(this, SHORTCUT_ID)
            .setShortLabel(label)
            .setLongLabel(label)
            .setIcon(shortcutIcon)
            .setIntent(intent)
            .build()

        return try {
            sm.requestPinShortcut(shortcut, null)
            true
        } catch (e: Exception) {
            Toast.makeText(this, "Error pinning shortcut: ${e.message}", Toast.LENGTH_SHORT).show()
            false
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UI Helpers & Loaders
    // ─────────────────────────────────────────────────────────────────────────

    private fun loadPresetIconsRow() {
        presetIconsRow.removeAllViews()

        for (item in IdentitySwapManager.PRESET_ITEMS) {
            val key = item.first
            val label = item.second
            val assetFile = item.third

            val isSelected = (!isCustomUploadSelected && key == selectedIconKey)

            val itemLayout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
                setPadding((6 * dp).toInt(), (4 * dp).toInt(), (6 * dp).toInt(), (4 * dp).toInt())
                layoutParams = LinearLayout.LayoutParams((72 * dp).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
            }

            val iconView = ImageView(this).apply {
                layoutParams = LinearLayout.LayoutParams((52 * dp).toInt(), (52 * dp).toInt())
                scaleType = ImageView.ScaleType.CENTER_CROP
                background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = 12f * dp
                    setColor(Color.parseColor("#181920"))
                    setStroke((2 * dp).toInt(), if (isSelected) Color.parseColor("#4CAF50") else Color.parseColor("#282A34"))
                }
                loadAssetOrFallback(assetFile, this)
            }
            itemLayout.addView(iconView)

            val tvLabel = TextView(this).apply {
                text = label
                textSize = 10f
                gravity = Gravity.CENTER
                setTextColor(if (isSelected) Color.parseColor("#4CAF50") else Color.parseColor("#9AA0A6"))
                setPadding(0, (4 * dp).toInt(), 0, 0)
                maxLines = 1
            }
            itemLayout.addView(tvLabel)

            itemLayout.setOnClickListener {
                selectedIconKey = key
                isCustomUploadSelected = false
                refreshIconSelectionUI()
            }

            presetIconsRow.addView(itemLayout)
        }
    }

    private fun loadPresetNamesRow() {
        presetNamesRow.removeAllViews()

        for (item in IdentitySwapManager.PRESET_ITEMS) {
            val key = item.first
            val label = item.second

            val chip = TextView(this).apply {
                text = label
                textSize = 12f
                setTypeface(null, if (key == selectedNameKey) Typeface.BOLD else Typeface.NORMAL)
                setTextColor(if (key == selectedNameKey) Color.parseColor("#0E0F13") else Color.parseColor("#ECEEF4"))
                background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = 16f * dp
                    setColor(if (key == selectedNameKey) Color.parseColor("#8AB4F8") else Color.parseColor("#1E2028"))
                    setStroke((1 * dp).toInt(), if (key == selectedNameKey) Color.parseColor("#8AB4F8") else Color.parseColor("#323542"))
                }
                setPadding((14 * dp).toInt(), (8 * dp).toInt(), (14 * dp).toInt(), (8 * dp).toInt())
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    marginEnd = (8 * dp).toInt()
                }
                setOnClickListener {
                    selectedNameKey = key
                    etCustomName.setText(label)
                    refreshNameSelectionUI()
                }
            }
            presetNamesRow.addView(chip)
        }
    }

    private fun refreshStatusCard() {
        val customIconFile = IdentitySwapManager.getIndependentIconFileName(this)
        val activeSwap = IdentitySwapManager.getActiveSwap(this)

        if (customIconFile == ICON_SAVE_FILENAME || activeSwap?.iconType == IdentitySwapModel.ICON_TYPE_CUSTOM) {
            val customBmp = IdentitySwapManager.getCustomIconBitmap(this, ICON_SAVE_FILENAME)
            if (customBmp != null) {
                ivCurrentDisguiseIcon.setImageBitmap(customBmp)
                val activeName = etCustomName.text.toString().trim().ifEmpty {
                    IdentitySwapManager.getDisplayNameForKey(selectedNameKey)
                }
                tvCurrentDisguiseName.text = activeName
                tvCurrentDisguiseDetails.text = "Logo: Custom Photo • Label: $activeName"
                tvCurrentDisguiseBadge.text = "CUSTOM PHOTO"
                tvCurrentDisguiseBadge.setTextColor(Color.parseColor("#81C995"))
                tvCurrentDisguiseBadge.background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = 8f * dp
                    setColor(Color.parseColor("#152C1E"))
                    setStroke((1 * dp).toInt(), Color.parseColor("#2D6843"))
                }
                return
            }
        }

        val activeIconKey = IdentitySwapManager.getCurrentIconKey(this)
        val activeNameKey = IdentitySwapManager.getCurrentNameKey(this)

        val assetFile = IdentitySwapManager.getAssetFileForIconKey(activeIconKey)
        loadAssetOrFallback(assetFile, ivCurrentDisguiseIcon)

        val activeName = IdentitySwapManager.getDisplayNameForKey(activeNameKey)
        tvCurrentDisguiseName.text = activeName

        val isDisguised = (activeIconKey != "default" || activeNameKey != "default")
        if (isDisguised) {
            tvCurrentDisguiseBadge.text = "ACTIVE CLOAK"
            tvCurrentDisguiseBadge.setTextColor(Color.parseColor("#81C995"))
            tvCurrentDisguiseBadge.background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 8f * dp
                setColor(Color.parseColor("#152C1E"))
                setStroke((1 * dp).toInt(), Color.parseColor("#2D6843"))
            }
            val iconLabel = IdentitySwapManager.PRESET_ITEMS.firstOrNull { it.first == activeIconKey }?.second ?: activeIconKey
            tvCurrentDisguiseDetails.text = "Logo: $iconLabel • Label: $activeName"
        } else {
            tvCurrentDisguiseBadge.text = "DEFAULT"
            tvCurrentDisguiseBadge.setTextColor(Color.parseColor("#9AA0A6"))
            tvCurrentDisguiseBadge.background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 8f * dp
                setColor(Color.parseColor("#1C1E24"))
                setStroke((1 * dp).toInt(), Color.parseColor("#353844"))
            }
            tvCurrentDisguiseDetails.text = "Original MobCare Identity Active"
        }
    }

    private fun refreshIconSelectionUI() {
        if (isCustomUploadSelected && galleryBitmap != null) {
            ivSelectedIconPreview.setImageBitmap(galleryBitmap)
            tvSelectedIconLabel.text = "Selected Logo: Custom Uploaded Photo"
        } else {
            val assetFile = IdentitySwapManager.getAssetFileForIconKey(selectedIconKey)
            loadAssetOrFallback(assetFile, ivSelectedIconPreview)
            val label = IdentitySwapManager.PRESET_ITEMS.firstOrNull { it.first == selectedIconKey }?.second ?: selectedIconKey
            tvSelectedIconLabel.text = "Selected Logo: $label"
        }

        loadPresetIconsRow()
    }

    private fun refreshNameSelectionUI() {
        loadPresetNamesRow()
        if (etCustomName.text.isNullOrEmpty()) {
            etCustomName.setText(IdentitySwapManager.getDisplayNameForKey(selectedNameKey))
        }
    }

    private fun loadAssetOrFallback(fileName: String, imageView: ImageView) {
        try {
            assets.open(fileName).use {
                val bmp = BitmapFactory.decodeStream(it)
                if (bmp != null) {
                    imageView.setImageBitmap(bmp)
                    return
                }
            }
        } catch (_: Exception) {}
        imageView.setImageResource(R.mipmap.ic_launcher)
    }

    private fun buildCard(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 20f * dp
            setColor(Color.parseColor("#181920"))
            setStroke((1 * dp).toInt(), Color.parseColor("#262934"))
        }
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            setMargins((16 * dp).toInt(), (12 * dp).toInt(), (16 * dp).toInt(), 0)
        }
        setPadding((18 * dp).toInt(), (18 * dp).toInt(), (18 * dp).toInt(), (18 * dp).toInt())
    }

    private fun sectionLabel(text: String) = TextView(this).apply {
        this.text = text
        setTextColor(Color.parseColor("#8AB4F8"))
        textSize = 11f
        setTypeface(null, Typeface.BOLD)
        letterSpacing = 0.08f
        setPadding(0, 0, 0, (12 * dp).toInt())
    }
}
