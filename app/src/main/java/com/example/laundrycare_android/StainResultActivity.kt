package com.example.laundrycare_android

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import org.json.JSONObject

class StainResultActivity : AppCompatActivity() {

    private var parsedTitle = ""
    private var parsedSummary = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stain_result)

        val btnBack = findViewById<Button>(R.id.btnBack)
        val btnFinish = findViewById<Button>(R.id.btnFinish)
        val tvGuideTitle = findViewById<TextView>(R.id.tvGuideTitle)
        val tvRawTags = findViewById<TextView>(R.id.tvRawTags)
        val tvSummary = findViewById<TextView>(R.id.tvSummary)
        val tvWarnings = findViewById<TextView>(R.id.tvWarnings)
        val tvCareSteps = findViewById<TextView>(R.id.tvCareSteps)

        btnBack.setOnClickListener { finish() }

        val jsonString = intent.getStringExtra("ai_json_data") ?: ""

        if (jsonString.isNotEmpty()) {
            try {
                val jsonObject = JSONObject(jsonString)
                val guide = jsonObject.optJSONObject("guide")

                if (guide != null) {
                    parsedTitle = guide.optString("title", "분석 결과")
                    parsedSummary = guide.optString("summary", "분석 요약입니다.")
                    tvGuideTitle.text = parsedTitle
                    tvSummary.text = parsedSummary

                    val tagsArray = guide.optJSONArray("raw_tags")
                    var tagsText = ""
                    if (tagsArray != null) {
                        for (i in 0 until tagsArray.length()) {
                            tagsText += "#${tagsArray.getString(i)}  "
                        }
                    }
                    tvRawTags.text = tagsText

                    val warningsArray = guide.optJSONArray("warnings")
                    var warningsText = ""
                    var hasCritical = false
                    if (warningsArray != null) {
                        for (i in 0 until warningsArray.length()) {
                            val warnObj = warningsArray.getJSONObject(i)
                            val isCritical = warnObj.optBoolean("is_critical", false)
                            val iconName = warnObj.optString("icon_name", "주의")
                            val desc = warnObj.optString("desc", "")

                            if (isCritical) {
                                hasCritical = true
                                warningsText += "🚨 [치명적 주의] $iconName\n$desc\n\n"
                            } else {
                                warningsText += "⚠️ $iconName\n$desc\n\n"
                            }
                        }
                    }
                    tvWarnings.text = warningsText
                    if (!hasCritical) {
                        tvWarnings.setTextColor(android.graphics.Color.parseColor("#666666"))
                    }

                    val stepsArray = guide.optJSONArray("careSteps")
                    var stepsText = ""
                    if (stepsArray != null) {
                        for (i in 0 until stepsArray.length()) {
                            val stepObj = stepsArray.getJSONObject(i)
                            val stepTitle = stepObj.optString("step", "단계")
                            val desc = stepObj.optString("desc", "")
                            stepsText += "✅ $stepTitle\n$desc\n\n"
                        }
                    }
                    tvCareSteps.text = stepsText
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "결과를 읽는 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
            }
        }

        btnFinish.setOnClickListener {
            // 🛑 1. 광클 방지: 누르자마자 즉시 버튼을 비활성화!
            btnFinish.isEnabled = false

            val db = FirebaseFirestore.getInstance()

            // 🛑 2. 데이터를 무식하게 다 가져오지 않고, 서버에서 '개수'만 초고속으로 세어오기 (count() 사용)
            db.collection("stains").count()
                .get(com.google.firebase.firestore.AggregateSource.SERVER)
                .addOnSuccessListener { snapshot ->
                    if (snapshot.count >= 10) {
                        // 한도 10개 초과 시: 경고창 띄우고 버튼 다시 살려줌
                        Toast.makeText(this, "얼룩 기록은 최대 10개까지만 저장할 수 있습니다.", Toast.LENGTH_LONG)
                            .show()
                        btnFinish.isEnabled = true
                    } else {
                        // 한도 미만 시: 본격적인 데이터 저장 시작
                        val savedData = hashMapOf(
                            "season" to "여름",
                            "mainCategory" to "상의",
                            "subCategory" to "반팔",
                            "stainType" to parsedTitle,
                            "solution" to parsedSummary,
                            "timestamp" to System.currentTimeMillis()
                        )

                        db.collection("stains").add(savedData).addOnSuccessListener {
                            Toast.makeText(this, "결과가 저장되었습니다!", Toast.LENGTH_SHORT).show()

                            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                val intent = Intent(this, MainActivity::class.java)
                                intent.putExtra("navigate_to", "stain")
                                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                                startActivity(intent)
                                finish()
                            }, 500)
                        }.addOnFailureListener {
                            // 저장 실패 시 버튼 살려주기
                            Toast.makeText(this, "저장에 실패했습니다.", Toast.LENGTH_SHORT).show()
                            btnFinish.isEnabled = true
                        }
                    }
                }.addOnFailureListener {
                    // 개수 확인 실패 시 버튼 살려주기
                    Toast.makeText(this, "데이터 확인에 실패했습니다.", Toast.LENGTH_SHORT).show()
                    btnFinish.isEnabled = true
                }
        }
    }
}