package com.example.laundrycare_android

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity

class LaundryActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_laundry)

        val btnBack = findViewById<ImageView>(R.id.btnBack)
        btnBack.setOnClickListener {
            finish()
        }

        val btnTimeRecommend = findViewById<LinearLayout>(R.id.btnTimeRecommend)
        val btnManageWasher = findViewById<LinearLayout>(R.id.btnManageWasher)
        val btnOpenMultiSelect = findViewById<Button>(R.id.btnOpenMultiSelect)
        val btnLaundryHistory = findViewById<LinearLayout>(R.id.btnLaundryHistory)

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

        btnManageWasher.setOnClickListener {
            val intent = Intent(this, WasherListActivity::class.java)
            startActivity(intent)
        }

        btnOpenMultiSelect.setOnClickListener {
            val intent = Intent(this, ClothMultiSelectActivity::class.java)
            startActivity(intent)
        }

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