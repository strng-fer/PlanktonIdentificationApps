# PlanktonDetectionApps

![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/kotlin-%237F52FF.svg?style=for-the-badge&logo=kotlin&logoColor=white)
![TensorFlow](https://img.shields.io/badge/TensorFlow-%23FF6F00.svg?style=for-the-badge&logo=TensorFlow&logoColor=white)

Aplikasi Android untuk deteksi dan identifikasi plankton menggunakan Machine Learning dengan TensorFlow Lite. Aplikasi ini memungkinkan pengguna untuk mengambil foto plankton menggunakan kamera atau memilih dari galeri, kemudian menganalisisnya menggunakan model AI yang telah dilatih.

## 🌟 Fitur Utama

- **📸 Capture & Analysis**: Ambil foto plankton langsung dari kamera atau pilih dari galeri
- **🤖 AI Detection**: Deteksi plankton menggunakan multiple model AI:
  - MobileNet V3 Small
  - ResNet50 V2
  - EfficientNet V2 B0
- **📊 Confidence Score**: Menampilkan tingkat kepercayaan hasil deteksi
- **🎯 Real-time Processing**: Analisis gambar secara real-time
- **⚙️ Settings**: Pengaturan untuk memilih model AI dan konfigurasi lainnya
- **📖 Documentation**: Panduan lengkap penggunaan aplikasi
- **ℹ️ About**: Informasi tentang aplikasi dan developer

## 🔧 Teknologi yang Digunakan

- **Language**: Kotlin
- **Framework**: Android SDK
- **ML Framework**: TensorFlow Lite
- **Min SDK**: 24 (Android 7.0)
- **Target SDK**: 36
- **Architecture**: MVVM Pattern

## 📱 Screenshot

*Screenshots akan ditambahkan di sini*

## 🚀 Instalasi

### Prerequisites
- Android Studio Arctic Fox atau lebih baru
- Android SDK 24 atau lebih tinggi
- Device atau emulator dengan kamera (optional)

### Langkah Instalasi

1. **Clone repository**
   ```bash
   git clone https://github.com/strng-fer/PlanktonIdentificationApps.git
   cd PlanktonIdentificationApps
   ```

2. **Buka project di Android Studio**
   - File → Open → Pilih folder project

3. **Sync Gradle**
   - Android Studio akan otomatis melakukan sync
   - Tunggu hingga proses selesai

4. **Build dan Run**
   - Click tombol "Run" atau gunakan shortcut `Shift + F10`
   - Pilih device atau emulator yang akan digunakan

## 🎯 Cara Penggunaan

### 1. Mengambil Foto Plankton
- Tap tombol **"Camera"** untuk mengambil foto baru
- Arahkan kamera ke objek plankton
- Tap untuk capture foto

### 2. Memilih dari Galeri
- Tap tombol **"Gallery"** untuk memilih foto dari galeri
- Pilih foto plankton yang ingin dianalisis

### 3. Analisis Hasil
- Setelah foto dipilih, aplikasi akan otomatis memproses
- Hasil deteksi akan ditampilkan dengan confidence score
- Hasil berupa nama spesies plankton dan persentase kepercayaan

### 4. Pengaturan Model
- Buka **Settings** dari menu utama
- Pilih model AI yang diinginkan:
  - **MobileNet V3 Small**: Ringan, cocok untuk device dengan spesifikasi rendah
  - **ResNet50 V2**: Balanced antara akurasi dan performa
  - **EfficientNet V2 B0**: Akurasi tinggi, membutuhkan resource lebih

## 📋 Permissions

Aplikasi memerlukan permissions berikut:
- `CAMERA`: Untuk mengambil foto
- `READ_EXTERNAL_STORAGE`: Untuk mengakses galeri
- `WRITE_EXTERNAL_STORAGE`: Untuk menyimpan foto (Android 7-9)
- `INTERNET`: Untuk fitur tambahan
- `VIBRATE`: Untuk feedback haptic

## 🏗️ Struktur Project

```
app/
├── src/main/
│   ├── java/com/example/planktondetectionapps/
│   │   ├── MainActivity.kt          # Main activity dengan camera & gallery
│   │   ├── SettingsActivity.kt      # Pengaturan aplikasi
│   │   ├── AboutActivity.kt         # Informasi aplikasi
│   │   └── DocumentationActivity.kt # Panduan penggunaan
│   ├── res/
│   │   ├── layout/                  # Layout files
│   │   ├── drawable/                # Icons dan images
│   │   ├── values/                  # Strings, colors, themes
│   │   └── xml/                     # File provider paths
│   └── assets/                      # ML models
└── build.gradle.kts                 # Dependencies dan konfigurasi
```

## 🤖 Model AI

Aplikasi menggunakan tiga model TensorFlow Lite yang telah dioptimasi:

1. **MobileNet V3 Small**
   - Size: ~2MB
   - Kecepatan: Sangat cepat
   - Akurasi: Baik
   - Cocok untuk: Device dengan RAM terbatas

2. **ResNet50 V2**
   - Size: ~98MB
   - Kecepatan: Sedang
   - Akurasi: Sangat baik
   - Cocok untuk: Penggunaan umum

3. **EfficientNet V2 B0**
   - Size: ~29MB
   - Kecepatan: Cepat
   - Akurasi: Excellent
   - Cocok untuk: Hasil terbaik

4. **Akan ditambahkan segera...**
   - Size: ~XXMB

## 📊 Performance

- **Inference Time**: 50-200ms (tergantung model dan device)
- **Memory Usage**: 50-150MB (tergantung model)
- **Supported Image Formats**: JPEG, PNG
- **Input Size**: 224x224 pixels (auto-resized)

## 🛠️ Dependencies

```kotlin
dependencies {
    implementation "androidx.core:core-ktx:1.12.0"
    implementation "androidx.appcompat:appcompat:1.6.1"
    implementation "com.google.android.material:material:1.11.0"
    implementation "androidx.activity:activity-ktx:1.8.2"
    implementation "androidx.constraintlayout:constraintlayout:2.1.4"
    
    // TensorFlow Lite
    implementation "org.tensorflow:tensorflow-lite:2.13.0"
    implementation "org.tensorflow:tensorflow-lite-support:0.4.4"
    implementation "org.tensorflow:tensorflow-lite-metadata:0.4.4"
}
```

## 🐛 Troubleshooting

### Camera tidak berfungsi
- Pastikan permission CAMERA sudah diberikan
- Restart aplikasi setelah memberikan permission
- Periksa apakah device memiliki kamera

### Hasil deteksi tidak akurat
- Pastikan foto plankton jelas dan tidak blur
- Gunakan pencahayaan yang cukup
- Coba gunakan model yang berbeda di Settings

### Aplikasi crash saat loading model
- Periksa apakah device memiliki RAM yang cukup
- Coba gunakan model yang lebih ringan (MobileNet V3 Small)
- Restart aplikasi

## 📞 Contact

### 👨‍💻 Development Team
- **Data Scientist Intern at BRIN**
  - **Feryadi Yulius** - [GitHub](https://github.com/strng-fer) | Email: feryadi.122450087@student.itera.ac.id
  - **Raid Muhammad Naufal** - [GitHub](https://github.com/rayths) | Email: raid122450027@student.itera.ac.id

### 👨‍🏫 BRIN Supervisor
- **Dr. Esa Prakasa, M.T**

### 📍 Repository
- **GitHub**: [PlanktonIdentificationApps](https://github.com/strng-fer/PlanktonIdentificationApps)

## 📄 License

```
Copyright (c) 2025 PlanktonDetectionApps

Licensed under the MIT License.
See LICENSE file for details.
```

## 🙏 Acknowledgments

- TensorFlow team untuk framework ML
- Android team untuk development tools
- Open source community untuk library yang digunakan
- Marine biology experts untuk dataset plankton

## 🚧 Roadmap

- [ ] Implementasi cloud sync untuk hasil deteksi
- [ ] Implementasi batch processing
- [ ] Implementasi dark mode
- [ ] Tambah multi-language support

---

**Made with ❤️ for marine biology research**
