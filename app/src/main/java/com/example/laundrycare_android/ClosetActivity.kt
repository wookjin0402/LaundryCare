package com.example.laundrycare_android

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class ClosetActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_closet)

        // 🌟 1. 화면 안의 ◀ 버튼 클릭 시 뒤로가기
        val btnBack = findViewById<Button>(R.id.btnBack)
        btnBack.setOnClickListener {
            finish()
        }

        // 🌟 2. 각 옷장 카테고리 버튼 연결 (세분화)
        // 상의
        findViewById<Button>(R.id.btnTopShort).setOnClickListener {
            showCategoryMessage("반팔")
        }
        findViewById<Button>(R.id.btnTopLong).setOnClickListener {
            showCategoryMessage("긴팔")
        }
        findViewById<Button>(R.id.btnTopOuter).setOnClickListener {
            showCategoryMessage("아우터")
        }

        // 하의
        findViewById<Button>(R.id.btnBottomShort).setOnClickListener {
            showCategoryMessage("반바지")
        }
        findViewById<Button>(R.id.btnBottomLong).setOnClickListener {
            showCategoryMessage("긴바지")
        }
        findViewById<Button>(R.id.btnBottomSkirt).setOnClickListener {
            showCategoryMessage("치마")
        }

        // 고급
        findViewById<Button>(R.id.btnLuxuryPremium).setOnClickListener {
            showCategoryMessage("명품")
        }
        findViewById<Button>(R.id.btnLuxuryFunctional).setOnClickListener {
            showCategoryMessage("기능성")
        }

        // 기타
        findViewById<Button>(R.id.btnEtcSocks).setOnClickListener {
            showCategoryMessage("양말")
        }
        findViewById<Button>(R.id.btnEtcUnderwear).setOnClickListener {
            showCategoryMessage("속옷")
        }
    }

    // 스마트폰 기기의 물리적 뒤로가기 버튼 처리
    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }

    // 버튼을 눌렀을 때 임시로 안내 문구를 띄워주는 함수 (추후 목록 화면으로 이동하도록 수정 가능)
    private fun showCategoryMessage(categoryName: String) {
        Toast.makeText(this, "${categoryName} 카테고리를 선택하셨습니다.", Toast.LENGTH_SHORT).show()

        // 💡 나중에 조원들과 "옷 목록 화면"을 만들면 이 아래에 코드를 넣으면 됩니다.
        // val intent = Intent(this, 목록화면이름::class.java)
        // intent.putExtra("category", categoryName)
        // startActivity(intent)
    }
}