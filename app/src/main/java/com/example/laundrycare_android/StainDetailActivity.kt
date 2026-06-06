package com.example.laundrycare_android

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore

class StainDetailActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()

    // 🌟 카테고리 배열 정의
    private val categories = arrayOf("음식물", "화장품", "생활/기타")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stain_detail)

        val tvTitle = findViewById<TextView>(R.id.tvStainDetailTitle)
        val btnBack = findViewById<ImageView>(R.id.btnStainDetailBack)
        val ivImage = findViewById<ImageView>(R.id.ivStainDetailImage)

        // 🌟 수정됨: EditText 대신 Spinner로 변경
        val spinnerCategory = findViewById<Spinner>(R.id.spinnerStainCategory)
        val tvDate = findViewById<TextView>(R.id.tvStainDetailDate)
        val tvGuide = findViewById<TextView>(R.id.tvStainDetailGuide)
        val btnSave = findViewById<Button>(R.id.btnSaveStain)

        val documentId = intent.getStringExtra("documentId")
        val isEditMode = intent.getBooleanExtra("isEditMode", false)

        btnBack.setOnClickListener { finish() }

        // 🌟 스피너에 배열(카테고리) 데이터 세팅
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categories)
        spinnerCategory.adapter = adapter

        // 수정 모드 세팅
        if (isEditMode) {
            tvTitle.text = "얼룩 정보 수정하기"
            spinnerCategory.isEnabled = true // 드롭다운 열어서 고를 수 있게 활성화
            btnSave.visibility = View.VISIBLE
            Toast.makeText(this, "수정 모드입니다. 카테고리를 변경하세요.", Toast.LENGTH_SHORT).show()
        }

        if (!documentId.isNullOrEmpty()) {
            db.collection("stains").document(documentId).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val stainType = document.getString("stainType") ?: "생활/기타"
                        val date = document.getString("date") ?: "날짜 미상"
                        val imageUrl = document.getString("imageUrl") ?: ""

                        // 🌟 파이어베이스에서 가져온 값이 스피너의 몇 번째 항목인지 찾아서 보여주기
                        val spinnerPosition = adapter.getPosition(stainType)
                        if (spinnerPosition >= 0) {
                            spinnerCategory.setSelection(spinnerPosition)
                        }

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

            // 저장 버튼 로직
            btnSave.setOnClickListener {
                // 🌟 스피너에서 현재 사용자가 선택한 글자를 가져옴
                val newStainType = spinnerCategory.selectedItem.toString()

                btnSave.isEnabled = false
                btnSave.text = "저장 중..."

                // 파이어베이스 데이터 업데이트
                db.collection("stains").document(documentId)
                    .update("stainType", newStainType)
                    .addOnSuccessListener {
                        Toast.makeText(this, "수정이 완료되었습니다.", Toast.LENGTH_SHORT).show()
                        finish()
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
            type == "음식물" -> "🍝 음식물 얼룩 가이드:\n주방세제를 바른 뒤 손으로 살살 비벼 애벌빨래 후 세탁하세요. 김치나 카레는 햇빛에 말리면 색이 옅어집니다."
            type == "화장품" -> "💄 화장품 얼룩 가이드:\n클렌징 오일이나 폼을 사용하여 얼룩 부위를 부드럽게 문질러 지운 뒤 미온수로 헹구세요."
            type == "생활/기타" -> "✨ 일반 오염 가이드:\n중성세제를 미온수에 풀어 애벌빨래를 진행한 후 표준 코스로 세탁하세요."
            else -> "✨ 일반 오염 가이드:\n중성세제를 미온수에 풀어 애벌빨래를 진행한 후 표준 코스로 세탁하세요."
        }
    }
}