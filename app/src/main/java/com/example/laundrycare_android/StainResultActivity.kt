package com.example.laundrycare_android

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class StainResultActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stain_result)

        val btnBack = findViewById<Button>(R.id.btnBack)
        val btnFinish = findViewById<Button>(R.id.btnFinish)
        val tvSolution = findViewById<TextView>(R.id.tvSolution)

        // 이전 화면에서 보낸 추가 정보가 있다면 받아볼 수 있습니다 (나중에 활용)
        val info = intent.getStringExtra("additionalInfo")

        btnBack.setOnClickListener {
            finish()
        }

        btnFinish.setOnClickListener {
            // 메인으로 돌아가거나 현재 결과창 닫기
            finish()
        }
    }
}