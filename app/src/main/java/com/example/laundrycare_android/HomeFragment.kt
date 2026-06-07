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
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.cardview.widget.CardView
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

        // 뷰 초기화
        tvWeatherTitle = view.findViewById(R.id.tvWeatherTitle)
        tvRecommend = view.findViewById(R.id.tvRecommend)
        tvClothesCount = view.findViewById(R.id.tvClothesCount)
        tvWashersCount = view.findViewById(R.id.tvWashersCount)
        tvStainsCount = view.findViewById(R.id.tvStainsCount)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        // 1. 마이페이지 버튼
        view.findViewById<TextView>(R.id.btnCategoryMenu).setOnClickListener {
            startActivity(Intent(requireContext(), MyPageActivity::class.java))
        }

        // 2. 3단 메인 버튼 클릭 리스너
        // 🌟 확실하게 수정: 세탁 하러가기 버튼 클릭 시 '세탁 코스 가이드(ClothMultiSelectActivity)'로 즉시 이동!
        view.findViewById<CardView>(R.id.btnGoLaundry).setOnClickListener {
            val intent = Intent(requireContext(), ClothMultiSelectActivity::class.java)
            intent.putExtra("mode", "batch")
            startActivity(intent)
        }

        // 🌟 옷 등록하기
        view.findViewById<CardView>(R.id.btnRegisterCloth).setOnClickListener {
            startActivity(Intent(requireContext(), ScanActivity::class.java))
        }

        // 🌟 얼룩 지우기
        view.findViewById<CardView>(R.id.btnEraseStain).setOnClickListener {
            startActivity(Intent(requireContext(), StainActivity::class.java))
        }

        // 3. 대시보드 탭 클릭 리스너
        view.findViewById<CardView>(R.id.tabCloset).setOnClickListener {
            (activity as? MainActivity)?.findViewById<BottomNavigationView>(R.id.bottomNavigationView)?.selectedItemId = R.id.nav_closet
        }
        view.findViewById<CardView>(R.id.tabWasher).setOnClickListener {
            startActivity(Intent(requireContext(), WasherListActivity::class.java))
        }
        view.findViewById<CardView>(R.id.tabStain).setOnClickListener {
            (activity as? MainActivity)?.findViewById<BottomNavigationView>(R.id.bottomNavigationView)?.selectedItemId = R.id.nav_stain
        }
        view.findViewById<CardView>(R.id.tabHistory).setOnClickListener {
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
        val request = Request.Builder().url(url).post(requestBody).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("WeatherAPI", "통신 완전 실패: ${e.message}")
                activity?.runOnUiThread { showFallbackWeather("네트워크 통신 실패") }
            }
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val responseData = response.body?.string()
                    try {
                        val json = JSONObject(responseData!!)
                        val weatherObj = json.getJSONObject("weather")
                        val rawTemp = weatherObj.getDouble("temp")
                        val formattedTemp = String.format("%.1f", rawTemp)
                        val humidity = weatherObj.getInt("humidity")
                        val rawDesc = weatherObj.getString("description")
                        val cleanDescription = when {
                            rawDesc.contains("비") || rawDesc.contains("소나기") || rawDesc.contains("이슬") -> "비"
                            rawDesc.contains("눈") -> "눈"
                            rawDesc.contains("구름") || rawDesc.contains("흐") || rawDesc.contains("안개") -> "흐림"
                            rawDesc.contains("맑") || rawDesc.contains("개") -> "맑음"
                            else -> "맑음"
                        }
                        var dustStatus = "보통"
                        if (json.has("dust")) {
                            val dustObj = json.getJSONObject("dust")
                            dustStatus = dustObj.getString("status")
                        }
                        val homeScreenTitle = "현재 날씨: $cleanDescription (${formattedTemp}℃)"
                        val homeScreenDesc = "습도: $humidity% / 미세먼지: $dustStatus"

                        activity?.runOnUiThread {
                            tvWeatherTitle.text = homeScreenTitle
                            tvRecommend.text = homeScreenDesc
                        }
                    } catch (e: Exception) {
                        Log.e("WeatherAPI", "JSON 파싱 실패: ${e.message}")
                    }               }
            }
        })
    }

    private fun showFallbackWeather(reason: String) {
        tvWeatherTitle.text = "현재 날씨: 맑음 (22.0℃) [$reason]"
        tvRecommend.text = "습도: 50% / 미세먼지: 보통"
    }
}