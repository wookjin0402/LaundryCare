package com.example.laundrycare_android

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LaundryHistoryDetailActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_laundry_history_detail)

        // XML의 ID와 정확히 매칭
        val btnBack = findViewById<ImageView>(R.id.btnBack)
        val tvDetail = findViewById<TextView>(R.id.tvLaundryDetail)

        btnBack.setOnClickListener { finish() }

        // 데이터 받기
        val washerInfo = intent.getStringExtra("washerInfo") ?: "정보 없음"
        val course = intent.getStringExtra("course") ?: "정보 없음"
        val warning = intent.getStringExtra("warning") ?: "경고 없음"
        val timestamp = intent.getLongExtra("timestamp", 0L)

        val sdf = SimpleDateFormat("yyyy년 MM월 dd일 HH:mm", Locale.KOREA)
        val dateString = if (timestamp != 0L) sdf.format(Date(timestamp)) else "날짜 오류"

        // 텍스트 설정
        tvDetail.text = """
            [세탁 일시]
            $dateString
            
            [세탁기 정보]
            $washerInfo
            
            [추천 세탁 코스]
            $course
            
            [주의사항]
            $warning
        """.trimIndent()
    }
}