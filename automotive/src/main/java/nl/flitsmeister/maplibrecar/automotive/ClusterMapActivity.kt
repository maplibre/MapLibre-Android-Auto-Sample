package nl.flitsmeister.maplibrecar.automotive

import android.graphics.Color
import org.maplibre.android.location.LocationComponent
import org.maplibre.android.location.LocationComponentOptions
import org.maplibre.android.location.modes.CameraMode

/**
 * A lean map for the instrument cluster: no controls, a tilted camera
 * tracking the vehicle with a navigation arrow as the location puck.
 */
class ClusterMapActivity : BaseMapActivity() {

    override val styleUri = "https://tiles.openfreemap.org/styles/dark"

    override val initialZoom = 17.0
    override val initialTilt = 60.0

    override val cameraMode = CameraMode.TRACKING_GPS

    override fun onCreateLocationComponentOptions(): LocationComponentOptions =
        LocationComponentOptions.builder(this)
            .gpsDrawable(R.drawable.nav_arrow)
            .backgroundTintColor(Color.TRANSPARENT)
            .accuracyAlpha(0f)
            .build()

    override fun onLocationComponentEnabled(locationComponent: LocationComponent) {
        locationComponent.tiltWhileTracking(initialTilt)
    }
}
