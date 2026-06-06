package com.example.laundrycare_android

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.location.*
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.firestore.FirebaseFirestore
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class HomeFragment : Fragment() {

    private lateinit var tvWeatherTitle: TextView
    private lateinit var tvRecommend: TextView

    private lateinit var tvClothesCount: TextView
    private lateinit var tvWashersCount: TextView
    private lateinit var tvStainsCount: TextView

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                fetchLocationAndWeather()
            }
            else -> {
                Toast.makeText(requireContext(), "위치 권한이 거부되었습니다.", Toast.LENGTH_SHORT).show()
                showFallbackWeather("위치 권한 거부됨")
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        tvWeatherTitle = view.findViewById(R.id.tvWeatherTitle)
        tvRecommend = view.findViewById(R.id.tvRecommend)

        tvClothesCount = view.findViewById(R.id.tvClothesCount)
        tvWashersCount = view.findViewById(R.id.tvWashersCount)
        tvStainsCount = view.findViewById(R.id.tvStainsCount)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        val btnCategoryMenu = view.findViewById<TextView>(R.id.btnCategoryMenu)
        btnCategoryMenu.setOnClickListener {
            startActivity(Intent(requireContext(), MyPageActivity::class.java))
        }

        view.findViewById<LinearLayout>(R.id.tabCloset).setOnClickListener {
            (activity as? MainActivity)?.findViewById<BottomNavigationView>(R.id.bottomNavigationView)?.selectedItemId = R.id.nav_closet
        }
        view.findViewById<LinearLayout>(R.id.tabWasher).setOnClickListener {
            startActivity(Intent(requireContext(), WasherListActivity::class.java))
        }
        view.findViewById<LinearLayout>(R.id.tabStain).setOnClickListener {
            (activity as? MainActivity)?.findViewById<BottomNavigationView>(R.id.bottomNavigationView)?.selectedItemId = R.id.nav_stain
        }
        view.findViewById<LinearLayout>(R.id.tabHistory).setOnClickListener {
            startActivity(Intent(requireContext(), LaundryHistoryActivity::class.java))
        }

        fetchDashboardCounts()
        checkLocationPermission()

        return view
    }

    private fun fetchDashboardCounts() {
        val db = FirebaseFirestore.getInstance()

        db.collection("clothes").get().addOnSuccessListener { snapshot ->
            tvClothesCount.text = "${snapshot.size()}벌"
        }
        db.collection("washers").get().addOnSuccessListener { snapshot ->
            tvWashersCount.text = "${snapshot.size()}대"
        }
        db.collection("stains").get().addOnSuccessListener { snapshot ->
            tvStainsCount.text = "${snapshot.size()}개"
        }
    }

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
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
                Log.e("WeatherAPI", "GPS 위치 가져오기 실패")
                showFallbackWeather("위치 가져오기 실패")
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

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("WeatherAPI", "통신 완전 실패: ${e.message}")
                activity?.runOnUiThread { showFallbackWeather("네트워크 통신 실패") }
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val responseData = response.body?.string()

                    try {
                        val json = JSONObject(responseData)
                        val weatherObj = json.getJSONObject("weather")

                        // 🌟 1. 온도 소수점 1자리로 깔끔하게 정리 (19.22 -> 19.2)
                        val rawTemp = weatherObj.getDouble("temp")
                        val formattedTemp = String.format("%.1f", rawTemp)

                        val humidity = weatherObj.getInt("humidity")

                        // 🌟 2. 튼구름, 온흐림 등 외계어 번역기 가동
                        val rawDesc = weatherObj.getString("description")
                        val cleanDescription = when {
                            rawDesc.contains("비") || rawDesc.contains("소나기") || rawDesc.contains("이슬") -> "비"
                            rawDesc.contains("눈") -> "눈"
                            rawDesc.contains("구름") || rawDesc.contains("흐") || rawDesc.contains("안개") -> "흐림"
                            rawDesc.contains("맑") || rawDesc.contains("개") -> "맑음"
                            else -> "맑음" // 애매한 건 맑음으로 퉁치기
                        }

                        // 미세먼지 파싱
                        var dustStatus = "보통"
                        if (json.has("dust")) {
                            val dustObj = json.getJSONObject("dust")
                            dustStatus = dustObj.getString("status")
                        }

                        // 메인 화면에 띄울 짧고 깔끔한 문구 (튼구름 -> 흐림)
                        val homeScreenTitle = "현재 날씨: $cleanDescription (${formattedTemp}℃)"
                        val homeScreenDesc = "습도: $humidity% / 미세먼지: $dustStatus"

                        // 추천 팝업창으로 넘길 길고 자세한 AI 분석 로직
                        val isBadWeather = cleanDescription == "비" || cleanDescription == "눈" || cleanDescription == "흐림"
                        val isDustBad = dustStatus.contains("나쁨") || dustStatus.contains("매우")
                        val isHumid = humidity > 70

                        var customRecommendation = ""
                        var customDryMethod = ""

                        if (isBadWeather) {
                            customRecommendation = "현재 날씨가 궂어서 세탁을 추천하지 않아요. 꼭 하셔야 한다면 세탁물을 모아두었다가 맑은 날 하시는 것을 권장합니다! 🌧️"
                            customDryMethod = "불가피할 경우 건조기 필수 사용"
                        } else if (isDustBad) {
                            customRecommendation = "날씨는 좋지만 미세먼지가 많아요. 😷 세탁 후 절대 밖에는 널지 마시고 햇빛이 닿는 실내에서 건조하는 것을 추천드려요!"
                            customDryMethod = "햇빛이 드는 창가 실내 건조"
                        } else if (isHumid) {
                            customRecommendation = "미세먼지도 없고 날씨도 맑지만, 습도가 높아서 빨래가 눅눅해질 수 있어요. 💦"
                            customDryMethod = "실내 건조 (제습기 또는 선풍기 활용)"
                        } else {
                            customRecommendation = "날씨가 완벽해요! ☀️ 밀린 빨래를 하기 가장 좋은 타이밍입니다! 세탁 후 실외에서 바짝 건조하는 것을 적극 추천드려요!"
                            customDryMethod = "야외 자연 건조 강력 추천"
                        }

                        // 팝업창용 공용 바구니 문자열
                        val sharedInfo = "오늘 날씨: $cleanDescription (${formattedTemp}℃) / 습도: $humidity% / 미세먼지: $dustStatus\n\n💡 $customRecommendation\n\n건조 방식: $customDryMethod"

                        activity?.runOnUiThread {
                            // UI 업데이트
                            tvWeatherTitle.text = homeScreenTitle
                            tvRecommend.text = homeScreenDesc

                            // 바구니에 저장
                            (activity as? MainActivity)?.sharedWeatherRecommend = sharedInfo
                        }
                    } catch (e: Exception) {
                        Log.e("WeatherAPI", "JSON 파싱 실패: ${e.message}")
                        activity?.runOnUiThread { showFallbackWeather("데이터 형식 변경됨") }
                    }
                } else {
                    activity?.runOnUiThread { showFallbackWeather("서버 에러 (${response.code})") }
                }
            }
        })
    }

    private fun showFallbackWeather(reason: String) {
        tvWeatherTitle.text = "현재 날씨: 맑음 (22.0℃) [$reason]"
        val fallbackText = "습도: 50% / 미세먼지: 보통"
        tvRecommend.text = fallbackText

        val defaultShared = "오늘 날씨: 맑음 (22.0℃) / 습도: 50% / 미세먼지: 보통\n\n💡 햇살이 좋아요! ☀️\n바람이 잘 통하는 곳에서 자연 건조를 추천해요.\n\n건조 방식: 자연 건조"
        (activity as? MainActivity)?.sharedWeatherRecommend = defaultShared
    }
}