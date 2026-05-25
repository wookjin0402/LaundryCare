package com.example.laundrycare_android

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class EditInfoActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_info)

        auth = FirebaseAuth.getInstance()

        findViewById<Button>(R.id.btnEditInfoBack).setOnClickListener { finish() }

        // 아이디 변경 버튼
        findViewById<Button>(R.id.btnChangeId).setOnClickListener {
            checkEmailVerification {
                showChangeEmailDialog()
            }
        }

        // 비밀번호 변경 버튼
        findViewById<Button>(R.id.btnChangePw).setOnClickListener {
            checkEmailVerification {
                sendPasswordResetEmail()
            }
        }

        // 계정 삭제 버튼
        findViewById<Button>(R.id.btnDeleteAccount).setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("🚨 회원 탈퇴 확인")
                .setMessage("정말로 계정을 삭제하시겠습니까?\n삭제된 계정과 데이터는 복구할 수 없습니다.")
                .setPositiveButton("확인") { _, _ -> deleteFirebaseAccount() }
                .setNegativeButton("취소", null)
                .show()
        }
    }

    // 🌟 이메일 인증 여부 확인 로직 (핵심 요구사항)
    private fun checkEmailVerification(onVerified: () -> Unit) {
        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(this, "로그인 정보가 없습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        // 최신 상태로 갱신 후 확인
        user.reload().addOnCompleteListener {
            if (user.isEmailVerified) {
                // 인증된 유저만 다음 로직 실행
                onVerified()
            } else {
                // 인증 안 된 유저는 거부하고 인증 메일 발송
                user.sendEmailVerification().addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "이메일 인증이 필요합니다.\n가입하신 이메일로 인증 메일을 발송했습니다.", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this, "인증 메일 발송에 실패했습니다. 다시 시도해주세요.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun showChangeEmailDialog() {
        val editText = EditText(this)
        editText.hint = "새로운 이메일(아이디) 입력"

        AlertDialog.Builder(this)
            .setTitle("아이디 변경")
            .setMessage("변경할 새로운 이메일 주소를 입력해주세요.\n변경 후 해당 이메일로 다시 인증해야 합니다.")
            .setView(editText)
            .setPositiveButton("변경") { _, _ ->
                val newEmail = editText.text.toString().trim()
                if (newEmail.isNotEmpty()) {
                    val user = auth.currentUser
                    user?.verifyBeforeUpdateEmail(newEmail)?.addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this, "새 이메일로 확인 메일이 발송되었습니다.\n확인 후 아이디가 변경됩니다.", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(this, "변경 실패. 재로그인이 필요할 수 있습니다.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun sendPasswordResetEmail() {
        val email = auth.currentUser?.email
        if (email != null) {
            auth.sendPasswordResetEmail(email).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "비밀번호 재설정 메일을 발송했습니다.\n이메일을 확인해주세요.", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, "메일 발송 실패", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun deleteFirebaseAccount() {
        val user = auth.currentUser
        user?.delete()?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(this, "계정이 정상적으로 삭제되었습니다.", Toast.LENGTH_LONG).show()
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            } else {
                Toast.makeText(this, "보안상의 이유로 재로그인 후 탈퇴가 가능합니다.", Toast.LENGTH_LONG).show()
            }
        }
    }
}