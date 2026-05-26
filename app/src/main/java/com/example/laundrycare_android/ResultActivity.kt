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

class ResultActivity : AppCompatActivity() {

    private var parsedLaundryTip = ""
    private var currentImageUrl = ""

    // AI가 분석한 초기 분류값을 임시 저장할 변수 (색상 추가)
    private var aiPredictedSeason = ""
    private var aiPredictedMain = ""
    private var aiPredictedSub = ""
    private var aiPredictedColor = ""

    private lateinit var spinnerSeason: Spinner
    private lateinit var spinnerMain: Spinner
    private lateinit var spinnerSub: Spinner
    private lateinit var etColor: EditText

    // 소분류 데이터 맵핑
    private val subCategoryMap = mapOf(
        "상의" to arrayOf("반팔", "긴팔", "아우터"),
        "하의" to arrayOf("반바지", "긴바지", "치마"),
        "고급" to arrayOf("명품", "기능성"),
        "기타" to arrayOf("양말", "속옷")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)

        val btnBack = findViewById<Button>(R.id.btnBack)
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

        // 1. 이미지 로드
        val clothImagePath = intent.getStringExtra("cloth_image_path")
        if (!clothImagePath.isNullOrEmpty()) {
            val imgFile = File(clothImagePath)
            if (imgFile.exists()) {
                Glide.with(this).load(imgFile).centerCrop().into(ivResultPhoto)
                currentImageUrl = clothImagePath
            }
        }

        // 2. 스피너 초기 어댑터 설정 (기본값 세팅)
        val seasons = arrayOf("봄", "여름", "가을", "겨울")
        spinnerSeason.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, seasons)

        val mainCategories = subCategoryMap.keys.toTypedArray()
        spinnerMain.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, mainCategories)

        // 3. AI JSON 데이터 파싱 및 예측값 저장
        val jsonString = intent.getStringExtra("ai_json_data") ?: ""
        if (jsonString.isNotEmpty()) {
            try {
                val jsonObject = JSONObject(jsonString)
                val guide = jsonObject.optJSONObject("guide")

                if (guide != null) {
                    tvGuideTitle.text = guide.optString("title", "의류 분석 결과")
                    parsedLaundryTip = guide.optString("summary", "세탁 가이드 요약")

                    // 🌟 AI가 예측한 색상, 계절, 분류값 가져오기
                    aiPredictedSeason = guide.optString("season", "")
                    aiPredictedMain = guide.optString("mainCategory", "")
                    aiPredictedSub = guide.optString("subCategory", "")
                    aiPredictedColor = guide.optString("color", "") // 색상 데이터 파싱

                    // 태그 처리
                    val tagsArray = guide.optJSONArray("raw_tags")
                    var tagsText = ""
                    if (tagsArray != null) {
                        for (i in 0 until tagsArray.length()) tagsText += "#${tagsArray.getString(i)}  "
                    }
                    tvRawTags.text = tagsText

                    // 경고 처리
                    val warningsArray = guide.optJSONArray("warnings")
                    var warningsText = ""
                    var hasCritical = false
                    if (warningsArray != null) {
                        for (i in 0 until warningsArray.length()) {
                            val warnObj = warningsArray.getJSONObject(i)
                            if (warnObj.optBoolean("is_critical", false)) {
                                hasCritical = true
                                warningsText += "🚨 [치명적 주의] ${warnObj.optString("icon_name")}\n${warnObj.optString("desc")}\n\n"
                            } else {
                                warningsText += "⚠️ ${warnObj.optString("icon_name")}\n${warnObj.optString("desc")}\n\n"
                            }
                        }
                    }
                    tvWarnings.text = warningsText
                    if (!hasCritical) tvWarnings.setTextColor(android.graphics.Color.parseColor("#666666"))

                    // 세탁 스텝 처리
                    val stepsArray = guide.optJSONArray("careSteps")
                    var stepsText = ""
                    if (stepsArray != null) {
                        for (i in 0 until stepsArray.length()) {
                            val stepObj = stepsArray.getJSONObject(i)
                            stepsText += "✅ ${stepObj.optString("step")}\n${stepObj.optString("desc")}\n\n"
                        }
                    }
                    tvCareSteps.text = stepsText
                }
            } catch (e: Exception) { e.printStackTrace() }
        }

        // 4. 입력창 및 스피너에 AI 예측값 자동 매칭 실행
        if (aiPredictedSeason.isNotEmpty()) {
            setSpinnerToValue(spinnerSeason, aiPredictedSeason)
        }

        if (aiPredictedMain.isNotEmpty()) {
            setSpinnerToValue(spinnerMain, aiPredictedMain)
        }

        // 🌟 색상 값이 있으면 EditText에 자동으로 채워줌
        if (aiPredictedColor.isNotEmpty()) {
            etColor.setText(aiPredictedColor)
        }

        // 5. 대분류 변경 시 소분류 어댑터 갱신 로직
        spinnerMain.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, position: Int, p3: Long) {
                val selectedMain = spinnerMain.selectedItem.toString()
                val subCategories = subCategoryMap[selectedMain] ?: arrayOf("기타")

                spinnerSub.adapter = ArrayAdapter(this@ResultActivity, android.R.layout.simple_spinner_dropdown_item, subCategories)

                // 대분류가 바뀌었을 때, AI가 예측한 소분류가 현재 선택된 대분류에 속한다면 자동으로 세팅
                if (aiPredictedSub.isNotEmpty() && subCategories.contains(aiPredictedSub)) {
                    setSpinnerToValue(spinnerSub, aiPredictedSub)
                    // 한 번 세팅 후 비워주어 사용자가 나중에 대분류를 바꿀 때 꼬이지 않게 함
                    aiPredictedSub = ""
                }
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }


        // 6. 저장 버튼 로직 (한도 확인 및 업로드)
        btnSave.setOnClickListener {
            btnSave.isEnabled = false
            btnSave.text = "한도 확인 중..."

            val db = FirebaseFirestore.getInstance()
            val storageRef = FirebaseStorage.getInstance().reference

            db.collection("clothes").count().get(com.google.firebase.firestore.AggregateSource.SERVER)
                .addOnSuccessListener { snapshot ->
                    val currentCount = snapshot.count

                    if (currentCount >= 100) {
                        Toast.makeText(this, "옷장은 최대 100장까지만 저장할 수 있습니다.", Toast.LENGTH_LONG).show()
                        btnSave.isEnabled = true
                        btnSave.text = "이대로 옷장에 저장하기"
                    } else {
                        btnSave.text = "클라우드 업로드 중..."

                        val fileUri = Uri.fromFile(File(currentImageUrl))
                        val imageRef = storageRef.child("clothes_images/${System.currentTimeMillis()}_cloth.jpg")

                        imageRef.putFile(fileUri).addOnSuccessListener {
                            imageRef.downloadUrl.addOnSuccessListener { uri ->
                                // 사용자가 최종 확인/수정한 스피너 및 텍스트 값을 저장함
                                val clothData = hashMapOf(
                                    "season" to spinnerSeason.selectedItem.toString(),
                                    "mainCategory" to spinnerMain.selectedItem.toString(),
                                    "subCategory" to (spinnerSub.selectedItem?.toString() ?: ""),
                                    "color" to etColor.text.toString(),
                                    "size" to etSize.text.toString(),
                                    "material" to etMaterial.text.toString(),
                                    "laundryTip" to parsedLaundryTip,
                                    "warnings" to tvWarnings.text.toString(),
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
                        }.addOnFailureListener {
                            btnSave.isEnabled = true
                            btnSave.text = "이대로 옷장에 저장하기"
                            Toast.makeText(this, "업로드 실패", Toast.LENGTH_SHORT).show()
                        }
                    }
                }.addOnFailureListener {
                    btnSave.isEnabled = true
                    btnSave.text = "이대로 옷장에 저장하기"
                    Toast.makeText(this, "서버 통신 오류", Toast.LENGTH_SHORT).show()
                }
        }
    }

    /**
     * 스피너의 아이템 리스트 중에서 주어진 텍스트와 일치하는 항목을 찾아 선택합니다.
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