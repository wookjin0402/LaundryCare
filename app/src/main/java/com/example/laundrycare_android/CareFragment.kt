package com.example.laundrycare_android

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment

class CareFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // fragment_care.xml 화면을 불러옵니다.
        val view = inflater.inflate(R.layout.fragment_care, container, false)

        // 방금 만든 '계절별 의류 보관 관리' 버튼을 찾습니다.
        val btnSeasonalStorage = view.findViewById<Button>(R.id.btnSeasonalStorage)

        // 🌟 버튼을 누르면 우리가 뼈대를 짜둔 보관 관리 화면으로 넘어갑니다!
        btnSeasonalStorage.setOnClickListener {
            startActivity(Intent(requireContext(), SeasonalStorageActivity::class.java))
        }

        return view
    }
}