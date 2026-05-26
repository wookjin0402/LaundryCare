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

        val spinnerSeason = findViewById<Spinner>(R.id.spinnerSeason)
        val spinnerMain = findViewById<Spinner>(R.id.spinnerMain)
        val spinnerSub = findViewById<Spinner>(R.id.spinnerSub)

        // 🌟 색상 및 사이즈 입력 필드 연결
        val etColor = findViewById<EditText>(R.id.etColor)
        val etSize = findViewById<EditText>(R.id.etSize)
        val etMaterial = findViewById<EditText>(R.id.etMaterial)

        btnBack.setOnClickListener { finish() }

        val clothImagePath = intent.getStringExtra("cloth_image_path")
        if (!clothImagePath.isNullOrEmpty()) {
            val imgFile = File(clothImagePath)
            if (imgFile.exists()) {
                Glide.with(this).load(imgFile).centerCrop().into(ivResultPhoto)
                currentImageUrl = clothImagePath
            }
        }

        val jsonString = intent.getStringExtra("ai_json_data") ?: ""
        if (jsonString.isNotEmpty()) {
            try {
                val jsonObject = JSONObject(jsonString)
                val guide = jsonObject.optJSONObject("guide")
                if (guide != null) {
                    tvGuideTitle.text = guide.optString("title", "의류 분석 결과")
                    parsedLaundryTip = guide.optString("summary", "세탁 가이드 요약")

                    // 🌟 AI가 색상을 분석했다면 자동으로 입력창에 넣어줌 (JSON에 'color' 필드가 있다고 가정)
                    val detectedColor = guide.optString("color", "색상 미상")
                    etColor.setText(detectedColor)

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
                                warningsText += "🚨 [치명적 주의] ${warnObj.optString("icon_name")}\n${warnObj.optString("desc")}\n\n"
                            } else {
                                warningsText += "⚠️ ${warnObj.optString("icon_name")}\n${warnObj.optString("desc")}\n\n"
                            }
                        }
                    }
                    tvWarnings.text = warningsText
                    if (!hasCritical) tvWarnings.setTextColor(android.graphics.Color.parseColor("#666666"))

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

        val subCategoryMap = mapOf(
            "상의" to arrayOf("반팔", "긴팔", "아우터"),
            "하의" to arrayOf("반바지", "긴바지", "치마"),
            "고급" to arrayOf("명품", "기능성"),
            "기타" to arrayOf("양말", "속옷")
        )
        spinnerSeason.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, arrayOf("봄", "여름", "가을", "겨울"))
        spinnerMain.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, subCategoryMap.keys.toTypedArray())
        spinnerMain.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                val selectedMain = spinnerMain.selectedItem.toString()
                spinnerSub.adapter = ArrayAdapter(this@ResultActivity, android.R.layout.simple_spinner_dropdown_item, subCategoryMap[selectedMain]!!)
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }

        btnSave.setOnClickListener {
            // 🛑 1. 광클 방지: 누르자마자 즉시 버튼을 비활성화해버림!
            btnSave.isEnabled = false
            btnSave.text = "한도 확인 중..."

            val db = FirebaseFirestore.getInstance()
            val storageRef = FirebaseStorage.getInstance().reference

            // 🛑 2. 데이터를 무식하게 다 가져오지 않고, 서버에서 '개수'만 초고속으로 세어오기 (count() 사용)
            db.collection("clothes").count().get(com.google.firebase.firestore.AggregateSource.SERVER)
                .addOnSuccessListener { snapshot ->
                    val currentCount = snapshot.count

                    if (currentCount >= 100) {
                        // 한도 초과 시: 경고창 띄우고 버튼 다시 살려줌
                        Toast.makeText(this, "옷장은 최대 100장까지만 저장할 수 있습니다.", Toast.LENGTH_LONG).show()
                        btnSave.isEnabled = true
                        btnSave.text = "이대로 옷장에 저장하기"
                    } else {
                        // 한도 미만 시: 본격적인 사진 업로드 시작
                        btnSave.text = "클라우드 업로드 중..."

                        val fileUri = Uri.fromFile(File(currentImageUrl))
                        val imageRef = storageRef.child("clothes_images/${System.currentTimeMillis()}_cloth.jpg")

                        imageRef.putFile(fileUri).addOnSuccessListener {
                            imageRef.downloadUrl.addOnSuccessListener { uri ->
                                // Firestore에 저장할 데이터 구성
                                val clothData = hashMapOf(
                                    "season" to spinnerSeason.selectedItem.toString(),
                                    "mainCategory" to spinnerMain.selectedItem.toString(),
                                    "subCategory" to spinnerSub.selectedItem.toString(),
                                    "color" to etColor.text.toString(),
                                    "size" to etSize.text.toString(),
                                    "material" to etMaterial.text.toString(),
                                    "laundryTip" to parsedLaundryTip,
                                    "warnings" to tvWarnings.text.toString(), // 🌟 주의사항 데이터 추가
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
                            // 사진 업로드 실패 시 버튼 다시 살려주기
                            btnSave.isEnabled = true
                            btnSave.text = "이대로 옷장에 저장하기"
                            Toast.makeText(this, "업로드 실패", Toast.LENGTH_SHORT).show()
                        }
                    }
                }.addOnFailureListener {
                    // 개수 확인(DB 연결) 자체를 실패했을 때 버튼 다시 살려주기
                    btnSave.isEnabled = true
                    btnSave.text = "이대로 옷장에 저장하기"
                    Toast.makeText(this, "서버 통신 오류", Toast.LENGTH_SHORT).show()
                }
        }
    }
}