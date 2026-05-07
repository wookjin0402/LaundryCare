package com.example.laundrycare_android

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class StainFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // 🌟 에러의 원인이었던 부분을 fragment_scan으로 확실하게 수정했습니다.
        return inflater.inflate(R.layout.fragment_scan, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. 우측 하단 + 버튼 설정
        val fabAddStain = view.findViewById<FloatingActionButton>(R.id.fabAddStain)
        fabAddStain.setOnClickListener {
            // + 버튼을 누르면 기존 '얼룩 스캔 및 정보 입력' 화면(StainActivity)으로 넘어갑니다.
            val intent = Intent(requireContext(), StainActivity::class.java)
            startActivity(intent)
        }

        // 2. 얼룩 리스트(RecyclerView) 기본 틀 설정
        val rvStainList = view.findViewById<RecyclerView>(R.id.rvStainList)
        rvStainList.layoutManager = LinearLayoutManager(requireContext())
    }
}