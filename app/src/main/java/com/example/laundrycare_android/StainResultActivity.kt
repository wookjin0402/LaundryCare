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
            val db = FirebaseFirestore.getInstance()

            db.collection("stains").get().addOnSuccessListener { snapshot ->
                if (snapshot.size() >= 10) {
                    Toast.makeText(this, "얼룩 기록은 최대 10개까지만 저장할 수 있습니다.", Toast.LENGTH_LONG).show()
                } else {
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
                    }
                }
            }.addOnFailureListener {
                Toast.makeText(this, "데이터 확인에 실패했습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}