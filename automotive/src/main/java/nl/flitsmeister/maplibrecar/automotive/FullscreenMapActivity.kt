package nl.flitsmeister.maplibrecar.automotive

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.FrameLayout.LayoutParams
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.location.modes.CameraMode
import org.maplibre.android.maps.MapView

/**
 * A fullscreen map with on-screen zoom and recenter controls, rendered
 * directly on the display it was launched on.
 */
class FullscreenMapActivity : BaseMapActivity() {

    override val styleUri = "https://tiles.openfreemap.org/styles/liberty"

    override fun onCreateContentView(mapView: MapView): View {
        val root = FrameLayout(this)
        root.addView(
            mapView,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        )

        val buttonSize = (BUTTON_SIZE_DP * resources.displayMetrics.density).toInt()
        val margin = (BUTTON_MARGIN_DP * resources.displayMetrics.density).toInt()
        val buttonBackground = {
            GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = BUTTON_CORNER_RADIUS_DP * resources.displayMetrics.density
                setColor(Color.argb(200, 40, 40, 40))
            }
        }

        fun button(label: String, textSize: Float) = Button(this).apply {
            text = label
            this.textSize = textSize
            setTextColor(Color.WHITE)
            background = buttonBackground()
            setPadding(0, 0, 0, 0)
        }

        val zoomIn = button("+", 24f)
        zoomIn.setOnClickListener {
            mapLibreMap?.let { map ->
                val zoom = minOf(map.cameraPosition.zoom + 1, MAX_ZOOM)
                val locationComponent = map.locationComponent
                if (locationComponent.isLocationComponentEnabled) {
                    locationComponent.zoomWhileTracking(zoom)
                } else {
                    map.animateCamera(CameraUpdateFactory.zoomTo(zoom))
                }
            }
        }

        val zoomOut = button("−", 24f)
        zoomOut.setOnClickListener {
            mapLibreMap?.let { map ->
                val zoom = maxOf(map.cameraPosition.zoom - 1, MIN_ZOOM)
                val locationComponent = map.locationComponent
                if (locationComponent.isLocationComponentEnabled) {
                    locationComponent.zoomWhileTracking(zoom)
                } else {
                    map.animateCamera(CameraUpdateFactory.zoomTo(zoom))
                }
            }
        }

        val recenter = button("◎", 20f)
        recenter.setOnClickListener {
            mapLibreMap?.locationComponent?.let { locationComponent ->
                if (locationComponent.isLocationComponentEnabled) {
                    locationComponent.cameraMode = CameraMode.TRACKING
                    locationComponent.zoomWhileTracking(initialZoom)
                }
            }
        }

        for ((index, view) in listOf(zoomOut, zoomIn).withIndex()) {
            root.addView(
                view,
                LayoutParams(buttonSize, buttonSize).apply {
                    gravity = Gravity.BOTTOM or Gravity.END
                    marginEnd = margin
                    bottomMargin = margin + index * (buttonSize + margin)
                }
            )
        }
        root.addView(
            recenter,
            LayoutParams(buttonSize, buttonSize).apply {
                gravity = Gravity.BOTTOM or Gravity.START
                marginStart = margin
                bottomMargin = margin
            }
        )

        return root
    }

    companion object {
        private const val BUTTON_SIZE_DP = 48
        private const val BUTTON_MARGIN_DP = 12
        private const val BUTTON_CORNER_RADIUS_DP = 8f
        private const val MIN_ZOOM = 2.0
        private const val MAX_ZOOM = 18.0
    }
}
