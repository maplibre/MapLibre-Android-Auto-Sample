package nl.flitsmeister.car_common.screens

import android.Manifest
import android.content.Intent
import android.util.Log
import androidx.car.app.CarContext
import androidx.car.app.CarToast
import androidx.car.app.Screen
import androidx.car.app.ScreenManager
import androidx.car.app.model.Action
import androidx.car.app.model.CarColor
import androidx.car.app.model.CarIcon
import androidx.car.app.model.MessageTemplate
import androidx.car.app.model.ParkedOnlyOnClickListener
import androidx.car.app.model.Template
import androidx.core.graphics.drawable.IconCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import nl.flitsmeister.car_common.GameMapLocation
import nl.flitsmeister.car_common.extentions.screenManager
import nl.flitsmeister.car_common.MyCarAppService
import nl.flitsmeister.car_common.PhonePermissionActivity
import nl.flitsmeister.car_common.R
import java.util.concurrent.Executor

class CarPermissionScreen(carContext: CarContext) : Screen(carContext) {
    init {
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onResume(owner: LifecycleOwner) {
                if (GameMapLocation.hasPermission(carContext)) carContext.screenManager.popToRoot()
            }
        })
    }

    override fun onGetTemplate(): Template {
        Log.v(LOG_TAG, "CarPermissionScreen.onGetTemplate")
        val message = if (MyCarAppService.appPlatform == "AAOS") {
            carContext.getString(R.string.aaos_no_location_permission_desc)
        } else {
            carContext.getString(R.string.aa_no_location_permission_desc)
        }
        val templateBuilder = MessageTemplate.Builder(message).apply {
            setTitle(carContext.getString(R.string.app_name))
            setIcon(
                CarIcon.Builder(
                    IconCompat.createWithResource(
                        carContext,
                        R.drawable.ic_launcher_foreground
                    )
                ).build()
            )
            if (MyCarAppService.appPlatform == "AAOS") {
                addAction(
                    Action.Builder()
                        .setBackgroundColor(CarColor.BLUE)
                        .setTitle(carContext.getString(R.string.aaos_open_location_permission))
                        .setOnClickListener(
                            ParkedOnlyOnClickListener.create(::clickedFixPermissionAAOS)
                        )
                        .build()
                )
            } else {
                addAction(
                    Action.Builder()
                        .setBackgroundColor(CarColor.BLUE)
                        .setTitle(carContext.getString(R.string.aa_open_location_permission))
                        .setOnClickListener(
                            ParkedOnlyOnClickListener.create(::clickedFixPermissionAA)
                        )
                        .build()
                )
            }
            addAction(
                Action.Builder()
                    .setBackgroundColor(CarColor.DEFAULT)
                    .setTitle(carContext.getString(R.string.close))
                    .setOnClickListener {
                        carContext.finishCarApp()
                    }
                    .build()
            )
        }

        return templateBuilder.build()
    }

    private fun clickedFixPermissionAAOS() {
        //Ask for permissions
        val requiredPermissions = listOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        val myExecutor = Executor { command -> command?.run() }
        carContext.requestPermissions(
            requiredPermissions,
            myExecutor, //An Executor makes sure code is run on the correct thread.
        ) { approved, rejected ->
            Log.v("FM AAOS", "$approved $rejected")
            if (GameMapLocation.hasPermission(carContext)) {
                //Granted
                CarToast.makeText(
                    carContext,
                    carContext.getString(R.string.permission_granted),
                    CarToast.LENGTH_LONG
                )
                    .show()
                //Close this screen
                val screenManager =
                    carContext.getCarService(CarContext.SCREEN_SERVICE) as ScreenManager
                screenManager.popToRoot()
            }
        }
    }

    private fun clickedFixPermissionAA() {
        //If this function is called, you're standing still, Android Auto has already checked that.
        //Check if permission is already given
        if (!GameMapLocation.hasPermission(carContext)) {
            //Not granted
            //Open screen on phone
            val intent = Intent(carContext, PhonePermissionActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            try {
                carContext.startActivity(intent)
                CarToast.makeText(
                    carContext,
                    carContext.getString(R.string.aa_opened_on_phone),
                    CarToast.LENGTH_LONG
                ).show()
            } catch (e: Exception) {
                e.printStackTrace()
                CarToast.makeText(
                    carContext,
                    carContext.getString(R.string.aa_open_on_phone_manual),
                    CarToast.LENGTH_LONG
                ).show()
            }
        } else {
            //Granted
            CarToast.makeText(
                carContext,
                carContext.getString(R.string.permission_granted),
                CarToast.LENGTH_LONG
            ).show()
            //Close this screen
            val screenManager =
                carContext.getCarService(CarContext.SCREEN_SERVICE) as ScreenManager
            screenManager.popToRoot()
        }
    }

    companion object {
        const val LOG_TAG = "CarPermissionScreen"
    }
}
