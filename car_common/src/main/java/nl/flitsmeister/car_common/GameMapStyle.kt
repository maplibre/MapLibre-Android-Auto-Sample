package nl.flitsmeister.car_common

import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.Style

/** One bundled style for the phone, Android Auto and Automotive. Tiles remain online. */
object GameMapStyle {
    const val URI = "asset://styles/gta-inspired.json"

    fun builder(): Style.Builder = Style.Builder().fromUri(URI)

    // Overview only; the first GPS fix moves the map to the user's actual position.
    fun initialCamera(): CameraPosition = CameraPosition.Builder()
        .target(LatLng(42.5, 12.5))
        .zoom(5.0)
        .build()
}
