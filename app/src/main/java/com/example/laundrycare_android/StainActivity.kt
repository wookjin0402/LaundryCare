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

        // 🌟 이미 완벽하게 들어가 있는 뒤로가기 기능! (현재 화면을 닫고 이전 리스트 화면으로 돌아갑니다)
        btnBack.setOnClickListener {
            finish()
        }

        // 스캔 버튼 클릭 시 (팀원이 만드는 스캔 화면으로 연결)
        btnScanStain.setOnClickListener {
            startActivity(Intent(this, ScanActivity::class.java))
        }

        // 결과 확인 버튼 클릭 시 (분석 결과 화면으로 이동)
        btnShowResult.setOnClickListener {
            // 💡 참고: 여기서 이동하는 곳은 'StainResultActivity' 입니다.
            // 아까 우리가 '수정 팝업 띄우고 옷장에 저장'하는 기능을 만든 곳은 'ResultActivity'이니,
            // 만약 여기서도 그 옷장 저장 기능을 쓰고 싶으시다면 이 부분을 ResultActivity::class.java 로 바꿔주시면 됩니다!
            val intent = Intent(this, StainResultActivity::class.java)
            // 입력한 정보를 결과 페이지로 전달해봅니다.
            intent.putExtra("additionalInfo", etStainInfo.text.toString())
            startActivity(intent)
        }
    }

    // 스마트폰 기기 자체의 물리적/제스처 뒤로가기를 했을 때도 똑같이 작동하도록 처리
    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }
}