package com.marverick.shonen

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class ExerciseListActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_exercise_list)

        val characterName = intent.getStringExtra("character_name") ?: "Trainee"
        findViewById<TextView>(R.id.characterHeader).text = "Training as: $characterName"

        findViewById<Button>(R.id.pushupButton).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }
        findViewById<Button>(R.id.situpButton).setOnClickListener {
            Toast.makeText(this, "Sit-up tracking coming soon", Toast.LENGTH_SHORT).show()
        }
        findViewById<Button>(R.id.squatButton).setOnClickListener {
            Toast.makeText(this, "Squat tracking coming soon", Toast.LENGTH_SHORT).show()
        }
        findViewById<Button>(R.id.runningButton).setOnClickListener {
            Toast.makeText(this, "Running tracking coming soon", Toast.LENGTH_SHORT).show()
        }
    }
}