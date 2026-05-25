package nl.flitsmeister.car_common

import android.animation.Animator
import android.animation.ValueAnimator
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
import nl.flitsmeister.car_common.extentions.windowManager
import org.maplibre.android.MapLibre
import org.maplibre.android.constants.MapLibreConstants
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapLibreMapOptions
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import kotlin.math.ln

class CarMapContainer(
    private val carContext: CarContext
) {

    var mapViewInstance: MapView? = null
        private set

    var mapLibreMapInstance: MapLibreMap? = null

    private var scaleAnimator: Animator? = null


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
    fun setupMap(): View {
        MapLibre.getInstance(carContext)

        val mapView = createMapViewInstance().apply {
            onStart()
            getMapAsync {
                mapViewInstance = this
                mapLibreMapInstance = it
                it.setStyle(
                    //TODO: Set your own style here
                    Style.Builder().fromUri("https://demotiles.maplibre.org/style.json")
                )
            }
        }
        mapViewInstance = mapView

        // if we just need the map and nothing drawn above it, we can just return `mapView` directly
        // since we want to draw, let's layer the attribution on top of it.
        val attribution = TextView(carContext).apply {
            setText(R.string.copyright_openstreetmap)
            setTextColor(carContext.getColor(R.color.osm_attribution))
        }

        val frameLayout = FrameLayout(carContext)
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
    fun cleanUpMap() {
        mapLibreMapInstance = null

        mapViewInstance?.run {
            onStop()
            onDestroy()
            carContext.windowManager.removeView(this)
        }
        mapViewInstance = null
    }

    private fun createMapViewInstance() =
        MapView(carContext, MapLibreMapOptions.createFromAttributes(carContext)).apply {
            setLayerType(View.LAYER_TYPE_HARDWARE, Paint())
        }

    companion object {
        const val LOG_TAG = "CarMapContainer"
        const val DOUBLE_CLICK_FACTOR = 2.0F
    }
}