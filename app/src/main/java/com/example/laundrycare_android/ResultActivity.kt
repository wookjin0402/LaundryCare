package com.example.laundrycare_android

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class ResultActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)

        val tvMockResult = findViewById<TextView>(R.id.tvMockResult)
        val btnEdit = findViewById<Button>(R.id.btnEdit)
        val btnSave = findViewById<Button>(R.id.btnSave)

        // 📦 카메라 화면에서 토스해준 택배 뜯기
        val scanType = intent.getStringExtra("scanType")

        // 택배 내용물에 따라 가짜 AI 결과 다르게 보여주기
        if (scanType == "MACHINE") {
            tvMockResult.text =
                "[ 🧺 세탁기 스캔 결과 ]\n\n• 기기 종류: 드럼 세탁기\n• 브랜드: LG 트롬\n• 추천 코스: 울/섬세 코스 (찬물)"
        } else {
            tvMockResult.text =
                "[ 👕 의류 스캔 결과 ]\n\n• 카테고리: 하의(자동 분류됨)\n• 혼용률: 면 100%\n• 세탁법: 30도 물세탁, 다림질(중온) 가능, 표백 금지"
        }

        btnEdit.setOnClickListener {
            Toast.makeText(this, "수정 화면 팝업이 뜰 예정입니다.", Toast.LENGTH_SHORT).show()
        }

        // ResultActivity.kt
        btnSave.setOnClickListener {
            Toast.makeText(this, "내 옷장에 성공적으로 저장되었습니다.", Toast.LENGTH_SHORT).show()

            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                val intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP

                // 🌟 "저장했다"는 신호를 true로 담아서 보냅니다.
                intent.putExtra("IS_SAVED", true)

                startActivity(intent)
                finish()
            }, 500)
        }
    }
}