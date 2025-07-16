package com.example.planktondetectionapps

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.TextView

/**
 * Manager untuk menyediakan informasi plankton
 * Updated untuk menggunakan struktur PlanktonInfo yang baru
 */
object PlanktonInfoManager {

    /**
     * Mendapatkan semua data plankton dengan struktur data yang benar
     */
    fun getAllPlanktonInfo(): List<PlanktonInfo> {
        return listOf(
            PlanktonInfo(
                name = "Achnanthes sp",
                description = "Diatom bentik yang berbentuk oval dengan struktur raphe yang khas. Sering ditemukan menempel pada substrat di lingkungan air tawar dan laut.",
                type = "Diatom",
                mainImageResId = R.drawable.achnanthes_sp_1,
                additionalImages = listOf(R.drawable.achnanthes_sp_1, R.drawable.achnanthes_sp_2)
            ),
            PlanktonInfo(
                name = "Bacteriastrum delicatulum",
                description = "Diatom planktonik yang berbentuk rantai dengan seta panjang dan halus. Hidup di perairan laut dan membentuk koloni.",
                type = "Diatom",
                mainImageResId = R.drawable.bacteriastrum_delicatulum_1,
                additionalImages = listOf(R.drawable.bacteriastrum_delicatulum_1)
            ),
            PlanktonInfo(
                name = "Bleakeleya notata",
                description = "Diatom dengan bentuk karakteristik yang memiliki notch atau lekukan pada bagian tengah.",
                type = "Diatom",
                mainImageResId = R.drawable.bleakeleya_notata_1,
                additionalImages = listOf(R.drawable.bleakeleya_notata_1, R.drawable.bleakeleya_notata_2, R.drawable.bleakeleya_notata_3)
            ),
            PlanktonInfo(
                name = "Chaetoceros affinis",
                description = "Diatom planktonik yang membentuk rantai dengan seta yang panjang dan bercabang.",
                type = "Diatom",
                mainImageResId = R.drawable.chaetoceros_affinis_1,
                additionalImages = listOf(R.drawable.chaetoceros_affinis_1)
            ),
            PlanktonInfo(
                name = "Chaetoceros diversus",
                description = "Spesies Chaetoceros dengan variasi morfologi yang beragam. Memiliki seta yang lebih pendek dibanding C. affinis.",
                type = "Diatom",
                mainImageResId = R.drawable.chaetoceros_diversus_1,
                additionalImages = listOf(R.drawable.chaetoceros_diversus_1, R.drawable.chaetoceros_diversus_2, R.drawable.chaetoceros_diversus_3)
            ),
            PlanktonInfo(
                name = "Chaetoceros peruvianus",
                description = "Diatom yang pertama kali ditemukan di perairan Peru. Memiliki seta yang khas dan struktur rantai yang rapat.",
                type = "Diatom",
                mainImageResId = R.drawable.chaetoceros_peruvianus_1,
                additionalImages = listOf(R.drawable.chaetoceros_peruvianus_1, R.drawable.chaetoceros_peruvianus_2)
            ),
            PlanktonInfo(
                name = "Coscinodiscus oculus-iridis",
                description = "Diatom sentrik berukuran besar dengan pola ornamen yang menyerupai mata.",
                type = "Diatom",
                mainImageResId = R.drawable.coscinodiscus_oculus_iridis_1,
                additionalImages = listOf(R.drawable.coscinodiscus_oculus_iridis_1, R.drawable.coscinodiscus_oculus_iridis_2, R.drawable.coscinodiscus_oculus_iridis_3)
            ),
            PlanktonInfo(
                name = "Diatom",
                description = "Kelompok alga mikroskopis dengan dinding sel dari silika. Merupakan produsen primer utama di ekosistem akuatik.",
                type = "Diatom",
                mainImageResId = R.drawable.diatom_1,
                additionalImages = listOf(R.drawable.diatom_1, R.drawable.diatom_2, R.drawable.diatom_3)
            ),
            PlanktonInfo(
                name = "Guinardia flaccida",
                description = "Diatom berbentuk silinder panjang yang hidup soliter atau dalam rantai pendek.",
                type = "Diatom",
                mainImageResId = R.drawable.guinardia_flaccida_1,
                additionalImages = listOf(R.drawable.guinardia_flaccida_1, R.drawable.guinardia_flaccida_2, R.drawable.guinardia_flaccida_3)
            ),
            PlanktonInfo(
                name = "Hemiaulus hauckii",
                description = "Diatom dengan bentuk yang unik memiliki elevasi di bagian tengah. Hidup sebagai plankton di perairan laut.",
                type = "Diatom",
                mainImageResId = R.drawable.hemiaulus_hauckii_1,
                additionalImages = listOf(R.drawable.hemiaulus_hauckii_1, R.drawable.hemiaulus_hauckii_2, R.drawable.hemiaulus_hauckii_3)
            ),
            PlanktonInfo(
                name = "Hemiaulus membranaceus",
                description = "Spesies Hemiaulus dengan membran tipis yang karakteristik. Adaptasi khusus untuk hidup mengapung di kolom air.",
                type = "Diatom",
                mainImageResId = R.drawable.hemiaulus_membranaceus_1,
                additionalImages = listOf(R.drawable.hemiaulus_membranaceus_1, R.drawable.hemiaulus_membranaceus_2, R.drawable.hemiaulus_membranaceus_3)
            ),
            PlanktonInfo(
                name = "Mastogloia sp",
                description = "Diatom dengan chamber tambahan (partecta) di sepanjang margin sel.",
                type = "Diatom",
                mainImageResId = R.drawable.mastogloia_sp_1,
                additionalImages = listOf(R.drawable.mastogloia_sp_1, R.drawable.mastogloia_sp_2, R.drawable.mastogloia_sp_3)
            ),
            PlanktonInfo(
                name = "Nitzschia",
                description = "Genus diatom dengan raphe yang ekssentrik dan keel yang khas. Sangat beragam dan adaptif.",
                type = "Diatom",
                mainImageResId = R.drawable.nitzschia_1,
                additionalImages = listOf(R.drawable.nitzschia_1, R.drawable.nitzschia_2)
            ),
            PlanktonInfo(
                name = "Nitzschia longissima",
                description = "Spesies Nitzschia yang sangat panjang dan ramping. Memiliki rasio panjang-lebar yang ekstrem.",
                type = "Diatom",
                mainImageResId = R.drawable.nitzschia_longissima_1,
                additionalImages = listOf(R.drawable.nitzschia_longissima_1, R.drawable.nitzschia_longissima_2, R.drawable.nitzschia_longissima_3)
            ),
            PlanktonInfo(
                name = "Plagiotropis lepidoptera",
                description = "Diatom dengan bentuk yang menyerupai sayap kupu-kupu. Memiliki ornamen yang halus dan simetris.",
                type = "Diatom",
                mainImageResId = R.drawable.plagiotropis_lepidoptera_1,
                additionalImages = listOf(R.drawable.plagiotropis_lepidoptera_1, R.drawable.plagiotropis_lepidoptera_2, R.drawable.plagiotropis_lepidoptera_3)
            ),
            PlanktonInfo(
                name = "Pleurosigma",
                description = "Diatom dengan bentuk sigmoid (S) yang karakteristik. Memiliki ornamen striae yang sangat halus dan teratur.",
                type = "Diatom",
                mainImageResId = R.drawable.pleurosigma_1,
                additionalImages = listOf(R.drawable.pleurosigma_1, R.drawable.pleurosigma_2, R.drawable.pleurosigma_3)
            ),
            PlanktonInfo(
                name = "Proboscia alata",
                description = "Diatom dengan ekstensi tubular panjang yang menyerupai proboscis. Adaptasi untuk hidup mengapung.",
                type = "Diatom",
                mainImageResId = R.drawable.proboscia_alata_1,
                additionalImages = listOf(R.drawable.proboscia_alata_1, R.drawable.proboscia_alata_2, R.drawable.proboscia_alata_3)
            ),
            PlanktonInfo(
                name = "Proboscia indica",
                description = "Spesies Proboscia yang ditemukan di perairan Indo-Pasifik. Memiliki morfologi yang sama dengan P. alata.",
                type = "Diatom",
                mainImageResId = R.drawable.proboscia_indica_1,
                additionalImages = listOf(R.drawable.proboscia_indica_1, R.drawable.proboscia_indica_2, R.drawable.proboscia_indica_3)
            ),
            PlanktonInfo(
                name = "Pseudo-nitzschia spp",
                description = "Kompleks spesies diatom yang sulit dibedakan secara morfologi. Beberapa spesies dapat menghasilkan asam domoic yang beracun.",
                type = "Diatom",
                mainImageResId = R.drawable.pseudo_nitzschia_spp_1,
                additionalImages = listOf(R.drawable.pseudo_nitzschia_spp_1, R.drawable.pseudo_nitzschia_spp_2, R.drawable.pseudo_nitzschia_spp_3)
            ),
            PlanktonInfo(
                name = "Pseudosolenia calcar-avis",
                description = "Diatom dengan bentuk yang menyerupai taji ayam. Memiliki struktur silika yang kuat dengan proyeksi tajam.",
                type = "Diatom",
                mainImageResId = R.drawable.pseudosolenia_calcar_avis_1,
                additionalImages = listOf(R.drawable.pseudosolenia_calcar_avis_1, R.drawable.pseudosolenia_calcar_avis_1)
            ),
            PlanktonInfo(
                name = "Rhizosolenia calcar-avis",
                description = "Diatom berbentuk silinder dengan ekstensi seperti tanduk. Nama mengacu pada bentuknya yang menyerupai taji ayam.",
                type = "Diatom",
                mainImageResId = R.drawable.rhizosolenia_calcar_avis_1,
                additionalImages = listOf(R.drawable.rhizosolenia_calcar_avis_1, R.drawable.rhizosolenia_calcar_avis_2, R.drawable.rhizosolenia_calcar_avis_3)
            ),
            PlanktonInfo(
                name = "Rhizosolenia cochlea",
                description = "Spesies Rhizosolenia dengan bentuk melingkar seperti siput. Memiliki struktur spiral yang unik.",
                type = "Diatom",
                mainImageResId = R.drawable.rhizosolenia_cochlea_1,
                additionalImages = listOf(R.drawable.rhizosolenia_cochlea_1, R.drawable.rhizosolenia_cochlea_2, R.drawable.rhizosolenia_cochlea_3)
            ),
            PlanktonInfo(
                name = "Rhizosolenia imbricata",
                description = "Diatom dengan sel-sel yang saling tumpang tindih dalam rantai. Struktur ini memberikan kekuatan mekanik.",
                type = "Diatom",
                mainImageResId = R.drawable.rhizosolenia_imbricata_1,
                additionalImages = listOf(R.drawable.rhizosolenia_imbricata_1, R.drawable.rhizosolenia_imbricata_2, R.drawable.rhizosolenia_imbricata_3)
            ),
            PlanktonInfo(
                name = "Tetramphora decussata",
                description = "Diatom dengan empat area yang tersusun dalam pola silang. Struktur yang kompleks dengan ornamen yang detail.",
                type = "Diatom",
                mainImageResId = R.drawable.tetramphora_decussata_1,
                additionalImages = listOf(R.drawable.tetramphora_decussata_1, R.drawable.tetramphora_decussata_2, R.drawable.tetramphora_decussata_3)
            ),
            PlanktonInfo(
                name = "Thalassionema nitzschioides",
                description = "Diatom berbentuk jarum yang hidup dalam koloni berbentuk bintang atau radial. Sangat umum di perairan laut.",
                type = "Diatom",
                mainImageResId = R.drawable.thalassionema_nitzschioides_1,
                additionalImages = listOf(R.drawable.thalassionema_nitzschioides_1, R.drawable.thalassionema_nitzschioides_2, R.drawable.thalassionema_nitzschioides_3)
            ),
            PlanktonInfo(
                name = "Toxarium undulatum",
                description = "Diatom dengan permukaan yang bergelombang karakteristik. Memiliki ornamen yang kompleks dengan pola berulang.",
                type = "Diatom",
                mainImageResId = R.drawable.toxarium_undulatum_1,
                additionalImages = listOf(R.drawable.toxarium_undulatum_1, R.drawable.toxarium_undulatum_2, R.drawable.toxarium_undulatum_3)
            )
        )
    }

    /**
     * Mendapatkan informasi plankton berdasarkan nama
     */
    fun getPlanktonByName(name: String): PlanktonInfo? {
        return getAllPlanktonInfo().find { it.name.equals(name, ignoreCase = true) }
    }

    /**
     * Mendapatkan informasi plankton berdasarkan nama prediksi
     */
    fun findPlanktonByPrediction(prediction: String): PlanktonInfo? {
        val allPlankton = getAllPlanktonInfo()

        // Cari exact match dulu
        allPlankton.find { it.name.equals(prediction, ignoreCase = true) }?.let { return it }

        // Cari partial match
        return allPlankton.find { it.name.contains(prediction, ignoreCase = true) }
    }

    /**
     * Menampilkan popup informasi plankton
     */
    fun showPlanktonInfoPopup(context: Context, planktonName: String) {
        val planktonInfo = getAllPlanktonInfo().find { it.name.equals(planktonName, ignoreCase = true) }

        if (planktonInfo == null) {
            // Jika tidak ditemukan, tampilkan pesan default
            AlertDialog.Builder(context)
                .setTitle("Informasi Plankton")
                .setMessage("Informasi untuk '$planktonName' tidak tersedia.")
                .setPositiveButton("OK", null)
                .show()
            return
        }

        try {
            // Buat custom dialog dengan background transparan untuk rounded corners
            val dialog = android.app.Dialog(context)
            dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
            dialog.setContentView(R.layout.dialog_plankton_info)

            // Set dialog window properties untuk rounded corners
            dialog.window?.let { window ->
                window.setBackgroundDrawableResource(android.R.color.transparent)
                window.setLayout(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT
                )
                window.attributes?.windowAnimations = android.R.style.Animation_Dialog
            }

            // Set data ke views dengan logika yang benar
            val titleText = dialog.findViewById<TextView>(R.id.planktonTitle)
            val typeText = dialog.findViewById<TextView>(R.id.classificationType)
            val descriptionText = dialog.findViewById<TextView>(R.id.planktonDescription)
            val imageView1 = dialog.findViewById<ImageView>(R.id.sampleImage1)
            val imageView2 = dialog.findViewById<ImageView>(R.id.sampleImage2)
            val imageView3 = dialog.findViewById<ImageView>(R.id.sampleImage3)
            val closeButton = dialog.findViewById<ImageView>(R.id.closeButton)

            // Header tetap "Informasi Plankton"
            titleText.text = "Informasi Plankton"

            // Spesies menampilkan nama hasil klasifikasi
            typeText.text = planktonInfo.name

            // Deskripsi tetap menampilkan deskripsi lengkap
            descriptionText.text = planktonInfo.description

            // Set close button listener
            closeButton?.setOnClickListener {
                dialog.dismiss()
            }

            // Load gambar-gambar contoh
            try {
                // Gambar pertama - selalu ada
                imageView1.setImageResource(planktonInfo.mainImageResId)

                // Gambar kedua dan ketiga dari additionalImages jika tersedia
                if (planktonInfo.additionalImages.isNotEmpty()) {
                    if (planktonInfo.additionalImages.size > 1) {
                        imageView2.setImageResource(planktonInfo.additionalImages[1])
                    } else {
                        imageView2.setImageResource(planktonInfo.mainImageResId)
                    }

                    if (planktonInfo.additionalImages.size > 2) {
                        imageView3.setImageResource(planktonInfo.additionalImages[2])
                    } else {
                        imageView3.setImageResource(planktonInfo.mainImageResId)
                    }
                } else {
                    // Jika tidak ada additional images, gunakan main image untuk semua
                    imageView2.setImageResource(planktonInfo.mainImageResId)
                    imageView3.setImageResource(planktonInfo.mainImageResId)
                }

                // Tambahkan click listeners untuk masing-masing gambar preview
                imageView1.setOnClickListener {
                    showFullSizeImage(context, planktonInfo.mainImageResId, "${planktonInfo.name} - Gambar 1")
                }

                imageView2.setOnClickListener {
                    val imageResId = if (planktonInfo.additionalImages.size > 1) {
                        planktonInfo.additionalImages[1]
                    } else {
                        planktonInfo.mainImageResId
                    }
                    showFullSizeImage(context, imageResId, "${planktonInfo.name} - Gambar 2")
                }

                imageView3.setOnClickListener {
                    val imageResId = if (planktonInfo.additionalImages.size > 2) {
                        planktonInfo.additionalImages[2]
                    } else {
                        planktonInfo.mainImageResId
                    }
                    showFullSizeImage(context, imageResId, "${planktonInfo.name} - Gambar 3")
                }

            } catch (e: Exception) {
                // If image resources don't exist, use default placeholder
                imageView1.setImageResource(android.R.drawable.ic_menu_gallery)
                imageView2.setImageResource(android.R.drawable.ic_menu_gallery)
                imageView3.setImageResource(android.R.drawable.ic_menu_gallery)
                android.util.Log.e("PlanktonInfoManager", "Error loading images: ${e.message}")
            }

            // Tampilkan dialog
            dialog.show()

        } catch (e: Exception) {
            android.util.Log.e("PlanktonInfoManager", "Error creating dialog: ${e.message}")
            // Fallback to simple dialog if layout inflation fails
            AlertDialog.Builder(context)
                .setTitle("Informasi Plankton")
                .setMessage("${planktonInfo.name}\n\nTipe: ${planktonInfo.type}\n\n${planktonInfo.description}")
                .setPositiveButton("Tutup", null)
                .show()
        }
    }

    /**
     * Menampilkan gambar dalam ukuran penuh saat preview gambar diklik
     */
    private fun showFullSizeImage(context: Context, imageResId: Int, title: String) {
        try {
            // Buat dialog fullscreen untuk menampilkan gambar
            val dialog = android.app.Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen)

            // Inflate layout untuk fullsize image
            val inflater = LayoutInflater.from(context)
            val fullImageView = inflater.inflate(R.layout.dialog_image_popup, null)

            dialog.setContentView(fullImageView)

            // Get UI elements
            val fullSizeImage = fullImageView.findViewById<ImageView>(R.id.fullSizeImage)
            val imageTitle = fullImageView.findViewById<TextView>(R.id.imageTitle)
            val closeButton = fullImageView.findViewById<ImageView>(R.id.closeButton)

            // Set image dan title
            fullSizeImage.setImageResource(imageResId)
            imageTitle.text = title

            // Set close button listener
            closeButton.setOnClickListener {
                dialog.dismiss()
            }

            // Set background click listener untuk menutup dialog
            fullImageView.setOnClickListener {
                dialog.dismiss()
            }

            // Prevent image click from closing dialog
            fullSizeImage.setOnClickListener {
                // Do nothing - prevent dialog from closing when image is clicked
            }

            dialog.show()

        } catch (e: Exception) {
            android.util.Log.e("PlanktonInfoManager", "Error showing full size image: ${e.message}")

            // Fallback: tampilkan toast atau alert sederhana
            AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage("Tidak dapat menampilkan gambar dalam ukuran penuh")
                .setPositiveButton("OK", null)
                .show()
        }
    }
}
