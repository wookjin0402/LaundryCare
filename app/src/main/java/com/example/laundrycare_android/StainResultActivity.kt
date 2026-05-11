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

        // 1. 이전 화면에서 넘어온 팀원의 완벽한 JSON 데이터 받기
        val jsonString = intent.getStringExtra("ai_json_data") ?: ""

        if (jsonString.isNotEmpty()) {
            try {
                val jsonObject = JSONObject(jsonString)
                val guide = jsonObject.optJSONObject("guide")

                if (guide != null) {
                    // 타이틀 & 요약
                    parsedTitle = guide.optString("title", "분석 결과")
                    parsedSummary = guide.optString("summary", "분석 요약입니다.")
                    tvGuideTitle.text = parsedTitle
                    tvSummary.text = parsedSummary

                    // 🌟 해시태그 파싱
                    val tagsArray = guide.optJSONArray("raw_tags")
                    var tagsText = ""
                    if (tagsArray != null) {
                        for (i in 0 until tagsArray.length()) {
                            tagsText += "#${tagsArray.getString(i)}  "
                        }
                    }
                    tvRawTags.text = tagsText

                    // 🌟 경고(Warnings) 파싱 & 빨간색 하이라이트 처리
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
                    // 치명적 에러가 없으면 기본 회색 톤으로, 있으면 빨간색 유지
                    if (!hasCritical) {
                        tvWarnings.setTextColor(android.graphics.Color.parseColor("#666666"))
                    }

                    // 🌟 케어 스텝 파싱
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

        // 2. 저장 버튼 클릭 시 파이어베이스에 저장하고 메인으로 돌아가기
        btnFinish.setOnClickListener {
            val db = FirebaseFirestore.getInstance()
            val savedData = hashMapOf(
                "title" to parsedTitle,
                "summary" to parsedSummary,
                "timestamp" to System.currentTimeMillis()
            )

            // 분석 결과를 얼룩(stains) 컬렉션에 임시로 저장합니다.
            db.collection("stains").add(savedData).addOnSuccessListener {
                Toast.makeText(this, "결과가 저장되었습니다!", Toast.LENGTH_SHORT).show()

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