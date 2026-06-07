package com.example.laundrycare_android

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import org.json.JSONObject
import java.io.File

class WasherResultActivity : AppCompatActivity() {

    private var currentImageUrl = ""
    private var aiPredictedType = ""

    private lateinit var spinnerWasherType: Spinner
    private lateinit var etWasherBrand: EditText
    private lateinit var etWasherModel: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_washer_result)

        val btnBack = findViewById<ImageView>(R.id.btnWasherBack)
        val btnSave = findViewById<Button>(R.id.btnWasherSave)
        val ivWasherPhoto = findViewById<ImageView>(R.id.ivWasherPhoto)

        spinnerWasherType = findViewById(R.id.spinnerWasherType)
        etWasherBrand = findViewById(R.id.etWasherBrand)
        etWasherModel = findViewById(R.id.etWasherModel)

        btnBack.setOnClickListener { finish() }

        val washerImagePath = intent.getStringExtra("washer_image_path")
        if (!washerImagePath.isNullOrEmpty()) {
            val imgFile = File(washerImagePath)
            if (imgFile.exists()) {
                Glide.with(this).load(imgFile).centerCrop().into(ivWasherPhoto)
                currentImageUrl = washerImagePath
            }
        }

        val washerTypes = arrayOf("드럼", "통돌이")
        spinnerWasherType.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, washerTypes)

        val jsonString = intent.getStringExtra("ai_washer_data") ?: ""
        if (jsonString.isNotEmpty()) {
            try {
                val jsonObject = JSONObject(jsonString)
                val washerInfo = jsonObject.optJSONObject("washer")

                if (washerInfo != null) {
                    aiPredictedType = washerInfo.optString("type", "드럼")
                    val detectedBrand = washerInfo.optString("brand", "")
                    val detectedModel = washerInfo.optString("model", "")

                    etWasherBrand.setText(detectedBrand)
                    etWasherModel.setText(detectedModel)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        if (aiPredictedType.isNotEmpty()) {
            setSpinnerToValue(spinnerWasherType, aiPredictedType)
        }

        btnSave.setOnClickListener {
            btnSave.isEnabled = false
            btnSave.text = "등록 중..."

            val db = FirebaseFirestore.getInstance()
            val storageRef = FirebaseStorage.getInstance().reference

            if (currentImageUrl.isNotEmpty()) {
                val fileUri = Uri.fromFile(File(currentImageUrl))
                val imageRef = storageRef.child("washer_images/${System.currentTimeMillis()}_washer.jpg")

                imageRef.putFile(fileUri).addOnSuccessListener {
                    imageRef.downloadUrl.addOnSuccessListener { uri ->

                        val washerData = hashMapOf(
                            "type" to spinnerWasherType.selectedItem.toString(),
                            "brand" to etWasherBrand.text.toString(),
                            "model" to etWasherModel.text.toString(),
                            "imageUrl" to uri.toString(),
                            "timestamp" to System.currentTimeMillis()
                        )

                        db.collection("washers").add(washerData)
                            .addOnSuccessListener {
                                Toast.makeText(this, "세탁기 등록 성공!", Toast.LENGTH_SHORT).show()
                                // 🌟 수정됨: MainActivity가 아닌 세탁 탭(LaundryActivity)으로 복귀
                                val intent = Intent(this, LaundryActivity::class.java).apply {
                                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                                }
                                startActivity(intent)
                                finish()
                            }
                            .addOnFailureListener {
                                btnSave.isEnabled = true
                                btnSave.text = "세탁기 등록 완료"
                                Toast.makeText(this, "DB 저장 실패", Toast.LENGTH_SHORT).show()
                            }
                    }
                }.addOnFailureListener {
                    btnSave.isEnabled = true
                    btnSave.text = "세탁기 등록 완료"
                    Toast.makeText(this, "이미지 업로드 실패", Toast.LENGTH_SHORT).show()
                }
            } else {
                btnSave.isEnabled = true
                btnSave.text = "세탁기 등록 완료"
                Toast.makeText(this, "이미지 파일이 없습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setSpinnerToValue(spinner: Spinner, value: String)  {
        val adapter = spinner.adapter
        for (i in 0 until adapter.count) {
            if (adapter.getItem(i).toString() == value) {
                spinner.setSelection(i)
                break
            }
        }
    }
}