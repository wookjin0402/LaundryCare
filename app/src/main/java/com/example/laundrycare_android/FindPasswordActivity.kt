package com.example.laundrycare_android

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class FindPasswordActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_find_password)

        auth = FirebaseAuth.getInstance()

        val etFindEmail = findViewById<EditText>(R.id.etFindEmail)
        val btnSendResetEmail = findViewById<Button>(R.id.btnSendResetEmail)
        val btnBackToLogin = findViewById<Button>(R.id.btnBackToLogin)

        btnSendResetEmail.setOnClickListener {
            val emailAddress = etFindEmail.text.toString().trim()

            if (emailAddress.isEmpty()) {
                Toast.makeText(this, "이메일을 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 🌟 파이어베이스 비밀번호 재설정 이메일 발송
            auth.sendPasswordResetEmail(emailAddress)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "재설정 메일이 발송되었습니다. 메일함을 확인해주세요.", Toast.LENGTH_LONG).show()
                        finish() // 로그인 화면으로 복귀
                    } else {
                        Toast.makeText(this, "발송 실패: 등록된 이메일인지 확인해주세요.", Toast.LENGTH_SHORT).show()
                    }
                }
        }

        btnBackToLogin.setOnClickListener {
            finish()
        }
    }
}