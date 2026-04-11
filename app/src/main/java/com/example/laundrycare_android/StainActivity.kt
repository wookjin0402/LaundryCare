package com.example.laundrycare_android

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class StainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stain)

        // [기본 베이스] 뒤로 가기 버튼 로직
        val btnBack = findViewById<Button>(R.id.btnBack)
        btnBack.setOnClickListener {
            finish() // 현재 화면을 닫고 이전 화면(메인 홈)으로 돌아감
        }

        // 얼룩 화면의 3가지 버튼들
        val btnStainGuide = findViewById<Button>(R.id.btnStainGuide)
        val btnStainScan = findViewById<Button>(R.id.btnStainScan)
        val btnStainCategory = findViewById<Button>(R.id.btnStainCategory)

        btnStainGuide.setOnClickListener {
            Toast.makeText(this, "얼룩 관리 가이드 화면으로 이동합니다.", Toast.LENGTH_SHORT).show()
        }

        btnStainScan.setOnClickListener {
            Toast.makeText(this, "비전 AI 얼룩 스캔 카메라를 켭니다.", Toast.LENGTH_SHORT).show()
        }

        btnStainCategory.setOnClickListener {
            Toast.makeText(this, "얼룩 카테고리 목록을 엽니다.", Toast.LENGTH_SHORT).show()
        }
    }
}