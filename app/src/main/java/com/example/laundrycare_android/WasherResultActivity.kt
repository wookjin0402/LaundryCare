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
    private var aiPredictedType = "" // AI가 예측한 세탁기 형태 (드럼 또는 통돌이)

    private lateinit var spinnerWasherType: Spinner
    private lateinit var etWasherBrand: EditText
    private lateinit var etWasherModel: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_washer_result)

        val btnBack = findViewById<Button>(R.id.btnWasherBack)
        val btnSave = findViewById<Button>(R.id.btnWasherSave)
        val ivWasherPhoto = findViewById<ImageView>(R.id.ivWasherPhoto)

        spinnerWasherType = findViewById(R.id.spinnerWasherType)
        etWasherBrand = findViewById(R.id.etWasherBrand)
        etWasherModel = findViewById(R.id.etWasherModel)

        btnBack.setOnClickListener { finish() }

        // 1. 촬영한 세탁기 이미지 로드
        val washerImagePath = intent.getStringExtra("washer_image_path")
        if (!washerImagePath.isNullOrEmpty()) {
            val imgFile = File(washerImagePath)
            if (imgFile.exists()) {
                Glide.with(this).load(imgFile).centerCrop().into(ivWasherPhoto)
                currentImageUrl = washerImagePath
            }
        }

        // 2. 스피너 아이템 세팅 (드럼 / 통돌이)
        val washerTypes = arrayOf("드럼", "통돌이")
        spinnerWasherType.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, washerTypes)

        // 3. AI 분석 JSON 데이터 파싱
        val jsonString = intent.getStringExtra("ai_washer_data") ?: ""
        if (jsonString.isNotEmpty()) {
            try {
                val jsonObject = JSONObject(jsonString)
                val washerInfo = jsonObject.optJSONObject("washer")

                if (washerInfo != null) {
                    // AI 분석 결과 추출 (예: "드럼" 또는 "통돌이")
                    aiPredictedType = washerInfo.optString("type", "드럼")
                    val detectedBrand = washerInfo.optString("brand", "")
                    val detectedModel = washerInfo.optString("model", "")

                    // 추출된 브랜드 및 모델명을 입력창에 설정
                    etWasherBrand.setText(detectedBrand)
                    etWasherModel.setText(detectedModel)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 4. AI가 판단한 세탁기 형태를 스피너 초기값으로 자동 매칭
        if (aiPredictedType.isNotEmpty()) {
            setSpinnerToValue(spinnerWasherType, aiPredictedType)
        }

        // 5. 등록 완료 버튼 클릭 시 Firestore에 저장
        btnSave.setOnClickListener {
            btnSave.isEnabled = false
            btnSave.text = "등록 중..."

            val db = FirebaseFirestore.getInstance()
            val storageRef = FirebaseStorage.getInstance().reference

            if (currentImageUrl.isNotEmpty()) {
                // 이미지 파일 업로드
                val fileUri = Uri.fromFile(File(currentImageUrl))
                val imageRef = storageRef.child("washer_images/${System.currentTimeMillis()}_washer.jpg")

                imageRef.putFile(fileUri).addOnSuccessListener {
                    imageRef.downloadUrl.addOnSuccessListener { uri ->

                        // 사용자가 최종 확인/수정한 데이터 바구니 생성
                        val washerData = hashMapOf(
                            "type" to spinnerWasherType.selectedItem.toString(),
                            "brand" to etWasherBrand.text.toString(),
                            "model" to etWasherModel.text.toString(),
                            "imageUrl" to uri.toString(),
                            "timestamp" to System.currentTimeMillis()
                        )

                        // Firestore 'washers' 컬렉션에 추가
                        db.collection("washers").add(washerData)
                            .addOnSuccessListener {
                                Toast.makeText(this, "세탁기 등록 성공!", Toast.LENGTH_SHORT).show()
                                val intent = Intent(this, MainActivity::class.java).apply {
                                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
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

    /**
     * 스피너 아이템 매칭 함수
     */
    private fun setSpinnerToValue(spinner: Spinner, value: String) {
        val adapter = spinner.adapter
        for (i in 0 until adapter.count) {
            if (adapter.getItem(i).toString() == value) {
                spinner.setSelection(i)
                break
            }
        }
    }
}