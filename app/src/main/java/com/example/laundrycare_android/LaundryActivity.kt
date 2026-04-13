package com.example.laundrycare_android

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class LaundryActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_laundry)

        val btnBack = findViewById<Button>(R.id.btnBack)
        btnBack.setOnClickListener {
            finish()
        }
    }
}