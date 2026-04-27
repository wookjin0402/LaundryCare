package com.example.laundrycare_android

import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class ResetPasswordActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reset_password)

        auth = Firebase.auth

        val etResetEmail = findViewById<EditText>(R.id.etResetEmail)
        val btnSendResetLink = findViewById<Button>(R.id.btnSendResetLink)

        btnSendResetLink.setOnClickListener {
            val email = etResetEmail.text.toString().trim()

            if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "정확한 이메일 주소를 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.sendPasswordResetEmail(email).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "비밀번호 재설정 메일을 보냈습니다.", Toast.LENGTH_LONG).show()
                    finish() // 메일 발송 후 다시 로그인 화면으로 돌아감
                } else {
                    Toast.makeText(this, "등록되지 않은 이메일이거나 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}