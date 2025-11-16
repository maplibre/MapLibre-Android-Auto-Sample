package nl.flitsmeister.car_common

import android.app.Presentation
import android.graphics.Rect
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.car.app.CarContext
import androidx.car.app.SurfaceCallback
import androidx.car.app.SurfaceContainer
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import nl.flitsmeister.car_common.extentions.appManager

class CarMapRenderer(
    private val carContext: CarContext,
    serviceLifecycle: Lifecycle
) : SurfaceCallback, DefaultLifecycleObserver, ICarMapRenderer {

    // The map container used to handle the map lifecycle
    private val mapContainer = CarMapContainer(carContext)

    // The surface to draw the map container on
    private var surfaceContainer: SurfaceContainer? = null

    // Handler to post actions to the UI thread
    private val uiHandler = Handler(Looper.getMainLooper())

    // The last known stable area
    private var lastKnownStableArea = Rect()
    private var lastKnownVisibleArea = Rect()

    private var presentation: Presentation? = null
    private var virtualDisplay: VirtualDisplay? = null

    init {
        serviceLifecycle.addObserver(this)
    }

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        try {
            carContext.appManager.setSurfaceCallback(this)
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Could not set surface callback", e)
            return
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        Log.v(LOG_TAG, "CarMapRenderer.onDestroy")
        mapContainer.cleanUpMap()
        surfaceContainer = null
        uiHandler.removeCallbacksAndMessages(null)
        try {
            carContext.appManager.setSurfaceCallback(null)
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Could not remove surface callback", e)
        }
    }

    override fun onSurfaceAvailable(surfaceContainer: SurfaceContainer) {
        Log.v(LOG_TAG, "CarMapRenderer.onSurfaceAvailable")
        this.surfaceContainer = surfaceContainer
        val virtualDisplay = carContext
            .getSystemService(DisplayManager::class.java)
            .createVirtualDisplay(
                "MapLibreSampleVirtualDisplay",
                surfaceContainer.width,
                surfaceContainer.height,
                surfaceContainer.dpi,
                surfaceContainer.surface,
                0
            ).also { this.virtualDisplay = it }
        val presentation = Presentation(carContext, virtualDisplay.display).also { this.presentation = it }
        presentation.setContentView(mapContainer.setupMap())
        presentation.show()
    }

    override fun onVisibleAreaChanged(visibleArea: Rect) {
        if (visibleArea != lastKnownVisibleArea) {
            Log.v(
                LOG_TAG,
                "onVisibleAreaChanged left(${visibleArea.left}) top(${visibleArea.top}) right(${visibleArea.right}) bottom(${visibleArea.bottom})"
            )
            lastKnownVisibleArea = visibleArea
        }
    }

    override fun onStableAreaChanged(stableArea: Rect) {
        if (stableArea != lastKnownStableArea) {
            Log.v(
                LOG_TAG,
                "onStableAreaChanged left(${stableArea.left}) top(${stableArea.top}) right(${stableArea.right}) bottom(${stableArea.bottom})"
            )
            //if only the vertical space has changed, you can ignore this mostly.
            lastKnownStableArea = stableArea
        }
    }

    override fun onSurfaceDestroyed(surfaceContainer: SurfaceContainer) {
        Log.v(LOG_TAG, "Surface destroyed")
        this.surfaceContainer = null
        uiHandler.removeCallbacksAndMessages(null)
    }

    override fun zoomInFromButton() {
        val centerX = surfaceContainer?.width?.toFloat()?.div(2) ?: -1f
        val centerY = surfaceContainer?.height?.toFloat()?.div(2) ?: -1f
        onScale(centerX, centerY, CarMapContainer.DOUBLE_CLICK_FACTOR)
    }

    override fun zoomOutFromButton() {
        val centerX = surfaceContainer?.width?.toFloat()?.div(2) ?: -1f
        val centerY = surfaceContainer?.height?.toFloat()?.div(2) ?: -1f
        onScale(centerX, centerY, -CarMapContainer.DOUBLE_CLICK_FACTOR)
    }

    //Map interactivity
    override fun onScale(focusX: Float, focusY: Float, scaleFactor: Float) {
        mapContainer.onScale(focusX, focusY, scaleFactor)
    }

    @Synchronized
    override fun onScroll(distanceX: Float, distanceY: Float) {
        Log.v(LOG_TAG, "onScroll distanceX($distanceX) distanceY($distanceY)")
        mapContainer.scrollBy(distanceX, distanceY)
    }

    override fun onClick(x: Float, y: Float) {
        super.onClick(x, y)
    }

    override fun onFling(velocityX: Float, velocityY: Float) {
        super.onFling(velocityX, velocityY)
        // We don't need to implement onFling since the MapView does this for us
    }

    companion object {
        const val LOG_TAG = "CarMapRenderer"
    }
}