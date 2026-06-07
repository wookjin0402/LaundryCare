package com.example.laundrycare_android

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialog
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
        val btnOptionsMenu = findViewById<TextView>(R.id.btnOptionsMenu) // 🌟 '⋮' 버튼 연결

        tvCategory = findViewById(R.id.tvStainCategory)
        tvDate = findViewById(R.id.tvStainDetailDate)
        tvGuide = findViewById(R.id.tvStainDetailGuide)
        ivImage = findViewById(R.id.ivStainDetailImage)

        btnBack.setOnClickListener { finish() }

        // 🌟 상단 '⋮' 버튼 터치 시 바텀시트 띄우기
        btnOptionsMenu.setOnClickListener {
            showBottomSheet()
        }
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

    private fun showBottomSheet() {
        val view = layoutInflater.inflate(R.layout.layout_bottom_sheet, null)
        val dialog = BottomSheetDialog(this)
        dialog.setContentView(view)

        // 🌟 수정 누르기 -> Edit 화면으로 이동
        view.findViewById<TextView>(R.id.tvEdit).setOnClickListener {
            dialog.dismiss()
            val intent = Intent(this, StainEditActivity::class.java)
            intent.putExtra("documentId", documentId)
            startActivity(intent)
        }

        // 🌟 삭제 누르기
        view.findViewById<TextView>(R.id.tvDelete).setOnClickListener {
            db.collection("stains").document(documentId!!).delete()
                .addOnSuccessListener {
                    Toast.makeText(this, "얼룩 정보가 삭제되었습니다.", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                    finish()
                }
        }

        view.findViewById<TextView>(R.id.tvCancel).setOnClickListener { dialog.dismiss() }
        dialog.show()
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