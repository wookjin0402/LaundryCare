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
        // 앱이 켜질 때 이미 로그인된 유저가 있다면? 바로 메인 화면으로 패스!
        if (auth.currentUser != null) {
            // 👇 여기를 CameraActivity에서 MainActivity로 바꿨습니다!
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish() // 로그인 화면은 꺼버리기
            return // 아래 코드들 실행 안 하고 여기서 끝냄
        }

        // 1. 버튼들 찾아오기
        val btnLogin = findViewById<Button>(R.id.btnLogin) // 로그인 버튼
        val btnGoToSignup = findViewById<Button>(R.id.btnGoToSignup) // 회원가입 이동 버튼
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
                            Toast.makeText(this, "로그인 성공!", Toast.LENGTH_SHORT).show()
                            // 👇 여기도 CameraActivity에서 MainActivity로 바꿨습니다!
                            val intent = Intent(this, MainActivity::class.java)
                            startActivity(intent)
                            finish()
                        }
                    } else {
                        Toast.makeText(this, "로그인 실패: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(this, "이메일과 비밀번호를 입력해주세요.", Toast.LENGTH_SHORT).show()
            }
        }

        // 3. [회원가입 버튼] 눌렀을 때의 동작
        btnGoToSignup.setOnClickListener {
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
        }
    }
}