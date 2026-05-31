package com.example.laundrycare_android

import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class CareActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_care)

        val btnBack = findViewById<ImageView>(R.id.btnBack)

        // 화면 안의 ◀ 버튼 클릭 시 뒤로가기
        btnBack.setOnClickListener {
            finish()
        }
    }

    // 스마트폰 기기의 물리적 뒤로가기 버튼을 눌렀을 때 강제 종료 및 이전 화면(홈) 이동
    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }
}