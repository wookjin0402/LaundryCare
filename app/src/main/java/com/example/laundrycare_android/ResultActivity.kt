package com.example.laundrycare_android

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
            tvMockResult.text = "[ 🧺 세탁기 스캔 결과 ]\n\n• 기기 종류: 드럼 세탁기\n• 브랜드: LG 트롬\n• 추천 코스: 울/섬세 코스 (찬물)"
        } else {
            tvMockResult.text = "[ 👕 의류 스캔 결과 ]\n\n• 카테고리: 상의 (맨투맨)\n• 혼용률: 면 100%\n• 세탁법: 30도 중성세제, 기계건조 금지"
        }

        btnEdit.setOnClickListener {
            Toast.makeText(this, "수정 화면 팝업이 뜰 예정입니다.", Toast.LENGTH_SHORT).show()
        }

        // 🌟 [우리가 놓쳤던 진짜 핵심!] 저장 버튼 누르면 창고에 데이터 쏙 넣기 🌟
        btnSave.setOnClickListener {
            // 1. 저장할 데이터 뭉치 만들기
            val newItem = if (scanType == "MACHINE") {
                ClothingItem("세탁기", "드럼세탁기", "울/섬세 코스 권장")
            } else {
                ClothingItem("상의", "면 100%", "30도 찬물 세탁")
            }

            // 2. 창고에 넣기 (이제 옷장에 보일 겁니다!)
            ClothingRepository.addItem(newItem)

            Toast.makeText(this, "옷장에 저장되었습니다!", Toast.LENGTH_SHORT).show()
            finish() // 현재 창 닫고 메인으로 돌아가기
        }
    }
}