package com.example.laundrycare_android

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity

class StainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stain)

        val btnBack = findViewById<Button>(R.id.btnBack)
        val btnScanStain = findViewById<Button>(R.id.btnScanStain)
        val btnShowResult = findViewById<Button>(R.id.btnShowResult)
        val etStainInfo = findViewById<EditText>(R.id.etStainInfo)

        btnBack.setOnClickListener {
            finish()
        }

        // 스캔 버튼 클릭 시
        btnScanStain.setOnClickListener {
            // 이제 옛날 화면(ScanActivity) 말고, 방금 만든 새 화면으로 이동!
            startActivity(Intent(this, StainCameraActivity::class.java))
        }

        // 결과 확인 버튼 클릭 시 (분석 결과 화면으로 이동)
        btnShowResult.setOnClickListener {
            val intent = Intent(this, StainResultActivity::class.java)
            // 입력한 정보를 결과 페이지로 전달해봅니다.
            intent.putExtra("additionalInfo", etStainInfo.text.toString())
            startActivity(intent)
        }
    }
}