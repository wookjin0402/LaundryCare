package com.example.laundrycare_android

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class StainActivity : AppCompatActivity() {

    private lateinit var rvStains: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stain)

        val btnBack = findViewById<Button>(R.id.btnBack)

        // 🌟 XML에 만든 [+] 버튼 가져오기
        val btnScanStain = findViewById<View>(R.id.btnScanStain)

        btnBack.setOnClickListener {
            finish()
        }

        // 🌟 [+] 버튼 누르면 묻지도 따지지도 않고 바로 스캔 화면으로 이동!
        btnScanStain.setOnClickListener {
            val intent = Intent(this, StainCameraActivity::class.java)
            startActivity(intent)
        }

        // 화면에 띄울 리스트(RecyclerView) 뼈대 세팅
        rvStains = findViewById(R.id.rvStains)
        rvStains.layoutManager = LinearLayoutManager(this)

        loadStainData()
    }

    private fun loadStainData() {
        val db = FirebaseFirestore.getInstance()

        // 파이어베이스 'stains' 컬렉션에서 데이터 가져오기 대기
        db.collection("stains")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null || snapshots == null) return@addSnapshotListener

                // 나중에 여기에 리스트에 데이터를 넣는 로직이 들어갑니다.
            }
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }
}