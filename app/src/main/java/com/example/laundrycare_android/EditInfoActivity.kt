package com.example.laundrycare_android

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth

class EditInfoActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_info)

        auth = FirebaseAuth.getInstance()

        // 뒤로가기 버튼 연결 (ImageView)
        findViewById<ImageView>(R.id.btnEditInfoBack).setOnClickListener { finish() }

        // 1. 아이디(이메일) 변경 전용 화면으로 이동
        findViewById<Button>(R.id.btnChangeId).setOnClickListener {
            checkEmailVerification {
                val intent = Intent(this, ChangeEmailActivity::class.java)
                startActivity(intent)
            }
        }

        // 2. 비밀번호 변경 전용 화면으로 이동
        findViewById<Button>(R.id.btnChangePw).setOnClickListener {
            checkEmailVerification {
                val intent = Intent(this, ChangePasswordActivity::class.java)
                startActivity(intent)
            }
        }

        // 3. 계정 삭제(회원 탈퇴) 버튼
        findViewById<Button>(R.id.btnDeleteAccount).setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("🚨 회원 탈퇴 확인")
                .setMessage("정말로 계정을 삭제하시겠습니까?\n삭제된 계정과 데이터는 복구할 수 없습니다.")
                .setPositiveButton("확인") { _, _ -> deleteFirebaseAccount() }
                .setNegativeButton("취소", null)
                .show()
        }
    }

    // 이메일 인증 여부 확인 로직
    private fun checkEmailVerification(onVerified: () -> Unit) {
        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(this, "로그인 정보가 없습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        user.reload().addOnCompleteListener {
            if (user.isEmailVerified) {
                onVerified()
            } else {
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

    // 계정 영구 삭제 로직 (Firebase Auth 탈퇴)
    private fun deleteFirebaseAccount() {
        val user = auth.currentUser ?: return

        user.delete().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(this, "계정이 정상적으로 삭제되었습니다.", Toast.LENGTH_LONG).show()
                navigateToLogin()
            } else {
                // 파이어베이스 보안 정책상 로그인한 지 오래된 경우 탈퇴가 막힘 -> 비밀번호 재인증 유도
                showReauthDialogForDeletion()
            }
        }
    }

    // 탈퇴 전 보안 재인증 다이얼로그 (비밀번호 입력)
    private fun showReauthDialogForDeletion() {
        val editText = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            hint = "현재 비밀번호 입력"
        }

        AlertDialog.Builder(this)
            .setTitle("보안 재인증")
            .setMessage("안전한 탈퇴를 위해 현재 비밀번호를 다시 한 번 입력해주세요.")
            .setView(editText)
            .setPositiveButton("확인") { _, _ ->
                val password = editText.text.toString().trim()
                if (password.isNotEmpty()) {
                    val user = auth.currentUser
                    val credential = EmailAuthProvider.getCredential(user?.email ?: "", password)

                    // 재인증 시도
                    user?.reauthenticate(credential)?.addOnCompleteListener { reauthTask ->
                        if (reauthTask.isSuccessful) {
                            // 재인증 성공 시 즉시 계정 삭제 실행
                            user.delete().addOnCompleteListener { deleteTask ->
                                if (deleteTask.isSuccessful) {
                                    Toast.makeText(this, "계정이 정상적으로 삭제되었습니다. 이용해 주셔서 감사합니다.", Toast.LENGTH_LONG).show()
                                    navigateToLogin()
                                } else {
                                    Toast.makeText(this, "삭제 실패: ${deleteTask.exception?.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } else {
                            Toast.makeText(this, "비밀번호가 올바르지 않습니다.", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Toast.makeText(this, "비밀번호를 입력해야 탈퇴가 가능합니다.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("취소", null)
            .show()
    }

    // 로그인 화면으로 튕기며 스택 초기화
    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}