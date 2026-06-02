package com.example.laundrycare_android

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore

class WasherDetailActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_washer_detail)

        val btnBack = findViewById<ImageView>(R.id.btnDetailBack)
        val tvDetailBrand = findViewById<TextView>(R.id.tvDetailBrand)
        val tvDetailModel = findViewById<TextView>(R.id.tvDetailModel)
        val tvDetailType = findViewById<TextView>(R.id.tvDetailType)
        val ivWasherPhoto = findViewById<ImageView>(R.id.ivWasherPhoto) // 🌟 사진 이미지뷰 연결

        btnBack.setOnClickListener { finish() }

        val documentId = intent.getStringExtra("documentId")

        if (documentId != null) {
            db.collection("washers").document(documentId).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val brand = document.getString("brand") ?: "알 수 없음"
                        val model = document.getString("model") ?: "모델명 없음"
                        val type = document.getString("type") ?: "알 수 없음"
                        val imageUrl = document.getString("imageUrl") ?: "" // 🌟 URL 파싱

                        tvDetailBrand.text = brand
                        tvDetailModel.text = if (model.isEmpty()) "등록된 모델명이 없습니다" else model
                        tvDetailType.text = type

                        // 🌟 Glide를 사용하여 이미지 띄우기
                        if (imageUrl.isNotEmpty()) {
                            Glide.with(this).load(imageUrl).into(ivWasherPhoto)
                        } else {
                            ivWasherPhoto.setBackgroundColor(android.graphics.Color.parseColor("#E0E0E0"))
                        }
                    } else {
                        Toast.makeText(this, "세탁기 정보를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "데이터를 불러오는 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(this, "잘못된 접근입니다.", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}