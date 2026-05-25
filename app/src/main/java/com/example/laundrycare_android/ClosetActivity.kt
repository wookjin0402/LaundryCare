package com.example.laundrycare_android

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class ClosetActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_closet)

        // ❌ 뒤로가기 버튼(btnBack) 관련 코드 삭제 완료

        // 상의
        findViewById<Button>(R.id.btnTopShort).setOnClickListener { showCategoryMessage("반팔") }
        findViewById<Button>(R.id.btnTopLong).setOnClickListener { showCategoryMessage("긴팔") }
        findViewById<Button>(R.id.btnTopOuter).setOnClickListener { showCategoryMessage("아우터") }

        // 하의
        findViewById<Button>(R.id.btnBottomShort).setOnClickListener { showCategoryMessage("반바지") }
        findViewById<Button>(R.id.btnBottomLong).setOnClickListener { showCategoryMessage("긴바지") }
        findViewById<Button>(R.id.btnBottomSkirt).setOnClickListener { showCategoryMessage("치마") }

        // 고급
        findViewById<Button>(R.id.btnLuxuryPremium).setOnClickListener { showCategoryMessage("명품") }
        findViewById<Button>(R.id.btnLuxuryFunctional).setOnClickListener { showCategoryMessage("기능성") }

        // 기타
        findViewById<Button>(R.id.btnEtcSocks).setOnClickListener { showCategoryMessage("양말") }
        findViewById<Button>(R.id.btnEtcUnderwear).setOnClickListener { showCategoryMessage("속옷") }
    }

    private fun showCategoryMessage(categoryName: String) {
        // 추후 카테고리별 목록 조회 화면 인텐트 연결 자리
    }
}