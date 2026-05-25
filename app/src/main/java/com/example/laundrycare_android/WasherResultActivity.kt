package com.example.laundrycare_android

import android.content.Intent
import android.os.Bundle
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
import org.json.JSONObject
import java.io.IOException

class WasherResultActivity : AppCompatActivity() {

    private lateinit var tvFinalCourse: TextView
    private lateinit var cardWarning: CardView
    private lateinit var tvWarningDesc: TextView
    private lateinit var pbFinalLoading: ProgressBar
    private lateinit var btnFinishLaundry: Button
    private lateinit var rvSelectedClothes: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_washer_result)

        tvFinalCourse = findViewById(R.id.tvFinalCourse)
        cardWarning = findViewById(R.id.cardWarning)
        tvWarningDesc = findViewById(R.id.tvWarningDesc)
        pbFinalLoading = findViewById(R.id.pbFinalLoading)
        btnFinishLaundry = findViewById(R.id.btnFinishLaundry)
        rvSelectedClothes = findViewById(R.id.rvSelectedClothes)

        val washerType = intent.getStringExtra("washer_type") ?: "드럼 세탁기"
        val brand = intent.getStringExtra("brand") ?: "LG"
        val model = intent.getStringExtra("model") ?: "기본모델"

        val clothImages = intent.getStringArrayListExtra("selected_cloth_images") ?: arrayListOf()

        rvSelectedClothes.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rvSelectedClothes.adapter = SelectedClothAdapter(clothImages)

        requestFinalGuide(washerType, brand, model)

        // 🌟 [핵심 변경] 버튼 클릭 시 파이어베이스 DB에 영구 저장!
        btnFinishLaundry.setOnClickListener {
            btnFinishLaundry.isEnabled = false
            btnFinishLaundry.text = "기록 저장 중..."

            val db = FirebaseFirestore.getInstance()

            // 저장할 데이터 묶음 만들기
            val historyData = hashMapOf(
                "timestamp" to System.currentTimeMillis(),
                "washer_info" to "$brand $model ($washerType)",
                "recommended_course" to tvFinalCourse.text.toString(),
                "warning_msg" to if (cardWarning.visibility == View.VISIBLE) tvWarningDesc.text.toString() else "경고 없음",
                "clothes_images" to clothImages // 바구니에 담았던 옷 사진들
            )

            // 'laundry_history'라는 컬렉션에 쏙 집어넣기
            db.collection("laundry_history").add(historyData)
                .addOnSuccessListener {
                    Toast.makeText(this, "세탁 기록이 안전하게 저장되었습니다!", Toast.LENGTH_SHORT).show()

                    // 저장 완료 후, 깔끔하게 메인 화면으로 돌려보내기
                    val intent = Intent(this, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP // 쌓인 화면들 싹 지우기
                    startActivity(intent)
                    finish()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "기록 저장에 실패했습니다.", Toast.LENGTH_SHORT).show()
                    btnFinishLaundry.isEnabled = true
                    btnFinishLaundry.text = "이 코스로 세탁 시작하기"
                }
        }
    }

    private fun requestFinalGuide(washerType: String, brand: String, model: String) {
        val client = OkHttpClient()
        val jsonBody = JSONObject().apply {
            put("washerType", washerType)
            put("brand", brand)
            put("modelName", model)
        }.toString()

        val request = Request.Builder()
            .url("http://34.64.101.110:3000/api/washers/guide")
            .post(RequestBody.create("application/json".toMediaTypeOrNull(), jsonBody))
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread { showMockResult() }
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val responseData = response.body?.string()
                    try {
                        val json = JSONObject(responseData)
                        val finalRecommendation = json.optString("final_recommendation", "인공지능 세탁 / 냉수")
                        val conflicts = json.optString("critical_conflicts", "")

                        runOnUiThread {
                            pbFinalLoading.visibility = View.GONE
                            btnFinishLaundry.visibility = View.VISIBLE
                            tvFinalCourse.text = finalRecommendation

                            if (conflicts.isNotEmpty()) {
                                cardWarning.visibility = View.VISIBLE
                                tvWarningDesc.text = conflicts
                            }
                        }
                    } catch (e: Exception) {
                        runOnUiThread { showMockResult() }
                    }
                } else {
                    runOnUiThread { showMockResult() }
                }
            }
        })
    }

    private fun showMockResult() {
        pbFinalLoading.visibility = View.GONE
        btnFinishLaundry.visibility = View.VISIBLE

        tvFinalCourse.text = "울/섬세 코스 (냉수 20℃)"

        cardWarning.visibility = View.VISIBLE
        tvWarningDesc.text = "⚠️ 이염 주의: 검은색 청바지와 흰색 반팔티가 섞여 있습니다. 무조건 분리 세탁하세요!\n\n⚠️ 스팀 기능 해제 필수: 울 소재 니트는 수축될 위험이 있습니다."
    }

    inner class SelectedClothAdapter(private val imageUrls: List<String>) :
        RecyclerView.Adapter<SelectedClothAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val ivThumb: ImageView = view.findViewById(R.id.ivClothSelectedThumb)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_selected_cloth, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val url = imageUrls[position]
            if (url.isNotEmpty()) {
                Glide.with(holder.itemView.context).load(url).into(holder.ivThumb)
            }
        }

        override fun getItemCount(): Int = imageUrls.size
    }
}