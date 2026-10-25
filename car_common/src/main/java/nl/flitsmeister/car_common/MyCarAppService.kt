package nl.flitsmeister.car_common

import android.content.Context
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.util.Log
import androidx.car.app.CarAppService
import androidx.car.app.Session
import androidx.car.app.validation.HostValidator
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat

class MyCarAppService : CarAppService() {
    private var locationForeground = false

    override fun onCreateSession(): Session {
        return MyCarSession(::startLocationForeground, ::stopLocationForeground)
    }

    private fun startLocationForeground(): Boolean {
        if (!GameMapLocation.hasPermission(this)) {
            stopLocationForeground()
            return false
        }
        if (locationForeground) return true
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(
            LOCATION_CHANNEL, getString(R.string.location_channel), NotificationManager.IMPORTANCE_LOW
        ))
        val notification = NotificationCompat.Builder(this, LOCATION_CHANNEL)
            .setSmallIcon(R.drawable.ic_my_location)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.car_location_active))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
        packageManager.getLaunchIntentForPackage(packageName)?.let { intent ->
            notification.setContentIntent(PendingIntent.getActivity(
                this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            ))
        }
        return try {
            ServiceCompat.startForeground(
                this, LOCATION_NOTIFICATION, notification.build(), ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
            locationForeground = true
            true
        } catch (error: SecurityException) {
            Log.w(LOG_TAG, "Open the phone app to allow the location session to start", error)
            false
        } catch (error: IllegalStateException) {
            Log.w(LOG_TAG, "The host did not allow a foreground location session", error)
            false
        }
    }

    private fun stopLocationForeground() {
        if (locationForeground) ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        locationForeground = false
    }

    override fun onDestroy() {
        stopLocationForeground()
        super.onDestroy()
    }

    override fun createHostValidator(): HostValidator {
        checkManifestForDebug(this)
        return if (isDebug) HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
        else HostValidator.Builder(this)
            .addAllowedHosts(R.array.car_template_hosts_allowlist)
            .build()
    }

    companion object {
        private const val LOCATION_CHANNEL = "car_location"
        private const val LOCATION_NOTIFICATION = 1001
        const val LOG_TAG = "CarAppService"
        var isDebug = false
        var appPlatform = "ANDROID_AUTO"

        fun checkManifestForDebug(context: Context) {
            try {
                val app = context.packageManager.getApplicationInfo(
                    context.packageName,
                    PackageManager.GET_META_DATA
                )
                val bundle = app.metaData
                isDebug = bundle.getBoolean("nl.flitsmeister.maplibrecar.IS_DEBUG") == true
                appPlatform =
                    bundle.getString("nl.flitsmeister.maplibrecar.APP_PLATFORM") ?: "ANDROID_AUTO"
            } catch (e: Exception) {
                Log.e(LOG_TAG, "Failed to check manifest (for debug mode and app platform)", e)
            }
        }
    }
}
