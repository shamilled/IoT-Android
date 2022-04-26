package com.example.test

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button

class MainActivity : AppCompatActivity() {
    private lateinit var changeViewer: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        changeViewer = findViewById(R.id.changeView)
        changeViewer.setOnClickListener {
            val intent = Intent(this, BLE::class.java)
            startActivity(intent)

        }

    }


}