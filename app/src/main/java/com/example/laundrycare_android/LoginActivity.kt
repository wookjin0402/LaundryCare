package com.example.laundrycare_android

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.ktx.auth

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        val auth = com.google.firebase.ktx.Firebase.auth

        // 🌟 [자동 로그인 체크] 🌟
        if (auth.currentUser != null) {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

        // 1. 버튼들 찾아오기
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val btnFindPassword = findViewById<Button>(R.id.btnFindPassword) // 🌟 추가됨
        val btnGoToSignup = findViewById<Button>(R.id.btnGoToSignup)
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)

        // 2. [로그인 버튼] 눌렀을 때의 동작
        btnLogin.setOnClickListener {
            val email = etEmail.text.toString()
            val password = etPassword.text.toString().trim()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                auth.signInWithEmailAndPassword(email, password).addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        if (user != null) {
                            // 🌟 추가됨: 이메일 인증을 완료한 유저만 로그인 허용
                            if (user.isEmailVerified) {
                                Toast.makeText(this, "로그인 성공!", Toast.LENGTH_SHORT).show()
                                val intent = Intent(this, MainActivity::class.java)
                                startActivity(intent)
                                finish()
                            } else {
                                Toast.makeText(this, "이메일 인증이 완료되지 않았습니다. 메일함을 확인해주세요.", Toast.LENGTH_LONG).show()
                                auth.signOut() // 인증 안됐으면 즉시 로그아웃 시켜서 앱 진입 차단
                            }
                        }
                    } else {
                        Toast.makeText(this, "로그인 실패: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(this, "이메일과 비밀번호를 입력해주세요.", Toast.LENGTH_SHORT).show()
            }
        }

        // 🌟 추가됨: [비밀번호 찾기 버튼] 눌렀을 때의 동작
        btnFindPassword.setOnClickListener {
            val intent = Intent(this, FindPasswordActivity::class.java)
            startActivity(intent)
        }

        // 3. [회원가입 버튼] 눌렀을 때의 동작
        btnGoToSignup.setOnClickListener {
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
        }
    }
}