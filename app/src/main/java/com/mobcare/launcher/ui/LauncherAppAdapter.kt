package com.mobcare.launcher.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mobcare.R
import com.mobcare.launcher.model.LauncherAppItem

class LauncherAppAdapter(
    private var appsList: List<LauncherAppItem>,
    private val onItemClick: (LauncherAppItem) -> Unit,
    private val onItemLongClick: (LauncherAppItem) -> Unit
) : RecyclerView.Adapter<LauncherAppAdapter.AppViewHolder>() {

    fun updateList(newList: List<LauncherAppItem>) {
        appsList = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_launcher_app, parent, false)
        return AppViewHolder(view)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        val item = appsList[position]
        holder.bind(item)
    }

    override fun getItemCount(): Int = appsList.size

    inner class AppViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivIcon: ImageView = itemView.findViewById(R.id.ivAppIcon)
        private val tvLabel: TextView = itemView.findViewById(R.id.tvAppLabel)

        fun bind(item: LauncherAppItem) {
            ivIcon.setImageDrawable(item.icon)
            tvLabel.text = item.label

            itemView.setOnClickListener {
                onItemClick(item)
            }

            itemView.setOnLongClickListener {
                onItemLongClick(item)
                true
            }
        }
    }
}
