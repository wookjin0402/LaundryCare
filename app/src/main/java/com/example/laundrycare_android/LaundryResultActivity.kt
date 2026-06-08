package com.example.laundrycare_android

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
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
    private val myUid get() = FirebaseAuth.getInstance().currentUser?.uid ?: "unknown_user"

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

        btnFinishLaundry.setOnClickListener { saveLaundryHistoryAndFinish() }
    }

    private fun analyzeLaundryCourseRealAPI(washerType: String, brand: String, model: String, clothIds: List<String>) {
        pbFinalLoading.visibility = View.VISIBLE
        btnFinishLaundry.visibility = View.GONE
        tvFinalCourse.text = "서버 AI 분석 중..."

        val jsonBody = JSONObject().apply {
            put("uid", myUid)
            put("washer_type", washerType)
            put("brand", brand.uppercase())
            put("model", model)
            put("cloth_ids", JSONArray(clothIds))
        }.toString()

        val request = Request.Builder()
            .url("$BASE_URL/api/laundry/recommend")
            .post(RequestBody.create("application/json".toMediaTypeOrNull(), jsonBody))
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread { showFallbackResult("서버 연결 실패") }
            }
            override fun onResponse(call: Call, response: Response) {
                val responseData = response.body?.string()
                runOnUiThread {
                    if (response.isSuccessful && responseData != null) updateUIWithResult(responseData)
                    else showFallbackResult("오류 (Code: ${response.code})")
                }
            }
        })
    }

    private fun updateUIWithResult(jsonString: String) {
        val jsonObject = JSONObject(jsonString)
        tvFinalCourse.text = jsonObject.optString("recommended_course", "표준 세탁")
        val hasWarning = jsonObject.optBoolean("has_critical_warning", false)
        val warningMsg = jsonObject.optString("warning_message", "")
        pbFinalLoading.visibility = View.GONE
        btnFinishLaundry.visibility = View.VISIBLE
        if (hasWarning && warningMsg.isNotEmpty()) {
            cardWarning.visibility = View.VISIBLE
            tvWarningDesc.text = warningMsg
        }
    }

    private fun showFallbackResult(reason: String) {
        pbFinalLoading.visibility = View.GONE
        btnFinishLaundry.visibility = View.VISIBLE
        tvFinalCourse.text = "기본 표준 세탁"
    }

    private fun saveLaundryHistoryAndFinish() {
        btnFinishLaundry.isEnabled = false

        val brand = intent.getStringExtra("brand") ?: "기본"
        val model = intent.getStringExtra("model") ?: ""
        val washerType = intent.getStringExtra("washer_type") ?: "세탁기"
        val selectedClothIds = intent.getStringArrayListExtra("selected_cloth_ids") ?: arrayListOf()
        val formattedWasherName = "${brand.uppercase()} $model ($washerType)".trim()
        val courseUsed = tvFinalCourse.text.toString()

        // 🌟 최종 수정 완료된 JSON 구성
        val jsonBody = JSONObject().apply {
            put("uid", myUid)
            put("clothIds", JSONArray(selectedClothIds))
            put("washerName", formattedWasherName)
            put("courseUsed", courseUsed)
        }.toString()

        Log.d("DEBUG_WASHER_SAVE", "백엔드 전송 데이터: $jsonBody")

        val request = Request.Builder()
            .url("$BASE_URL/api/clothes/wash-complete")
            .post(RequestBody.create("application/json".toMediaTypeOrNull(), jsonBody))
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    btnFinishLaundry.isEnabled = true
                    Toast.makeText(this@LaundryResultActivity, "연결 실패", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                runOnUiThread {
                    if (response.isSuccessful) {
                        Toast.makeText(this@LaundryResultActivity, "세탁 기록 저장 성공!", Toast.LENGTH_SHORT).show()

                        Handler(Looper.getMainLooper()).postDelayed({
                            val mainIntent = Intent(this@LaundryResultActivity, MainActivity::class.java)
                            mainIntent.putExtra("navigate_to_fragment", "laundry")
                            mainIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                            startActivity(mainIntent)
                            finish()
                        }, 500)
                    } else {
                        btnFinishLaundry.isEnabled = true
                        val responseData = response.body?.string()
                        Log.e("DEBUG_WASHER_SAVE", "서버 응답 에러: $responseData")
                        Toast.makeText(this@LaundryResultActivity, "저장 실패(Code: ${response.code})", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }
}

class LaundryClothesAdapter(private val images: List<String>) : RecyclerView.Adapter<LaundryClothesAdapter.ViewHolder>() {
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) { val ivThumb: ImageView = view.findViewById(android.R.id.icon) }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val iv = ImageView(parent.context).apply {
            id = android.R.id.icon
            layoutParams = ViewGroup.MarginLayoutParams(150, 150).apply { marginEnd = 20 }
            scaleType = ImageView.ScaleType.CENTER_CROP
            setBackgroundColor(0xFFE0E0E0.toInt())
        }
        return ViewHolder(iv)
    }
    override fun onBindViewHolder(h: ViewHolder, p: Int) {
        Glide.with(h.itemView.context).load(if(images[p].startsWith("http")) images[p] else File(images[p])).into(h.ivThumb)
    }
    override fun getItemCount() = images.size
}