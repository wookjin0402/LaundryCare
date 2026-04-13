package com.example.laundrycare_android

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class MyPageActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_page)

        auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser

        // [추가됨] 이메일을 표시할 TextView 찾기 및 이메일 세팅
        val tvUserEmail = findViewById<TextView>(R.id.tvUserEmail)
        if (currentUser != null && currentUser.email != null) {
            tvUserEmail.text = currentUser.email
        } else {
            tvUserEmail.text = "로그인 정보 없음"
        }

        // 뒤로 가기
        val btnBack = findViewById<Button>(R.id.btnBack)
        btnBack.setOnClickListener {
            finish()
        }

        val btnEditInfo = findViewById<Button>(R.id.btnEditInfo)
        val btnNotifications = findViewById<Button>(R.id.btnNotifications)
        val btnSettings = findViewById<Button>(R.id.btnSettings)
        val btnLogout = findViewById<Button>(R.id.btnLogout)

        // 1. 개인정보 변경 화면으로 이동
        btnEditInfo.setOnClickListener {
            startActivity(Intent(this, EditInfoActivity::class.java))
        }

        // 2. 알림 관리 화면으로 이동
        btnNotifications.setOnClickListener {
            startActivity(Intent(this, NotificationsActivity::class.java))
        }

        // 3. 설정 화면으로 이동
        btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        // 4. 로그아웃 로직
        btnLogout.setOnClickListener {
            auth.signOut()
            Toast.makeText(this, "로그아웃 되었습니다.", Toast.LENGTH_SHORT).show()

            val intent = Intent(this, ProfileActivity::class.java)
            // 백그라운드에 쌓인 모든 화면을 지우고 로그인 화면을 새로 띄움
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
}