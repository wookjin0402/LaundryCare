package com.example.laundrycare_android

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ClothDetailActivity : AppCompatActivity() {
    private lateinit var docId: String
    private lateinit var tvDetailContent: TextView
    private lateinit var ivClothPhoto: ImageView
    private lateinit var btnDiscard: Button // 🌟 버리기 버튼 추가

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cloth_detail)

        ivClothPhoto = findViewById(R.id.ivClothPhoto)
        tvDetailContent = findViewById(R.id.tvDetailContent)
        val btnBack = findViewById<ImageView>(R.id.btnBack)
        val btnOptionsMenu = findViewById<TextView>(R.id.btnOptionsMenu)
        btnDiscard = findViewById(R.id.btnDiscard) // 🌟 XML에 추가한 버튼 ID와 일치해야 함

        docId = intent.getStringExtra("docId") ?: ""

        // 🌟 '옷장 다이어트'에서 넘어왔는지 확인
        val isFromDiet = intent.getBooleanExtra("IS_FROM_DIET", false)

        // 🌟 다이어트에서 온 경우에만 버튼 활성화
        if (isFromDiet) {
            btnDiscard.visibility = View.VISIBLE
            btnDiscard.setOnClickListener {
                showDiscardPopup()
            }
        } else {
            btnDiscard.visibility = View.GONE
        }

        btnBack.setOnClickListener { finish() }
        btnOptionsMenu.setOnClickListener { showBottomSheet() }
    }

    // 🌟 삭제 및 추천 팝업 로직
    private fun showDiscardPopup() {
        AlertDialog.Builder(this)
            .setTitle("의류 버리기")
            .setMessage("정말 삭제하시겠습니까?")
            .setPositiveButton("삭제") { _, _ ->
                FirebaseFirestore.getInstance().collection("clothes").document(docId).delete()
                    .addOnSuccessListener {
                        // 🌟 삭제 후 추천 팝업 띄우기
                        AlertDialog.Builder(this)
                            .setTitle("알림")
                            .setMessage("가까운 의류 수거함에 버리거나 중고거래를 추천드려요!")
                            .setPositiveButton("확인") { _, _ -> finish() }
                            .show()
                    }
            }
            .setNegativeButton("취소", null)
            .show()
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

                    var dateRaw = document.getString("date") ?: ""
                    if (dateRaw.isEmpty()) {
                        val timestamp = document.getLong("timestamp") ?: 0L
                        if (timestamp > 0L) {
                            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.KOREA)
                            dateRaw = sdf.format(Date(timestamp))
                        } else {
                            dateRaw = "미입력"
                        }
                    }

                    val displayDate = if (dateRaw.length >= 10) {
                        val y = dateRaw.substring(0, 4)
                        val m = dateRaw.substring(5, 7)
                        val d = dateRaw.substring(8, 10)
                        "${y}년 ${m}월 ${d}일"
                    } else {
                        dateRaw
                    }

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