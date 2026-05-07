package com.example.laundrycare_android

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide

class ResultActivity : AppCompatActivity() {

    // 사용자가 수정할 수 있도록 var(변수)로 선언합니다.
    private var currentCategory = "반팔"
    private var currentMaterial = "정보 없음"
    private var currentLaundryTip = "세탁 주의사항 없음"
    private var currentImageUrl = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)

        val ivResultPhoto = findViewById<ImageView>(R.id.ivResultPhoto)
        val tvMockResult = findViewById<TextView>(R.id.tvMockResult)
        val btnSave = findViewById<Button>(R.id.btnSave)
        val btnEdit = findViewById<Button>(R.id.btnEdit)

        // 이전 화면에서 넘어온 데이터 받기 (처음 한 번만)
        currentCategory = intent.getStringExtra("category") ?: "반팔"
        currentMaterial = intent.getStringExtra("material") ?: "정보 없음"
        currentLaundryTip = intent.getStringExtra("laundryTip") ?: "세탁 주의사항 없음"
        currentImageUrl = intent.getStringExtra("imageUrl") ?: ""
        val scanType = intent.getStringExtra("scanType") ?: ""

        // 화면에 글자를 띄워주는 기능 (수정 후에도 다시 불려야 해서 함수로 뺐습니다)
        fun updateResultText() {
            if (scanType == "MACHINE") {
                tvMockResult.text =
                    "[ 🧺 세탁기 스캔 결과 ]\n\n• 기기 종류: 드럼 세탁기\n• 브랜드: LG 트롬\n• 추천 코스: 울/섬세 코스 (찬물)"
            } else {
                tvMockResult.text = "카테고리: $currentCategory\n소재: $currentMaterial\n\n[세탁 팁]\n$currentLaundryTip"
            }
        }

        // 처음에 화면 세팅하기
        updateResultText()
        if (currentImageUrl.isNotEmpty()) {
            Glide.with(this).load(currentImageUrl).into(ivResultPhoto)
        }

        // 🌟 수정 버튼: 팝업창을 띄워서 카테고리와 소재를 고칠 수 있게 합니다!
        btnEdit.setOnClickListener {
            // 팝업창 안에 들어갈 입력칸(EditText) 만들기
            val layout = LinearLayout(this)
            layout.orientation = LinearLayout.VERTICAL
            layout.setPadding(50, 40, 50, 10)

            val etCategory = EditText(this)
            etCategory.hint = "카테고리 (예: 반팔, 긴바지, 명품)"
            etCategory.setText(currentCategory)

            val etMaterial = EditText(this)
            etMaterial.hint = "소재 (예: 면, 폴리에스터)"
            etMaterial.setText(currentMaterial)

            layout.addView(etCategory)
            layout.addView(etMaterial)

            // 팝업창 띄우기
            AlertDialog.Builder(this)
                .setTitle("스캔 결과 수정")
                .setView(layout)
                .setPositiveButton("수정 완료") { _, _ ->
                    // 사용자가 입력한 값으로 데이터 교체!
                    currentCategory = etCategory.text.toString()
                    currentMaterial = etMaterial.text.toString()

                    // 화면 글자 새로고침
                    updateResultText()
                    Toast.makeText(this, "수정되었습니다. 이제 저장해보세요!", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("취소", null)
                .show()
        }

        // 🌟 저장 버튼: 수정된 최신 데이터를 창고에 넣기!
        btnSave.setOnClickListener {
            val newItem = ClothingItem(
                imageUrl = currentImageUrl,
                category = currentCategory, // 수정한 카테고리가 들어갑니다!
                material = currentMaterial,
                laundryTip = currentLaundryTip
            )

            TempWardrobeDB.myClothes.add(newItem)

            Toast.makeText(this, "내 옷장($currentCategory)에 성공적으로 저장되었습니다.", Toast.LENGTH_SHORT).show()

            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                val intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                intent.putExtra("IS_SAVED", true)
                startActivity(intent)
                finish()
            }, 500)
        }
    }
}