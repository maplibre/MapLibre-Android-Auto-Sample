package nl.flitsmeister.car_common.screens

import androidx.car.app.CarContext
import androidx.car.app.CarToast
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.CarIcon
import androidx.car.app.model.Template
import androidx.car.app.navigation.model.NavigationTemplate
import androidx.core.graphics.drawable.IconCompat
import nl.flitsmeister.car_common.CarMapRenderer
import nl.flitsmeister.car_common.R
import nl.flitsmeister.car_common.extentions.addAction
import nl.flitsmeister.car_common.extentions.screenManager
import nl.flitsmeister.car_common.extentions.setActionStrip
import nl.flitsmeister.car_common.extentions.setIcon
import nl.flitsmeister.car_common.extentions.setMapActionStrip

class CarMapScreen(
    private val carContext: CarContext,
    private val carMapRenderer: CarMapRenderer
) : Screen(carContext) {

    override fun onResume() {
        super.onResume()
        // the location permission might have been granted while the
        // permission screen was on top of this one
        carMapRenderer.refreshLocationComponent()
    }

    override fun onGetTemplate(): Template {
        val templateBuilder = NavigationTemplate.Builder()
        templateBuilder.apply {
            setActionStrip(buildActionStrip())
            if (carContext.carAppApiLevel >= 2) {
                setMapActionStrip(buildMapActionStrip(carMapRenderer))
            }
        }
        return templateBuilder.build()
    }

    private fun buildActionStrip(): ActionStrip.Builder {
        val actionStripBuilder = ActionStrip.Builder()
        actionStripBuilder.apply {
            addAction(Action.Builder().apply {
                setTitle("Test")
                setOnClickListener {
                    CarToast.makeText(carContext, "Test", CarToast.LENGTH_LONG).show()
                }
            })
            addAction(Action.Builder().apply {
                //setTitle("Menu") //There can be only 1 with a title
                setIcon(
                    CarIcon.Builder(
                        IconCompat.createWithResource(
                            carContext,
                            R.drawable.ic_menu
                        )
                    )
                )
                setOnClickListener {
                    carContext.screenManager.push(CarMenuScreen(carContext))
                }
            })
            return actionStripBuilder
        }
    }

    private fun buildMapActionStrip(carMapRenderer: CarMapRenderer): ActionStrip.Builder {
        val actionStripBuilder = ActionStrip.Builder()
        actionStripBuilder.apply {
            addAction(Action.PAN) // Needed to enable map interactivity! (pan and zoom gestures)
            addAction(Action.Builder().apply {
                //setTitle("Zoom in")
                setIcon(
                    CarIcon.Builder(
                        IconCompat.createWithResource(
                            carContext,
                            R.drawable.ic_zoom_in
                        )
                    )
                )
                setOnClickListener {
                    carMapRenderer.zoomInFromButton()
                }
            })
            addAction(Action.Builder().apply {
                //setTitle("Zoom out")
                setIcon(
                    CarIcon.Builder(
                        IconCompat.createWithResource(
                            carContext,
                            R.drawable.ic_zoom_out
                        )
                    )
                )
                setOnClickListener {
                    carMapRenderer.zoomOutFromButton()
                }
            })
        }
        return actionStripBuilder
    }

}