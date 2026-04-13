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

        // 로그인 안 된 유저는 로그인 화면으로 보내기 (이메일 인증 조건 삭제 완료)
        val currentUser = auth.currentUser
        if (currentUser == null) {
            startActivity(Intent(this, ProfileActivity::class.java))
            finish()
            return
        }

        val btnScan = findViewById<Button>(R.id.btnScan)
        val btnCategoryMenu = findViewById<TextView>(R.id.btnCategoryMenu)
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)

        // 1. 거대 SCAN 버튼 클릭 시 스캔 화면으로 이동
        btnScan.setOnClickListener {
            startActivity(Intent(this, ScanActivity::class.java))
        }

        // 2. 우측 상단 햄버거 메뉴 클릭 시 마이페이지로 이동
        btnCategoryMenu.setOnClickListener {
            startActivity(Intent(this, MyPageActivity::class.java))
        }

        // 3. 하단바 5개 버튼 클릭 시 각각의 화면으로 이동
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> Toast.makeText(this, "현재 홈 화면입니다.", Toast.LENGTH_SHORT).show()
                R.id.nav_stain -> startActivity(Intent(this, StainActivity::class.java))
                R.id.nav_closet -> startActivity(Intent(this, ClosetActivity::class.java))
                R.id.nav_care -> startActivity(Intent(this, CareActivity::class.java))
                R.id.nav_laundry -> startActivity(Intent(this, LaundryActivity::class.java))
            }
            true
        }
    }
}