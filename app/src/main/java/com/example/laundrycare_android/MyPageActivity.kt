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

        val tvUserEmail = findViewById<TextView>(R.id.tvUserEmail)
        if (currentUser != null && currentUser.email != null) {
            tvUserEmail.text = currentUser.email
        } else {
            tvUserEmail.text = "로그인 정보 없음"
        }

        val btnBack = findViewById<Button>(R.id.btnBack)
        btnBack.setOnClickListener {
            finish()
        }

        val btnEditInfo = findViewById<Button>(R.id.btnEditInfo)
        val btnNotifications = findViewById<Button>(R.id.btnNotifications)
        val btnSettings = findViewById<Button>(R.id.btnSettings)
        val btnLogout = findViewById<Button>(R.id.btnLogout)

        btnEditInfo.setOnClickListener {
            startActivity(Intent(this, EditInfoActivity::class.java))
        }

        btnNotifications.setOnClickListener {
            startActivity(Intent(this, NotificationsActivity::class.java))
        }

        btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        // 🌟 로그아웃 로직 수정: 로그아웃하면 확실하게 "LoginActivity"로 돌아가도록 변경!
        btnLogout.setOnClickListener {
            auth.signOut()
            Toast.makeText(this, "로그아웃 되었습니다.", Toast.LENGTH_SHORT).show()

            val intent = Intent(this, LoginActivity::class.java) // <--- 여기가 핵심입니다.
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
}