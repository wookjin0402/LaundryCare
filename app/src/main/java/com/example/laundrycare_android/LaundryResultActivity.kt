package com.example.laundrycare_android

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException

class LaundryResultActivity : AppCompatActivity() {

    private lateinit var tvFinalCourse: TextView
    private lateinit var cardWarning: CardView
    private lateinit var tvWarningDesc: TextView
    private lateinit var pbFinalLoading: ProgressBar
    private lateinit var btnFinishLaundry: Button
    private lateinit var rvSelectedClothes: RecyclerView

    private val client = OkHttpClient()
    private val BASE_URL = "http://34.64.101.110:3000"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_laundry_result)

        tvFinalCourse = findViewById(R.id.tvFinalCourse)
        cardWarning = findViewById(R.id.cardWarning)
        tvWarningDesc = findViewById(R.id.tvWarningDesc)
        pbFinalLoading = findViewById(R.id.pbFinalLoading)
        btnFinishLaundry = findViewById(R.id.btnFinishLaundry)
        rvSelectedClothes = findViewById(R.id.rvSelectedClothes)

        rvSelectedClothes.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        val brand = intent.getStringExtra("brand") ?: "기본"
        val model = intent.getStringExtra("model") ?: ""
        val washerType = intent.getStringExtra("washer_type") ?: "드럼 세탁기"
        val selectedImages = intent.getStringArrayListExtra("selected_cloth_images") ?: arrayListOf()
        val selectedClothIds = intent.getStringArrayListExtra("selected_cloth_ids") ?: arrayListOf()

        rvSelectedClothes.adapter = LaundryClothesAdapter(selectedImages)

        analyzeLaundryCourseRealAPI(washerType, brand, model, selectedClothIds)

        btnFinishLaundry.setOnClickListener {
            saveLaundryHistoryAndFinish()
        }
    }

    private fun analyzeLaundryCourseRealAPI(washerType: String, brand: String, model: String, clothIds: List<String>) {
        pbFinalLoading.visibility = View.VISIBLE
        btnFinishLaundry.visibility = View.GONE
        cardWarning.visibility = View.GONE
        tvFinalCourse.text = "서버 AI 분석 중..."

        val jsonBody = JSONObject().apply {
            put("washer_type", washerType)
            put("brand", brand)
            put("model", model)
            put("cloth_ids", JSONArray(clothIds))
        }.toString()

        val request = Request.Builder()
            .url("$BASE_URL/api/laundry/recommend")
            .post(RequestBody.create("application/json".toMediaTypeOrNull(), jsonBody))
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Log.e("API_ERROR", "통신 실패: ${e.message}")
                    showFallbackResult("서버 연결 실패")
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseData = response.body?.string()
                runOnUiThread {
                    if (response.isSuccessful && responseData != null) {
                        updateUIWithResult(responseData)
                    } else {
                        Log.e("API_ERROR", "서버 에러 코드: ${response.code}")
                        showFallbackResult("서버 응답 오류 (Code: ${response.code})")
                    }
                }
            }
        })
    }

    private fun updateUIWithResult(jsonString: String) {
        try {
            val jsonObject = JSONObject(jsonString)
            val course = jsonObject.optString("recommended_course", "표준 세탁")
            val hasWarning = jsonObject.optBoolean("has_critical_warning", false)
            val warningMsg = jsonObject.optString("warning_message", "")

            pbFinalLoading.visibility = View.GONE
            btnFinishLaundry.visibility = View.VISIBLE
            tvFinalCourse.text = course

            if (hasWarning && warningMsg.isNotEmpty()) {
                cardWarning.visibility = View.VISIBLE
                tvWarningDesc.text = warningMsg
            }

        } catch (e: Exception) {
            e.printStackTrace()
            showFallbackResult("데이터 파싱 오류")
        }
    }

    private fun showFallbackResult(reason: String) {
        runOnUiThread {
            Toast.makeText(this@LaundryResultActivity, "$reason: 기본 표준 세탁 코스를 추천합니다.", Toast.LENGTH_SHORT).show()
            pbFinalLoading.visibility = View.GONE
            btnFinishLaundry.visibility = View.VISIBLE
            tvFinalCourse.text = "기본 표준 세탁"
        }
    }

    private fun saveLaundryHistoryAndFinish() {
        btnFinishLaundry.isEnabled = false
        btnFinishLaundry.text = "기록 저장 중..."

        val washerInfoString = "${intent.getStringExtra("brand") ?: ""} ${intent.getStringExtra("model") ?: ""} (${intent.getStringExtra("washer_type") ?: ""})".trim()
        val clothesImages = intent.getStringArrayListExtra("selected_cloth_images") ?: arrayListOf()
        val warningMessage = if (cardWarning.visibility == View.VISIBLE) tvWarningDesc.text.toString() else "경고 없음"

        val db = FirebaseFirestore.getInstance()
        val historyData = hashMapOf(
            "timestamp" to System.currentTimeMillis(),
            "washer_info" to washerInfoString,
            "recommended_course" to tvFinalCourse.text.toString(),
            "warning_msg" to warningMessage,
            "clothes_images" to clothesImages
        )

        db.collection("laundry_history").add(historyData)
            .addOnSuccessListener {
                Toast.makeText(this, "세탁이 시작되었습니다! 기록이 저장되었습니다.", Toast.LENGTH_SHORT).show()

                // 🌟 수정됨: 저장이 완료되면 세탁기록 히스토리 화면으로 즉시 이동합니다.
                val intent = Intent(this, LaundryHistoryActivity::class.java)
                startActivity(intent)
                finish() // 현재 결과 화면 닫기
            }
            .addOnFailureListener {
                Toast.makeText(this, "기록 저장 실패", Toast.LENGTH_SHORT).show()
                btnFinishLaundry.isEnabled = true
                btnFinishLaundry.text = "이 코스로 세탁 시작하기"
            }
    }
}

class LaundryClothesAdapter(private val images: List<String>) :
    RecyclerView.Adapter<LaundryClothesAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivThumb: ImageView = view.findViewById(android.R.id.icon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val imageView = ImageView(parent.context).apply {
            id = android.R.id.icon
            layoutParams = ViewGroup.MarginLayoutParams(
                (70 * resources.displayMetrics.density).toInt(),
                (70 * resources.displayMetrics.density).toInt()
            ).apply { marginEnd = (12 * resources.displayMetrics.density).toInt() }
            scaleType = ImageView.ScaleType.CENTER_CROP
            setBackgroundColor(android.graphics.Color.parseColor("#E0E0E0"))
            clipToOutline = true
        }
        return ViewHolder(imageView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val imgUrl = images[position]
        if (imgUrl.isNotEmpty()) {
            if (imgUrl.startsWith("http") || imgUrl.startsWith("content")) {
                Glide.with(holder.itemView.context).load(imgUrl).into(holder.ivThumb)
            } else {
                Glide.with(holder.itemView.context).load(File(imgUrl)).into(holder.ivThumb)
            }
        }
    }

    override fun getItemCount(): Int = images.size
}