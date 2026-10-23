package com.mobcare.dashboard

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mobcare.R
import com.mobcare.security.IntruderCaptureManager
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

data class GalleryItem(
    val file: File,
    val timestamp: Long,
    val title: String
)

class GalleryActivity : AppCompatActivity() {

    private lateinit var btnBack: ImageView
    private lateinit var tabScreenshots: Button
    private lateinit var tabIntruders: Button
    private lateinit var tvEmptyState: TextView
    private lateinit var tvCountBadge: TextView
    private lateinit var rvMediaGrid: RecyclerView

    private var currentTab = "SCREENSHOTS" // "SCREENSHOTS" or "INTRUDERS"
    private val mediaList = mutableListOf<GalleryItem>()
    private lateinit var adapter: GalleryMediaAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gallery)

        btnBack = findViewById(R.id.btnBack)
        tabScreenshots = findViewById(R.id.tabScreenshots)
        tabIntruders = findViewById(R.id.tabIntruders)
        tvEmptyState = findViewById(R.id.tvEmptyState)
        tvCountBadge = findViewById(R.id.tvCountBadge)
        rvMediaGrid = findViewById(R.id.rvMediaGrid)

        btnBack.setOnClickListener { finish() }

        rvMediaGrid.layoutManager = GridLayoutManager(this, 3)
        adapter = GalleryMediaAdapter(this, mediaList) { item ->
            showFullImageView(item)
        }
        rvMediaGrid.adapter = adapter

        val initialTab = intent.getStringExtra("INITIAL_TAB") ?: "SCREENSHOTS"
        currentTab = initialTab

        tabScreenshots.setOnClickListener { switchTab("SCREENSHOTS") }
        tabIntruders.setOnClickListener { switchTab("INTRUDERS") }

        updateTabUi()
        loadMedia()
    }

    private fun switchTab(tab: String) {
        currentTab = tab
        updateTabUi()
        loadMedia()
    }

    private fun updateTabUi() {
        val activeTint = ColorStateList.valueOf(androidx.core.content.ContextCompat.getColor(this, R.color.btn_primary_bg))
        val inactiveTint = ColorStateList.valueOf(androidx.core.content.ContextCompat.getColor(this, R.color.btn_secondary_bg))
        val activeText = androidx.core.content.ContextCompat.getColor(this, R.color.btn_primary_text)
        val inactiveText = androidx.core.content.ContextCompat.getColor(this, R.color.btn_secondary_text)

        if (currentTab == "SCREENSHOTS") {
            tabScreenshots.backgroundTintList = activeTint
            tabScreenshots.setTextColor(activeText)
            tabIntruders.backgroundTintList = inactiveTint
            tabIntruders.setTextColor(inactiveText)
        } else {
            tabIntruders.backgroundTintList = activeTint
            tabIntruders.setTextColor(activeText)
            tabScreenshots.backgroundTintList = inactiveTint
            tabScreenshots.setTextColor(inactiveText)
        }
    }

    private fun loadMedia() {
        Executors.newSingleThreadExecutor().execute {
            val list = mutableListOf<GalleryItem>()
            val dir = if (currentTab == "SCREENSHOTS") {
                File(filesDir, "screenshots")
            } else {
                File(filesDir, "intruders")
            }

            if (dir.exists()) {
                val files = dir.listFiles()?.filter { it.extension in listOf("jpg", "jpeg", "png") }
                    ?.sortedByDescending { it.lastModified() } ?: emptyList()

                val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
                files.forEach { f ->
                    list.add(
                        GalleryItem(
                            file = f,
                            timestamp = f.lastModified(),
                            title = sdf.format(Date(f.lastModified()))
                        )
                    )
                }
            }

            Handler(Looper.getMainLooper()).post {
                mediaList.clear()
                mediaList.addAll(list)
                adapter.notifyDataSetChanged()

                tvCountBadge.text = "${mediaList.size} ITEMS"
                tvEmptyState.visibility = if (mediaList.isEmpty()) View.VISIBLE else View.GONE
                tvEmptyState.text = if (currentTab == "SCREENSHOTS") {
                    "No silent screenshots captured yet."
                } else {
                    "No intruder photos captured yet."
                }
            }
        }
    }

    private fun showFullImageView(item: GalleryItem) {
        val dialog = Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.BLACK))

        val density = resources.displayMetrics.density

        val root = android.widget.RelativeLayout(this).apply {
            setBackgroundColor(Color.BLACK)
        }

        // Top bar
        val topBar = android.widget.LinearLayout(this).apply {
            id = View.generateViewId()
            orientation = android.widget.LinearLayout.HORIZONTAL
            setPadding((16 * density).toInt(), (32 * density).toInt(), (16 * density).toInt(), (16 * density).toInt())
            setBackgroundColor(Color.parseColor("#CC121316"))
        }

        val btnClose = ImageView(this).apply {
            setImageResource(R.drawable.ic_chevron_left)
            setPadding((6 * density).toInt(), (6 * density).toInt(), (6 * density).toInt(), (6 * density).toInt())
            setOnClickListener { dialog.dismiss() }
        }
        topBar.addView(btnClose, android.widget.LinearLayout.LayoutParams((36 * density).toInt(), (36 * density).toInt()))

        val titleTv = TextView(this).apply {
            text = item.title
            setTextColor(Color.parseColor("#E8EAED"))
            textSize = 16f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding((12 * density).toInt(), 0, 0, 0)
        }
        topBar.addView(titleTv, android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

        val btnDelete = Button(this).apply {
            text = "Delete"
            setTextColor(Color.parseColor("#F28B82"))
            setBackgroundColor(Color.TRANSPARENT)
            setOnClickListener {
                if (item.file.exists()) {
                    item.file.delete()
                    Toast.makeText(this@GalleryActivity, "Deleted successfully", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
                loadMedia()
            }
        }
        topBar.addView(btnDelete)

        val topParams = android.widget.RelativeLayout.LayoutParams(
            android.widget.RelativeLayout.LayoutParams.MATCH_PARENT,
            android.widget.RelativeLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            addRule(android.widget.RelativeLayout.ALIGN_PARENT_TOP)
        }
        root.addView(topBar, topParams)

        // ImageView
        val fullIv = ImageView(this).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
            Executors.newSingleThreadExecutor().execute {
                val bmp = BitmapFactory.decodeFile(item.file.absolutePath)
                Handler(Looper.getMainLooper()).post {
                    setImageBitmap(bmp)
                }
            }
        }

        val ivParams = android.widget.RelativeLayout.LayoutParams(
            android.widget.RelativeLayout.LayoutParams.MATCH_PARENT,
            android.widget.RelativeLayout.LayoutParams.MATCH_PARENT
        ).apply {
            addRule(android.widget.RelativeLayout.BELOW, topBar.id)
        }
        root.addView(fullIv, ivParams)

        dialog.setContentView(root)
        dialog.show()
    }
}

class GalleryMediaAdapter(
    private val context: Context,
    private val items: List<GalleryItem>,
    private val onItemClick: (GalleryItem) -> Unit
) : RecyclerView.Adapter<GalleryMediaAdapter.ViewHolder>() {

    class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val ivThumbnail: ImageView = v.findViewById(R.id.ivThumbnail)
        val tvTimestamp: TextView = v.findViewById(R.id.tvTimestamp)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_gallery_media, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvTimestamp.text = item.title

        // Efficient thumbnail loading
        holder.ivThumbnail.tag = item.file.absolutePath
        holder.ivThumbnail.setImageBitmap(null)

        Executors.newSingleThreadExecutor().execute {
            val bmp = decodeSampledBitmap(item.file.absolutePath, 200, 200)
            Handler(Looper.getMainLooper()).post {
                if (holder.ivThumbnail.tag == item.file.absolutePath) {
                    holder.ivThumbnail.setImageBitmap(bmp)
                }
            }
        }

        holder.itemView.setOnClickListener {
            onItemClick(item)
        }
    }

    override fun getItemCount(): Int = items.size

    private fun decodeSampledBitmap(path: String, reqWidth: Int, reqHeight: Int): Bitmap? {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeFile(path, options)

        var inSampleSize = 1
        if (options.outHeight > reqHeight || options.outWidth > reqWidth) {
            val halfHeight = options.outHeight / 2
            val halfWidth = options.outWidth / 2
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }

        options.inJustDecodeBounds = false
        options.inSampleSize = inSampleSize
        return BitmapFactory.decodeFile(path, options)
    }
}
