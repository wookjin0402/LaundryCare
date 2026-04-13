package com.example.laundrycare_android

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth

class ChangePasswordActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_change_password)

        auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser

        val btnBack = findViewById<Button>(R.id.btnBack)
        val etCurrentPassword = findViewById<TextInputEditText>(R.id.etCurrentPassword)
        val etNewPassword = findViewById<TextInputEditText>(R.id.etNewPassword)
        val etNewPasswordConfirm = findViewById<TextInputEditText>(R.id.etNewPasswordConfirm)
        val btnChangePassword = findViewById<Button>(R.id.btnChangePassword)

        // 뒤로 가기
        btnBack.setOnClickListener {
            finish()
        }

        // 비밀번호 변경 버튼 클릭 시
        btnChangePassword.setOnClickListener {
            val currentPass = etCurrentPassword.text.toString().trim()
            val newPass = etNewPassword.text.toString().trim()
            val confirmPass = etNewPasswordConfirm.text.toString().trim()

            // 1. 빈칸 검사
            if (currentPass.isEmpty() || newPass.isEmpty() || confirmPass.isEmpty()) {
                Toast.makeText(this, "모든 칸을 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 2. 새 비밀번호 일치 검사
            if (newPass != confirmPass) {
                Toast.makeText(this, "새 비밀번호가 서로 일치하지 않습니다.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 3. 비밀번호 길이 검사 (파이어베이스는 최소 6자리 요구)
            if (newPass.length < 6) {
                Toast.makeText(this, "새 비밀번호는 6자리 이상이어야 합니다.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (currentUser != null && currentUser.email != null) {
                // 4. 현재 비밀번호로 보안 재인증
                val credential = EmailAuthProvider.getCredential(currentUser.email!!, currentPass)

                currentUser.reauthenticate(credential).addOnCompleteListener { reauthTask ->
                    if (reauthTask.isSuccessful) {

                        // 5. 재인증 성공 시 새 비밀번호로 업데이트
                        currentUser.updatePassword(newPass).addOnCompleteListener { updateTask ->
                            if (updateTask.isSuccessful) {
                                Toast.makeText(this, "비밀번호가 성공적으로 변경되었습니다.", Toast.LENGTH_SHORT).show()
                                finish() // 성공 후 이전 화면으로 돌아가기
                            } else {
                                Toast.makeText(this, "변경 실패: ${updateTask.exception?.message}", Toast.LENGTH_SHORT).show()
                            }
                        }

                    } else {
                        Toast.makeText(this, "현재 비밀번호가 틀렸습니다.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}