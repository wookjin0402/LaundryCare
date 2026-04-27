package com.example.laundrycare_android

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class EditInfoActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_info)

        findViewById<Button>(R.id.btnBack).setOnClickListener { finish() }

        // 이메일 변경 화면으로 이동
        findViewById<Button>(R.id.btnGoChangeEmail).setOnClickListener {
            startActivity(Intent(this, ChangeEmailActivity::class.java))
        }

        // 비밀번호 변경 화면으로 이동
        findViewById<Button>(R.id.btnGoChangePassword).setOnClickListener {
            startActivity(Intent(this, ChangePasswordActivity::class.java))
        }
    }
}