package com.example.laundrycare_android

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ResultActivity : AppCompatActivity() {

    private var parsedLaundryTip = ""
    private var currentImageUrl = ""

    private var aiPredictedSeason = ""
    private var aiPredictedMain = ""
    private var aiPredictedSub = ""
    private var aiPredictedColor = ""
    private var aiPredictedMaterial = ""

    private lateinit var spinnerSeason: Spinner
    private lateinit var spinnerMain: Spinner
    private lateinit var spinnerSub: Spinner
    private lateinit var etColor: EditText

    private val myUid get() = FirebaseAuth.getInstance().currentUser?.uid ?: "unknown_user"

    private val subCategoryMap = mapOf(
        "상의" to arrayOf("반팔", "긴팔", "아우터"),
        "하의" to arrayOf("반바지", "긴바지", "치마"),
        "고급" to arrayOf("명품", "기능성"),
        "기타" to arrayOf("양말", "속옷")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)

        val btnBack = findViewById<ImageView>(R.id.btnBack)
        val btnSave = findViewById<Button>(R.id.btnSave)
        val ivResultPhoto = findViewById<ImageView>(R.id.ivResultPhoto)
        val tvGuideTitle = findViewById<TextView>(R.id.tvGuideTitle)
        val tvRawTags = findViewById<TextView>(R.id.tvRawTags)
        val tvWarnings = findViewById<TextView>(R.id.tvWarnings)
        val tvCareSteps = findViewById<TextView>(R.id.tvCareSteps)

        spinnerSeason = findViewById(R.id.spinnerSeason)
        spinnerMain = findViewById(R.id.spinnerMain)
        spinnerSub = findViewById(R.id.spinnerSub)

        etColor = findViewById(R.id.etColor)
        val etSize = findViewById<EditText>(R.id.etSize)
        val etMaterial = findViewById<EditText>(R.id.etMaterial)

        // 🌟 강제 초기화: XML에 하드코딩된 텍스트를 시작하자마자 강제로 지워버립니다.
        etColor.setText("")

        btnBack.setOnClickListener { finish() }

        val clothImagePath = intent.getStringExtra("cloth_image_path")
        if (!clothImagePath.isNullOrEmpty()) {
            val imgFile = File(clothImagePath)
            if (imgFile.exists()) {
                Glide.with(this).load(imgFile).centerCrop().into(ivResultPhoto)
                currentImageUrl = clothImagePath
            }
        }

        val seasons = arrayOf("봄", "여름", "가을", "겨울")
        spinnerSeason.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, seasons)

        val mainCategories = subCategoryMap.keys.toTypedArray()
        spinnerMain.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, mainCategories)

        val jsonString = intent.getStringExtra("ai_json_data") ?: ""
        Log.d("ResultActivity", "수신된 JSON: $jsonString")

        if (jsonString.isNotEmpty()) {
            try {
                val jsonObject = JSONObject(jsonString)
                val guide = jsonObject.optJSONObject("guide") ?: jsonObject

                tvGuideTitle.text = guide.optString("title", "의류 분석 결과")
                parsedLaundryTip = guide.optString("summary", "AI 분석 결과를 확인하세요.")

                aiPredictedSeason = jsonObject.optString("season", "")
                aiPredictedMain = jsonObject.optString("mainCategory", "")
                aiPredictedSub = jsonObject.optString("subCategory", "")
                aiPredictedColor = jsonObject.optString("color", "")

                // 🌟 소재 배열 추출 (수정된 로직)
                val materialsArray = jsonObject.optJSONArray("extracted_materials")
                if (materialsArray != null && materialsArray.length() > 0) {
                    val materialList = mutableListOf<String>()
                    for (i in 0 until materialsArray.length()) {
                        materialList.add(materialsArray.getString(i))
                    }
                    aiPredictedMaterial = materialList.joinToString(", ")
                } else {
                    aiPredictedMaterial = ""
                }

                val tagsArray = guide.optJSONArray("raw_tags")
                var tagsText = ""
                if (tagsArray != null) {
                    for (i in 0 until tagsArray.length()) tagsText += "#${tagsArray.getString(i)}  "
                }
                tvRawTags.text = tagsText

                val warningsArray = guide.optJSONArray("warnings")
                var warningsText = ""
                var hasCritical = false
                if (warningsArray != null) {
                    for (i in 0 until warningsArray.length()) {
                        val warnObj = warningsArray.getJSONObject(i)
                        if (warnObj.optBoolean("is_critical", false)) {
                            hasCritical = true
                            warningsText += "🚨 [치명적 주의] ${warnObj.optString("icon_name", "경고")}\n${warnObj.optString("desc", "주의가 필요합니다.")}\n\n"
                        } else {
                            warningsText += "⚠️ ${warnObj.optString("icon_name", "주의")}\n${warnObj.optString("desc", "참고사항입니다.")}\n\n"
                        }
                    }
                }
                tvWarnings.text = if (warningsText.isEmpty()) "특이사항 없음" else warningsText
                if (!hasCritical) tvWarnings.setTextColor(android.graphics.Color.parseColor("#666666"))

                val stepsArray = guide.optJSONArray("careSteps")
                var stepsText = ""
                if (stepsArray != null) {
                    for (i in 0 until stepsArray.length()) {
                        val stepObj = stepsArray.getJSONObject(i)
                        stepsText += "✅ ${stepObj.optString("step", "단계")}\n${stepObj.optString("desc", "진행하세요.")}\n\n"
                    }
                }
                tvCareSteps.text = if (stepsText.isEmpty()) "관리 정보 없음" else stepsText

            } catch (e: Exception) {
                Log.e("ResultActivity", "JSON 파싱 에러: ${e.message}")
            }
        }

        if (aiPredictedSeason.isNotEmpty()) setSpinnerToValue(spinnerSeason, aiPredictedSeason)
        if (aiPredictedMain.isNotEmpty()) setSpinnerToValue(spinnerMain, aiPredictedMain)

        // 🌟 색상 입력: JSON에서 받은 값이 있으면 채우고, 없으면 힌트를 띄움
        if (aiPredictedColor.isNotEmpty() && aiPredictedColor != "null") {
            etColor.setText(aiPredictedColor)
        } else {
            etColor.hint = "색상을 입력하세요"
        }

        // 🌟 소재 입력: 백엔드가 빈 깡통 [] 을 보내면 "인식 실패"라고 힌트를 띄움
        if (aiPredictedMaterial.isNotEmpty() && aiPredictedMaterial != "null") {
            etMaterial.setText(aiPredictedMaterial)
        } else {
            etMaterial.setText("")
            etMaterial.hint = "소재 인식 실패 (백엔드 데이터 확인 필요)"
        }

        spinnerMain.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, position: Int, p3: Long) {
                val selectedMain = spinnerMain.selectedItem.toString()
                val subCategories = subCategoryMap[selectedMain] ?: arrayOf("기타")

                spinnerSub.adapter = ArrayAdapter(this@ResultActivity, android.R.layout.simple_spinner_dropdown_item, subCategories)

                if (aiPredictedSub.isNotEmpty() && subCategories.contains(aiPredictedSub)) {
                    setSpinnerToValue(spinnerSub, aiPredictedSub)
                    aiPredictedSub = ""
                }
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }

        btnSave.setOnClickListener {
            if (currentImageUrl.isEmpty()) {
                Toast.makeText(this, "저장할 이미지 파일이 없습니다.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (spinnerMain.selectedItem == null || spinnerMain.selectedItem.toString().isEmpty()) {
                Toast.makeText(this, "카테고리가 선택되지 않아 저장할 수 없습니다.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnSave.isEnabled = false
            btnSave.text = "클라우드 업로드 중..."

            val db = FirebaseFirestore.getInstance()
            val storageRef = FirebaseStorage.getInstance().reference

            db.collection("clothes").get()
                .addOnSuccessListener { snapshot ->
                    if (snapshot.size() >= 100) {
                        Toast.makeText(this, "옷장은 최대 100장까지만 저장할 수 있습니다.", Toast.LENGTH_LONG).show()
                        btnSave.isEnabled = true
                        btnSave.text = "이대로 옷장에 저장하기"
                        return@addOnSuccessListener
                    }

                    val fileUri = Uri.fromFile(File(currentImageUrl))
                    val imageRef = storageRef.child("clothes_images/${System.currentTimeMillis()}_cloth.jpg")

                    imageRef.putFile(fileUri).addOnSuccessListener {
                        imageRef.downloadUrl.addOnSuccessListener { uri ->
                            val currentTimeMillis = System.currentTimeMillis()
                            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.KOREA)
                            val currentDate = dateFormat.format(Date(currentTimeMillis))

                            val clothData = hashMapOf(
                                "uid" to myUid,
                                "season" to (spinnerSeason.selectedItem?.toString() ?: ""),
                                "mainCategory" to spinnerMain.selectedItem.toString(),
                                "subCategory" to (spinnerSub.selectedItem?.toString() ?: ""),
                                "color" to etColor.text.toString().trim(),
                                "size" to etSize.text.toString().trim(),
                                "material" to etMaterial.text.toString().trim(),
                                "laundryTip" to parsedLaundryTip,
                                "warnings" to tvWarnings.text.toString(),
                                "careSteps" to tvCareSteps.text.toString(),
                                "imageUrl" to uri.toString(),
                                "timestamp" to currentTimeMillis,
                                "date" to currentDate
                            )

                            db.collection("clothes").add(clothData)
                                .addOnSuccessListener {
                                    Toast.makeText(this, "저장 완료!", Toast.LENGTH_SHORT).show()
                                    val intent = Intent(this, MainActivity::class.java).apply {
                                        putExtra("navigate_to", "closet")
                                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                                    }
                                    startActivity(intent)
                                    finish()
                                }
                        }
                    }.addOnFailureListener {
                        btnSave.isEnabled = true
                        btnSave.text = "이대로 옷장에 저장하기"
                        Toast.makeText(this, "사진 업로드에 실패했습니다.", Toast.LENGTH_SHORT).show()
                    }
                }.addOnFailureListener {
                    btnSave.isEnabled = true
                    btnSave.text = "이대로 옷장에 저장하기"
                    Toast.makeText(this, "서버와 연결할 수 없습니다.", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun setSpinnerToValue(spinner: Spinner, value: String) {
        val adapter = spinner.adapter
        if (adapter != null) {
            for (i in 0 until adapter.count) {
                if (adapter.getItem(i).toString() == value) {
                    spinner.setSelection(i)
                    break
                }
            }
        }
    }
}