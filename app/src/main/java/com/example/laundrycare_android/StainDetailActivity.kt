package com.example.laundrycare_android

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore

class StainDetailActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private var documentId: String? = null

    private lateinit var tvCategory: TextView
    private lateinit var tvDate: TextView
    private lateinit var tvGuide: TextView
    private lateinit var ivImage: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stain_detail)

        documentId = intent.getStringExtra("documentId")

        val btnBack = findViewById<ImageView>(R.id.btnStainDetailBack)
        val btnOptionsMenu = findViewById<TextView>(R.id.btnOptionsMenu)

        tvCategory = findViewById(R.id.tvStainCategory)
        tvDate = findViewById(R.id.tvStainDetailDate)
        tvGuide = findViewById(R.id.tvStainDetailGuide)
        ivImage = findViewById(R.id.ivStainDetailImage)

        btnBack.setOnClickListener { finish() }

        // 🌟 최적화 완료: '수정'과 '삭제' 기능이 모두 필요 없어졌으므로 더보기(⋮) 버튼을 아예 숨겨버립니다.
        btnOptionsMenu.visibility = View.GONE
    }

    override fun onResume() {
        super.onResume()
        if (!documentId.isNullOrEmpty()) {
            loadStainData()
        } else {
            Toast.makeText(this, "잘못된 접근입니다.", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun loadStainData() {
        db.collection("stains").document(documentId!!).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val stainType = document.getString("stainType") ?: "생활/기타"
                    val date = document.getString("date") ?: "날짜 미상"
                    val imageUrl = document.getString("imageUrl") ?: ""

                    tvCategory.text = stainType
                    tvDate.text = "등록일: $date"
                    tvGuide.text = getStainCareGuide(stainType)

                    if (imageUrl.isNotEmpty()) {
                        Glide.with(this).load(imageUrl).into(ivImage)
                    } else {
                        ivImage.setBackgroundColor(android.graphics.Color.parseColor("#E0E0E0"))
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "서버 연결 실패", Toast.LENGTH_SHORT).show()
            }
    }

    private fun getStainCareGuide(type: String): String {
        return when (type) {
            "음식물" -> "🍝 음식물 얼룩 가이드:\n주방세제를 바른 뒤 손으로 살살 비벼 애벌빨래 후 세탁하세요. 김치나 카레는 햇빛에 말리면 색이 옅어집니다."
            "화장품" -> "💄 화장품 얼룩 가이드:\n클렌징 오일이나 폼을 사용하여 얼룩 부위를 부드럽게 문질러 지운 뒤 미온수로 헹구세요."
            "생활/기타" -> "✨ 일반 오염 가이드:\n중성세제를 미온수에 풀어 애벌빨래를 진행한 후 표준 코스로 세탁하세요."
            else -> "✨ 일반 오염 가이드:\n중성세제를 미온수에 풀어 애벌빨래를 진행한 후 표준 코스로 세탁하세요."
        }
    }
}