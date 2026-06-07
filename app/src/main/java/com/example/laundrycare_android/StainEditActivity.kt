package com.example.laundrycare_android

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore

class StainEditActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private lateinit var documentId: String
    private lateinit var spinnerCategory: Spinner

    private val categories = arrayOf("음식물", "화장품", "생활/기타")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stain_edit) // 생성해 둔 얼룩 수정 전용 레이아웃 사용

        documentId = intent.getStringExtra("documentId") ?: ""
        if (documentId.isEmpty()) {
            Toast.makeText(this, "얼룩 정보를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val btnEditBack = findViewById<ImageView>(R.id.btnEditBack)
        spinnerCategory = findViewById(R.id.spinnerStainCategory)
        val btnSave = findViewById<Button>(R.id.btnSaveStain)

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categories)
        spinnerCategory.adapter = adapter

        btnEditBack.setOnClickListener { finish() }

        loadExistingData(adapter)

        // 🌟 수정 내용 파이어베이스 일괄 업데이트 로직
        btnSave.setOnClickListener {
            val newStainType = spinnerCategory.selectedItem.toString()

            btnSave.isEnabled = false
            btnSave.text = "저장 중..."

            db.collection("stains").document(documentId)
                .update("stainType", newStainType)
                .addOnSuccessListener {
                    Toast.makeText(this, "수정이 완료되었습니다.", Toast.LENGTH_SHORT).show()
                    finish() // 저장 후 안전하게 이전 화면으로 복귀
                }
                .addOnFailureListener {
                    Toast.makeText(this, "수정에 실패했습니다.", Toast.LENGTH_SHORT).show()
                    btnSave.isEnabled = true
                    btnSave.text = "수정 내용 저장"
                }
        }
    }

    private fun loadExistingData(adapter: ArrayAdapter<String>) {
        db.collection("stains").document(documentId).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val stainType = document.getString("stainType") ?: "생활/기타"
                    val spinnerPosition = adapter.getPosition(stainType)
                    if (spinnerPosition >= 0) {
                        spinnerCategory.setSelection(spinnerPosition)
                    }
                }
            }
    }
}