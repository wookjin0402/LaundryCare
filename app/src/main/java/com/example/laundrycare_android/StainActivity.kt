package com.example.laundrycare_android

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
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

        val btnBack = findViewById<ImageView>(R.id.btnBack)
        val btnScanStain = findViewById<View>(R.id.btnScanStain)

        btnBack.setOnClickListener {
            finish()
        }

        btnScanStain.setOnClickListener {
            val intent = Intent(this, StainCameraActivity::class.java)
            startActivity(intent)
        }

        rvStains = findViewById(R.id.rvStains)
        rvStains.layoutManager = LinearLayoutManager(this)

        loadStainData()
    }

    private fun loadStainData() {
        val db = FirebaseFirestore.getInstance()

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