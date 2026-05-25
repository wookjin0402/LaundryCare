package com.example.laundrycare_android

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class MyPageActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_page)

        findViewById<Button>(R.id.btnMyPageBack).setOnClickListener { finish() }

        findViewById<LinearLayout>(R.id.btnGoEditInfo).setOnClickListener {
            startActivity(Intent(this, EditInfoActivity::class.java))
        }

        findViewById<LinearLayout>(R.id.btnLogout).setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            Toast.makeText(this, "로그아웃 되었습니다.", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
}