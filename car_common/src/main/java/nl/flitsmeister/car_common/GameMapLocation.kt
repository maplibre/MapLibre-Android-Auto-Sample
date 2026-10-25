package nl.flitsmeister.car_common

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.util.Log
import androidx.core.content.ContextCompat
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.LocationComponentOptions
import org.maplibre.android.location.OnLocationCameraTransitionListener
import org.maplibre.android.location.engine.LocationEngineRequest
import org.maplibre.android.location.modes.CameraMode
import org.maplibre.android.location.modes.RenderMode
import org.maplibre.android.maps.MapLibreMap

object GameMapLocation {
    fun hasPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    /** Call after style load and after permission changes. MapView owns engine start/stop. */
    @SuppressLint("MissingPermission")
    fun enable(context: Context, map: MapLibreMap, recenter: Boolean = false): Boolean {
        val component = map.locationComponent
        if (!hasPermission(context)) {
            if (component.isLocationComponentActivated) {
                component.isLocationComponentEnabled = false
            }
            return false
        }
        val style = map.style?.takeIf { it.isFullyLoaded } ?: return false
        try {
            if (!component.isLocationComponentActivated) {
                component.activateLocationComponent(
                    LocationComponentActivationOptions.builder(context, style)
                        .locationComponentOptions(
                            LocationComponentOptions.builder(context)
                                .gpsDrawable(R.drawable.ic_player_arrow)
                                .foregroundTintColor(Color.WHITE)
                                .accuracyColor(Color.rgb(136, 207, 214))
                                .accuracyAlpha(0.2f)
                                .build()
                        )
                        .locationEngineRequest(
                            LocationEngineRequest.Builder(1_000L)
                                .setFastestInterval(1_000L)
                                .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
                                .build()
                        )
                        .useDefaultLocationEngine(true)
                        .build()
                )
            }
            val wasEnabled = component.isLocationComponentEnabled
            component.isLocationComponentEnabled = true
            component.renderMode = RenderMode.GPS
            if (!wasEnabled || recenter) {
                // GPS course avoids compass jitter in a car. Stay top-down like a minimap.
                component.setCameraMode(
                    CameraMode.TRACKING_GPS, 750L, 16.0, null, 0.0,
                    object : OnLocationCameraTransitionListener {
                        override fun onLocationCameraTransitionFinished(cameraMode: Int) {
                            // The SDK skips transition zoom without a fix, or if already tracking.
                            component.zoomWhileTracking(16.0, 0L)
                            component.tiltWhileTracking(0.0, 0L)
                        }

                        override fun onLocationCameraTransitionCanceled(cameraMode: Int) = Unit
                    }
                )
            }
            return true
        } catch (error: SecurityException) {
            // Permission can be revoked between the check and the engine request.
            Log.w("GameMapLocation", "Location permission is no longer available", error)
            return false
        }
    }
}
