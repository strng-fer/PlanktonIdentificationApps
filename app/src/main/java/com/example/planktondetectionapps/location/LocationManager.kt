package com.example.planktondetectionapps.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager as AndroidLocationManager
import android.os.Bundle
import android.util.Log
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

/**
 * Location Manager untuk mengelola GPS dan informasi lokasi
 * Mendukung clustering lokasi ke tingkat kota, kabupaten, kecamatan, atau kelurahan
 */
class LocationManager(private val context: Context) {

    private val androidLocationManager = context.getSystemService(Context.LOCATION_SERVICE) as AndroidLocationManager
    private val geocoder = Geocoder(context, Locale("id", "ID")) // Menggunakan locale Indonesia
    private var currentLocation: Location? = null
    private var locationListener: LocationListener? = null

    companion object {
        private const val TAG = "LocationManager"
        private const val MIN_TIME_BETWEEN_UPDATES = 60000L // 1 menit
        private const val MIN_DISTANCE_CHANGE_FOR_UPDATES = 100f // 100 meter
    }

    /**
     * Interface untuk callback lokasi
     */
    interface LocationCallback {
        fun onLocationObtained(locationInfo: LocationInfo)
        fun onLocationError(error: String)
        fun onPermissionRequired()
    }

    /**
     * Data class untuk informasi lokasi yang sudah di-cluster
     */
    data class LocationInfo(
        val latitude: Double,
        val longitude: Double,
        val accuracy: Float,
        val address: String,
        val city: String,
        val district: String,
        val subDistrict: String,
        val village: String,
        val clusteredLocation: String, // Lokasi yang sudah di-cluster
        val locationLevel: LocationLevel,
        val timestamp: Long = System.currentTimeMillis()
    )

    /**
     * Enum untuk level clustering lokasi
     */
    enum class LocationLevel {
        VILLAGE,    // Kelurahan/Desa
        SUB_DISTRICT, // Kecamatan
        DISTRICT,   // Kabupaten/Kota
        CITY,       // Kota/Provinsi
        UNKNOWN     // Tidak diketahui
    }

    /**
     * Cek apakah permission lokasi sudah diberikan
     */
    fun hasLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Dapatkan lokasi saat ini dengan callback
     */
    fun getCurrentLocation(callback: LocationCallback) {
        Log.d(TAG, "getCurrentLocation() called")

        if (!hasLocationPermission()) {
            Log.w(TAG, "Location permission not granted")
            callback.onPermissionRequired()
            return
        }

        if (!isLocationEnabled()) {
            Log.w(TAG, "Location services not enabled")
            callback.onLocationError("GPS tidak aktif. Silakan aktifkan GPS di pengaturan.")
            return
        }

        try {
            // Coba dapatkan last known location terlebih dahulu
            val lastKnownLocation = getLastKnownLocation()
            if (lastKnownLocation != null && isLocationRecent(lastKnownLocation)) {
                Log.d(TAG, "Using recent last known location")
                processLocation(lastKnownLocation, callback)
                return
            }

            // Request location update
            Log.d(TAG, "Requesting fresh location update")
            requestLocationUpdate(callback)

        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception when accessing location", e)
            callback.onLocationError("Error mengakses lokasi: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error when getting location", e)
            callback.onLocationError("Error mendapatkan lokasi: ${e.message}")
        }
    }

    /**
     * Request location update dari GPS
     */
    private fun requestLocationUpdate(callback: LocationCallback) {
        try {
            locationListener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    Log.d(TAG, "New location received: ${location.latitude}, ${location.longitude}")
                    currentLocation = location
                    stopLocationUpdates()
                    processLocation(location, callback)
                }

                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {
                    Log.w(TAG, "Location provider disabled: $provider")
                    callback.onLocationError("GPS provider dinonaktifkan")
                }
            }

            // Request updates dari GPS provider (lebih akurat)
            if (androidLocationManager.isProviderEnabled(AndroidLocationManager.GPS_PROVIDER)) {
                Log.d(TAG, "Requesting GPS location updates")
                androidLocationManager.requestLocationUpdates(
                    AndroidLocationManager.GPS_PROVIDER,
                    MIN_TIME_BETWEEN_UPDATES,
                    MIN_DISTANCE_CHANGE_FOR_UPDATES,
                    locationListener!!
                )
            }

            // Fallback ke network provider jika GPS tidak tersedia
            if (androidLocationManager.isProviderEnabled(AndroidLocationManager.NETWORK_PROVIDER)) {
                Log.d(TAG, "Requesting network location updates as fallback")
                androidLocationManager.requestLocationUpdates(
                    AndroidLocationManager.NETWORK_PROVIDER,
                    MIN_TIME_BETWEEN_UPDATES,
                    MIN_DISTANCE_CHANGE_FOR_UPDATES,
                    locationListener!!
                )
            }

        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception in requestLocationUpdate", e)
            callback.onLocationError("Permission error: ${e.message}")
        }
    }

    /**
     * Dapatkan last known location
     */
    private fun getLastKnownLocation(): Location? {
        try {
            val gpsLocation = androidLocationManager.getLastKnownLocation(AndroidLocationManager.GPS_PROVIDER)
            val networkLocation = androidLocationManager.getLastKnownLocation(AndroidLocationManager.NETWORK_PROVIDER)

            return when {
                gpsLocation != null && networkLocation != null -> {
                    if (gpsLocation.time > networkLocation.time) gpsLocation else networkLocation
                }
                gpsLocation != null -> gpsLocation
                networkLocation != null -> networkLocation
                else -> null
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception getting last known location", e)
            return null
        }
    }

    /**
     * Cek apakah lokasi masih recent (dalam 5 menit terakhir)
     */
    private fun isLocationRecent(location: Location): Boolean {
        val fiveMinutesAgo = System.currentTimeMillis() - (5 * 60 * 1000)
        return location.time > fiveMinutesAgo
    }

    /**
     * Cek apakah location services aktif
     */
    private fun isLocationEnabled(): Boolean {
        return androidLocationManager.isProviderEnabled(AndroidLocationManager.GPS_PROVIDER) ||
                androidLocationManager.isProviderEnabled(AndroidLocationManager.NETWORK_PROVIDER)
    }

    /**
     * Proses lokasi dan dapatkan informasi alamat
     */
    private fun processLocation(location: Location, callback: LocationCallback) {
        Log.d(TAG, "Processing location: ${location.latitude}, ${location.longitude}")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val locationInfo = getLocationInfo(location)
                withContext(Dispatchers.Main) {
                    callback.onLocationObtained(locationInfo)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing location", e)
                withContext(Dispatchers.Main) {
                    callback.onLocationError("Error memproses lokasi: ${e.message}")
                }
            }
        }
    }

    /**
     * Dapatkan informasi lokasi lengkap dengan clustering
     */
    private suspend fun getLocationInfo(location: Location): LocationInfo {
        return withContext(Dispatchers.IO) {
            try {
                val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)

                if (addresses?.isNotEmpty() == true) {
                    val address = addresses[0]
                    val locationInfo = parseAddress(address, location)
                    Log.d(TAG, "Location info obtained: ${locationInfo.clusteredLocation}")
                    locationInfo
                } else {
                    Log.w(TAG, "No address found for coordinates")
                    createFallbackLocationInfo(location)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error getting address from coordinates", e)
                createFallbackLocationInfo(location)
            }
        }
    }

    /**
     * Parse alamat dari Geocoder dan tentukan clustering
     */
    private fun parseAddress(address: Address, location: Location): LocationInfo {
        val subLocality = address.subLocality ?: "" // Kelurahan/Desa
        val locality = address.locality ?: "" // Kecamatan
        val subAdminArea = address.subAdminArea ?: "" // Kabupaten
        val adminArea = address.adminArea ?: "" // Provinsi
        val fullAddress = address.getAddressLine(0) ?: ""

        // Tentukan level clustering dan lokasi yang akan ditampilkan
        val (clusteredLocation, locationLevel) = determineClusteredLocation(
            village = subLocality,
            subDistrict = locality,
            district = subAdminArea,
            city = adminArea
        )

        return LocationInfo(
            latitude = location.latitude,
            longitude = location.longitude,
            accuracy = location.accuracy,
            address = fullAddress,
            city = adminArea,
            district = subAdminArea,
            subDistrict = locality,
            village = subLocality,
            clusteredLocation = clusteredLocation,
            locationLevel = locationLevel
        )
    }

    /**
     * Tentukan clustering lokasi berdasarkan prioritas
     */
    private fun determineClusteredLocation(
        village: String,
        subDistrict: String,
        district: String,
        city: String
    ): Pair<String, LocationLevel> {

        return when {
            // Prioritas 1: Kelurahan/Desa
            village.isNotEmpty() -> {
                val location = if (subDistrict.isNotEmpty()) {
                    "Kelurahan $village, Kecamatan $subDistrict"
                } else {
                    "Kelurahan $village"
                }
                Pair(location, LocationLevel.VILLAGE)
            }

            // Prioritas 2: Kecamatan
            subDistrict.isNotEmpty() -> {
                val location = if (district.isNotEmpty()) {
                    "Kecamatan $subDistrict, $district"
                } else {
                    "Kecamatan $subDistrict"
                }
                Pair(location, LocationLevel.SUB_DISTRICT)
            }

            // Prioritas 3: Kabupaten/Kota
            district.isNotEmpty() -> {
                val location = if (city.isNotEmpty()) {
                    "$district, $city"
                } else {
                    district
                }
                Pair(location, LocationLevel.DISTRICT)
            }

            // Prioritas 4: Provinsi/Kota
            city.isNotEmpty() -> {
                Pair(city, LocationLevel.CITY)
            }

            // Fallback: Koordinat
            else -> {
                Pair("Lokasi Tidak Dikenal", LocationLevel.UNKNOWN)
            }
        }
    }

    /**
     * Buat fallback location info jika geocoding gagal
     */
    private fun createFallbackLocationInfo(location: Location): LocationInfo {
        return LocationInfo(
            latitude = location.latitude,
            longitude = location.longitude,
            accuracy = location.accuracy,
            address = "Koordinat: ${String.format("%.6f", location.latitude)}, ${String.format("%.6f", location.longitude)}",
            city = "",
            district = "",
            subDistrict = "",
            village = "",
            clusteredLocation = "Lokasi GPS (${String.format("%.4f", location.latitude)}, ${String.format("%.4f", location.longitude)})",
            locationLevel = LocationLevel.UNKNOWN
        )
    }

    /**
     * Hentikan location updates
     */
    fun stopLocationUpdates() {
        try {
            locationListener?.let { listener ->
                androidLocationManager.removeUpdates(listener)
                locationListener = null
                Log.d(TAG, "Location updates stopped")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping location updates", e)
        }
    }

    /**
     * Cleanup resources
     */
    fun cleanup() {
        stopLocationUpdates()
        currentLocation = null
    }
}
