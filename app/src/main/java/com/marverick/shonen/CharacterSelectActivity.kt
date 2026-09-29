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
                runKm = intArrayOf(2, 3, 4, 6, 8, 10)
            )
        }
        findViewById<Button>(R.id.saitamaButton).setOnClickListener {
            openExercises(
                name = "Saitama",
                ranks = arrayOf("C-Class", "B-Class", "A-Class", "S-Class"),
                repTargets = intArrayOf(40, 60, 80, 100),
                runKm = intArrayOf(4, 6, 8, 10)
            )
        }
    }

    private fun openExercises(name: String, ranks: Array<String>, repTargets: IntArray, runKm: IntArray) {
        val intent = Intent(this, ExerciseListActivity::class.java)
        intent.putExtra("character_name", name)
        intent.putExtra("ranks", ranks)
        intent.putExtra("rep_targets", repTargets)
        intent.putExtra("run_km", runKm)
        startActivity(intent)
    }
}