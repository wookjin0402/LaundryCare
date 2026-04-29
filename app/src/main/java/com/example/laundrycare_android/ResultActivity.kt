package com.example.laundrycare_android

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide

class ResultActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)

        val ivResultPhoto = findViewById<ImageView>(R.id.ivResultPhoto)
        val tvMockResult = findViewById<TextView>(R.id.tvMockResult)
        val btnSave = findViewById<Button>(R.id.btnSave)
        val btnEdit = findViewById<Button>(R.id.btnEdit) // 조원 분이 추가하신 수정 버튼 연결

        // 이전 화면에서 넘어온 데이터 받기
        val category = intent.getStringExtra("category") ?: "미분류"
        val material = intent.getStringExtra("material") ?: "정보 없음"
        val laundryTip = intent.getStringExtra("laundryTip") ?: "세탁 주의사항 없음"
        val imageUrl = intent.getStringExtra("imageUrl") ?: ""
        val scanType = intent.getStringExtra("scanType") ?: "" // 팀원 분의 스캔 타입 데이터

        // 화면에 데이터 표시 (세탁기와 의류 스캔 결과 구분)
        if (scanType == "MACHINE") {
            tvMockResult.text =
                "[ 🧺 세탁기 스캔 결과 ]\n\n• 기기 종류: 드럼 세탁기\n• 브랜드: LG 트롬\n• 추천 코스: 울/섬세 코스 (찬물)"
        } else {
            // 왕만두님이 작성하신 동적 데이터 표시 로직 적용
            tvMockResult.text = "카테고리: $category\n소재: $material\n\n[세탁 팁]\n$laundryTip"

            // 왕만두님이 작성하신 사진 로딩 로직 적용
            if (imageUrl.isNotEmpty()) {
                Glide.with(this).load(imageUrl).into(ivResultPhoto)
            }
        }

        btnEdit.setOnClickListener {
            Toast.makeText(this, "수정 화면 팝업이 뜰 예정입니다.", Toast.LENGTH_SHORT).show()
        }

        // 조원 분이 작성하신 고도화된 저장 버튼 로직 적용
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