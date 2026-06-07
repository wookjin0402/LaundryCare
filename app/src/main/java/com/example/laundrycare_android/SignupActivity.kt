package com.example.laundrycare_android

import android.os.Bundle
import android.text.InputType
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class SignUpActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private var isPassVisible = false
    private var isPassConfirmVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        auth = Firebase.auth

        val etEmail = findViewById<EditText>(R.id.etSignUpEmail)
        val etPass = findViewById<EditText>(R.id.etSignUpPassword)
        val etPassConfirm = findViewById<EditText>(R.id.etSignUpPasswordConfirm)

        val btnVerify = findViewById<Button>(R.id.btnSendVerification)
        val btnComplete = findViewById<Button>(R.id.btnCompleteSignUp)
        val btnTogglePass = findViewById<Button>(R.id.btnTogglePass)
        val btnTogglePassConfirm = findViewById<Button>(R.id.btnTogglePassConfirm)

        btnTogglePass.setOnClickListener {
            isPassVisible = !isPassVisible
            etPass.inputType = if (isPassVisible) InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD else InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            btnTogglePass.text = if (isPassVisible) "숨김" else "표시"
            etPass.setSelection(etPass.text.length)
        }

        btnTogglePassConfirm.setOnClickListener {
            isPassConfirmVisible = !isPassConfirmVisible
            etPassConfirm.inputType = if (isPassConfirmVisible) InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD else InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            btnTogglePassConfirm.text = if (isPassConfirmVisible) "숨김" else "표시"
            etPassConfirm.setSelection(etPassConfirm.text.length)
        }

        btnVerify.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val pass = etPass.text.toString().trim()

            if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "이메일 형식에 맞게 입력해주십시오.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val checkResult = checkPasswordValid(pass)
            if (checkResult != "통과") {
                Toast.makeText(this, checkResult, Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            auth.createUserWithEmailAndPassword(email, pass).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    auth.currentUser?.sendEmailVerification()?.addOnCompleteListener { emailTask ->
                        if (emailTask.isSuccessful) {
                            Toast.makeText(this, "인증메일이 오는데 1~2분 정도 소요될 수 있습니다.", Toast.LENGTH_LONG).show()
                        }
                    }
                } else {
                    Toast.makeText(this, "이미 가입된 이메일이거나 오류입니다.", Toast.LENGTH_SHORT).show()
                }
            }
        }

        btnComplete.setOnClickListener {
            val pass = etPass.text.toString().trim()
            val passConfirm = etPassConfirm.text.toString().trim()

            val checkResult = checkPasswordValid(pass)
            if (checkResult != "통과") {
                Toast.makeText(this, checkResult, Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            if (pass != passConfirm) {
                Toast.makeText(this, "비밀번호가 서로 일치하지 않습니다.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val user = auth.currentUser
            if (user != null) {
                user.reload().addOnCompleteListener {
                    if (user.isEmailVerified) {
                        Toast.makeText(this, "인증이 완료되었습니다.", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Toast.makeText(this, "이메일 인증을 하지 않으면 계정을 생성할 수 없습니다.", Toast.LENGTH_LONG).show()
                    }
                }
            } else {
                Toast.makeText(this, "이메일 인증을 먼저 진행해 주십시오.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkPasswordValid(password: String): String {
        if (password.length !in 8..20) return "비밀번호는 8자 이상, 20자 이하로 설정해 주세요."
        if (!password.any { it.isLetter() }) return "비밀번호에는 영문자가 꼭 포함되어야 합니다."
        if (!password.any { it.isDigit() }) return "비밀번호에는 숫자가 꼭 포함되어야 합니다."
        val specialCharRegex = "[!@#\$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]+".toRegex()
        if (!password.contains(specialCharRegex)) return "비밀번호에는 특수문자가 꼭 포함되어야 합니다."
        return "통과"
    }
}