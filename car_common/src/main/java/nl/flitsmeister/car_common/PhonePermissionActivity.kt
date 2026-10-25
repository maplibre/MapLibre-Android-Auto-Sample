package nl.flitsmeister.car_common

import android.Manifest
import android.app.Activity
import android.os.Bundle

class PhonePermissionActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (GameMapLocation.hasPermission(this)) {
            finish()
        } else if (savedInstanceState == null) {
            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_REQUEST
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_REQUEST) finish()
    }

    companion object {
        private const val LOCATION_REQUEST = 123
    }
}
