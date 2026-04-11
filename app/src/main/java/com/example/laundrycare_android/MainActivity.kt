package com.example.laundrycare_android

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        auth = Firebase.auth

        // 🚨 [문지기 로직] 로그인이 안 되어 있거나 이메일 인증이 안 된 유저라면?
        val currentUser = auth.currentUser
        if (currentUser == null || !currentUser.isEmailVerified) {
            // 메인 화면을 보여주지 않고 즉시 로그인(ProfileActivity) 화면으로 쫓아냄!
            startActivity(Intent(this, ProfileActivity::class.java))
            finish()
            return // 아래 코드는 실행하지 않고 여기서 멈춤
        }

        // --- 여기서부터는 정상 로그인된 사람만 볼 수 있는 진짜 메인 화면 로직 ---

        // 방금 만든 새 디자인 부품들 찾아오기 (이전 btnGoProfile은 지웠습니다!)
        val btnScan = findViewById<Button>(R.id.btnScan)
        val btnCategoryMenu = findViewById<TextView>(R.id.btnCategoryMenu)
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)

        // 1. 거대 SCAN 버튼 클릭 시
        btnScan.setOnClickListener {
            Toast.makeText(this, "비전 AI 카메라 스캔 준비 중...", Toast.LENGTH_SHORT).show()
        }

        // 2. 우측 상단 햄버거 메뉴 클릭 시
        btnCategoryMenu.setOnClickListener {
            startActivity(Intent(this, MyPageActivity::class.java))
        }

        // 3. 하단바 5개 버튼 클릭 시 동작 설정
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                // 홈 버튼을 누르면 메인 화면 최상단으로 스크롤하거나 새로고침 하는 등의 동작 (현재는 이미 홈이므로 유지)
                R.id.nav_home -> Toast.makeText(this, "현재 홈 화면입니다.", Toast.LENGTH_SHORT).show()

                // 얼룩 버튼을 누르면 얼룩 화면으로 이동!
                R.id.nav_stain -> startActivity(android.content.Intent(this, StainActivity::class.java))

                R.id.nav_closet -> Toast.makeText(this, "옷장 화면", Toast.LENGTH_SHORT).show()
                R.id.nav_care -> Toast.makeText(this, "의류관리 화면", Toast.LENGTH_SHORT).show()
                R.id.nav_laundry -> Toast.makeText(this, "세탁 신청 화면", Toast.LENGTH_SHORT).show()
            }
            true
        }
    }
}