package com.example.laundrycare_android

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView

class WasherListActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 🌟 이 부분에서 activity_washer_list.xml 파일을 정상적으로 연결합니다.
        setContentView(R.layout.activity_washer_list)

        val btnBack = findViewById<Button>(R.id.btnWasherListBack)
        val btnAddWasher = findViewById<Button>(R.id.btnAddWasher)
        val rvWasherList = findViewById<RecyclerView>(R.id.rvWasherList)
        val tvEmptyWasher = findViewById<TextView>(R.id.tvEmptyWasher)

        btnBack.setOnClickListener { finish() }

        // 새 세탁기 등록 버튼 누르면 카메라(스캔) 화면으로 이동
        btnAddWasher.setOnClickListener {
            val intent = Intent(this, WasherCameraActivity::class.java)
            startActivity(intent)
        }
    }
}