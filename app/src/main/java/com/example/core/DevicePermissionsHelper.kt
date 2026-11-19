package com.example.core

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.net.Uri
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.TimeZone

enum class AppPermissionKind {
    MICROPHONE,
    CAMERA,
    LOCATION
}

data class DevicePermissionsSnapshot(
    val microphoneGranted: Boolean = false,
    val cameraGranted: Boolean = false,
    val locationGranted: Boolean = false
)

data class LocationRegionContext(
    val label: String,
    val countryCode: String? = null,
    val timeZoneId: String = TimeZone.getDefault().id,
    val isFromGoogleLocation: Boolean = false,
    val updatedAtMillis: Long = System.currentTimeMillis()
)

object DevicePermissionsHelper {

    val locationPermissions: Array<String> = arrayOf(
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    fun isMicrophoneGranted(context: Context): Boolean =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

    fun isCameraGranted(context: Context): Boolean =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

    fun isLocationGranted(context: Context): Boolean =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

    fun inspectPermissions(context: Context): DevicePermissionsSnapshot = DevicePermissionsSnapshot(
        microphoneGranted = isMicrophoneGranted(context),
        cameraGranted = isCameraGranted(context),
        locationGranted = isLocationGranted(context)
    )

    fun createAppSettingsIntent(context: Context): Intent =
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", context.packageName, null)
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

    fun fallbackTimezoneRegionContext(): LocationRegionContext {
        val tz = TimeZone.getDefault()
        val cityHint = tz.id.substringAfter('/', tz.id).replace('_', ' ')
        val country = Locale.getDefault().displayCountry.takeIf { it.isNotBlank() }
        val label = listOfNotNull(cityHint.takeIf { it.isNotBlank() }, country)
            .distinct()
            .joinToString(", ")
            .ifBlank { tz.id }
        return LocationRegionContext(
            label = label,
            countryCode = Locale.getDefault().country.takeIf { it.isNotBlank() },
            timeZoneId = tz.id,
            isFromGoogleLocation = false
        )
    }

    /**
     * Queries Google Play Services FusedLocationProviderClient when foreground location
     * permission is granted. Gracefully falls back to device timezone/locale context
     * whenever permission is denied, offline, or location services are unavailable.
     */
    @SuppressLint("MissingPermission")
    fun fetchGoogleLocationContext(
        context: Context,
        onResult: (LocationRegionContext) -> Unit
    ) {
        if (!isLocationGranted(context)) {
            onResult(fallbackTimezoneRegionContext())
            return
        }
        val appContext = context.applicationContext
        runCatching {
            val client = LocationServices.getFusedLocationProviderClient(appContext)
            client.lastLocation
                .addOnSuccessListener { lastLoc ->
                    if (lastLoc != null) {
                        resolveCoordinatesLabel(appContext, lastLoc.latitude, lastLoc.longitude, onResult)
                    } else {
                        val cts = CancellationTokenSource()
                        client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.token)
                            .addOnSuccessListener { freshLoc ->
                                if (freshLoc != null) {
                                    resolveCoordinatesLabel(appContext, freshLoc.latitude, freshLoc.longitude, onResult)
                                } else {
                                    onResult(fallbackTimezoneRegionContext())
                                }
                            }
                            .addOnFailureListener {
                                onResult(fallbackTimezoneRegionContext())
                            }
                    }
                }
                .addOnFailureListener {
                    onResult(fallbackTimezoneRegionContext())
                }
        }.onFailure {
            onResult(fallbackTimezoneRegionContext())
        }
    }

    @Suppress("DEPRECATION")
    private fun resolveCoordinatesLabel(
        context: Context,
        lat: Double,
        lon: Double,
        onResult: (LocationRegionContext) -> Unit
    ) {
        val tzId = TimeZone.getDefault().id
        val fallbackLabel = String.format(Locale.US, "%.2f°, %.2f° • %s", lat, lon, tzId)
        runCatching {
            if (!Geocoder.isPresent()) {
                onResult(
                    LocationRegionContext(
                        label = fallbackLabel,
                        timeZoneId = tzId,
                        isFromGoogleLocation = true
                    )
                )
                return
            }
            val geocoder = Geocoder(context, Locale.getDefault())
            val addresses = geocoder.getFromLocation(lat, lon, 1)
            val addr = addresses?.firstOrNull()
            val place = listOfNotNull(
                addr?.locality ?: addr?.subAdminArea ?: addr?.adminArea,
                addr?.countryName
            ).distinct().joinToString(", ")
            onResult(
                LocationRegionContext(
                    label = place.ifBlank { fallbackLabel },
                    countryCode = addr?.countryCode,
                    timeZoneId = tzId,
                    isFromGoogleLocation = true
                )
            )
        }.onFailure {
            onResult(
                LocationRegionContext(
                    label = fallbackLabel,
                    timeZoneId = tzId,
                    isFromGoogleLocation = true
                )
            )
        }
    }
}
