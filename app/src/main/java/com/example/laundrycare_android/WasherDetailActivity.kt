package com.example.laundrycare_android

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONObject
import java.io.IOException

class WasherDetailActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private var documentId: String? = null

    // 🌟 로그인한 유저의 고유 UID를 가져오는 변수
    private val myUid get() = FirebaseAuth.getInstance().currentUser?.uid ?: "unknown_user"

    // 🌟 API 통신을 위한 OkHttp 클라이언트 추가
    private val client = OkHttpClient()
    private val BASE_URL = "http://34.64.101.110:3000"

    private lateinit var tvDetailBrand: TextView
    private lateinit var tvDetailModel: TextView
    private lateinit var tvDetailType: TextView
    private lateinit var ivWasherPhoto: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_washer_detail)

        val btnBack = findViewById<ImageView>(R.id.btnDetailBack)
        val btnOptionsMenu = findViewById<TextView>(R.id.btnOptionsMenu)

        tvDetailBrand = findViewById(R.id.tvDetailBrand)
        tvDetailModel = findViewById(R.id.tvDetailModel)
        tvDetailType = findViewById(R.id.tvDetailType)
        ivWasherPhoto = findViewById(R.id.ivWasherPhoto)

        documentId = intent.getStringExtra("documentId")

        btnBack.setOnClickListener { finish() }

        btnOptionsMenu.setOnClickListener {
            showBottomSheet()
        }
    }

    override fun onResume() {
        super.onResume()
        if (documentId != null) {
            loadWasherData()
        } else {
            Toast.makeText(this, "잘못된 접근입니다.", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun loadWasherData() {
        // 내 개인 창고(users/myUid/washers)에서 데이터 찾기
        db.collection("users").document(myUid).collection("washers").document(documentId!!).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val brand = document.getString("brand") ?: "알 수 없음"
                    val model = document.getString("model") ?: "모델명 없음"
                    val type = document.getString("type") ?: "알 수 없음"
                    val imageUrl = document.getString("imageUrl") ?: ""

                    tvDetailBrand.text = brand
                    tvDetailModel.text = if (model.isEmpty()) "등록된 모델명이 없습니다" else model
                    tvDetailType.text = type

                    if (imageUrl.isNotEmpty()) {
                        Glide.with(this).load(imageUrl).into(ivWasherPhoto)
                    } else {
                        ivWasherPhoto.setBackgroundColor(android.graphics.Color.parseColor("#E0E0E0"))
                    }

                    // 🌟 3번 미션: UI 세팅이 끝나면, 이 정보를 바탕으로 백엔드에 상세 스펙(specs)을 물어봅니다!
                    fetchWasherSpecsFromAPI(brand, type, model)

                } else {
                    Toast.makeText(this, "세탁기 정보를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
    }

    // 🌟 핵심 추가: 백엔드 팀원의 요구사항을 완벽하게 반영한 상세 스펙 조회 API 로직
    private fun fetchWasherSpecsFromAPI(rawBrand: String, rawType: String, rawModel: String) {

        // 1. 데이터 규격화 (앱 화면의 소문자/축약어를 DB 키값에 맞춰 변환)
        val formattedBrand = WasherMapper.toServerBrand(rawBrand)       // 예: lg -> LG
        val formattedType = WasherMapper.toServerWasherType(rawType)    // 예: 드럼 -> 드럼 세탁기
        val demoModel = "test"                                          // 시연을 위해 백엔드에서 뚫어둔 "test" 강제 세팅

        // 2. JSON 바디 생성
        val jsonBody = JSONObject().apply {
            put("brand", formattedBrand)
            put("washerType", formattedType)
            put("modelName", demoModel) // 실제 앱 런칭 때는 rawModel을 넣으면 됩니다.
        }.toString()

        // 3. POST /api/washers/specs 호출
        val request = Request.Builder()
            .url("$BASE_URL/api/washers/specs")
            .post(RequestBody.create("application/json".toMediaTypeOrNull(), jsonBody))
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Log.e("WasherDetail", "스펙 API 통신 실패: ${e.message}")
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseData = response.body?.string()
                runOnUiThread {
                    if (response.isSuccessful && responseData != null) {
                        Log.d("WasherDetail", "서버 응답 성공: $responseData")
                        // 💡 추후 UI(XML)에 스펙을 보여줄 공간(TextView)을 만들게 되면,
                        // 여기서 responseData(JSON)를 파싱해서 텍스트에 꽂아주면 됩니다.
                        Toast.makeText(this@WasherDetailActivity, "서버에서 세탁기 상세 스펙을 성공적으로 가져왔습니다!", Toast.LENGTH_SHORT).show()
                    } else {
                        Log.e("WasherDetail", "스펙 API 에러: ${response.code}")
                    }
                }
            }
        })
    }

    private fun showBottomSheet() {
        val view = layoutInflater.inflate(R.layout.layout_bottom_sheet, null)
        val dialog = BottomSheetDialog(this)
        dialog.setContentView(view)

        view.findViewById<TextView>(R.id.tvEdit).setOnClickListener {
            dialog.dismiss()
            val intent = Intent(this, WasherEditActivity::class.java)
            intent.putExtra("documentId", documentId)
            startActivity(intent)
        }

        view.findViewById<TextView>(R.id.tvDelete).setOnClickListener {
            if (documentId != null) {
                // 내 개인 창고에서 삭제
                db.collection("users").document(myUid).collection("washers").document(documentId!!).delete()
                    .addOnSuccessListener {
                        Toast.makeText(this, "세탁기가 삭제되었습니다.", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                        finish()
                    }
            }
        }

        view.findViewById<TextView>(R.id.tvCancel).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }
}