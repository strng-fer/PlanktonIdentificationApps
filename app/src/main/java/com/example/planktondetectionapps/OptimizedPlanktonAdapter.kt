package com.example.planktondetectionapps

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.*

/**
 * Optimized adapter untuk galeri plankton dengan thumbnail loading dan lazy loading
 * Menggunakan thumbnail untuk preview dan full image hanya saat diperlukan
 */
class OptimizedPlanktonAdapter(
    private val context: Context,
    private val planktonList: List<PlanktonInfo>
) : RecyclerView.Adapter<OptimizedPlanktonAdapter.PlanktonViewHolder>() {

    // Cache untuk thumbnail yang sudah di-load
    private val thumbnailCache = mutableMapOf<Int, Bitmap>()

    // Scope untuk coroutines
    private val adapterScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    /**
     * ViewHolder untuk item plankton dengan optimasi memory
     */
    class PlanktonViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val thumbnailImageView: ImageView = itemView.findViewById(R.id.planktonThumbnail)
        val nameTextView: TextView = itemView.findViewById(R.id.planktonName)
        val descriptionTextView: TextView = itemView.findViewById(R.id.planktonDescription)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlanktonViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_plankton, parent, false)
        return PlanktonViewHolder(view)
    }

    override fun onBindViewHolder(holder: PlanktonViewHolder, position: Int) {
        val plankton = planktonList[position]

        // Set text data immediately
        holder.nameTextView.text = plankton.name
        // Gabungkan type dan description karena tidak ada field terpisah untuk type
        val fullDescription = "${plankton.type} - ${plankton.description}"
        holder.descriptionTextView.text = if (fullDescription.length > 100) {
            "${fullDescription.take(100)}..."
        } else {
            fullDescription
        }

        // Load thumbnail asynchronously
        loadThumbnail(holder.thumbnailImageView, plankton.mainImageResId)

        // Set click listener untuk menampilkan full image dengan error handling
        holder.itemView.setOnClickListener {
            try {
                showFullImageDialog(plankton)
            } catch (e: Exception) {
                android.util.Log.e("OptimizedPlanktonAdapter", "Error showing popup for ${plankton.name}: ${e.message}")
            }
        }

        // Tambahkan click listener untuk thumbnail juga
        holder.thumbnailImageView.setOnClickListener {
            try {
                showFullImageDialog(plankton)
            } catch (e: Exception) {
                android.util.Log.e("OptimizedPlanktonAdapter", "Error showing popup for ${plankton.name}: ${e.message}")
            }
        }
    }

    /**
     * Load thumbnail dengan optimasi memory dan caching
     */
    private fun loadThumbnail(imageView: ImageView, imageResId: Int) {
        // Check cache first
        thumbnailCache[imageResId]?.let { cachedThumbnail ->
            imageView.setImageBitmap(cachedThumbnail)
            return
        }

        // Set placeholder while loading
        imageView.setImageResource(R.drawable.ic_plankton_logo)

        // Load thumbnail in background
        adapterScope.launch {
            try {
                val thumbnail = withContext(Dispatchers.IO) {
                    createThumbnail(imageResId)
                }

                // Cache the thumbnail
                thumbnailCache[imageResId] = thumbnail

                // Set thumbnail to ImageView on main thread
                imageView.setImageBitmap(thumbnail)
            } catch (e: Exception) {
                android.util.Log.e("OptimizedAdapter", "Error loading thumbnail: ${e.message}")
                // Keep placeholder if error occurs
            }
        }
    }

    /**
     * Create thumbnail dengan ukuran yang lebih kecil untuk menghemat memory
     */
    private fun createThumbnail(imageResId: Int): Bitmap {
        val options = BitmapFactory.Options().apply {
            // First decode to get image dimensions
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeResource(context.resources, imageResId, options)

        // Calculate sample size for thumbnail (target: 150x150 dp)
        val targetSize = (150 * context.resources.displayMetrics.density).toInt()
        options.inSampleSize = calculateInSampleSize(options, targetSize, targetSize)

        // Decode with sample size
        options.inJustDecodeBounds = false
        options.inPreferredConfig = Bitmap.Config.RGB_565 // Use less memory

        return BitmapFactory.decodeResource(context.resources, imageResId, options)
    }

    /**
     * Calculate optimal sample size untuk thumbnail
     */
    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2

            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    /**
     * Show full image dialog dengan loading yang optimized
     */
    private fun showFullImageDialog(plankton: PlanktonInfo) {
        val dialogView = LayoutInflater.from(context)
            .inflate(R.layout.dialog_image_popup, null)

        val dialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .create()

        // Set transparent background
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val fullImageView = dialogView.findViewById<ImageView>(R.id.fullSizeImage)
        val titleTextView = dialogView.findViewById<TextView>(R.id.imageTitle)
        val closeButton = dialogView.findViewById<ImageView>(R.id.closeButton)

        // Set title
        titleTextView.text = plankton.name

        // Load full image asynchronously
        loadFullImage(fullImageView, plankton.mainImageResId)

        // Close button
        closeButton.setOnClickListener {
            dialog.dismiss()
        }

        // Close on background click
        dialogView.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    /**
     * Load full image dengan optimasi untuk dialog
     */
    private fun loadFullImage(imageView: ImageView, imageResId: Int) {
        // Show thumbnail first if available
        thumbnailCache[imageResId]?.let { thumbnail ->
            imageView.setImageBitmap(thumbnail)
        }

        adapterScope.launch {
            try {
                val fullImage = withContext(Dispatchers.IO) {
                    // Load with reasonable quality for dialog display
                    val options = BitmapFactory.Options().apply {
                        inSampleSize = 2 // Reduce size by half to prevent OOM
                        inPreferredConfig = Bitmap.Config.RGB_565
                    }
                    BitmapFactory.decodeResource(context.resources, imageResId, options)
                }

                imageView.setImageBitmap(fullImage)
            } catch (e: Exception) {
                android.util.Log.e("OptimizedAdapter", "Error loading full image: ${e.message}")
            }
        }
    }

    override fun getItemCount(): Int = planktonList.size

    /**
     * Clean up resources when adapter is destroyed
     */
    fun cleanup() {
        adapterScope.cancel()
        thumbnailCache.clear()
    }
}
