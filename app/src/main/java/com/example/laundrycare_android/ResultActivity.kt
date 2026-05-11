package com.example.laundrycare_android

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import org.json.JSONObject

class ResultActivity : AppCompatActivity() {

    private var parsedLaundryTip = "" // AI가 내려준 summary 저장용
    private var currentImageUrl = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)

        val btnBack = findViewById<Button>(R.id.btnBack)
        val btnSave = findViewById<Button>(R.id.btnSave)

        val tvGuideTitle = findViewById<TextView>(R.id.tvGuideTitle)
        val tvRawTags = findViewById<TextView>(R.id.tvRawTags)
        val tvWarnings = findViewById<TextView>(R.id.tvWarnings)
        val tvCareSteps = findViewById<TextView>(R.id.tvCareSteps)

        val spinnerSeason = findViewById<Spinner>(R.id.spinnerSeason)
        val spinnerMain = findViewById<Spinner>(R.id.spinnerMain)
        val spinnerSub = findViewById<Spinner>(R.id.spinnerSub)
        val etMaterial = findViewById<EditText>(R.id.etMaterial)

        btnBack.setOnClickListener { finish() }

        // 🌟 1. AI JSON 파싱 후 화면에 그리기
        val jsonString = intent.getStringExtra("ai_json_data") ?: ""
        if (jsonString.isNotEmpty()) {
            try {
                val jsonObject = JSONObject(jsonString)
                val guide = jsonObject.optJSONObject("guide")

                if (guide != null) {
                    tvGuideTitle.text = guide.optString("title", "의류 분석 결과")
                    parsedLaundryTip = guide.optString("summary", "세탁 가이드 요약")

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
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "결과 파싱 오류", Toast.LENGTH_SHORT).show()
            }
        }

        // 🌟 2. 옷장 카테고리 스피너 3단계 설정
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

        // 🌟 3. 파이어베이스 [옷장(clothes) 컬렉션]에 완벽하게 저장
        btnSave.setOnClickListener {
            val db = FirebaseFirestore.getInstance()

            val clothData = hashMapOf(
                "season" to spinnerSeason.selectedItem.toString(),
                "mainCategory" to spinnerMain.selectedItem.toString(),
                "subCategory" to spinnerSub.selectedItem.toString(),
                "material" to etMaterial.text.toString(),
                "laundryTip" to parsedLaundryTip, // 화면에 뜬 요약본을 세탁 팁으로 저장
                "imageUrl" to currentImageUrl, // 차후 이미지 스토리지 연동 시 사용
                "timestamp" to System.currentTimeMillis()
            )

            db.collection("clothes").add(clothData).addOnSuccessListener {
                Toast.makeText(this, "옷장에 완벽하게 저장되었습니다!", Toast.LENGTH_SHORT).show()

                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    val intent = Intent(this, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                    startActivity(intent)
                    finish()
                }, 500)
            }
        }
    }
}