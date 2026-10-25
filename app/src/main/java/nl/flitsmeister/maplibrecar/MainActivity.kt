package nl.flitsmeister.maplibrecar

import android.Manifest
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import nl.flitsmeister.car_common.GameMapLocation
import nl.flitsmeister.car_common.GameMapStyle
import nl.flitsmeister.car_common.R
import nl.flitsmeister.maplibrecar.ui.theme.MapLibreCarTheme
import org.maplibre.android.MapLibre
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapLibreMapOptions
import org.maplibre.android.maps.MapView

class MainActivity : ComponentActivity() {
    private lateinit var mapView: MapView
    private var mapLibreMap: MapLibreMap? = null

    private val requestLocation = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        if (GameMapLocation.hasPermission(this)) {
            enableLocation(recenter = true)
        } else {
            Toast.makeText(this, R.string.location_permission_needed, Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        MapLibre.getInstance(this)
        // Create before onStart: constructing inside AndroidView can miss lifecycle callbacks.
        mapView = MapView(this, MapLibreMapOptions.createFromAttributes(this).apply {
            textureMode(true)
            camera(GameMapStyle.initialCamera())
        })
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync { map ->
            if (isDestroyed) return@getMapAsync
            mapLibreMap = map
            map.setStyle(GameMapStyle.builder()) { enableLocation() }
        }
        setContent {
            MapLibreCarTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    floatingActionButton = {
                        FloatingActionButton(onClick = ::locate) {
                            Icon(
                                painterResource(R.drawable.ic_my_location),
                                contentDescription = stringResource(R.string.recenter)
                            )
                        }
                    }
                ) { innerPadding ->
                    AndroidView(
                        modifier = Modifier.fillMaxSize().padding(innerPadding),
                        factory = { mapView }
                    )
                }
            }
        }
        if (savedInstanceState == null && !GameMapLocation.hasPermission(this)) {
            requestLocationPermission()
        }
    }

    private fun requestLocationPermission() {
        requestLocation.launch(arrayOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        ))
    }

    private fun locate() {
        if (GameMapLocation.hasPermission(this)) enableLocation(recenter = true)
        else requestLocationPermission()
    }

    private fun enableLocation(recenter: Boolean = false) {
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            mapLibreMap?.let { GameMapLocation.enable(this, it, recenter) }
        }
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
        enableLocation()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
        enableLocation()
    }

    override fun onPause() {
        mapView.onPause()
        super.onPause()
    }

    override fun onStop() {
        mapView.onStop()
        super.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        mapLibreMap = null
        mapView.onDestroy()
        super.onDestroy()
    }

    @Deprecated("Deprecated in Java")
    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }
}
