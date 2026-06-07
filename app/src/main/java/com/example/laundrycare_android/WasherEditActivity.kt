package com.example.laundrycare_android

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore

class WasherEditActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private lateinit var documentId: String

    private lateinit var etEditBrand: EditText
    private lateinit var etEditModel: EditText
    private lateinit var etEditType: EditText
    private lateinit var btnSaveWasher: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_washer_edit)

        documentId = intent.getStringExtra("documentId") ?: ""
        if (documentId.isEmpty()) {
            Toast.makeText(this, "세탁기 정보를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val btnEditBack = findViewById<ImageView>(R.id.btnEditBack)
        etEditBrand = findViewById(R.id.etEditBrand)
        etEditModel = findViewById(R.id.etEditModel)
        etEditType = findViewById(R.id.etEditType)
        btnSaveWasher = findViewById(R.id.btnSaveWasher)

        btnEditBack.setOnClickListener { finish() }

        loadExistingData()

        btnSaveWasher.setOnClickListener {
            val brand = etEditBrand.text.toString().trim()
            val model = etEditModel.text.toString().trim()
            val type = etEditType.text.toString().trim()

            if (brand.isEmpty() || type.isEmpty()) {
                Toast.makeText(this, "브랜드와 기기 형태는 필수입니다.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnSaveWasher.isEnabled = false
            btnSaveWasher.text = "저장 중..."

            val updatedData = mapOf(
                "brand" to brand,
                "model" to model,
                "type" to type
            )

            db.collection("washers").document(documentId)
                .update(updatedData)
                .addOnSuccessListener {
                    Toast.makeText(this, "수정 완료!", Toast.LENGTH_SHORT).show()
                    finish() // 완료되면 상세화면으로 돌아감
                }
                .addOnFailureListener {
                    Toast.makeText(this, "수정 실패", Toast.LENGTH_SHORT).show()
                    btnSaveWasher.isEnabled = true
                    btnSaveWasher.text = "수정 내용 저장"
                }
        }
    }

    private fun loadExistingData() {
        db.collection("washers").document(documentId).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    etEditBrand.setText(document.getString("brand") ?: "")
                    etEditModel.setText(document.getString("model") ?: "")
                    etEditType.setText(document.getString("type") ?: "")
                }
            }
    }
}