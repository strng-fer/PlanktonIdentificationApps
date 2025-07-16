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
 * Compatible dengan PlanktonInfo data class yang baru
 */
class PlanktonAdapter(private val planktonList: List<PlanktonInfo>) :
    RecyclerView.Adapter<PlanktonAdapter.PlanktonViewHolder>() {

    companion object {
        // Image cache untuk mengurangi loading berulang
        private val imageCache = LruCache<Int, Bitmap>(50)
    }

    /**
     * ViewHolder untuk item plankton dengan property yang sesuai
     */
    class PlanktonViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val planktonThumbnail: ImageView = itemView.findViewById(R.id.planktonThumbnail)
        val planktonName: TextView = itemView.findViewById(R.id.planktonName)
        val planktonDescription: TextView = itemView.findViewById(R.id.planktonDescription)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlanktonViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_plankton, parent, false)
        return PlanktonViewHolder(view)
    }

    override fun onBindViewHolder(holder: PlanktonViewHolder, position: Int) {
        val plankton = planktonList[position]

        // Log untuk debugging
        android.util.Log.d("PlanktonAdapter", "Binding item ${holder.adapterPosition}: ${plankton.name}")

        try {
            // Set text data
            holder.planktonName.text = plankton.name
            // Gabungkan type dan description karena layout tidak memiliki field terpisah untuk type
            val fullDescription = "${plankton.type} - ${plankton.description}"
            holder.planktonDescription.text = fullDescription

            // Load main image with simple approach first
            try {
                holder.planktonThumbnail.setImageResource(plankton.mainImageResId)
            } catch (e: Exception) {
                android.util.Log.e("PlanktonAdapter", "Error loading main image for ${plankton.name}: ${e.message}")
                holder.planktonThumbnail.setImageResource(R.drawable.ic_microscope)
            }

            // Tambahkan click listener yang aman untuk gambar
            holder.planktonThumbnail.setOnClickListener {
                try {
                    showImagePopup(it, plankton.mainImageResId, plankton.name, plankton)
                } catch (e: Exception) {
                    android.util.Log.e("PlanktonAdapter", "Error showing popup for ${plankton.name}: ${e.message}")
                }
            }

            // Tambahkan click listener untuk seluruh item
            holder.itemView.setOnClickListener {
                try {
                    showImagePopup(it, plankton.mainImageResId, plankton.name, plankton)
                } catch (e: Exception) {
                    android.util.Log.e("PlanktonAdapter", "Error showing popup for ${plankton.name}: ${e.message}")
                }
            }

        } catch (e: Exception) {
            android.util.Log.e("PlanktonAdapter", "Error binding data: ${e.message}")
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
    private fun showImagePopup(view: View, imageResource: Int, planktonName: String, plankton: PlanktonInfo) {
        val context = view.context

        try {
            val dialog = Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen)

            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialog.setContentView(R.layout.dialog_image_popup)

            // Get elements from the popup layout (sesuai dengan layout yang ada)
            val fullSizeImage = dialog.findViewById<ImageView>(R.id.fullSizeImage)
            val imageTitle = dialog.findViewById<TextView>(R.id.imageTitle)
            val closeButton = dialog.findViewById<ImageView>(R.id.closeButton)

            // Set data plankton
            imageTitle.text = "${plankton.name} (${plankton.type})"

            // Load main image dengan optimasi
            loadImageOptimized(context, plankton.mainImageResId, fullSizeImage, 800)

            // Set listener untuk tombol close
            closeButton.setOnClickListener {
                dialog.dismiss()
            }

            // Set listener untuk menutup dialog ketika area background diklik
            dialog.findViewById<View>(android.R.id.content).setOnClickListener {
                dialog.dismiss()
            }

            // Mencegah dialog menutup ketika gambar diklik
            fullSizeImage.setOnClickListener {
                // Do nothing - prevent dialog from closing when image is clicked
            }

            dialog.show()

        } catch (e: Exception) {
            android.util.Log.e("PlanktonAdapter", "Error creating popup dialog: ${e.message}")
        }
    }
}
