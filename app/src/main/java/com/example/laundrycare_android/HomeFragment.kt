package com.example.laundrycare_android

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.location.*
import okhttp3.*
import org.json.JSONObject
import java.io.IOException

class HomeFragment : Fragment() {

    private lateinit var tvWeatherTitle: TextView
    private lateinit var tvRecommend: TextView
    private lateinit var tvCostSaving: TextView

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // 위치 권한 요청
    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                fetchLocationAndWeather()
            }
            else -> {
                Toast.makeText(requireContext(), "위치 권한이 거부되었습니다.", Toast.LENGTH_SHORT).show()
                showFallbackWeather()
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
        tvCostSaving = view.findViewById(R.id.tvCostSaving)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        val btnScan = view.findViewById<Button>(R.id.btnScan)
        btnScan.setOnClickListener {
            startActivity(Intent(requireContext(), CameraActivity::class.java))
        }

        val btnCategoryMenu = view.findViewById<TextView>(R.id.btnCategoryMenu)
        btnCategoryMenu.setOnClickListener {
            startActivity(Intent(requireContext(), MyPageActivity::class.java))
        }

        // 화면 켜질 때 권한 체크 및 날씨 로드
        checkLocationPermission()

        return view
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
                showFallbackWeather()
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    private fun sendLocationToServer(lat: Double, lon: Double) {
        val client = OkHttpClient()
        val url = "http://34.64.101.110:3000/api/weather/recommend?lat=$lat&lon=$lon"
        val request = Request.Builder().url(url).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                activity?.runOnUiThread { showFallbackWeather() }
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val responseData = response.body?.string()
                    try {
                        val json = JSONObject(responseData)
                        val weather = json.getString("weather")
                        val recommend = json.getString("recommend")
                        val savedCost = json.getInt("savedCost")

                        activity?.runOnUiThread {
                            tvWeatherTitle.text = "현재 날씨: $weather"
                            tvRecommend.text = recommend
                            tvCostSaving.text = "건조 절감액: ${String.format("%,d", savedCost)}원"
                        }
                    } catch (e: Exception) {
                        activity?.runOnUiThread { showFallbackWeather() }
                    }
                } else {
                    activity?.runOnUiThread { showFallbackWeather() }
                }
            }
        })
    }

    private fun showFallbackWeather() {
        tvWeatherTitle.text = "현재 날씨: 맑음 (22℃)"
        tvRecommend.text = "햇살이 좋아요! ☀️\n바람이 잘 통하는 곳에서 자연 건조를 추천해요."
        tvCostSaving.text = "💡 예상 건조기 절감 비용: 1,500원"
    }
}