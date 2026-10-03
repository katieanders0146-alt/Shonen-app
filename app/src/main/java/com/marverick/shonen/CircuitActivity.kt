package com.marverick.shonen

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class CircuitActivity : AppCompatActivity() {

    private lateinit var stepText: TextView
    private lateinit var startButton: Button

    private var steps: List<String> = listOf("pushup", "squat", "situp")
    private var target = 20
    private var stepIndex = 0

    private val stepLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        stepIndex++
        showStep()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_circuit)

        stepText = findViewById(R.id.stepText)
        startButton = findViewById(R.id.startButton)

        steps = intent.getStringArrayExtra("steps")?.toList() ?: steps
        target = intent.getIntExtra("target", 20)

        startButton.setOnClickListener {
            if (stepIndex < steps.size) {
                val i = Intent(this, MainActivity::class.java)
                i.putExtra("exercise", steps[stepIndex])
                i.putExtra("target", target)
                stepLauncher.launch(i)
            } else {
                finish()
            }
        }
        showStep()
    }

    private fun exerciseName(type: String) = when (type) {
        "pushup" -> "Push-up"
        "squat" -> "Squat"
        "situp" -> "Sit-up"
        else -> type
    }

    private fun showStep() {
        if (stepIndex >= steps.size) {
            stepText.text = "Circuit complete!"
            startButton.text = "Done"
        } else {
            stepText.text = "Step ${stepIndex + 1} of ${steps.size}: ${exerciseName(steps[stepIndex])} — $target reps"
            startButton.text = "Start"
        }
    }
}