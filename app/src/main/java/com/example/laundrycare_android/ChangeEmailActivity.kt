package com.example.laundrycare_android

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class ChangeEmailActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_change_email)

        val auth = FirebaseAuth.getInstance()
        val user = auth.currentUser

        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }

        findViewById<Button>(R.id.btnSendEmailVerify).setOnClickListener {
            val newEmail = findViewById<EditText>(R.id.etNewEmail).text.toString().trim()

            if (newEmail.isEmpty()) {
                Toast.makeText(this, "이메일을 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Firebase의 verifyBeforeUpdateEmail을 사용하여 새 이메일로 인증 메일을 보냅니다.
            user?.verifyBeforeUpdateEmail(newEmail)?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "인증 메일을 발송했습니다. 메일 확인 후 다시 로그인해주세요.", Toast.LENGTH_LONG).show()
                    auth.signOut() // 보안을 위해 로그아웃 처리
                    finish()
                } else {
                    Toast.makeText(this, "오류: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}