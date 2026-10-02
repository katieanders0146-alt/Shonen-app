package com.marverick.shonen

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class TimerActivity : AppCompatActivity() {

    private lateinit var timeText: TextView
    private lateinit var startButton: Button
    private var label = "Training"
    private var targetMinutes = 15
    private var running = false
    private var accumulatedMs = 0L
    private var segmentStart = 0L
    private var targetAnnounced = false

    private val handler = Handler(Looper.getMainLooper())
    private val ticker = object : Runnable {
        override fun run() {
            updateDisplay()
            if (running) handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_timer)
        timeText = findViewById(R.id.timeText)
        startButton = findViewById(R.id.startButton)

        label = intent.getStringExtra("label") ?: "Training"
        targetMinutes = intent.getIntExtra("target_minutes", 15)

        startButton.setOnClickListener { if (running) pauseTimer() else startTimer() }
        updateDisplay()
    }

    private fun startTimer() {
        running = true
        segmentStart = SystemClock.elapsedRealtime()
        startButton.text = "Pause"
        handler.post(ticker)
    }

    private fun pauseTimer() {
        running = false
        accumulatedMs += SystemClock.elapsedRealtime() - segmentStart
        startButton.text = "Resume"
        updateDisplay()
    }

    private fun updateDisplay() {
        val currentMs = accumulatedMs + if (running) SystemClock.elapsedRealtime() - segmentStart else 0L
        val totalSeconds = (currentMs / 1000).toInt()
        timeText.text = String.format(
            "%s (manual timer — not motion-verified)\n%02d:%02d / %d:00",
            label, totalSeconds / 60, totalSeconds % 60, targetMinutes
        )
        if (!targetAnnounced && totalSeconds >= targetMinutes * 60) {
            targetAnnounced = true
            Toast.makeText(this, "$label target reached!", Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(ticker)
    }
}