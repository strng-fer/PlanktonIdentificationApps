package com.example.planktondetectionapps

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.TextView
import com.example.planktondetectionapps.PlanktonInfoManager

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
                additionalImages = listOf(R.drawable.pseudosolenia_calcar_avis_1, R.drawable.pseudosolenia_calcar_avis_2)
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
            // Inflate custom layout untuk popup
            val inflater = LayoutInflater.from(context)
            val popupView = inflater.inflate(R.layout.dialog_plankton_info, null)

            // Set data ke views dengan ID yang benar sesuai layout
            val titleText = popupView.findViewById<TextView>(R.id.planktonTitle)
            val typeText = popupView.findViewById<TextView>(R.id.classificationType)
            val descriptionText = popupView.findViewById<TextView>(R.id.planktonDescription)
            val imageView = popupView.findViewById<ImageView>(R.id.sampleImage1)

            titleText.text = planktonInfo.name
            typeText.text = planktonInfo.type
            descriptionText.text = planktonInfo.description

            // Safely set image resource
            try {
                imageView.setImageResource(planktonInfo.mainImageResId)
            } catch (e: Exception) {
                // If image resource doesn't exist, use a default placeholder
                imageView.setImageResource(android.R.drawable.ic_menu_gallery)
            }

            // Tampilkan dialog
            AlertDialog.Builder(context)
                .setView(popupView)
                .setPositiveButton("Tutup", null)
                .show()
        } catch (e: Exception) {
            // Fallback to simple dialog if layout inflation fails
            AlertDialog.Builder(context)
                .setTitle("Informasi Plankton")
                .setMessage("${planktonInfo.name}\n\nTipe: ${planktonInfo.type}\n\n${planktonInfo.description}")
                .setPositiveButton("Tutup", null)
                .show()
        }
    }
}
