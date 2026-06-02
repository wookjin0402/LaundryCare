package com.example.laundrycare_android

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class LaundryActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_laundry)

        val btnBack = findViewById<ImageView>(R.id.btnBack)
        btnBack.setOnClickListener {
            finish()
        }

        // 기존 조원분 로직 버튼들
        val btnLaundryBatch = findViewById<LinearLayout>(R.id.btnLaundryBatch)
        val btnTimeRecommend = findViewById<LinearLayout>(R.id.btnTimeRecommend)
        // 🌟 btnCourseGuide는 XML에서 삭제했으므로 여기서도 제거

        // 내 세탁기 관리 버튼
        val btnManageWasher = findViewById<LinearLayout>(R.id.btnManageWasher)

        // 🌟 새롭게 추가할 2개의 버튼 찾아오기
        val btnOpenMultiSelect = findViewById<Button>(R.id.btnOpenMultiSelect)
        val btnLaundryHistory = findViewById<LinearLayout>(R.id.btnLaundryHistory)

        // 1. 세탁 묶음 자동 분류
        btnLaundryBatch.setOnClickListener {
            val intent = Intent(this, ClothMultiSelectActivity::class.java)
            startActivity(intent)
        }

        // 2. 세탁 및 건조 시점 추천
        btnTimeRecommend.setOnClickListener {
            val conditionList = arrayOf("땀을 많이 흘렸어요", "커피/음식물 얼룩이 묻었어요", "잠깐 입어서 깨끗해요")

            AlertDialog.Builder(this)
                .setTitle("현재 빨랫감의 상태는 어떤가요?")
                .setSingleChoiceItems(conditionList, -1) { dialog, which ->
                    val resultMessage = when(which) {
                        0 -> "🌤 현재 맑고 건조함\n➡️ 당장 '전체 세탁' 후 '자연 건조'를 추천합니다!"
                        1 -> "🚨 오염 감지\n➡️ 얼룩이 굳기 전에 '부분 세척' 후 전체 세탁하세요."
                        2 -> "💨 가벼운 착용\n➡️ 세탁기 대신 '스타일러(환기) 후 보관'을 추천합니다."
                        else -> ""
                    }
                    dialog.dismiss()

                    AlertDialog.Builder(this)
                        .setTitle("AI 맞춤 추천")
                        .setMessage(resultMessage)
                        .setPositiveButton("확인", null)
                        .show()
                }
                .setNegativeButton("취소", null)
                .show()
        }

        // 🌟 3. [삭제됨] 기기 맞춤 세탁 코스 가이드 로직 삭제

        // 4. 내 세탁기 관리 버튼 클릭 이벤트
        btnManageWasher.setOnClickListener {
            val intent = Intent(this, WasherListActivity::class.java)
            startActivity(intent)
        }

        // 🌟 5. [추가] 오늘 빨래할 옷 바구니에 담기
        btnOpenMultiSelect.setOnClickListener {
            val intent = Intent(this, ClothMultiSelectActivity::class.java)
            startActivity(intent)
        }

        // 🌟 6. [추가] 나의 세탁 기록 히스토리
        btnLaundryHistory.setOnClickListener {
            val intent = Intent(this, LaundryHistoryActivity::class.java)
            startActivity(intent)
        }
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }
}