package com.example.laundrycare_android

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide

class ResultActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)

        val ivResultPhoto = findViewById<ImageView>(R.id.ivResultPhoto)
        val tvMockResult = findViewById<TextView>(R.id.tvMockResult)
        val btnSave = findViewById<Button>(R.id.btnSave)

        // 이전 화면에서 넘어온 데이터 받기
        val category = intent.getStringExtra("category") ?: "미분류"
        val material = intent.getStringExtra("material") ?: "정보 없음"
        val laundryTip = intent.getStringExtra("laundryTip") ?: "세탁 주의사항 없음"
        val imageUrl = intent.getStringExtra("imageUrl") ?: ""

        // 화면에 데이터 표시
        tvMockResult.text = "카테고리: $category\n소재: $material\n\n[세탁 팁]\n$laundryTip"

        if (imageUrl.isNotEmpty()) {
            Glide.with(this).load(imageUrl).into(ivResultPhoto)
        }

        btnSave.setOnClickListener {
            // 저장 로직 (필요 시 구현)
            finish()
        }
    }
}