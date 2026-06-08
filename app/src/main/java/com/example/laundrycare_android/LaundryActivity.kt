package com.example.laundrycare_android

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Looper
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class LaundryActivity : AppCompatActivity() {

    // 🌟 팝업에 넘겨줄 '진짜 날씨 텍스트'를 담을 그릇 (기본값 설정)
    private var currentWeatherGuide: String = "날씨 정보를 불러오는 중입니다..."

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // 위치 권한 요청 런처
    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false)) {
            fetchLocationAndWeather()
        } else {
            currentWeatherGuide = "위치 권한이 없어 날씨를 불러올 수 없습니다."
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_laundry)

        // 🌟 화면이 켜지자마자 위치와 날씨 데이터를 백그라운드에서 가져옵니다.
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        checkLocationPermission()

        val btnBack = findViewById<ImageView>(R.id.btnBack)
        btnBack.setOnClickListener {
            finish()
        }

        val btnTimeRecommend = findViewById<LinearLayout>(R.id.btnTimeRecommend)
        val btnManageWasher = findViewById<LinearLayout>(R.id.btnManageWasher)
        val btnOpenMultiSelect = findViewById<Button>(R.id.btnOpenMultiSelect)
        val btnLaundryHistory = findViewById<LinearLayout>(R.id.btnLaundryHistory)

        btnTimeRecommend.setOnClickListener {
            val conditionList = arrayOf("땀을 많이 흘렸어요", "커피/음식물 얼룩이 묻었어요", "잠깐 입어서 깨끗해요")

            AlertDialog.Builder(this)
                .setTitle("현재 빨랫감의 상태는 어떤가요?")
                .setSingleChoiceItems(conditionList, -1) { dialog, which ->
                    val resultMessage = when(which) {
                        0 -> "🌤 현재 맑고 건조함\n➡️ 당장 '전체 세탁' 후 '자연 건조'를 추천합니다!"
                        1 -> "🚨 오염 감지\n➡️ 얼룩이 굳기 전에 '부분 세척' 후 전체 세탁하세요."
                        2 -> "💨 가벼운 착용\n➡️ 세탁기 대신 '스타일러(환기) 후 보관'을 추천합니다."
                        else -> ""
                    }
                    dialog.dismiss()

                    AlertDialog.Builder(this)
                        .setTitle("AI 맞춤 추천")
                        .setMessage(resultMessage)
                        .setPositiveButton("확인", null)
                        .show()
                }
                .setNegativeButton("취소", null)
                .show()
        }

        btnManageWasher.setOnClickListener {
            val intent = Intent(this, WasherListActivity::class.java)
            startActivity(intent)
        }

        // 🌟 핵심 수정: 옷 고르는 화면으로 넘어갈 때, 완성된 날씨 텍스트를 인텐트에 담아 보냅니다!
        btnOpenMultiSelect.setOnClickListener {
            val intent = Intent(this, ClothMultiSelectActivity::class.java)
            intent.putExtra("weatherGuide", currentWeatherGuide)
            startActivity(intent)
        }

        btnLaundryHistory.setOnClickListener {
            val intent = Intent(this, LaundryHistoryActivity::class.java)
            startActivity(intent)
        }
    }

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fetchLocationAndWeather()
        } else {
            locationPermissionRequest.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }
    }

    private fun fetchLocationAndWeather() {
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    sendLocationToServer(location.latitude, location.longitude)
                } else {
                    val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000).build()
                    fusedLocationClient.requestLocationUpdates(locationRequest, object : LocationCallback() {
                        override fun onLocationResult(p0: LocationResult) {
                            val newLocation = p0.lastLocation
                            if (newLocation != null) sendLocationToServer(newLocation.latitude, newLocation.longitude)
                            fusedLocationClient.removeLocationUpdates(this)
                        }
                    }, Looper.getMainLooper())
                }
            }.addOnFailureListener {
                currentWeatherGuide = "GPS 위치를 찾을 수 없습니다."
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    private fun sendLocationToServer(lat: Double, lon: Double) {
        val client = OkHttpClient()
        val url = "http://34.64.101.110:3000/api/weather/recommend"
        val jsonBody = JSONObject().apply {
            put("lat", lat)
            put("lon", lon)
        }
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = jsonBody.toString().toRequestBody(mediaType)
        val request = Request.Builder().url(url).post(requestBody).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                currentWeatherGuide = "서버 연결에 실패하여 날씨를 가져올 수 없습니다."
            }
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val responseData = response.body?.string()
                    try {
                        val json = JSONObject(responseData!!)
                        val weatherObj = json.getJSONObject("weather")
                        val rawTemp = weatherObj.getDouble("temp")
                        val humidity = weatherObj.getInt("humidity")
                        val rawDesc = weatherObj.getString("description")

                        // 날씨에 맞춰서 팝업창에 띄워줄 똑똑한 건조 팁 생성
                        val cleanDescription = when {
                            rawDesc.contains("비") || rawDesc.contains("소나기") || rawDesc.contains("이슬") -> "비"
                            rawDesc.contains("눈") -> "눈"
                            rawDesc.contains("구름") || rawDesc.contains("흐") || rawDesc.contains("안개") -> "흐림"
                            rawDesc.contains("맑") || rawDesc.contains("개") -> "맑음"
                            else -> "맑음"
                        }

                        val tip = when (cleanDescription) {
                            "비" -> "비가 오고 습도가 높습니다. 실내 건조 시 제습기 사용을 강력히 권장합니다."
                            "눈" -> "눈이 내리고 기온이 낮습니다. 실내 건조와 잦은 환기가 필요합니다."
                            "흐림" -> "날씨가 흐리고 햇빛이 적습니다. 여건이 된다면 건조기 사용을 추천합니다."
                            else -> "날씨가 맑습니다! 야외 자연 건조하기 아주 좋은 날씨입니다."
                        }

                        // 🌟 옷 매칭 완료 팝업에 들어갈 최종 문장 세팅
                        val tempStr = String.format("%.1f", rawTemp)
                        currentWeatherGuide = "현재 날씨: $cleanDescription (기온: ${tempStr}℃, 습도: $humidity%)\n👉 $tip"

                    } catch (e: Exception) {
                        currentWeatherGuide = "날씨 데이터를 분석하는 중 오류가 발생했습니다."
                    }
                } else {
                    currentWeatherGuide = "날씨 서버와 통신할 수 없습니다. (Code: ${response.code})"
                }
            }
        })
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }
}