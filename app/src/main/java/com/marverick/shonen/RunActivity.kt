package com.marverick.shonen

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

class RunActivity : AppCompatActivity() {

    private lateinit var distanceText: TextView
    private lateinit var timeText: TextView
    private lateinit var paceText: TextView
    private lateinit var statusText: TextView
    private lateinit var startButton: Button

    private var targetKm = 2
    private var running = false
    private var totalMeters = 0f
    private var lastAccepted: Location? = null
    private var startTime = 0L
    private var targetAnnounced = false

    private val fusedClient by lazy { LocationServices.getFusedLocationProviderClient(this) }
    private val handler = Handler(Looper.getMainLooper())

    private val ticker = object : Runnable {
        override fun run() {
            if (running) {
                updateDisplay()
                handler.postDelayed(this, 1000)
            }
        }
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            for (loc in result.locations) handleLocation(loc)
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
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun beginRun() {
        totalMeters = 0f
        lastAccepted = null
        targetAnnounced = false
        startTime = SystemClock.elapsedRealtime()
        running = true
        startButton.text = "Finish"
        statusText.text = "Waiting for GPS signal..."

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L)
            .setMinUpdateIntervalMillis(1000L)
            .build()
        fusedClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
        handler.post(ticker)
    }

    private fun handleLocation(loc: Location) {
        if (!running) return
        if (!loc.hasAccuracy() || loc.accuracy > 25f) {
            statusText.text = "Weak GPS signal, keep going..."
            return
        }
        statusText.text = "Tracking"

        val last = lastAccepted
        if (last == null) {
            lastAccepted = loc
            return
        }

        val step = loc.distanceTo(last)
        val seconds = (loc.elapsedRealtimeNanos - last.elapsedRealtimeNanos) / 1_000_000_000f
        if (seconds > 0f && step / seconds > 12f) return

        if (step >= 5f) {
            totalMeters += step
            lastAccepted = loc
            updateDisplay()
            if (!targetAnnounced && totalMeters >= targetKm * 1000f) {
                targetAnnounced = true
                Toast.makeText(this, "Distance target reached!", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun updateDisplay() {
        val km = totalMeters / 1000f
        distanceText.text = String.format("%.2f / %d km", km, targetKm)
        val seconds = ((SystemClock.elapsedRealtime() - startTime) / 1000).toInt()
        timeText.text = String.format("%02d:%02d", seconds / 60, seconds % 60)
        if (km >= 0.05f) {
            val paceSec = (seconds / km).toInt()
            paceText.text = String.format("Pace: %d:%02d /km", paceSec / 60, paceSec % 60)
        }
    }

    private fun finishRun() {
        updateDisplay()
        running = false
        fusedClient.removeLocationUpdates(locationCallback)
        handler.removeCallbacks(ticker)
        startButton.text = "Start"
        statusText.text = "Run finished"
    }

    override fun onDestroy() {
        super.onDestroy()
        running = false
        fusedClient.removeLocationUpdates(locationCallback)
        handler.removeCallbacks(ticker)
    }
}}