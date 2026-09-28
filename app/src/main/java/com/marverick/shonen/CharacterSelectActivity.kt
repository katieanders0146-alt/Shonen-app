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
            openExercises("Sung Jin-Woo")
        }
        findViewById<Button>(R.id.saitamaButton).setOnClickListener {
            openExercises("Saitama")
        }
    }

    private fun openExercises(characterName: String) {
        val intent = Intent(this, ExerciseListActivity::class.java)
        intent.putExtra("character_name", characterName)
        startActivity(intent)
    }
}