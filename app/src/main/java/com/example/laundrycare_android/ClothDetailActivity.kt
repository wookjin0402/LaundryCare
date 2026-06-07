package com.example.laundrycare_android

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.firestore.FirebaseFirestore

class ClothDetailActivity : AppCompatActivity() {
    private lateinit var docId: String
    private lateinit var tvDetailContent: TextView
    private lateinit var ivClothPhoto: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cloth_detail)

        ivClothPhoto = findViewById(R.id.ivClothPhoto)
        tvDetailContent = findViewById(R.id.tvDetailContent)
        val btnBack = findViewById<ImageView>(R.id.btnBack)
        val btnOptionsMenu = findViewById<TextView>(R.id.btnOptionsMenu)

        docId = intent.getStringExtra("docId") ?: ""

        btnBack.setOnClickListener { finish() }
        btnOptionsMenu.setOnClickListener { showBottomSheet() }
    }

    override fun onResume() {
        super.onResume()
        if (docId.isNotEmpty()) {
            fetchClothDataFromDB()
        }
    }

    private fun fetchClothDataFromDB() {
        FirebaseFirestore.getInstance().collection("clothes").document(docId).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val season = document.getString("season") ?: ""
                    val main = document.getString("mainCategory") ?: ""
                    val sub = document.getString("subCategory") ?: ""
                    val color = document.getString("color") ?: "미입력"
                    val size = document.getString("size") ?: "미입력"
                    val material = document.getString("material") ?: "미입력"
                    val warnings = document.getString("warnings") ?: "특이사항 없음"
                    val careSteps = document.getString("careSteps") ?: "관리 정보 없음"
                    val laundryTip = document.getString("laundryTip") ?: "AI 분석 결과를 확인하세요."
                    val imageUrl = document.getString("imageUrl") ?: ""

                    // 등록 날짜 파싱 (YYYY-MM-DD -> YYYY년 MM월 DD일)
                    val dateRaw = document.getString("date") ?: "미입력"
                    val displayDate = if (dateRaw.length >= 10) {
                        val y = dateRaw.substring(0, 4)
                        val m = dateRaw.substring(5, 7)
                        val d = dateRaw.substring(8, 10)
                        "${y}년 ${m}월 ${d}일"
                    } else {
                        dateRaw
                    }

                    // 🌟 trimMargin("|") 적용: 각 줄 앞에 |를 붙이면 들여쓰기 상관없이 깔끔하게 출력됩니다.
                    val detailText = """
                        |[ 옷 기본 정보 ]
                        |등록일: $displayDate
                        |계절: $season
                        |분류: $main ($sub)
                        |색상: $color
                        |사이즈: $size
                        |소재: $material
                        |
                        |[ 세탁 주의사항 ]
                        |$warnings
                        |
                        |[ 관리 주의사항 ]
                        |$careSteps
                        |
                        |[ AI 요약 팁 ]
                        |$laundryTip
                    """.trimMargin("|")

                    tvDetailContent.text = detailText

                    if (imageUrl.isNotEmpty()) {
                        Glide.with(this).load(imageUrl).centerCrop().into(ivClothPhoto)
                    }
                }
            }
    }

    private fun showBottomSheet() {
        val view = layoutInflater.inflate(R.layout.layout_bottom_sheet, null)
        val dialog = BottomSheetDialog(this)
        dialog.setContentView(view)

        view.findViewById<TextView>(R.id.tvEdit).setOnClickListener {
            dialog.dismiss()
            val intent = Intent(this, ClothEditActivity::class.java)
            intent.putExtra("docId", docId)
            startActivity(intent)
        }

        view.findViewById<TextView>(R.id.tvDelete).setOnClickListener {
            FirebaseFirestore.getInstance().collection("clothes").document(docId).delete()
                .addOnSuccessListener { finish() }
        }
        view.findViewById<TextView>(R.id.tvCancel).setOnClickListener { dialog.dismiss() }
        dialog.show()
    }
}