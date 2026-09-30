package com.marverick.shonen

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.location.Location
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

class RunTrackingService : Service() {

    private val binder = LocalBinder()
    private val fusedClient by lazy { LocationServices.getFusedLocationProviderClient(this) }

    var targetKm = 2
        private set
    var totalMeters = 0f
        private set
    var startTimeMs = 0L
        private set
    var trackingStatus = "Waiting for GPS signal..."
        private set

    private var lastAccepted: Location? = null
    private var listener: (() -> Unit)? = null

    inner class LocalBinder : Binder() {
        fun getService(): RunTrackingService = this@RunTrackingService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    fun setListener(l: (() -> Unit)?) {
        listener = l
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (startTimeMs == 0L) {
            targetKm = intent?.getIntExtra("target_km", 2) ?: 2
            startTracking()
        }
        return START_STICKY
    }

    private fun startTracking() {
        totalMeters = 0f
        lastAccepted = null
        startTimeMs = System.currentTimeMillis()
        trackingStatus = "Waiting for GPS signal..."

        startForeground(NOTIFICATION_ID, buildNotification())

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L)
            .setMinUpdateIntervalMillis(1000L)
            .build()
        try {
            fusedClient.requestLocationUpdates(request, locationCallback, mainLooper)
        } catch (e: SecurityException) {
            trackingStatus = "Location permission missing"
        }
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            for (loc in result.locations) handleLocation(loc)
        }
    }

    private fun handleLocation(loc: Location) {
        if (!loc.hasAccuracy() || loc.accuracy > 25f) {
            trackingStatus = "Weak GPS signal, keep going..."
            listener?.invoke()
            return
        }
        trackingStatus = "Tracking"

        val last = lastAccepted
        if (last == null) {
            lastAccepted = loc
            listener?.invoke()
            return
        }

        val step = loc.distanceTo(last)
        val seconds = (loc.elapsedRealtimeNanos - last.elapsedRealtimeNanos) / 1_000_000_000f
        if (seconds > 0f && step / seconds > 12f) return

        if (step >= 5f) {
            totalMeters += step
            lastAccepted = loc
            updateNotification()
        }
        listener?.invoke()
    }

    fun stopTracking() {
        fusedClient.removeLocationUpdates(locationCallback)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun buildNotification(): Notification {
        val channelId = "run_tracking"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(
                NotificationChannel(channelId, "Run tracking", NotificationManager.IMPORTANCE_LOW)
            )
        }
        val km = totalMeters / 1000f
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Run in progress")
            .setContentText(String.format("%.2f / %d km", km, targetKm))
            .setSmallIcon(android.R.drawable.ic_menu_directions)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification() {
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, buildNotification())
    }

    override fun onDestroy() {
        super.onDestroy()
        fusedClient.removeLocationUpdates(locationCallback)
    }

    companion object {
        const val NOTIFICATION_ID = 1001
    }
}