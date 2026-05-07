package com.example.laundrycare_android

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment

class HomeFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        // 1. 커다란 스캔 버튼 -> CameraActivity 연결
        val btnScan = view.findViewById<Button>(R.id.btnScan)
        btnScan.setOnClickListener {
            val intent = Intent(requireContext(), CameraActivity::class.java)
            startActivity(intent)
        }

        // 🌟 2. 우측 상단 메뉴(≡) 버튼 -> "MyPageActivity"(진짜 내 정보)로 정확히 연결!
        val btnCategoryMenu = view.findViewById<TextView>(R.id.btnCategoryMenu)
        btnCategoryMenu.setOnClickListener {
            val intent = Intent(requireContext(), MyPageActivity::class.java)
            startActivity(intent)
        }

        return view
    }
}