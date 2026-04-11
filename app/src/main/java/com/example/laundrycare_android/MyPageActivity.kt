package com.example.laundrycare_android

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class MyPageActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_page)

        auth = FirebaseAuth.getInstance()

        // [기본 베이스] 뒤로 가기 로직
        val btnBack = findViewById<Button>(R.id.btnBack)
        btnBack.setOnClickListener {
            finish()
        }

        val btnEditInfo = findViewById<Button>(R.id.btnEditInfo)
        val btnNotifications = findViewById<Button>(R.id.btnNotifications)
        val btnSettings = findViewById<Button>(R.id.btnSettings)
        val btnLogout = findViewById<Button>(R.id.btnLogout)

        btnEditInfo.setOnClickListener {
            Toast.makeText(this, "개인정보 수정 화면으로 이동합니다.", Toast.LENGTH_SHORT).show()
        }

        btnNotifications.setOnClickListener {
            Toast.makeText(this, "알림 관리 화면으로 이동합니다.", Toast.LENGTH_SHORT).show()
        }

        btnSettings.setOnClickListener {
            Toast.makeText(this, "설정 화면으로 이동합니다.", Toast.LENGTH_SHORT).show()
        }

        btnLogout.setOnClickListener {
            auth.signOut()
            Toast.makeText(this, "로그아웃 되었습니다.", Toast.LENGTH_SHORT).show()

            val intent = Intent(this, ProfileActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
}