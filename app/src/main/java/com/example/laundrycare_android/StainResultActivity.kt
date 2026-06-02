package com.example.laundrycare_android

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

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
    private var selectedClothType = ""

    private val client = OkHttpClient()
    private val BASE_URL = "http://34.64.101.110:3000"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stain_result)

        findViewById<ImageView>(R.id.btnStainResultBack).setOnClickListener { finish() }

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

        btnSaveStain.setOnClickListener { saveStainWithImage() }
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

                        // 🌟 1. 백엔드에서 계산해 준 네모 박스 좌표 데이터 추출
                        val box = json.optJSONObject("box")

                        // 🌟 2. 백엔드에서 매핑 완료한 얼룩 종류 및 신뢰도 데이터 추출
                        finalStainType = json.optString("stainType", "일반 오염")
                        val confidence = json.optDouble("confidence", 0.0)

                        runOnUiThread {
                            pbLoading.visibility = View.GONE

                            // 🌟 3. 화면에 얼룩 네모 박스 시각화
                            if (box != null) {
                                drawBoundingBox(box.getInt("x"), box.getInt("y"), box.getInt("w"), box.getInt("h"))
                            }

                            // 🌟 4. 분석 결과 토스트 알림 (예: 커피 (95%))
                            val confPercent = (confidence * 100).toInt()
                            Toast.makeText(this@StainResultActivity, "AI 인식: $finalStainType ($confPercent%)", Toast.LENGTH_SHORT).show()

                            // 🌟 5. 수동 선택 팝업을 건너뛰고, 곧바로 의류 재질 선택 다이얼로그 실행!
                            showClothSelectionDialog()
                        }
                    } catch (e: Exception) {
                        runOnUiThread { showFallbackMockUI("데이터 파싱 오류") }
                    }
                } else {
                    runOnUiThread { showFallbackMockUI("서버 500 에러") }
                }
            }
        })
    }

    private fun showFallbackMockUI(reason: String) {
        Toast.makeText(this, "$reason: 임시 데이터로 진행합니다.", Toast.LENGTH_SHORT).show()
        pbLoading.visibility = View.GONE
        drawBoundingBox(250, 400, 400, 400)
        finalStainType = "커피" // 실패 시 기본값 세팅
        showClothSelectionDialog()
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

    private fun showClothSelectionDialog() {
        val clothTypes = arrayOf("일반(면/폴리)", "민감성(실크/울)", "가죽/모피")
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("어떤 재질의 옷인가요?")
            .setCancelable(false)
            .setItems(clothTypes) { _, which ->
                selectedClothType = clothTypes[which]
                pbLoading.visibility = View.VISIBLE

                determineCareGuideLocally()
            }
            .show()
    }

    private fun determineCareGuideLocally() {
        pbLoading.visibility = View.GONE

        if (selectedClothType == "민감성(실크/울)" || selectedClothType == "가죽/모피") {
            finalCareTip = "⚠️ 세탁소 방문 권장\n해당 재질($selectedClothType)은 홈케어 시 옷감이 손상될 위험이 매우 높습니다. 전문가(세탁소)에게 맡기시는 것을 강력히 권장합니다."
            cardSolution.setBackgroundColor(android.graphics.Color.parseColor("#FFEBEE"))
        } else {
            finalCareTip = when (finalStainType) {
                "커피" -> "☕ 커피 얼룩:\n따뜻한 물과 주방세제(또는 식초)를 1:1로 섞어 칫솔로 톡톡 두드린 후 세탁하세요."
                "화장품" -> "💄 화장품 얼룩:\n클렌징 오일이나 폼을 묻혀 살살 문지른 후, 미온수로 헹궈내세요."
                "피", "혈흔" -> "🩸 혈흔:\n절대 뜨거운 물을 쓰지 마시고, 찬물과 과산화수소로 닦아내세요."
                else -> "✨ $finalStainType 오염:\n중성세제를 미온수에 풀어 애벌빨래를 진행한 후 세탁하세요."
            }
            cardSolution.setBackgroundColor(android.graphics.Color.parseColor("#E8F5E9"))
        }

        cardSolution.visibility = View.VISIBLE
        tvCareTip.text = finalCareTip
        btnSaveStain.visibility = View.VISIBLE
    }

    private fun saveStainWithImage() {
        btnSaveStain.isEnabled = false
        btnSaveStain.text = "저장 중..."
        pbLoading.visibility = View.VISIBLE // 🌟 에러 수정 완료

        val fileUri = Uri.fromFile(File(currentImagePath))
        val storageRef = FirebaseStorage.getInstance().reference.child("stains/stain_${System.currentTimeMillis()}.jpg")

        storageRef.putFile(fileUri)
            .addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    saveDataToFirestore(downloadUri.toString())
                }.addOnFailureListener {
                    Toast.makeText(this, "이미지 URL 획득 실패", Toast.LENGTH_SHORT).show()
                    resetSaveButton()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "이미지 업로드 실패", Toast.LENGTH_SHORT).show()
                resetSaveButton()
            }
    }

    private fun saveDataToFirestore(imageUrl: String) {
        val db = FirebaseFirestore.getInstance()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.KOREA)
        val today = dateFormat.format(Date())

        val savedData = hashMapOf(
            "stainType" to finalStainType,
            "clothType" to selectedClothType,
            "solution" to finalCareTip,
            "imageUrl" to imageUrl,
            "date" to today,
            "timestamp" to System.currentTimeMillis()
        )

        db.collection("stains").add(savedData)
            .addOnSuccessListener {
                pbLoading.visibility = View.GONE
                Toast.makeText(this, "얼룩 기록이 완벽하게 저장되었습니다!", Toast.LENGTH_SHORT).show()

                Handler(Looper.getMainLooper()).postDelayed({
                    val intent = Intent(this, MainActivity::class.java)
                    intent.putExtra("navigate_to", "stain")
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                    startActivity(intent)
                    finish()
                }, 500)
            }
            .addOnFailureListener {
                Toast.makeText(this, "DB 저장 실패", Toast.LENGTH_SHORT).show()
                resetSaveButton()
            }
    }

    private fun resetSaveButton() {
        pbLoading.visibility = View.GONE
        btnSaveStain.isEnabled = true
        btnSaveStain.text = "얼룩 기록 저장하기"
    }
}