package com.mobcare.dashboard

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.mobcare.R

enum class NavTab {
    HOME,
    ACTIVITY,
    PROTECTION,
    SETTINGS
}

object BottomNavHelper {

    fun setup(activity: Activity, currentTab: NavTab) {
        val navHome = activity.findViewById<LinearLayout>(R.id.navHome) ?: return
        val navActivity = activity.findViewById<LinearLayout>(R.id.navActivity) ?: return
        val navProtection = activity.findViewById<LinearLayout>(R.id.navProtection) ?: return
        val navSettings = activity.findViewById<LinearLayout>(R.id.navSettings) ?: return

        val ivNavHome = activity.findViewById<ImageView>(R.id.ivNavHome)
        val tvNavHome = activity.findViewById<TextView>(R.id.tvNavHome)
        val indNavHome = activity.findViewById<View>(R.id.indNavHome)

        val ivNavActivity = activity.findViewById<ImageView>(R.id.ivNavActivity)
        val tvNavActivity = activity.findViewById<TextView>(R.id.tvNavActivity)
        val indNavActivity = activity.findViewById<View>(R.id.indNavActivity)

        val ivNavProtection = activity.findViewById<ImageView>(R.id.ivNavProtection)
        val tvNavProtection = activity.findViewById<TextView>(R.id.tvNavProtection)
        val indNavProtection = activity.findViewById<View>(R.id.indNavProtection)

        val ivNavSettings = activity.findViewById<ImageView>(R.id.ivNavSettings)
        val tvNavSettings = activity.findViewById<TextView>(R.id.tvNavSettings)
        val indNavSettings = activity.findViewById<View>(R.id.indNavSettings)

        val activeColor = Color.parseColor("#4CAF50")
        val inactiveColor = Color.parseColor("#6B7280")

        // Reset all
        tvNavHome?.setTextColor(inactiveColor)
        indNavHome?.visibility = View.GONE
        ivNavHome?.setColorFilter(inactiveColor)

        tvNavActivity?.setTextColor(inactiveColor)
        indNavActivity?.visibility = View.GONE
        ivNavActivity?.setColorFilter(inactiveColor)

        tvNavProtection?.setTextColor(inactiveColor)
        indNavProtection?.visibility = View.GONE
        ivNavProtection?.setColorFilter(inactiveColor)

        tvNavSettings?.setTextColor(inactiveColor)
        indNavSettings?.visibility = View.GONE
        ivNavSettings?.setColorFilter(inactiveColor)

        // Highlight current
        when (currentTab) {
            NavTab.HOME -> {
                tvNavHome?.setTextColor(activeColor)
                indNavHome?.visibility = View.VISIBLE
                ivNavHome?.setColorFilter(activeColor)
            }
            NavTab.ACTIVITY -> {
                tvNavActivity?.setTextColor(activeColor)
                indNavActivity?.visibility = View.VISIBLE
                ivNavActivity?.setColorFilter(activeColor)
            }
            NavTab.PROTECTION -> {
                tvNavProtection?.setTextColor(activeColor)
                indNavProtection?.visibility = View.VISIBLE
                ivNavProtection?.setColorFilter(activeColor)
            }
            NavTab.SETTINGS -> {
                tvNavSettings?.setTextColor(activeColor)
                indNavSettings?.visibility = View.VISIBLE
                ivNavSettings?.setColorFilter(activeColor)
            }
        }

        // Nav click listeners with seamless transitions
        navHome.setOnClickListener {
            if (currentTab != NavTab.HOME) {
                val intent = Intent(activity, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                }
                activity.startActivity(intent)
                activity.overridePendingTransition(0, 0)
            }
        }

        navActivity.setOnClickListener {
            if (currentTab != NavTab.ACTIVITY) {
                val intent = Intent(activity, AppTimelineActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                }
                activity.startActivity(intent)
                activity.overridePendingTransition(0, 0)
            }
        }

        navProtection.setOnClickListener {
            if (currentTab != NavTab.PROTECTION) {
                val intent = Intent(activity, HardSecurityActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                }
                activity.startActivity(intent)
                activity.overridePendingTransition(0, 0)
            }
        }

        navSettings.setOnClickListener {
            if (currentTab != NavTab.SETTINGS) {
                val intent = Intent(activity, SettingsActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                }
                activity.startActivity(intent)
                activity.overridePendingTransition(0, 0)
            }
        }
    }
}
