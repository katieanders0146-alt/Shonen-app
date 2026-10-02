package com.marverick.shonen

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class CharacterSelectActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_character_select)

        findViewById<Button>(R.id.jinWooButton).setOnClickListener {
            openExercises(
                name = "Sung Jin-Woo",
                ranks = arrayOf("E-rank", "D-rank", "C-rank", "B-rank", "A-rank", "S-rank"),
                repTargets = intArrayOf(20, 35, 50, 65, 80, 100),
                runKm = intArrayOf(2, 3, 4, 6, 8, 10),
                timerMinutes = intArrayOf(10, 15, 20, 25, 30, 40),
                exerciseNames = arrayOf("Push-up", "Sit-up", "Squat", "10 km Running"),
                exerciseTypes = arrayOf("pushup", "situp", "squat", "run")
            )
        }
        findViewById<Button>(R.id.saitamaButton).setOnClickListener {
            openExercises(
                name = "Saitama",
                ranks = arrayOf("C-Class", "B-Class", "A-Class", "S-Class"),
                repTargets = intArrayOf(40, 60, 80, 100),
                runKm = intArrayOf(4, 6, 8, 10),
                timerMinutes = intArrayOf(15, 20, 25, 30),
                exerciseNames = arrayOf("Push-up", "Sit-up", "Squat", "10 km Running"),
                exerciseTypes = arrayOf("pushup", "situp", "squat", "run")
            )
        }
        findViewById<Button>(R.id.gokuButton).setOnClickListener {
            openExercises(
                name = "Goku",
                ranks = arrayOf(
                    "Base", "Kaio-ken", "Super Saiyan",
                    "Super Saiyan 2", "Super Saiyan 3", "Super Saiyan Blue"
                ),
                repTargets = intArrayOf(20, 35, 50, 65, 80, 100),
                runKm = intArrayOf(2, 3, 4, 6, 8, 10),
                timerMinutes = intArrayOf(10, 15, 20, 25, 30, 40),
                exerciseNames = arrayOf(
                    "Martial-arts practice",
                    "Strength/endurance conditioning",
                    "Running/conditioning",
                    "Turtle-shell loaded movement",
                    "Gravity training"
                ),
                exerciseTypes = arrayOf("timer", "circuit", "run", "circuit", "timer")
            )
        }
    }

    private fun openExercises(
        name: String, ranks: Array<String>, repTargets: IntArray, runKm: IntArray,
        timerMinutes: IntArray, exerciseNames: Array<String>, exerciseTypes: Array<String>
    ) {
        val intent = Intent(this, ExerciseListActivity::class.java)
        intent.putExtra("character_name", name)
        intent.putExtra("ranks", ranks)
        intent.putExtra("rep_targets", repTargets)
        intent.putExtra("run_km", runKm)
        intent.putExtra("timer_minutes", timerMinutes)
        intent.putExtra("exercise_names", exerciseNames)
        intent.putExtra("exercise_types", exerciseTypes)
        startActivity(intent)
    }
}