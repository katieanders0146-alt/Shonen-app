package com.marverick.shonen

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class ExerciseListActivity : AppCompatActivity() {

    private val ranks = listOf("E-rank", "D-rank", "C-rank", "B-rank", "S-rank")
    private val repTargets = listOf(20, 40, 60, 80, 100)
    private val runKm = listOf(2, 4, 6, 8, 10)
    private var rankIndex = 0

    private lateinit var pushupButton: Button
    private lateinit var situpButton: Button
    private lateinit var squatButton: Button
    private lateinit var runningButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_exercise_list)

        val characterName = intent.getStringExtra("character_name") ?: "Trainee"
        findViewById<TextView>(R.id.characterHeader).text = "Training as: $characterName"

        pushupButton = findViewById(R.id.pushupButton)
        situpButton = findViewById(R.id.situpButton)
        squatButton = findViewById(R.id.squatButton)
        runningButton = findViewById(R.id.runningButton)

        val rankSpinner = findViewById<Spinner>(R.id.rankSpinner)
        rankSpinner.adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, ranks)
        rankSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?, view: View?, position: Int, id: Long
            ) {
                rankIndex = position
                refreshLabels()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        pushupButton.setOnClickListener { startTracker("pushup") }
        situpButton.setOnClickListener { startTracker("situp") }
        squatButton.setOnClickListener { startTracker("squat") }
        runningButton.setOnClickListener {
            Toast.makeText(this, "Running tracker is the next stage", Toast.LENGTH_SHORT).show()
        }

        refreshLabels()
    }

    private fun refreshLabels() {
        val reps = repTargets[rankIndex]
        pushupButton.text = "Push-up: $reps reps"
        situpButton.text = "Sit-up: $reps reps"
        squatButton.text = "Squat: $reps reps"
        runningButton.text = "Run: ${runKm[rankIndex]} km"
    }

    private fun startTracker(exercise: String) {
        val i = Intent(this, MainActivity::class.java)
        i.putExtra("exercise", exercise)
        i.putExtra("target", repTargets[rankIndex])
        startActivity(i)
    }
}