package com.example.planktondetectionapps

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

/**
 * Manager class untuk menangani informasi plankton dan popup
 */
class PlanktonInfoManager {

    companion object {
        /**
         * Data lengkap informasi plankton dengan gambar contoh dan deskripsi
         */
        private val planktonDatabase = mapOf(
            "Achnanthes sp" to PlanktonInfo(
                name = "Achnanthes sp",
                description = "Diatom bentik yang memiliki karakteristik bentuk elips dengan raphe yang melengkung. Biasanya ditemukan menempel pada substrat keras di lingkungan perairan dangkal. Memiliki ukuran yang relatif kecil dan berperan penting dalam ekosistem bentik sebagai produser primer.",
                classification = "Achnanthes sp",
                imageResource = R.drawable.achnanthes_sp_1,
                sampleImages = listOf(
                    R.drawable.achnanthes_sp_1,
                    R.drawable.achnanthes_sp_2,
                    R.drawable.diatom_1
                )
            ),

            "Bacteriastrum delicatulum" to PlanktonInfo(
                name = "Bacteriastrum delicatulum",
                description = "Diatom planktonik yang membentuk rantai dengan karakteristik proses atau duri yang panjang. Spesies ini sering ditemukan di perairan tropis dan subtropis. Memiliki kemampuan beradaptasi yang baik terhadap perubahan kondisi lingkungan dan berperan sebagai indikator kualitas perairan.",
                classification = "Bacteriastrum delicatulum",
                imageResource = R.drawable.bacteriastrum_delicatulum_1,
                sampleImages = listOf(
                    R.drawable.bacteriastrum_delicatulum_1,
                    R.drawable.diatom_2,
                    R.drawable.diatom_3
                )
            ),

            "Bleakeleya notata" to PlanktonInfo(
                name = "Bleakeleya notata",
                description = "Diatom planktonik dengan bentuk yang khas dan pola ornamentasi yang unik. Spesies ini memiliki kemampuan adaptasi yang baik di berbagai kondisi perairan. Sering ditemukan dalam komunitas fitoplankton di perairan pesisir dan estuari.",
                classification = "Bleakeleya notata",
                imageResource = R.drawable.bleakeleya_notata_1,
                sampleImages = listOf(
                    R.drawable.bleakeleya_notata_1,
                    R.drawable.bleakeleya_notata_2,
                    R.drawable.bleakeleya_notata_3
                )
            ),

            "Chaetoceros affinis" to PlanktonInfo(
                name = "Chaetoceros affinis",
                description = "Diatom planktonik yang membentuk rantai dengan setae (duri) yang panjang dan kuat. Spesies ini sangat umum ditemukan di perairan laut dan merupakan salah satu komponen penting dalam rantai makanan laut. Memiliki kemampuan reproduksi yang cepat dalam kondisi yang sesuai.",
                classification = "Chaetoceros affinis",
                imageResource = R.drawable.chaetoceros_affinis_1,
                sampleImages = listOf(
                    R.drawable.chaetoceros_affinis_1,
                    R.drawable.diatom_1,
                    R.drawable.diatom_2
                )
            ),

            "Chaetoceros diversus" to PlanktonInfo(
                name = "Chaetoceros diversus",
                description = "Diatom planktonik dari genus Chaetoceros dengan morfologi setae yang beragam. Spesies ini menunjukkan variasi bentuk yang signifikan dan sering ditemukan dalam komunitas fitoplankton yang beragam. Berperan penting dalam produktivitas primer perairan laut.",
                classification = "Chaetoceros diversus",
                imageResource = R.drawable.chaetoceros_diversus_1,
                sampleImages = listOf(
                    R.drawable.chaetoceros_diversus_1,
                    R.drawable.chaetoceros_diversus_2,
                    R.drawable.chaetoceros_diversus_3
                )
            ),

            "Chaetoceros peruvianus" to PlanktonInfo(
                name = "Chaetoceros peruvianus",
                description = "Spesies Chaetoceros yang pertama kali ditemukan di perairan Peru. Memiliki karakteristik setae yang khas dan sering membentuk rantai panjang. Spesies ini merupakan indikator kondisi perairan yang produktif dan sering ditemukan dalam kondisi upwelling.",
                classification = "Chaetoceros peruvianus",
                imageResource = R.drawable.chaetoceros_peruvianus_1,
                sampleImages = listOf(
                    R.drawable.chaetoceros_peruvianus_1,
                    R.drawable.chaetoceros_peruvianus_2,
                    R.drawable.diatom_3
                )
            ),

            "Coscinodiscus oculus-iridis" to PlanktonInfo(
                name = "Coscinodiscus oculus-iridis",
                description = "Diatom planktonik dengan bentuk bundar dan pola areolae yang sangat indah menyerupai mata iris. Spesies ini memiliki ukuran yang relatif besar dan struktur yang kompleks. Sering digunakan sebagai objek studi morfologi diatom karena keindahan strukturnya.",
                classification = "Coscinodiscus oculus-iridis",
                imageResource = R.drawable.coscinodiscus_oculus_iridis_1,
                sampleImages = listOf(
                    R.drawable.coscinodiscus_oculus_iridis_1,
                    R.drawable.coscinodiscus_oculus_iridis_2,
                    R.drawable.coscinodiscus_oculus_iridis_3
                )
            ),

            "Diatom" to PlanktonInfo(
                name = "Diatom",
                description = "Diatom adalah kelompok alga mikroskopis yang memiliki dinding sel dari silika. Mereka merupakan salah satu kelompok fitoplankton yang paling penting di ekosistem perairan, berperan sebagai produser primer dan sumber makanan bagi berbagai organisme laut. Diatom memiliki bentuk yang sangat beragam dan indah.",
                classification = "Diatom",
                imageResource = R.drawable.diatom_1,
                sampleImages = listOf(
                    R.drawable.diatom_1,
                    R.drawable.diatom_2,
                    R.drawable.diatom_3
                )
            ),

            "Guinardia flaccida" to PlanktonInfo(
                name = "Guinardia flaccida",
                description = "Diatom planktonik dengan bentuk silindris yang panjang dan fleksibel. Spesies ini sering membentuk rantai dan memiliki kemampuan berenang yang baik. Ditemukan di berbagai kondisi perairan dan merupakan komponen penting dalam komunitas fitoplankton laut.",
                classification = "Guinardia flaccida",
                imageResource = R.drawable.guinardia_flaccida_1,
                sampleImages = listOf(
                    R.drawable.guinardia_flaccida_1,
                    R.drawable.guinardia_flaccida_2,
                    R.drawable.guinardia_flaccida_3
                )
            ),

            "Hemiaulus hauckii" to PlanktonInfo(
                name = "Hemiaulus hauckii",
                description = "Diatom planktonik dengan bentuk yang khas dan sering berasosiasi dengan organisme lain. Spesies ini memiliki adaptasi khusus untuk hidup di kolom air dan berperan dalam transfer energi dalam ekosistem laut. Menunjukkan variasi morfologi yang menarik.",
                classification = "Hemiaulus hauckii",
                imageResource = R.drawable.hemiaulus_hauckii_1,
                sampleImages = listOf(
                    R.drawable.hemiaulus_hauckii_1,
                    R.drawable.hemiaulus_hauckii_2,
                    R.drawable.hemiaulus_hauckii_3
                )
            ),

            "Hemiaulus membranaceus" to PlanktonInfo(
                name = "Hemiaulus membranaceus",
                description = "Spesies Hemiaulus dengan karakteristik membran yang tipis dan transparan. Diatom ini memiliki struktur yang unik dan sering ditemukan dalam kondisi perairan tertentu. Berperan dalam siklus nutrien dan produktivitas perairan laut.",
                classification = "Hemiaulus membranaceus",
                imageResource = R.drawable.hemiaulus_membranaceus_1,
                sampleImages = listOf(
                    R.drawable.hemiaulus_membranaceus_1,
                    R.drawable.hemiaulus_membranaceus_2,
                    R.drawable.hemiaulus_membranaceus_3
                )
            ),

            "Mastogloia sp" to PlanktonInfo(
                name = "Mastogloia sp",
                description = "Diatom bentik dengan karakteristik chambers atau ruang khusus di sepanjang tepi sel. Genus ini memiliki keragaman spesies yang tinggi dan sering ditemukan di berbagai habitat perairan. Struktur chambers mereka merupakan ciri khas yang membedakan dari diatom lainnya.",
                classification = "Mastogloia sp",
                imageResource = R.drawable.mastogloia_sp_1,
                sampleImages = listOf(
                    R.drawable.mastogloia_sp_1,
                    R.drawable.mastogloia_sp_2,
                    R.drawable.mastogloia_sp_3
                )
            ),

            "Nitzschia" to PlanktonInfo(
                name = "Nitzschia",
                description = "Genus diatom dengan bentuk memanjang dan raphe yang berada di tepi sel. Nitzschia memiliki keragaman spesies yang sangat tinggi dan ditemukan di berbagai habitat perairan. Beberapa spesies dapat menghasilkan toksin dan berperan penting dalam ekologi perairan.",
                classification = "Nitzschia",
                imageResource = R.drawable.nitzschia_1,
                sampleImages = listOf(
                    R.drawable.nitzschia_1,
                    R.drawable.nitzschia_2,
                    R.drawable.diatom_1
                )
            ),

            "Nitzschia longissima" to PlanktonInfo(
                name = "Nitzschia longissima",
                description = "Spesies Nitzschia dengan bentuk yang sangat panjang dan ramping. Diatom ini memiliki kemampuan bergerak yang baik dan sering ditemukan dalam komunitas fitoplankton. Spesies ini menunjukkan adaptasi morfologi yang unik untuk kehidupan planktonik.",
                classification = "Nitzschia longissima",
                imageResource = R.drawable.nitzschia_longissima_1,
                sampleImages = listOf(
                    R.drawable.nitzschia_longissima_1,
                    R.drawable.nitzschia_longissima_2,
                    R.drawable.nitzschia_longissima_3
                )
            ),

            "Plagiotropis lepidoptera" to PlanktonInfo(
                name = "Plagiotropis lepidoptera",
                description = "Diatom dengan bentuk yang menyerupai sayap kupu-kupu, sesuai dengan nama spesiesnya 'lepidoptera'. Memiliki struktur yang sangat indah dan kompleks. Spesies ini menunjukkan keragaman morfologi yang luar biasa dalam kelompok diatom.",
                classification = "Plagiotropis lepidoptera",
                imageResource = R.drawable.plagiotropis_lepidoptera_1,
                sampleImages = listOf(
                    R.drawable.plagiotropis_lepidoptera_1,
                    R.drawable.plagiotropis_lepidoptera_2,
                    R.drawable.plagiotropis_lepidoptera_3
                )
            ),

            "Pleurosigma" to PlanktonInfo(
                name = "Pleurosigma",
                description = "Diatom dengan bentuk sigmoid (melengkung seperti huruf S) yang sangat karakteristik. Genus ini memiliki struktur silika yang sangat halus dan sering digunakan sebagai objek test dalam mikroskopi. Ditemukan di berbagai habitat perairan dan memiliki kemampuan bergerak yang baik.",
                classification = "Pleurosigma",
                imageResource = R.drawable.pleurosigma_1,
                sampleImages = listOf(
                    R.drawable.pleurosigma_1,
                    R.drawable.pleurosigma_2,
                    R.drawable.pleurosigma_3
                )
            ),

            "Proboscia alata" to PlanktonInfo(
                name = "Proboscia alata",
                description = "Diatom planktonik dengan bentuk silindris panjang dan proses atau 'sayap' yang khas. Spesies ini memiliki kemampuan untuk membentuk rantai dan berperan penting dalam komunitas fitoplankton laut. Struktur 'alata' (bersayap) memberikan nama pada spesies ini.",
                classification = "Proboscia alata",
                imageResource = R.drawable.proboscia_alata_1,
                sampleImages = listOf(
                    R.drawable.proboscia_alata_1,
                    R.drawable.proboscia_alata_2,
                    R.drawable.proboscia_alata_3
                )
            ),

            "Proboscia indica" to PlanktonInfo(
                name = "Proboscia indica",
                description = "Spesies Proboscia yang pertama kali ditemukan di perairan India. Memiliki karakteristik morfologi yang unik dan sering ditemukan di perairan tropis. Berperan penting dalam produktivitas primer dan siklus karbon di ekosistem laut.",
                classification = "Proboscia indica",
                imageResource = R.drawable.proboscia_indica_1,
                sampleImages = listOf(
                    R.drawable.proboscia_indica_1,
                    R.drawable.proboscia_indica_2,
                    R.drawable.proboscia_indica_3
                )
            ),

            "Pseudo-nitzschia spp" to PlanktonInfo(
                name = "Pseudo-nitzschia spp",
                description = "Kompleks spesies diatom yang sangat penting dalam ekologi laut. Beberapa spesies dalam genus ini dapat menghasilkan asam domoic yang bersifat toksik. Memiliki bentuk yang sangat ramping dan sering membentuk rantai. Merupakan objek penelitian penting dalam studi HAB (Harmful Algal Blooms).",
                classification = "Pseudo-nitzschia spp",
                imageResource = R.drawable.pseudo_nitzschia_spp_1,
                sampleImages = listOf(
                    R.drawable.pseudo_nitzschia_spp_1,
                    R.drawable.pseudo_nitzschia_spp_2,
                    R.drawable.pseudo_nitzschia_spp_3
                )
            ),

            "Pseudosolenia calcar-avis" to PlanktonInfo(
                name = "Pseudosolenia calcar-avis",
                description = "Diatom planktonik dengan bentuk yang sangat khas menyerupai taji ayam (calcar-avis). Memiliki struktur yang unik dan berperan dalam komunitas fitoplankton laut. Spesies ini menunjukkan adaptasi morfologi yang luar biasa untuk kehidupan planktonik.",
                classification = "Pseudosolenia calcar-avis",
                imageResource = R.drawable.pseudosolenia_calcar_avis_1,
                sampleImages = listOf(
                    R.drawable.pseudosolenia_calcar_avis_1,
                    R.drawable.pseudosolenia_calcar_avis_2,
                    R.drawable.diatom_1
                )
            ),

            "Rhizosolenia calcar-avis" to PlanktonInfo(
                name = "Rhizosolenia calcar-avis",
                description = "Diatom planktonik dengan bentuk silindris panjang dan struktur terminal yang menyerupai taji. Spesies ini merupakan komponen penting dalam komunitas fitoplankton laut dan memiliki kemampuan untuk membentuk rantai panjang. Berperan dalam transfer energi dalam ekosistem laut.",
                classification = "Rhizosolenia calcar-avis",
                imageResource = R.drawable.rhizosolenia_calcar_avis_1,
                sampleImages = listOf(
                    R.drawable.rhizosolenia_calcar_avis_1,
                    R.drawable.rhizosolenia_calcar_avis_2,
                    R.drawable.rhizosolenia_calcar_avis_3
                )
            ),

            "Rhizosolenia cochlea" to PlanktonInfo(
                name = "Rhizosolenia cochlea",
                description = "Spesies Rhizosolenia dengan bentuk yang menyerupai spiral cochlea. Memiliki struktur yang sangat unik dan kompleks. Diatom ini menunjukkan keragaman morfologi yang luar biasa dalam genus Rhizosolenia dan berperan penting dalam ekosistem planktonik.",
                classification = "Rhizosolenia cochlea",
                imageResource = R.drawable.rhizosolenia_cochlea_1,
                sampleImages = listOf(
                    R.drawable.rhizosolenia_cochlea_1,
                    R.drawable.rhizosolenia_cochlea_2,
                    R.drawable.rhizosolenia_cochlea_3
                )
            ),

            "Rhizosolenia imbricata" to PlanktonInfo(
                name = "Rhizosolenia imbricata",
                description = "Diatom dengan struktur yang berlapis-lapis (imbricata) seperti genteng. Spesies ini memiliki adaptasi morfologi yang unik dan sering ditemukan dalam komunitas fitoplankton laut. Berperan dalam produktivitas primer dan siklus nutrien di perairan laut.",
                classification = "Rhizosolenia imbricata",
                imageResource = R.drawable.rhizosolenia_imbricata_1,
                sampleImages = listOf(
                    R.drawable.rhizosolenia_imbricata_1,
                    R.drawable.rhizosolenia_imbricata_2,
                    R.drawable.rhizosolenia_imbricata_3
                )
            ),

            "Tetramphora decussata" to PlanktonInfo(
                name = "Tetramphora decussata",
                description = "Diatom dengan struktur yang bersilangan (decussata) dan memiliki empat bagian utama. Spesies ini menunjukkan kompleksitas morfologi yang tinggi dan berperan dalam keragaman komunitas diatom. Struktur bersilangnya merupakan ciri khas yang mudah dikenali.",
                classification = "Tetramphora decussata",
                imageResource = R.drawable.tetramphora_decussata_1,
                sampleImages = listOf(
                    R.drawable.tetramphora_decussata_1,
                    R.drawable.tetramphora_decussata_2,
                    R.drawable.tetramphora_decussata_3
                )
            ),

            "Thalassionema nitzschioides" to PlanktonInfo(
                name = "Thalassionema nitzschioides",
                description = "Diatom planktonik dengan bentuk yang menyerupai Nitzschia namun dengan karakteristik yang berbeda. Spesies ini sering membentuk koloni dan berperan penting dalam komunitas fitoplankton laut. Memiliki kemampuan adaptasi yang baik terhadap berbagai kondisi perairan.",
                classification = "Thalassionema nitzschioides",
                imageResource = R.drawable.thalassionema_nitzschioides_1,
                sampleImages = listOf(
                    R.drawable.thalassionema_nitzschioides_1,
                    R.drawable.thalassionema_nitzschioides_2,
                    R.drawable.thalassionema_nitzschioides_3
                )
            ),

            "Toxarium undulatum" to PlanktonInfo(
                name = "Toxarium undulatum",
                description = "Diatom dengan bentuk bergelombang (undulatum) yang sangat karakteristik. Nama 'Toxarium' mengindikasikan potensi toksisitas, meskipun hal ini masih memerlukan penelitian lebih lanjut. Spesies ini menunjukkan morfologi yang unik dan menarik untuk studi taksonomi diatom.",
                classification = "Toxarium undulatum",
                imageResource = R.drawable.toxarium_undulatum_1,
                sampleImages = listOf(
                    R.drawable.toxarium_undulatum_1,
                    R.drawable.toxarium_undulatum_2,
                    R.drawable.toxarium_undulatum_3
                )
            )
        )

        /**
         * Menampilkan popup informasi plankton
         */
        fun showPlanktonInfoPopup(context: Context, planktonName: String) {
            val activity = context as? AppCompatActivity ?: return

            val planktonInfo = planktonDatabase[planktonName] ?: PlanktonInfo(
                name = planktonName,
                description = "Informasi detail untuk $planktonName sedang dalam pengembangan. $planktonName merupakan salah satu jenis plankton yang ditemukan dalam ekosistem perairan dan berperan penting dalam rantai makanan laut.",
                classification = "Diatom",
                imageResource = R.drawable.ic_microscope,
                sampleImages = listOf(
                    R.drawable.diatom_1,
                    R.drawable.diatom_2,
                    R.drawable.diatom_3
                )
            )

            val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_plankton_info, null)

            // Setup views
            val titleText = dialogView.findViewById<TextView>(R.id.planktonTitle)
            val classificationText = dialogView.findViewById<TextView>(R.id.classificationType)
            val descriptionText = dialogView.findViewById<TextView>(R.id.planktonDescription)
            val sampleImage1 = dialogView.findViewById<ImageView>(R.id.sampleImage1)
            val sampleImage2 = dialogView.findViewById<ImageView>(R.id.sampleImage2)
            val sampleImage3 = dialogView.findViewById<ImageView>(R.id.sampleImage3)
            val closeButton = dialogView.findViewById<ImageView>(R.id.closeButton)

            // Set content
            titleText.text = "Informasi Plankton"
            classificationText.text = planktonInfo.classification
            descriptionText.text = planktonInfo.description

            // Set sample images
            val sampleImages = listOf(sampleImage1, sampleImage2, sampleImage3)
            planktonInfo.sampleImages.forEachIndexed { index, imageRes ->
                if (index < sampleImages.size) {
                    sampleImages[index].setImageResource(imageRes)
                    sampleImages[index].setOnClickListener {
                        showFullSizeImage(context, imageRes, planktonInfo.name)
                    }
                }
            }

            // Create dialog
            val dialog = AlertDialog.Builder(context)
                .setView(dialogView)
                .create()

            // Set transparent background so our rounded background shows
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

            // Close button
            closeButton.setOnClickListener {
                dialog.dismiss()
            }

            dialog.show()
        }

        /**
         * Menampilkan gambar dalam ukuran penuh
         */
        private fun showFullSizeImage(context: Context, imageRes: Int, title: String) {
            val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_image_popup, null)

            val fullSizeImage = dialogView.findViewById<ImageView>(R.id.fullSizeImage)
            val imageTitle = dialogView.findViewById<TextView>(R.id.imageTitle)
            val closeButton = dialogView.findViewById<ImageView>(R.id.closeButton)

            fullSizeImage.setImageResource(imageRes)
            imageTitle.text = title

            val dialog = AlertDialog.Builder(context)
                .setView(dialogView)
                .create()

            closeButton.setOnClickListener {
                dialog.dismiss()
            }

            // Close on background click
            dialogView.setOnClickListener {
                dialog.dismiss()
            }

            dialog.show()
        }
    }
}
