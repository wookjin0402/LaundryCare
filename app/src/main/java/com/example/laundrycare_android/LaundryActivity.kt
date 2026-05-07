package com.example.laundrycare_android

import android.app.AlertDialog
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class LaundryActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 🌟 아까 우리가 예쁘게 만든 3개 버튼 화면으로 교체!
        setContentView(R.layout.fragment_laundry)

        // 🌟 왕만두님이 만드신 뒤로가기 로직 (살림)
        val btnBack = findViewById<Button>(R.id.btnBack)
        btnBack.setOnClickListener {
            finish()
        }

        // 🌟 조원분이 만드신 3가지 세탁 추천 로직 (살림)
        val btnLaundryBatch = findViewById<LinearLayout>(R.id.btnLaundryBatch)
        val btnTimeRecommend = findViewById<LinearLayout>(R.id.btnTimeRecommend)
        val btnCourseGuide = findViewById<LinearLayout>(R.id.btnCourseGuide)

        // 1. 세탁 묶음 자동 분류
        btnLaundryBatch.setOnClickListener {
            val clothesList = arrayOf("흰색 면 티셔츠", "검은색 데님 바지", "빨간색 수건", "울 니트")
            val checkedItems = booleanArrayOf(false, false, false, false)

            AlertDialog.Builder(this)
                .setTitle("오늘 세탁할 옷을 선택해주세요 (세탁 바구니)")
                .setMultiChoiceItems(clothesList, checkedItems) { _, which, isChecked ->
                    checkedItems[which] = isChecked
                }
                .setPositiveButton("분류 시작") { _, _ ->
                    AlertDialog.Builder(this)
                        .setTitle("분류 결과")
                        .setMessage("✅ 그룹 1 (일반): 흰색 면 티셔츠\n" +
                                "🚨 단독 세탁: 검은색 데님 바지 (이염 주의!)\n" +
                                "❌ 세탁 불가: 울 니트 (드라이클리닝 권장)")
                        .setPositiveButton("확인", null)
                        .show()
                }
                .setNegativeButton("취소", null)
                .show()
        }

        // 2. 세탁 및 건조 시점 추천
        btnTimeRecommend.setOnClickListener {
            val conditionList = arrayOf("땀을 많이 흘렸어요", "커피/음식물 얼룩이 묻었어요", "잠깐 입어서 깨끗해요")

            AlertDialog.Builder(this)
                .setTitle("현재 빨랫감의 상태는 어떤가요?")
                .setSingleChoiceItems(conditionList, -1) { dialog, which ->
                    val resultMessage = when(which) {
                        0 -> "🌤 현재 맑고 건조함\n➡️ 당장 '전체 세탁' 후 '자연 건조'를 추천합니다! (건조기 절감액: 약 350원)"
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

        // 3. 세탁 코스 가이드
        btnCourseGuide.setOnClickListener {
            val machineList = arrayOf("등록된 기기: LG 트롬 F21VDD", "등록된 기기: 삼성 비스포크 그랑데")

            AlertDialog.Builder(this)
                .setTitle("사용할 세탁기를 선택해주세요")
                .setItems(machineList) { _, which ->
                    val machineName = if(which == 0) "LG 트롬" else "삼성 비스포크"

                    AlertDialog.Builder(this)
                        .setTitle("최적 코스 안내")
                        .setMessage("기기: $machineName\n\n" +
                                "옷감 손상을 막기 위해 해당 기기의\n" +
                                "👉 [울/섬세 코스 + 냉수(20도) + 약한 탈수]\n" +
                                "설정을 권장합니다.")
                        .setPositiveButton("세탁기로 전송") { _, _ ->
                            Toast.makeText(this, "세탁기에 코스를 전송했습니다 (시연용)", Toast.LENGTH_SHORT).show()
                        }
                        .show()
                }
                .show()
        }
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }
}