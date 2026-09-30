package com.marverick.shonen

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class ExerciseListActivity : AppCompatActivity() {

    private var ranks: List<String> = listOf("Default")
    private var repTargets: List<Int> = listOf(20)
    private var runKm: List<Int> = listOf(2)
    private var exerciseNames: List<String> = listOf("Push-up")
    private var exerciseTypes: List<String> = listOf("pushup")
    private var rankIndex = 0
    private val exerciseButtons = mutableListOf<Pair<Button, String>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_exercise_list)

        val characterName = intent.getStringExtra("character_name") ?: "Trainee"
        findViewById<TextView>(R.id.characterHeader).text = "Training as: $characterName"

        ranks = intent.getStringArrayExtra("ranks")?.toList() ?: ranks
        repTargets = intent.getIntArrayExtra("rep_targets")?.toList() ?: repTargets
        runKm = intent.getIntArrayExtra("run_km")?.toList() ?: runKm
        exerciseNames = intent.getStringArrayExtra("exercise_names")?.toList() ?: exerciseNames
        exerciseTypes = intent.getStringArrayExtra("exercise_types")?.toList() ?: exerciseTypes

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

        buildExerciseButtons()
        refreshLabels()
    }

    private fun buildExerciseButtons() {
        val container = findViewById<LinearLayout>(R.id.exerciseContainer)
        container.removeAllViews()
        exerciseButtons.clear()

        for (i in exerciseNames.indices) {
            val type = exerciseTypes.getOrElse(i) { "none" }
            val button = Button(this)
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.bottomMargin = 24
            button.layoutParams = params
            button.setOnClickListener { onExerciseTapped(type) }
            container.addView(button)
            exerciseButtons.add(button to type)
        }
    }

    private fun onExerciseTapped(type: String) {
        when (type) {
            "pushup", "situp", "squat" -> {
                val i = Intent(this, MainActivity::class.java)
                i.putExtra("exercise", type)
                i.putExtra("target", repTargets[rankIndex])
                startActivity(i)
            }
            "run" -> {
                val i = Intent(this, RunActivity::class.java)
                i.putExtra("target_km", runKm[rankIndex])
                startActivity(i)
            }
            else -> {
                Toast.makeText(this, "Tracking coming soon for this training method", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun refreshLabels() {
        for (i in exerciseButtons.indices) {
            val (button, type) = exerciseButtons[i]
            val name = exerciseNames.getOrElse(i) { "Exercise" }
            button.text = when (type) {
                "pushup", "situp", "squat" -> "$name: ${repTargets[rankIndex]} reps"
                "run" -> "$name: ${runKm[rankIndex]} km"
                else -> "$name (coming soon)"
            }
        }
    }
}