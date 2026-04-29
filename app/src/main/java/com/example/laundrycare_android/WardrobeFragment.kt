package com.example.laundrycare_android

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class WardrobeFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ClothingAdapter
    private val clothingList = mutableListOf<ClothingItem>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // 1. 화면(도화지)을 깔아주는 기본 함수
        return inflater.inflate(R.layout.fragment_wardrobe, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 왕만두님이 작성하신 리스트 뷰 연결
        recyclerView = view.findViewById(R.id.rvWardrobe)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // 가상의 옷 데이터 불러오기
        loadMockData()

        // 🌟 조원 분의 '저장 신호' (IS_SAVED) 로직 융합
        val isSaved = activity?.intent?.getBooleanExtra("IS_SAVED", false) ?: false
        if (isSaved) {
            // 결과 화면에서 저장 버튼을 누르고 넘어왔다면, 리스트 맨 위에 방금 스캔한(가상의) 옷을 추가합니다!
            val newItem = ClothingItem(
                imageUrl = "https://images.unsplash.com/photo-1542272604-780c8d52a5ce",
                category = "방금 스캔한 하의 (새로 저장됨)",
                material = "면 100%",
                laundryTip = "30도 물세탁, 다림질 가능"
            )
            clothingList.add(0, newItem) // 맨 위에 옷 추가
        }

        adapter = ClothingAdapter(clothingList)
        recyclerView.adapter = adapter
    }

    private fun loadMockData() {
        clothingList.add(ClothingItem("https://images.unsplash.com/photo-1521572163474-6864f9cf17ab", "흰색 면 티셔츠", "면 100%", "30도 물세탁"))
        clothingList.add(ClothingItem("https://images.unsplash.com/photo-1576566588028-4147f3842f27", "검은색 후드티", "폴리에스터 혼방", "건조기 금지"))
    }
}