package com.example.laundrycare_android

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import org.json.JSONObject
import java.io.File

class ResultActivity : AppCompatActivity() {

    private var parsedLaundryTip = ""
    private var currentImageUrl = ""

    private var aiPredictedSeason = ""
    private var aiPredictedMain = ""
    private var aiPredictedSub = ""
    private var aiPredictedColor = ""

    private lateinit var spinnerSeason: Spinner
    private lateinit var spinnerMain: Spinner
    private lateinit var spinnerSub: Spinner
    private lateinit var etColor: EditText

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

        btnBack.setOnClickListener { finish() }

        // 1. 이미지 로드 (null 방어 추가)
        val clothImagePath = intent.getStringExtra("cloth_image_path")
        if (!clothImagePath.isNullOrEmpty()) {
            val imgFile = File(clothImagePath)
            if (imgFile.exists()) {
                Glide.with(this).load(imgFile).centerCrop().into(ivResultPhoto)
                currentImageUrl = clothImagePath
            } else {
                Toast.makeText(this, "이미지 파일을 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "전달된 이미지 경로가 없습니다.", Toast.LENGTH_SHORT).show()
        }

        val seasons = arrayOf("봄", "여름", "가을", "겨울")
        spinnerSeason.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, seasons)

        val mainCategories = subCategoryMap.keys.toTypedArray()
        spinnerMain.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, mainCategories)

        // 3. AI JSON 데이터 파싱 (안전장치 추가)
        val jsonString = intent.getStringExtra("ai_json_data") ?: ""
        Log.d("ResultActivity", "수신된 JSON: $jsonString") // 확인용 로그 추가

        if (jsonString.isNotEmpty()) {
            try {
                val jsonObject = JSONObject(jsonString)
                // 서버가 'guide'로 안 감싸고 바로 보냈을 경우를 대비한 유연한 파싱
                val guide = jsonObject.optJSONObject("guide") ?: jsonObject

                tvGuideTitle.text = guide.optString("title", "의류 분석 결과")
                parsedLaundryTip = guide.optString("summary", "AI 분석 결과를 확인하세요.")

                // 🌟 수정된 부분: guide 내부가 아닌 jsonObject(루트)에서 카테고리 정보 파싱
                aiPredictedSeason = jsonObject.optString("season", "")
                aiPredictedMain = jsonObject.optString("mainCategory", "")
                aiPredictedSub = jsonObject.optString("subCategory", "")
                aiPredictedColor = jsonObject.optString("color", "")

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
                Toast.makeText(this, "데이터 분석 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                // 파싱 에러가 나도 빈 텍스트로 기본 세팅되게 함
            }
        } else {
            Toast.makeText(this, "AI 분석 데이터를 받지 못했습니다.", Toast.LENGTH_SHORT).show()
        }

        // 4. 스피너 초기화
        if (aiPredictedSeason.isNotEmpty()) setSpinnerToValue(spinnerSeason, aiPredictedSeason)
        if (aiPredictedMain.isNotEmpty()) setSpinnerToValue(spinnerMain, aiPredictedMain)
        if (aiPredictedColor.isNotEmpty()) etColor.setText(aiPredictedColor)

        // 5. 스피너 연동
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

        // 6. 저장 로직 (빈 데이터 컷팅 및 에러가 적은 전통적 방식)
        btnSave.setOnClickListener {
            // [방어 1] 이미지가 없으면 컷
            if (currentImageUrl.isEmpty()) {
                Toast.makeText(this, "저장할 이미지 파일이 없습니다.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // [방어 2] 메인 카테고리가 비어있거나 '분석 실패'면 유령 데이터 방지를 위해 컷
            if (spinnerMain.selectedItem == null || spinnerMain.selectedItem.toString().isEmpty()) {
                Toast.makeText(this, "카테고리가 선택되지 않아 저장할 수 없습니다.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnSave.isEnabled = false
            btnSave.text = "클라우드 업로드 중..."

            val db = FirebaseFirestore.getInstance()
            val storageRef = FirebaseStorage.getInstance().reference

            // get()으로 전체 문서를 가져와서 갯수 세기
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

                            val clothData = hashMapOf(
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
                                "timestamp" to System.currentTimeMillis()
                            )

                            db.collection("clothes").add(clothData).addOnSuccessListener {
                                Toast.makeText(this, "저장 완료!", Toast.LENGTH_SHORT).show()
                                val intent = Intent(this, MainActivity::class.java).apply {
                                    putExtra("navigate_to", "closet")
                                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                                }
                                startActivity(intent)
                                finish()
                            }
                        }
                    }.addOnFailureListener { e ->
                        Log.e("ResultActivity", "Storage 업로드 에러: ${e.message}")
                        btnSave.isEnabled = true
                        btnSave.text = "이대로 옷장에 저장하기"
                        Toast.makeText(this, "사진 업로드에 실패했습니다.", Toast.LENGTH_SHORT).show()
                    }
                }.addOnFailureListener { e ->
                    Log.e("ResultActivity", "Firestore 읽기 에러: ${e.message}")
                    btnSave.isEnabled = true
                    btnSave.text = "이대로 옷장에 저장하기"
                    Toast.makeText(this, "서버와 연결할 수 없습니다.", Toast.LENGTH_SHORT).show()
                }
        }
    }

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