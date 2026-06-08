package com.example.laundrycare_android

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
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

    // 🌟 수정: 이전 액티비티에서 넘겨준 uid를 최우선으로 받고, 없으면 FirebaseAuth에서 가져옵니다.
    private val myUid get() = intent.getStringExtra("uid") ?: FirebaseAuth.getInstance().currentUser?.uid ?: "unknown_user"

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

                val extractedWasherType = jsonObject.optString("extracted_washer_type", "")
                val extractedBrand = jsonObject.optString("extracted_brand", "")

                if (extractedWasherType.contains("드럼")) {
                    aiPredictedType = "드럼"
                } else if (extractedWasherType.contains("통돌이")) {
                    aiPredictedType = "통돌이"
                }

                if (extractedBrand.isNotEmpty()) {
                    etWasherBrand.setText(extractedBrand)
                }

                val washerInfo = jsonObject.optJSONObject("washer")
                if (washerInfo != null) {
                    val detectedModel = washerInfo.optString("model", "")
                    if (detectedModel.isNotEmpty()) {
                        etWasherModel.setText(detectedModel)
                    }
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
                    if (isFinishing || isDestroyed) return@addOnSuccessListener

                    imageRef.downloadUrl.addOnSuccessListener { uri ->
                        if (isFinishing || isDestroyed) return@addOnSuccessListener

                        // 🌟 수정: 백엔드 팀원 요청에 맞게 텍스트 규격화 (대문자/풀네임)
                        val rawType = spinnerWasherType.selectedItem.toString()
                        val rawBrand = etWasherBrand.text.toString()
                        val rawModel = etWasherModel.text.toString()

                        val formattedType = WasherMapper.toServerWasherType(rawType)
                        val formattedBrand = WasherMapper.toServerBrand(rawBrand)

                        val washerData = hashMapOf(
                            "uid" to myUid, // 필수 추가 파라미터
                            "type" to formattedType, // 예: "드럼 세탁기"
                            "brand" to formattedBrand, // 예: "LG"
                            "model" to rawModel,
                            "imageUrl" to uri.toString(),
                            "timestamp" to System.currentTimeMillis()
                        )

                        // 🌟 핵심 수정: 유저 아이디(uid) 밑으로 완벽하게 묶어서 저장되도록 경로 변경
                        db.collection("users").document(myUid).collection("washers").add(washerData)
                            .addOnSuccessListener {
                                if (!isFinishing && !isDestroyed) {
                                    Toast.makeText(this, "세탁기 등록 성공!", Toast.LENGTH_SHORT).show()

                                    // 메인 화면(MainActivity) 세탁 탭으로 이동
                                    val intent = Intent(this, MainActivity::class.java).apply {
                                        putExtra("navigate_to_fragment", "laundry")
                                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                                    }
                                    startActivity(intent)
                                    finish()
                                }
                            }
                            .addOnFailureListener {
                                if (isFinishing || isDestroyed) return@addOnFailureListener
                                btnSave.isEnabled = true
                                btnSave.text = "세탁기 등록 완료"
                                Toast.makeText(this, "DB 저장 실패", Toast.LENGTH_SHORT).show()
                            }
                    }
                }.addOnFailureListener {
                    if (isFinishing || isDestroyed) return@addOnFailureListener
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