package com.example.laundrycare_android

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.auth.FirebaseAuth // 🌟 유저 UID를 가져오기 위해 추가
import com.google.firebase.firestore.FirebaseFirestore

class WasherDetailActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private var documentId: String? = null

    // 🌟 로그인한 유저의 고유 UID를 가져오는 변수 추가
    private val myUid get() = FirebaseAuth.getInstance().currentUser?.uid ?: "unknown_user"

    private lateinit var tvDetailBrand: TextView
    private lateinit var tvDetailModel: TextView
    private lateinit var tvDetailType: TextView
    private lateinit var ivWasherPhoto: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_washer_detail)

        val btnBack = findViewById<ImageView>(R.id.btnDetailBack)
        val btnOptionsMenu = findViewById<TextView>(R.id.btnOptionsMenu)

        tvDetailBrand = findViewById(R.id.tvDetailBrand)
        tvDetailModel = findViewById(R.id.tvDetailModel)
        tvDetailType = findViewById(R.id.tvDetailType)
        ivWasherPhoto = findViewById(R.id.ivWasherPhoto)

        documentId = intent.getStringExtra("documentId")

        btnBack.setOnClickListener { finish() }

        btnOptionsMenu.setOnClickListener {
            showBottomSheet()
        }
    }

    override fun onResume() {
        super.onResume()
        if (documentId != null) {
            loadWasherData()
        } else {
            Toast.makeText(this, "잘못된 접근입니다.", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun loadWasherData() {
        // 🌟 수정: 공용 'washers' 창고가 아닌 '내 개인 창고(users/myUid/washers)'에서 데이터 찾기
        db.collection("users").document(myUid).collection("washers").document(documentId!!).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val brand = document.getString("brand") ?: "알 수 없음"
                    val model = document.getString("model") ?: "모델명 없음"
                    val type = document.getString("type") ?: "알 수 없음"
                    val imageUrl = document.getString("imageUrl") ?: ""

                    tvDetailBrand.text = brand
                    tvDetailModel.text = if (model.isEmpty()) "등록된 모델명이 없습니다" else model
                    tvDetailType.text = type

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
    }

    private fun showBottomSheet() {
        val view = layoutInflater.inflate(R.layout.layout_bottom_sheet, null)
        val dialog = BottomSheetDialog(this)
        dialog.setContentView(view)

        view.findViewById<TextView>(R.id.tvEdit).setOnClickListener {
            dialog.dismiss()
            val intent = Intent(this, WasherEditActivity::class.java)
            intent.putExtra("documentId", documentId)
            startActivity(intent)
        }

        view.findViewById<TextView>(R.id.tvDelete).setOnClickListener {
            if (documentId != null) {
                // 🌟 수정: 삭제할 때도 '내 개인 창고'의 경로를 명시하여 삭제
                db.collection("users").document(myUid).collection("washers").document(documentId!!).delete()
                    .addOnSuccessListener {
                        Toast.makeText(this, "세탁기가 삭제되었습니다.", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                        finish()
                    }
            }
        }

        view.findViewById<TextView>(R.id.tvCancel).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }
}