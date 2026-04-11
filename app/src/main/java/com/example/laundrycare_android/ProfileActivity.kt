package com.example.laundrycare_android

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class ProfileActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private var isPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        auth = Firebase.auth

        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnToggleVisibility = findViewById<Button>(R.id.btnToggleVisibility)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val btnGoResetPassword = findViewById<Button>(R.id.btnGoResetPassword)
        val btnGoSignUp = findViewById<Button>(R.id.btnGoSignUp)

        // 비밀번호 표시/숨김
        btnToggleVisibility.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            etPassword.inputType = if (isPasswordVisible) InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD else InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            btnToggleVisibility.text = if (isPasswordVisible) "숨김" else "표시"
            etPassword.setSelection(etPassword.text.length)
        }

        // 1. 로그인 버튼 로직
        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val pass = etPassword.text.toString().trim()

            // 팝업 조건 1: 이메일 빈칸
            if (email.isEmpty()) {
                Toast.makeText(this, "이메일을 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 팝업 조건 2: 비밀번호 빈칸
            if (pass.isEmpty()) {
                Toast.makeText(this, "비밀번호를 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 파이어베이스 로그인 시도
            auth.signInWithEmailAndPassword(email, pass).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    // (옵션) 가입 시 이메일 인증을 필수로 했으므로 여기서도 확인
                    if (user != null && user.isEmailVerified) {
                        Toast.makeText(this, "로그인 성공", Toast.LENGTH_SHORT).show()

                        // 로그인 성공 시 메인 화면으로 이동 (MainActivity가 메인화면이라고 가정)
                        finish() // 뒤로가기 했을 때 로그인 화면이 다시 안 나오게 종료
                    } else {
                        Toast.makeText(this, "이메일 인증을 완료해주세요.", Toast.LENGTH_SHORT).show()
                        auth.signOut()
                    }
                } else {
                    // 팝업 조건 3: 정보 틀림
                    Toast.makeText(this, "이메일이나 비밀번호가 맞지 않아요 .\n다시 입력해주세요.", Toast.LENGTH_LONG).show()
                }
            }
        }

        // 2. 비밀번호 찾기 화면으로 이동
        btnGoResetPassword.setOnClickListener {
            startActivity(Intent(this, ResetPasswordActivity::class.java))
        }

        // 3. 회원가입 화면으로 이동
        btnGoSignUp.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }
    }
}