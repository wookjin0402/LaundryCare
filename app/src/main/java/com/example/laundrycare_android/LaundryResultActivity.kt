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

    // 실제 서버 통신을 위한 OkHttpClient 및 기본 URL 세팅
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

        // 리사이클러뷰 가로 방향 설정
        rvSelectedClothes.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        // 이전 화면에서 넘겨받은 데이터 세팅
        val brand = intent.getStringExtra("brand") ?: "기본"
        val model = intent.getStringExtra("model") ?: ""
        val washerType = intent.getStringExtra("washer_type") ?: "드럼 세탁기"
        val selectedImages = intent.getStringArrayListExtra("selected_cloth_images") ?: arrayListOf()
        val selectedClothIds = intent.getStringArrayListExtra("selected_cloth_ids") ?: arrayListOf()

        // 🌟 이름 충돌을 피하기 위해 LaundryClothesAdapter 사용
        rvSelectedClothes.adapter = LaundryClothesAdapter(selectedImages)

        // 진짜 서버 API 호출 시작
        analyzeLaundryCourseRealAPI(washerType, brand, model, selectedClothIds)

        // 세탁 시작 버튼 클릭 이벤트
        btnFinishLaundry.setOnClickListener {
            saveLaundryHistoryAndFinish()
        }
    }

    /**
     * 앱 단독이 아닌, Node.js 백엔드 서버로 데이터를 보내 진짜 AI 분석 결과를 받아옵니다.
     */
    private fun analyzeLaundryCourseRealAPI(washerType: String, brand: String, model: String, clothIds: List<String>) {
        pbFinalLoading.visibility = View.VISIBLE
        btnFinishLaundry.visibility = View.GONE
        cardWarning.visibility = View.GONE
        tvFinalCourse.text = "서버 AI 분석 중..."

        // 1. 서버로 보낼 JSON 데이터 만들기 (세탁기 정보 + 옷 ID 배열)
        val jsonBody = JSONObject().apply {
            put("washer_type", washerType)
            put("brand", brand)
            put("model", model)
            put("cloth_ids", JSONArray(clothIds))
        }.toString()

        // 2. 백엔드의 세탁 추천 API 주소로 POST 요청 세팅
        val request = Request.Builder()
            .url("$BASE_URL/api/laundry/recommend")
            .post(RequestBody.create("application/json".toMediaTypeOrNull(), jsonBody))
            .build()

        // 3. 비동기로 실제 서버 요청 보내기
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

    /**
     * 서버에서 받아온 진짜 JSON 결과를 파싱하여 화면에 뿌려주는 함수
     */
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

    /**
     * 서버가 꺼져있거나 통신 실패 시 튕기지 않도록 방어하는 임시 UI 함수
     * 🌟 메인 스레드 충돌 방지를 위해 runOnUiThread 적용 완료
     */
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

        // 🌟 히스토리 화면과 데이터를 맞추기 위해 수정된 부분
        val washerInfoString = "${intent.getStringExtra("brand") ?: ""} ${intent.getStringExtra("model") ?: ""} (${intent.getStringExtra("washer_type") ?: ""})".trim()
        val clothesImages = intent.getStringArrayListExtra("selected_cloth_images") ?: arrayListOf()
        val warningMessage = if (cardWarning.visibility == View.VISIBLE) tvWarningDesc.text.toString() else "경고 없음"

        val db = FirebaseFirestore.getInstance()
        // 🌟 LaundryHistoryActivity에서 요구하는 Key 값에 정확히 맞추어 데이터 저장
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
                val intent = Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                startActivity(intent)
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "기록 저장 실패", Toast.LENGTH_SHORT).show()
                btnFinishLaundry.isEnabled = true
                btnFinishLaundry.text = "이 코스로 세탁 시작하기"
            }
    }
}

// 가로형 옷 요약 리스트 어댑터
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