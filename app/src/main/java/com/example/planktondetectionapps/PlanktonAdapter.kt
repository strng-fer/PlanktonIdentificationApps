package com.example.planktondetectionapps

import android.app.Dialog
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import android.util.LruCache

/**
 * Adapter untuk menampilkan daftar plankton dalam RecyclerView dengan optimasi performa
 */
class PlanktonAdapter(private val planktonList: List<PlanktonInfo>) :
    RecyclerView.Adapter<PlanktonAdapter.PlanktonViewHolder>() {

    companion object {
        // Image cache untuk mengurangi loading berulang
        private val imageCache = LruCache<Int, Bitmap>(50)
    }

    /**
     * ViewHolder untuk item plankton
     */
    class PlanktonViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val planktonImage: ImageView = itemView.findViewById(R.id.planktonImage)
        val planktonName: TextView = itemView.findViewById(R.id.planktonName)
        val planktonDescription: TextView = itemView.findViewById(R.id.planktonDescription)
        val sampleImage1: ImageView = itemView.findViewById(R.id.sampleImage1)
        val sampleImage2: ImageView = itemView.findViewById(R.id.sampleImage2)
        val sampleImage3: ImageView = itemView.findViewById(R.id.sampleImage3)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlanktonViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_plankton, parent, false)
        return PlanktonViewHolder(view)
    }

    override fun onBindViewHolder(holder: PlanktonViewHolder, position: Int) {
        val plankton = planktonList[position]
        val context = holder.itemView.context

        // Log untuk debugging
        android.util.Log.d("PlanktonAdapter", "Binding item $position: ${plankton.name}")

        holder.planktonName.text = plankton.name
        holder.planktonDescription.text = plankton.description

        // Load main image with simple approach first
        try {
            holder.planktonImage.setImageResource(plankton.imageResource)
        } catch (e: Exception) {
            android.util.Log.e("PlanktonAdapter", "Error loading main image for ${plankton.name}: ${e.message}")
            holder.planktonImage.setImageResource(R.drawable.ic_microscope)
        }

        // Set sample images with simple loading
        if (plankton.sampleImages.isNotEmpty()) {
            try {
                val imageRes1 = plankton.sampleImages.getOrElse(0) { plankton.imageResource }
                val imageRes2 = plankton.sampleImages.getOrElse(1) { plankton.imageResource }
                val imageRes3 = plankton.sampleImages.getOrElse(2) { plankton.imageResource }

                holder.sampleImage1.setImageResource(imageRes1)
                holder.sampleImage2.setImageResource(imageRes2)
                holder.sampleImage3.setImageResource(imageRes3)
            } catch (e: Exception) {
                android.util.Log.e("PlanktonAdapter", "Error loading sample images for ${plankton.name}: ${e.message}")
                holder.sampleImage1.setImageResource(R.drawable.ic_microscope)
                holder.sampleImage2.setImageResource(R.drawable.ic_microscope)
                holder.sampleImage3.setImageResource(R.drawable.ic_microscope)
            }
        }

        // Set click listener untuk gambar utama
        holder.planktonImage.setOnClickListener {
            showImagePopup(holder.itemView, plankton.imageResource, plankton.name)
        }

        // Set click listeners untuk gambar contoh
        holder.sampleImage1.setOnClickListener {
            val imageRes = plankton.sampleImages.getOrElse(0) { plankton.imageResource }
            showImagePopup(holder.itemView, imageRes, "${plankton.name} - Contoh 1")
        }

        holder.sampleImage2.setOnClickListener {
            val imageRes = plankton.sampleImages.getOrElse(1) { plankton.imageResource }
            showImagePopup(holder.itemView, imageRes, "${plankton.name} - Contoh 2")
        }

        holder.sampleImage3.setOnClickListener {
            val imageRes = plankton.sampleImages.getOrElse(2) { plankton.imageResource }
            showImagePopup(holder.itemView, imageRes, "${plankton.name} - Contoh 3")
        }
    }

    override fun getItemCount(): Int = planktonList.size

    override fun getItemViewType(position: Int): Int = 0

    /**
     * Load image dengan optimasi dan caching untuk mengurangi lag
     */
    private fun loadImageOptimized(context: android.content.Context, imageRes: Int, imageView: ImageView, targetSize: Int) {
        // Cek cache terlebih dahulu
        val cacheKey = "${imageRes}_$targetSize"
        val cachedBitmap = imageCache.get(cacheKey.hashCode())

        if (cachedBitmap != null) {
            imageView.setImageBitmap(cachedBitmap)
            return
        }

        // Load image dalam background thread untuk menghindari blocking UI
        Thread {
            try {
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeResource(context.resources, imageRes, options)

                // Calculate inSampleSize
                options.inSampleSize = calculateInSampleSize(options, targetSize, targetSize)
                options.inJustDecodeBounds = false
                options.inPreferredConfig = Bitmap.Config.RGB_565 // Menggunakan lebih sedikit memory

                val bitmap = BitmapFactory.decodeResource(context.resources, imageRes, options)
                bitmap?.let {
                    imageCache.put(cacheKey.hashCode(), it)

                    // Update UI di main thread
                    imageView.post {
                        imageView.setImageBitmap(it)
                    }
                } ?: run {
                    // Jika bitmap null, gunakan fallback
                    imageView.post {
                        imageView.setImageResource(R.drawable.ic_microscope)
                    }
                }
            } catch (e: Exception) {
                // Log error untuk debugging
                android.util.Log.e("PlanktonAdapter", "Error loading image resource $imageRes: ${e.message}")
                // Fallback ke loading normal jika ada error
                imageView.post {
                    try {
                        imageView.setImageResource(imageRes)
                    } catch (fallbackError: Exception) {
                        // Jika fallback juga gagal, gunakan default image
                        imageView.setImageResource(R.drawable.ic_microscope)
                    }
                }
            }
        }.start()
    }

    /**
     * Calculate sample size untuk optimasi memory
     */
    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
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
     * Menampilkan popup gambar full size dengan optimasi loading
     */
    private fun showImagePopup(view: View, imageResource: Int, planktonName: String) {
        val context = view.context
        val dialog = Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen)

        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_image_popup)

        val fullSizeImage = dialog.findViewById<ImageView>(R.id.fullSizeImage)
        val imageTitle = dialog.findViewById<TextView>(R.id.imageTitle)
        val closeButton = dialog.findViewById<ImageView>(R.id.closeButton)
        val backgroundLayout = dialog.findViewById<View>(android.R.id.content)

        // Load image dengan optimasi untuk popup
        loadImageOptimized(context, imageResource, fullSizeImage, 800)
        imageTitle.text = planktonName

        // Set listener untuk tombol close
        closeButton.setOnClickListener {
            dialog.dismiss()
        }

        // Set listener untuk menutup dialog ketika area background diklik
        backgroundLayout.setOnClickListener {
            dialog.dismiss()
        }

        // Mencegah dialog menutup ketika gambar diklik
        fullSizeImage.setOnClickListener {
            // Do nothing - prevent dialog from closing when image is clicked
        }

        dialog.show()
    }
}
