package com.example.laundrycare_android

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.ktx.auth

class SignupActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_signup)
        // 1. 파이어베이스 인증 객체 가져오기 (빨간줄 뜨면 Alt+Enter!)
        val auth = com.google.firebase.ktx.Firebase.auth

        /// 2. 화면 부품들 가져오기 (XML ID와 꼭 대조하세요!)
        val etName = findViewById<EditText>(R.id.etName)
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnSendVerify = findViewById<Button>(R.id.btnSendVerify) // 👈 새로 만든 '인증 발송' 버튼
        val btnSignup = findViewById<Button>(R.id.btnSignup) // 👈 최종 '회원가입 완료' 버튼

        // 3. [인증 발송] 버튼 클릭 시 로직
        btnSendVerify.setOnClickListener {
            val email = etEmail.text.toString()
            val password = etPassword.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                // 이메일 확인을 위해 먼저 계정을 임시 생성합니다.
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            // 생성 성공하면 바로 인증 메일 발송! 🚀
                            auth.currentUser?.sendEmailVerification()?.addOnCompleteListener { verifyTask ->
                                if (verifyTask.isSuccessful) {
                                    Toast.makeText(this, "인증 메일이 발송되었습니다! 네이버 메일을 확인하세요.", Toast.LENGTH_LONG).show()
                                }
                            }
                        } else {
                            Toast.makeText(this, "발송 실패: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
            } else {
                Toast.makeText(this, "이메일과 비밀번호를 먼저 입력해주세요.", Toast.LENGTH_SHORT).show()
            }
        }

        // 4. [회원가입 완료] 버튼 클릭 시 로직
        btnSignup.setOnClickListener {
            // 서버에 "이 사람 진짜 인증했나요?"라고 다시 물어봅니다. (중요!) 🔄
            auth.currentUser?.reload()?.addOnCompleteListener {
                if (auth.currentUser?.isEmailVerified == true) {
                    // ✅ 인증 성공!
                    Toast.makeText(this, "인증 확인 완료! 회원가입이 성공적으로 끝났습니다.", Toast.LENGTH_SHORT).show()
                    finish() // 로그인 화면으로 이동
                } else {
                    // ❌ 인증 안 됨
                    Toast.makeText(this, "아직 이메일 인증이 되지 않았습니다. 메일함의 링크를 클릭해주세요!", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}