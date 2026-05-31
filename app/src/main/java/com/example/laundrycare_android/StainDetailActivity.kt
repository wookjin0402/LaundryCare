package com.example.laundrycare_android

import android.os.Bundle
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

        val btnBack = findViewById<ImageView>(R.id.btnStainDetailBack)
        val ivImage = findViewById<ImageView>(R.id.ivStainDetailImage)
        val tvType = findViewById<TextView>(R.id.tvStainDetailType)
        val tvDate = findViewById<TextView>(R.id.tvStainDetailDate)
        val tvGuide = findViewById<TextView>(R.id.tvStainDetailGuide)

        val documentId = intent.getStringExtra("documentId")

        btnBack.setOnClickListener {
            finish()
        }

        if (documentId != null) {
            db.collection("stains").document(documentId).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val stainType = document.getString("stainType") ?: "알 수 없는 얼룩"
                        val date = document.getString("date") ?: "정보 없음"
                        val imageUrl = document.getString("imageUrl") ?: ""

                        tvType.text = stainType
                        tvDate.text = date

                        // 이미지 로드
                        if (imageUrl.isNotEmpty()) {
                            Glide.with(this).load(imageUrl).into(ivImage)
                        }

                        // 얼룩 종류별 맞춤형 세탁 팁 매칭
                        tvGuide.text = getStainCareGuide(stainType)

                    } else {
                        Toast.makeText(this, "얼룩 기록을 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "데이터 로딩 실패", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(this, "잘못된 접근입니다.", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    /**
     * 얼룩 종류별 최적의 초동 조치 방법을 반환하는 함수 (심사용 텍스트)
     */
    private fun getStainCareGuide(type: String): String {
        return when {
            type.contains("커피") -> "☕ 커피 얼룩 가이드:\n일반 세제를 사용하면 얼룩이 고착될 수 있습니다. 따뜻한 물과 주방세제(또는 식초)를 1:1로 섞어 얼룩 부위를 칫솔로 톡톡 두드린 후 세탁기에 돌리시는 것을 추천합니다."
            type.contains("김치") -> "🌶️ 김치 국물 가이드:\n주방세제를 얼룩 안팎에 바른 뒤 손으로 살살 비벼 1차 애벌빨래를 해주십시오. 락스 대용으로 양파즙을 발라두었다가 하루 뒤 세탁하면 흔적이 깔끔하게 제거됩니다."
            type.contains("기름") -> "🍔 기름/생선 얼룩 가이드:\n기름 성분은 일반 세탁으로 잘 빠지지 않습니다. 베이킹소다를 얼룩 위에 뿌려 기름기를 흡착시킨 뒤, 주방세제를 묻혀 미온수로 문지른 후 세탁하십시오."
            type.contains("피") || type.contains("혈흔") -> "🩸 혈흔 가이드:\n절대 뜨거운 물을 사용하지 마십시오. 단백질 성분이 응고되어 고착됩니다. 반드시 '찬물'을 사용하고, 과산화수소를 살짝 묻혀 거품이 일어날 때 닦아내면 효과적입니다."
            else -> "✨ 일반 오염 가이드:\n오염 물질이 섬유 안으로 완전히 흡수되기 전 중성세제를 미온수에 풀어 애벌빨래를 진행한 후 기기 맞춤 표준 코스로 세탁하시는 것을 권장합니다."
        }
    }
}