package com.marverick.shonen

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class RunActivity : AppCompatActivity() {

    private lateinit var distanceText: TextView
    private lateinit var timeText: TextView
    private lateinit var paceText: TextView
    private lateinit var statusText: TextView
    private lateinit var startButton: Button

    private var targetKm = 2
    private var running = false
    private var boundService: RunTrackingService? = null
    private var targetAnnounced = false

    private val handler = Handler(Looper.getMainLooper())
    private val ticker = object : Runnable {
        override fun run() {
            refreshFromService()
            if (running) handler.postDelayed(this, 1000)
        }
    }

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            boundService = (binder as RunTrackingService.LocalBinder).getService()
            boundService?.setListener { refreshFromService() }
            refreshFromService()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            boundService = null
        }
    }

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
            if (results[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
                beginRun()
            } else {
                statusText.text = "Precise location permission is needed to track your run"
            }
        }

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_run)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        distanceText = findViewById(R.id.distanceText)
        timeText = findViewById(R.id.timeText)
        paceText = findViewById(R.id.paceText)
        statusText = findViewById(R.id.statusText)
        startButton = findViewById(R.id.startButton)

        targetKm = intent.getIntExtra("target_km", 2)
        distanceText.text = String.format("%.2f / %d km", 0f, targetKm)

        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        // If a run is already in progress (e.g. you reopened the app after the screen was off),
        // reconnect to it instead of starting fresh.
        val alreadyRunning = bindService(Intent(this, RunTrackingService::class.java), connection, 0)
        if (alreadyRunning) {
            running = true
            startButton.text = "Finish"
            handler.post(ticker)
        }

        startButton.setOnClickListener {
            if (running) finishRun() else startRun()
        }
    }

    private fun startRun() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            beginRun()
        } else {
            permissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }
    }

    private fun beginRun() {
        targetAnnounced = false
        running = true
        startButton.text = "Finish"
        statusText.text = "Waiting for GPS signal..."

        val serviceIntent = Intent(this, RunTrackingService::class.java)
        serviceIntent.putExtra("target_km", targetKm)
        ContextCompat.startForegroundService(this, serviceIntent)
        bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE)

        handler.post(ticker)
    }

    private fun refreshFromService() {
        val service = boundService ?: return
        val km = service.totalMeters / 1000f
        distanceText.text = String.format("%.2f / %d km", km, targetKm)
        statusText.text = service.trackingStatus

        val elapsed = ((System.currentTimeMillis() - service.startTimeMs) / 1000).toInt()
        timeText.text = String.format("%02d:%02d", elapsed / 60, elapsed % 60)

        if (km >= 0.05f) {
            val paceSec = (elapsed / km).toInt()
            paceText.text = String.format("Pace: %d:%02d /km", paceSec / 60, paceSec % 60)
        }

        if (!targetAnnounced && km >= targetKm) {
            targetAnnounced = true
            Toast.makeText(this, "Distance target reached!", Toast.LENGTH_LONG).show()
        }
    }

    private fun finishRun() {
        running = false
        boundService?.stopTracking()
        try { unbindService(connection) } catch (e: IllegalArgumentException) { }
        boundService = null
        startButton.text = "Start"
        statusText.text = "Run finished"
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(ticker)
        if (boundService != null) {
            try { unbindService(connection) } catch (e: IllegalArgumentException) { }
        }
        // Deliberately NOT stopping the service here — tracking should keep going
        // even if this screen closes or the phone screen turns off.
    }
}