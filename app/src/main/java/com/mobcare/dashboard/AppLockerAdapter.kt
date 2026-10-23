package com.mobcare.dashboard

import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mobcare.R
import com.mobcare.security.AppLockerManager

data class AppLockerItem(
    val packageName: String,
    val appName: String,
    val icon: Drawable
)

class AppLockerAdapter(
    private var apps: List<AppLockerItem>,
    private val onToggle: (AppLockerItem) -> Unit
) : RecyclerView.Adapter<AppLockerAdapter.ViewHolder>() {

    fun updateList(newList: List<AppLockerItem>) {
        apps = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_app_locker, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = apps[position]
        holder.bind(item)
    }

    override fun getItemCount(): Int = apps.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val frameAppContainer: FrameLayout = itemView.findViewById(R.id.frameAppContainer)
        private val ivAppIcon: ImageView = itemView.findViewById(R.id.ivAppIcon)
        private val ivCheckmark: ImageView = itemView.findViewById(R.id.ivCheckmark)
        private val tvAppName: TextView = itemView.findViewById(R.id.tvAppName)

        fun bind(item: AppLockerItem) {
            val context = itemView.context
            val density = context.resources.displayMetrics.density

            ivAppIcon.setImageDrawable(item.icon)
            tvAppName.text = item.appName

            val isLocked = AppLockerManager.getLockedPackages(context).contains(item.packageName)

            if (isLocked) {
                frameAppContainer.background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = 14f * density
                    setColor(Color.parseColor("#163A34"))
                    setStroke((2.5f * density).toInt(), Color.parseColor("#81C995"))
                }
                ivCheckmark.visibility = View.VISIBLE
                tvAppName.setTextColor(Color.parseColor("#81C995"))
            } else {
                frameAppContainer.background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = 14f * density
                    setColor(Color.parseColor("#1E1F24"))
                    setStroke((1f * density).toInt(), Color.parseColor("#2B2C33"))
                }
                ivCheckmark.visibility = View.GONE
                tvAppName.setTextColor(Color.parseColor("#E8EAED"))
            }

            itemView.setOnClickListener {
                onToggle(item)
            }
        }
    }
}