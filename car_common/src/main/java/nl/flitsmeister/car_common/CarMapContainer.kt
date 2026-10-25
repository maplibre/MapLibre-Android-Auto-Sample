package nl.flitsmeister.car_common

import android.animation.Animator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.TextView
import androidx.annotation.MainThread
import androidx.car.app.CarContext
import androidx.car.app.CarToast
import org.maplibre.android.MapLibre
import org.maplibre.android.constants.MapLibreConstants
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapLibreMapOptions
import org.maplibre.android.maps.MapView
import kotlin.math.ln

class CarMapContainer(
    private val carContext: CarContext,
    private val startLocationForeground: () -> Boolean
) {

    var mapViewInstance: MapView? = null
        private set

    var mapLibreMapInstance: MapLibreMap? = null

    private var scaleAnimator: Animator? = null
    private var started = false
    private var resumed = false


    fun scrollBy(x: Float, y: Float) {
        mapLibreMapInstance?.scrollBy(-x, -y, 0)
    }

    private fun createScaleAnimator(
        currentZoom: Double,
        zoomAddition: Double,
        animationFocalPoint: PointF?,
    ): Animator {
        val animator =
            ValueAnimator.ofFloat(currentZoom.toFloat(), (currentZoom + zoomAddition).toFloat())
        animator.apply {
            duration = MapLibreConstants.ANIMATION_DURATION.toLong()
            interpolator = DecelerateInterpolator()
            addUpdateListener { animation ->
                animationFocalPoint?.let {
                    mapLibreMapInstance?.setZoom(
                        (animation.animatedValue as Float).toDouble(),
                        it, 0
                    )
                }
            }
        }
        return animator
    }

    private fun doubleClickZoomWithAnimation(zoomFocalPoint: PointF?, isZoomIn: Boolean) {
        cancelCurrentAnimator(scaleAnimator)
        val currentZoom = mapLibreMapInstance?.zoom
        currentZoom?.let {
            scaleAnimator = createScaleAnimator(
                it,
                if (isZoomIn) 1.0 else -1.0,
                zoomFocalPoint
            )
            scaleAnimator?.start()
        }
    }

    private fun cancelCurrentAnimator(animator: Animator?) {
        if (animator != null && animator.isStarted) {
            animator.cancel()
        }
    }

    fun onScale(focusX: Float, focusY: Float, scaleFactor: Float) {
        if (scaleFactor == DOUBLE_CLICK_FACTOR) {
            doubleClickZoomWithAnimation(PointF(focusX, focusY), true)
            return
        }
        if (scaleFactor == -DOUBLE_CLICK_FACTOR) {
            doubleClickZoomWithAnimation(PointF(focusX, focusY), false)
            return
        }
        if (!scaleFactor.isFinite() || scaleFactor <= 0f) return
        val currentZoomLevel = mapLibreMapInstance?.zoom

        // Calculate the additional zoom level based on the scale factor.
        val zoomAdditional =
            (ln(
                scaleFactor.toDouble()
            ) / ln(Math.PI / 2)) * MapLibreConstants.ZOOM_RATE

        currentZoomLevel?.let {
            mapLibreMapInstance?.setZoom(it + zoomAdditional, PointF(focusX, focusY), 0)
        }
    }

    @MainThread
    fun setupMap(context: Context): View {
        MapLibre.getInstance(context)

        val mapView = createMapViewInstance(context)
        mapViewInstance = mapView
        mapView.onCreate(null)
        mapView.getMapAsync { map ->
            if (mapViewInstance !== mapView) return@getMapAsync
            mapLibreMapInstance = map
            // Car templates do not forward taps to the MapView attribution dialog.
            map.uiSettings.isAttributionEnabled = false
            map.setStyle(GameMapStyle.builder()) {
                if (mapViewInstance === mapView && started) refreshLocation()
            }
        }

        // if we just need the map and nothing drawn above it, we can just return `mapView` directly
        // since we want to draw, let's layer the attribution on top of it.
        val attribution = TextView(context).apply {
            setText(R.string.copyright_openstreetmap)
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.argb(210, 23, 25, 28))
            textSize = 10f
        }

        val frameLayout = FrameLayout(context)
        frameLayout.addView(
            mapView,
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        frameLayout.addView(
            attribution, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM or Gravity.END
            )
        )

        return frameLayout
    }

    @MainThread
    fun startMap() {
        if (!started && mapViewInstance != null) {
            mapViewInstance?.onStart()
            started = true
            refreshLocation()
        }
    }

    fun resumeMap() {
        if (!resumed && mapViewInstance != null) {
            mapViewInstance?.onResume()
            resumed = true
        }
    }

    fun pauseMap() {
        if (resumed) mapViewInstance?.onPause()
        resumed = false
    }

    fun stopMap() {
        pauseMap()
        if (started) mapViewInstance?.onStop()
        started = false
    }

    fun refreshLocation(recenter: Boolean = false) {
        if (!started) return
        val map = mapLibreMapInstance ?: return
        if (map.style?.isFullyLoaded != true) return
        if (!GameMapLocation.hasPermission(carContext)) {
            GameMapLocation.enable(carContext, map)
            return
        }
        if (startLocationForeground()) {
            GameMapLocation.enable(carContext, map, recenter)
        } else if (recenter) {
            CarToast.makeText(carContext, R.string.open_phone_for_location, CarToast.LENGTH_LONG).show()
        }
    }

    @MainThread
    fun cleanUpMap() {
        cancelCurrentAnimator(scaleAnimator)
        scaleAnimator = null
        stopMap()
        mapLibreMapInstance = null
        // The Presentation owns this view, not carContext.windowManager.
        mapViewInstance?.onDestroy()
        mapViewInstance = null
    }

    private fun createMapViewInstance(context: Context) =
        MapView(context, MapLibreMapOptions.createFromAttributes(context).apply {
            camera(GameMapStyle.initialCamera())
        }).apply {
            setLayerType(View.LAYER_TYPE_HARDWARE, Paint())
        }

    companion object {
        const val LOG_TAG = "CarMapContainer"
        const val DOUBLE_CLICK_FACTOR = 2.0F
    }
}
