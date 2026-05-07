package com.example.laundrycare_android

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment

class LaundryFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.activity_laundry, container, false)

        // 🌟 화면 안의 ◀ 버튼 클릭 시 뒤로가기
        val btnBack = view.findViewById<Button>(R.id.btnBack)
        btnBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        return view
    }
}