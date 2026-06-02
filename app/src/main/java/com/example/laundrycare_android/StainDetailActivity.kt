package com.example.laundrycare_android

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore

class StainDetailActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stain_detail)

        val tvTitle = findViewById<TextView>(R.id.tvStainDetailTitle)
        val btnBack = findViewById<ImageView>(R.id.btnStainDetailBack)
        val ivImage = findViewById<ImageView>(R.id.ivStainDetailImage)

        // 🌟 수정됨: TextView에서 EditText로 뷰 찾기 변경
        val etType = findViewById<EditText>(R.id.etStainDetailType)
        val tvDate = findViewById<TextView>(R.id.tvStainDetailDate)
        val tvGuide = findViewById<TextView>(R.id.tvStainDetailGuide)
        val btnSave = findViewById<Button>(R.id.btnSaveStain)

        val documentId = intent.getStringExtra("documentId")
        val isEditMode = intent.getBooleanExtra("isEditMode", false)

        btnBack.setOnClickListener { finish() }

        // 🌟 수정 모드에 따른 화면 세팅
        if (isEditMode) {
            tvTitle.text = "얼룩 정보 수정하기"
            etType.isEnabled = true // 글자 수정 가능하게 활성화
            etType.setBackgroundResource(android.R.drawable.edit_text) // 입력창처럼 보이게 테두리 추가
            btnSave.visibility = View.VISIBLE // 저장 버튼 표시
            Toast.makeText(this, "수정 모드입니다. 얼룩 종류를 변경하세요.", Toast.LENGTH_SHORT).show()
        }

        if (!documentId.isNullOrEmpty()) {
            db.collection("stains").document(documentId).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val stainType = document.getString("stainType") ?: "알 수 없는 얼룩"
                        val date = document.getString("date") ?: "날짜 미상"
                        val imageUrl = document.getString("imageUrl") ?: ""

                        etType.setText(stainType)
                        tvDate.text = "등록일: $date"

                        if (imageUrl.isNotEmpty()) {
                            Glide.with(this).load(imageUrl).into(ivImage)
                        }

                        tvGuide.text = getStainCareGuide(stainType)
                    } else {
                        Toast.makeText(this, "데이터를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "서버 연결 실패", Toast.LENGTH_SHORT).show()
                    finish()
                }

            // 🌟 수정 사항을 파이어베이스에 저장하는 로직
            btnSave.setOnClickListener {
                val newStainType = etType.text.toString().trim()
                if (newStainType.isEmpty()) {
                    Toast.makeText(this, "얼룩 종류를 입력해주세요.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                btnSave.isEnabled = false // 중복 클릭 방지
                btnSave.text = "저장 중..."

                // 파이어베이스 데이터 업데이트
                db.collection("stains").document(documentId)
                    .update("stainType", newStainType)
                    .addOnSuccessListener {
                        Toast.makeText(this, "수정이 완료되었습니다.", Toast.LENGTH_SHORT).show()
                        finish() // 성공 시 목록 화면으로 돌아감
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "수정에 실패했습니다.", Toast.LENGTH_SHORT).show()
                        btnSave.isEnabled = true
                        btnSave.text = "수정 내용 저장하기"
                    }
            }
        } else {
            Toast.makeText(this, "잘못된 접근입니다.", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun getStainCareGuide(type: String): String {
        return when {
            type.contains("커피") -> "☕ 커피 얼룩 가이드:\n따뜻한 물과 주방세제(또는 식초)를 1:1로 섞어 얼룩 부위를 칫솔로 톡톡 두드린 후 세탁하세요."
            type.contains("김치") -> "🌶️ 김치 국물 가이드:\n주방세제를 바른 뒤 손으로 살살 비벼 애벌빨래 후, 양파즙을 활용하면 효과적입니다."
            type.contains("기름") -> "🍔 기름/생선 얼룩 가이드:\n베이킹소다를 뿌려 기름기를 흡착시킨 뒤, 주방세제를 묻혀 미온수로 문지르세요."
            type.contains("피") || type.contains("혈흔") -> "🩸 혈흔 가이드:\n절대 뜨거운 물 금지! '찬물'과 과산화수소를 사용하여 닦아내세요."
            else -> "✨ 일반 오염 가이드:\n중성세제를 미온수에 풀어 애벌빨래를 진행한 후 표준 코스로 세탁하세요."
        }
    }
}