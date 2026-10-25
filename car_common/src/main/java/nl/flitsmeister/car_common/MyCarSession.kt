package nl.flitsmeister.car_common

import android.content.Intent
import android.content.res.Configuration
import android.util.Log
import androidx.car.app.CarContext
import androidx.car.app.CarToast
import androidx.car.app.Screen
import androidx.car.app.Session
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import nl.flitsmeister.car_common.extentions.screenManager
import nl.flitsmeister.car_common.screens.CarMapScreen
import nl.flitsmeister.car_common.screens.CarPermissionScreen

class MyCarSession(
    private val startLocationForeground: () -> Boolean,
    private val stopLocationForeground: () -> Unit
) : Session() {
    private lateinit var carMapRenderer: CarMapRenderer
    private var carConfiguration: Configuration? = null

    init {
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStop(owner: LifecycleOwner) = stopLocationForeground()
            override fun onDestroy(owner: LifecycleOwner) = stopLocationForeground()
        })
    }

    override fun onCreateScreen(intent: Intent): Screen {
        Log.v(LOG_TAG, "onCreateScreen: $intent")
        carMapRenderer = CarMapRenderer(carContext, lifecycle, startLocationForeground)

        val carMapScreen = CarMapScreen(carContext, carMapRenderer)
        //if location permission is not granted; Add the permission screen unto the stack
        if (!GameMapLocation.hasPermission(carContext)) {
            Log.v(LOG_TAG, "onCreateScreen: Location permission not granted")
            carContext.screenManager.push(carMapScreen)
            val carPermissionScreen = CarPermissionScreen(carContext)
            // The host pushes the returned screen itself.
            return carPermissionScreen
        } else {
            return carMapScreen
        }
    }

    override fun onCarConfigurationChanged(newConfiguration: Configuration) {
        Log.v(LOG_TAG, "onCarConfigurationChanged: old: $carConfiguration, new: $newConfiguration")
        carConfiguration = newConfiguration
    }

    override fun onNewIntent(intent: Intent) {
        Log.v(LOG_TAG, "onNewIntent: $intent")
        super.onNewIntent(intent)

        when (intent.action) {
            CarContext.ACTION_NAVIGATE -> {
                // When user speaks "navigate to X" to Google Assistant, this action will be triggered
                navigateFromIntent(intent)
            }
            //TODO: Add your own actions here, for example: When a use clicks a notification.
            INTENT_ACTION_CLICKED_NOTIFICATION -> {
                clickedNotification(intent)
            }
        }
    }

    private fun navigateFromIntent(intent: Intent) {
        val uri = intent.data ?: return
        val query = uri.query ?: return
        if (uri.scheme != "geo") return
        CarToast.makeText(carContext, "Navigating to $query", CarToast.LENGTH_LONG).show()
    }

    private fun clickedNotification(intent: Intent) {
        CarToast.makeText(carContext, "Clicked notification to $intent", CarToast.LENGTH_LONG)
            .show()
    }

    companion object {
        const val LOG_TAG = "MyCarSession"
        const val INTENT_ACTION_CLICKED_NOTIFICATION = "clicked_notification"

        //TODO: Add your own navigation logic
        var isRouteActive = false
    }
}
