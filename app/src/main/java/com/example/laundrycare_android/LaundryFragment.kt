package com.example.laundrycare_android

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment

class LaundryFragment : Fragment(R.layout.fragment_laundry) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. 내 세탁기 관리
        view.findViewById<LinearLayout>(R.id.btnManageWasher)?.setOnClickListener {
            startActivity(Intent(requireContext(), WasherListActivity::class.java))
        }

        // 2. 세탁 코스 가이드
        view.findViewById<Button>(R.id.btnOpenMultiSelect)?.setOnClickListener {
            val intent = Intent(requireContext(), ClothMultiSelectActivity::class.java)
            intent.putExtra("mode", "batch")

            // 🌟 날씨 정보 동기화 후 전달
            val mainActivity = activity as? MainActivity
            val weatherGuide = mainActivity?.sharedWeatherRecommend ?: "날씨 정보를 불러오는 중입니다..."
            intent.putExtra("weatherGuide", weatherGuide)

            startActivity(intent)
        }

        // 3. 나의 세탁 기록 히스토리
        view.findViewById<LinearLayout>(R.id.btnLaundryHistory)?.setOnClickListener {
            startActivity(Intent(requireContext(), LaundryHistoryActivity::class.java))
        }

        // 4. 세탁 및 건조 시점 추천 (🌟 원상 복구: 옷 고르는 화면으로 정상 이동 및 날씨 데이터 전달)
        view.findViewById<LinearLayout>(R.id.btnTimeRecommend)?.setOnClickListener {
            val intent = Intent(requireContext(), ClothMultiSelectActivity::class.java)
            intent.putExtra("mode", "recommend")

            val mainActivity = activity as? MainActivity
            val weatherGuide = mainActivity?.sharedWeatherRecommend ?: "날씨 정보를 불러오는 중입니다..."
            intent.putExtra("weatherGuide", weatherGuide)

            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        updateWeatherUI()
    }

    private fun updateWeatherUI() {
        val tvLaundryIndexTitle = view?.findViewById<TextView>(R.id.tvLaundryIndexTitle)
        val tvLaundryIndexDesc = view?.findViewById<TextView>(R.id.tvLaundryIndexDesc)
        val mainActivity = activity as? MainActivity

        // MainActivity에 저장된 최신 날씨 정보를 가져옵니다.
        val weatherGuide = mainActivity?.sharedWeatherRecommend ?: "날씨 정보를 불러오는 중입니다..."

        tvLaundryIndexTitle?.text = "오늘의 세탁 지수"
        tvLaundryIndexDesc?.text = weatherGuide
    }
}