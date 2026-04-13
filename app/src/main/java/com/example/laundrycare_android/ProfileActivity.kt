package com.example.laundrycare_android

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class ProfileActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private var isPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        // 🌟 [수정 핵심] 에러를 유발하는 KTX 확장 기능 대신,
        // 절대 튕기지 않는 파이어베이스 정석 초기화 방식으로 변경했습니다! 🌟
        auth = FirebaseAuth.getInstance()

        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnToggleVisibility = findViewById<Button>(R.id.btnToggleVisibility)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val btnGoResetPassword = findViewById<Button>(R.id.btnGoResetPassword)
        val btnGoSignUp = findViewById<Button>(R.id.btnGoSignUp)

        // 비밀번호 표시/숨김 (문법 충돌 방지를 위해 명확하게 분리)
        btnToggleVisibility.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            if (isPasswordVisible) {
                etPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                btnToggleVisibility.text = "숨김"
            } else {
                etPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                btnToggleVisibility.text = "표시"
            }
            etPassword.setSelection(etPassword.text.length)
        }

        // 1. 로그인 버튼 로직
        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val pass = etPassword.text.toString().trim()

            if (email.isEmpty()) {
                Toast.makeText(this, "이메일을 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (pass.isEmpty()) {
                Toast.makeText(this, "비밀번호를 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.signInWithEmailAndPassword(email, pass).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "로그인 성공!", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this, "이메일이나 비밀번호가 맞지 않아요.\n다시 입력해주세요.", Toast.LENGTH_LONG).show()
                }
            }
        }

        // 2. 비밀번호 찾기 화면으로 이동
        btnGoResetPassword.setOnClickListener {
            startActivity(Intent(this, ResetPasswordActivity::class.java))
        }

        // 3. 회원가입 화면으로 이동
        btnGoSignUp.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java)) // 👈 대문자 U를 소문자 u로 변경!
        }
    }
}