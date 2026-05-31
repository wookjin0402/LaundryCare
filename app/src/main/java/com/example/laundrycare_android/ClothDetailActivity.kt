package com.example.laundrycare_android

import android.content.Intent
import android.os.Bundle
import android.widget.*
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

    // 🌟 핵심: 수정 화면에서 '저장'을 누르고 뒤로 돌아왔을 때, 최신 데이터를 DB에서 즉시 다시 불러옵니다!
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

                    // 2.jpg 화면에 뿌려질 텍스트 단 하나도 빠짐없이 완벽 조립
                    val detailText = """
                        [ 옷 기본 정보 ]
                        계절: $season
                        분류: $main ($sub)
                        색상: $color
                        사이즈: $size
                        소재: $material
                        
                        [ 세탁 주의사항 ]
                        $warnings
                        
                        [ 관리 주의사항 ]
                        $careSteps
                        
                        [ AI 요약 팁 ]
                        $laundryTip
                    """.trimIndent()

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

        // 🌟 수정 버튼을 누르면 팝업창 대신 '새로운 넓은 수정 화면'으로 넘어갑니다.
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