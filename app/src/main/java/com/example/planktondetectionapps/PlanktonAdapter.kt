package com.example.planktondetectionapps

import android.app.Dialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

/**
 * Adapter untuk menampilkan daftar plankton dalam RecyclerView
 */
class PlanktonAdapter(private val planktonList: List<PlanktonInfo>) :
    RecyclerView.Adapter<PlanktonAdapter.PlanktonViewHolder>() {

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

        holder.planktonName.text = plankton.name
        holder.planktonDescription.text = plankton.description
        holder.planktonImage.setImageResource(plankton.imageResource)

        // Set click listener untuk gambar utama
        holder.planktonImage.setOnClickListener {
            showImagePopup(holder.itemView, plankton.imageResource, plankton.name)
        }

        // Set click listeners untuk gambar contoh
        holder.sampleImage1.setOnClickListener {
            showImagePopup(holder.itemView, plankton.imageResource, "${plankton.name} - Contoh 1")
        }

        holder.sampleImage2.setOnClickListener {
            showImagePopup(holder.itemView, plankton.imageResource, "${plankton.name} - Contoh 2")
        }

        holder.sampleImage3.setOnClickListener {
            showImagePopup(holder.itemView, plankton.imageResource, "${plankton.name} - Contoh 3")
        }
    }

    override fun getItemCount(): Int = planktonList.size

    /**
     * Menampilkan popup gambar full size
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

        // Set gambar dan judul
        fullSizeImage.setImageResource(imageResource)
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
