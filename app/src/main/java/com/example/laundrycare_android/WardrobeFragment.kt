package com.example.laundrycare_android

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class WardrobeFragment : Fragment() {

    // 1. 화면(도화지)을 깔아주는 기본 함수
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_wardrobe, container, false)
    }

    // 🌟 2. [우리가 추가한 핵심 코드!] 도화지가 깔린 직후에 진열대를 세팅하는 함수 🌟
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 화면(XML)에서 진열대(RecyclerView) 찾아오기
        val recyclerView = view.findViewById<RecyclerView>(R.id.rvWardrobe)

        // 리스트를 위아래(세로)로 스크롤되게 방향 설정
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // 아까 고용한 직원(Adapter) 부르기! (창고 데이터도 같이 넘겨줌)
        val adapter = ClothingAdapter(ClothingRepository.itemList)
        recyclerView.adapter = adapter

        // 옷장 탭을 누르고 들어올 때마다 최신 상태로 싹 새로고침
        adapter.notifyDataSetChanged()
    }
}