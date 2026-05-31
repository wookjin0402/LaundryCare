package com.example.laundrycare_android

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.firestore.FirebaseFirestore
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONObject
import java.io.File
import java.io.IOException

class StainResultActivity : AppCompatActivity() {

    private lateinit var ivStainPhoto: ImageView
    private lateinit var viewBoundingBox: View
    private lateinit var pbLoading: ProgressBar
    private lateinit var cardSolution: CardView
    private lateinit var tvCareTip: TextView
    private lateinit var btnSaveStain: Button

    private var currentImagePath = ""
    private var finalStainType = ""
    private var finalCareTip = ""

    private val client = OkHttpClient()
    private val BASE_URL = "http://34.64.101.110:3000"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stain_result)

        // 🌟 새로 추가한 뒤로가기 버튼 클릭 이벤트 연동
        findViewById<ImageView>(R.id.btnStainResultBack).setOnClickListener {
            finish()
        }

        ivStainPhoto = findViewById(R.id.ivStainPhoto)
        viewBoundingBox = findViewById(R.id.viewBoundingBox)
        pbLoading = findViewById(R.id.pbLoading)
        cardSolution = findViewById(R.id.cardSolution)
        tvCareTip = findViewById(R.id.tvCareTip)
        btnSaveStain = findViewById(R.id.btnSaveStain)

        currentImagePath = intent.getStringExtra("stain_image_path") ?: ""
        if (currentImagePath.isNotEmpty()) {
            Glide.with(this).load(File(currentImagePath)).into(ivStainPhoto)
            analyzeStainImage()
        } else {
            Toast.makeText(this, "이미지를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show()
            finish()
        }

        btnSaveStain.setOnClickListener { saveStainDataToFirebase() }
    }

    private fun analyzeStainImage() {
        val file = File(currentImagePath)
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("stainImage", file.name, RequestBody.create("image/jpeg".toMediaTypeOrNull(), file))
            .build()

        val request = Request.Builder().url("$BASE_URL/api/stains/analyze").post(requestBody).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread { showFallbackMockUI("서버 연결 실패") }
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val responseData = response.body?.string()
                    try {
                        val json = JSONObject(responseData)
                        val box = json.optJSONObject("box")
                        runOnUiThread {
                            pbLoading.visibility = View.GONE
                            if (box != null) {
                                drawBoundingBox(box.getInt("x"), box.getInt("y"), box.getInt("w"), box.getInt("h"))
                            }
                            showBottomSheet()
                        }
                    } catch (e: Exception) {
                        runOnUiThread { showFallbackMockUI("데이터 파싱 오류") }
                    }
                } else {
                    Log.e("API_ERROR", "분석 에러 코드: ${response.code}")
                    runOnUiThread { showFallbackMockUI("서버 500 에러") }
                }
            }
        })
    }

    private fun showFallbackMockUI(reason: String) {
        Toast.makeText(this, "$reason: 임시 UI로 테스트를 진행합니다.", Toast.LENGTH_SHORT).show()
        pbLoading.visibility = View.GONE
        drawBoundingBox(250, 400, 400, 400)
        showBottomSheet()
    }

    private fun drawBoundingBox(x: Int, y: Int, w: Int, h: Int) {
        viewBoundingBox.visibility = View.VISIBLE
        viewBoundingBox.setBackgroundResource(R.drawable.box_border)

        val params = viewBoundingBox.layoutParams as FrameLayout.LayoutParams
        params.leftMargin = x
        params.topMargin = y
        params.width = w
        params.height = h
        viewBoundingBox.layoutParams = params
    }

    private fun showBottomSheet() {
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.bottom_sheet_stain, null)
        dialog.setContentView(view)

        val btnCoffee = view.findViewById<Button>(R.id.btnCoffee)
        val btnMakeup = view.findViewById<Button>(R.id.btnMakeup)
        val btnBlood = view.findViewById<Button>(R.id.btnBlood)

        val clickListener = View.OnClickListener { v ->
            finalStainType = (v as Button).text.toString().replace(Regex("[^가-힣 ]"), "").trim()
            dialog.dismiss()
            pbLoading.visibility = View.VISIBLE
            requestStainGuide()
        }

        btnCoffee.setOnClickListener(clickListener)
        btnMakeup.setOnClickListener(clickListener)
        btnBlood.setOnClickListener(clickListener)

        dialog.setCancelable(false)
        dialog.show()
    }

    private fun requestStainGuide() {
        val jsonBody = JSONObject().apply {
            put("uid", "user_123")
            put("clothId", "cloth_123")
            put("stainType", finalStainType)
        }.toString()

        val request = Request.Builder()
            .url("$BASE_URL/api/stains/guide")
            .post(RequestBody.create("application/json".toMediaTypeOrNull(), jsonBody))
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread { showFallbackGuide() }
            }

            override fun onResponse(call: Call, response: Response) {
                runOnUiThread { pbLoading.visibility = View.GONE }
                if (response.isSuccessful) {
                    val responseData = response.body?.string()
                    try {
                        val json = JSONObject(responseData)
                        finalCareTip = json.optString("care_tip", "가이드를 찾을 수 없습니다.")
                        runOnUiThread {
                            cardSolution.visibility = View.VISIBLE
                            tvCareTip.text = finalCareTip
                            btnSaveStain.visibility = View.VISIBLE
                        }
                    } catch (e: Exception) {
                        runOnUiThread { showFallbackGuide() }
                    }
                } else {
                    runOnUiThread { showFallbackGuide() }
                }
            }
        })
    }

    private fun showFallbackGuide() {
        finalCareTip = "($finalStainType) 주방세제와 식초를 1:1로 섞어 칫솔로 가볍게 두드리며 닦아주세요. (임시 데이터)"
        cardSolution.visibility = View.VISIBLE
        tvCareTip.text = finalCareTip
        btnSaveStain.visibility = View.VISIBLE
    }

    private fun saveStainDataToFirebase() {
        btnSaveStain.isEnabled = false
        val db = FirebaseFirestore.getInstance()

        db.collection("stains").count().get(com.google.firebase.firestore.AggregateSource.SERVER)
            .addOnSuccessListener { snapshot ->
                if (snapshot.count >= 10) {
                    Toast.makeText(this, "얼룩 기록은 최대 10개까지만 저장할 수 있습니다.", Toast.LENGTH_LONG).show()
                    btnSaveStain.isEnabled = true
                } else {
                    val savedData = hashMapOf(
                        "stainType" to finalStainType,
                        "solution" to finalCareTip,
                        "timestamp" to System.currentTimeMillis()
                    )
                    db.collection("stains").add(savedData).addOnSuccessListener {
                        Toast.makeText(this, "얼룩 홈케어 기록이 저장되었습니다!", Toast.LENGTH_SHORT).show()
                        Handler(Looper.getMainLooper()).postDelayed({
                            val intent = Intent(this, MainActivity::class.java)
                            intent.putExtra("navigate_to", "stain")
                            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                            startActivity(intent)
                            finish()
                        }, 500)
                    }.addOnFailureListener {
                        btnSaveStain.isEnabled = true
                        Toast.makeText(this, "저장 실패", Toast.LENGTH_SHORT).show()
                    }
                }
            }
    }
}