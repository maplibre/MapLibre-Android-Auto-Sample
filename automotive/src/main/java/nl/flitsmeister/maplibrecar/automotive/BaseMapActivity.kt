package nl.flitsmeister.maplibrecar.automotive

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.location.LocationComponent
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.LocationComponentOptions
import org.maplibre.android.location.modes.CameraMode
import org.maplibre.android.location.modes.RenderMode
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style

/**
 * A plain [Activity] that renders a [MapView] directly, outside of the car
 * app templates. This reaches displays the templates cannot, such as the
 * instrument cluster.
 *
 * The location puck is fed from the raw [LocationManager] instead of the
 * default location engine, so it also works on automotive emulators that
 * do not ship one.
 */
abstract class BaseMapActivity : Activity(), LocationListener {

    protected var mapLibreMap: MapLibreMap? = null
        private set

    private lateinit var mapView: MapView
    private var locationManager: LocationManager? = null
    private var lastLocation: Location? = null
    private var lastUpdateTime: Long = 0
    private var locationComponentActivated = false

    /** The style to load once the map is ready. */
    protected abstract val styleUri: String

    /** The camera position applied before the first location update. */
    protected open val initialZoom: Double = 15.0
    protected open val initialTilt: Double = 0.0

    /** How the camera follows the location puck (a [CameraMode] constant). */
    @CameraMode.Mode
    protected open val cameraMode: Int = CameraMode.TRACKING

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MapLibre.getInstance(this)
        mapView = MapView(this)
        setContentView(onCreateContentView(mapView))
        mapView.onCreate(savedInstanceState)

        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_LOCATION_PERMISSION
            )
        }

        mapView.getMapAsync { map ->
            mapLibreMap = map
            map.setMinZoomPreference(MIN_ZOOM)
            map.setMaxZoomPreference(MAX_ZOOM)
            map.moveCamera(
                CameraUpdateFactory.newCameraPosition(
                    CameraPosition.Builder()
                        .zoom(initialZoom)
                        .tilt(initialTilt)
                        .build()
                )
            )
            map.setStyle(Style.Builder().fromUri(styleUri)) { style ->
                enableLocationComponent(map, style)
            }
        }
    }

    /** Override to layer views, such as zoom controls, on top of the map. */
    protected open fun onCreateContentView(mapView: MapView): View = mapView

    override fun onLocationChanged(location: Location) {
        if (!shouldForwardLocation(location)) return
        lastLocation = location
        lastUpdateTime = SystemClock.elapsedRealtime()
        mapLibreMap?.locationComponent?.forceLocationUpdate(location)
    }

    private fun shouldForwardLocation(location: Location): Boolean {
        val last = lastLocation ?: return true
        val now = SystemClock.elapsedRealtime()
        val changed = location.latitude != last.latitude ||
            location.longitude != last.longitude ||
            location.bearing != last.bearing
        return changed && now - lastUpdateTime >= LOCATION_UPDATE_THROTTLE_MS
    }

    /** Override to customize the puck. */
    protected open fun onCreateLocationComponentOptions(): LocationComponentOptions =
        LocationComponentOptions.builder(this).build()

    /** Override to react once the location component is enabled. */
    protected open fun onLocationComponentEnabled(locationComponent: LocationComponent) {}

    @Suppress("MissingPermission")
    private fun enableLocationComponent(map: MapLibreMap, style: Style) {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        // Note: LocationComponent#isLocationComponentEnabled throws until the
        // component has been activated, so guard on our own flag instead.
        if (locationComponentActivated) {
            return
        }
        val locationComponent = map.locationComponent
        locationComponent.activateLocationComponent(
            LocationComponentActivationOptions.builder(this, style)
                .useDefaultLocationEngine(false)
                .locationComponentOptions(onCreateLocationComponentOptions())
                .build()
        )
        locationComponent.isLocationComponentEnabled = true
        locationComponentActivated = true
        locationComponent.cameraMode = cameraMode
        locationComponent.renderMode = RenderMode.GPS
        onLocationComponentEnabled(locationComponent)

        locationManager = getSystemService(LocationManager::class.java).also {
            it.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                LOCATION_UPDATE_INTERVAL_MS,
                0f,
                this
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_LOCATION_PERMISSION && grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            val map = mapLibreMap ?: return
            map.style?.let { enableLocationComponent(map, it) }
        }
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        mapView.onPause()
        super.onPause()
    }

    override fun onStop() {
        mapView.onStop()
        super.onStop()
    }

    override fun onDestroy() {
        locationManager?.removeUpdates(this)
        mapView.onDestroy()
        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }

    companion object {
        private const val REQUEST_LOCATION_PERMISSION = 1
        private const val MIN_ZOOM = 2.0
        private const val MAX_ZOOM = 18.0
        private const val LOCATION_UPDATE_INTERVAL_MS = 100L
        private const val LOCATION_UPDATE_THROTTLE_MS = 300L
    }
}
