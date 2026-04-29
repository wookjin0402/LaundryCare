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
        val view = inflater.inflate(R.layout.fragment_wardrobe, container, false)

        recyclerView = view.findViewById(R.id.rvWardrobe)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // 가상의 옷 데이터 추가 (나중에는 스캔 화면에서 넘겨받게 됩니다)
        loadMockData()

        adapter = ClothingAdapter(clothingList)
        recyclerView.adapter = adapter

        return view
    }

    private fun loadMockData() {
        clothingList.add(ClothingItem("https://images.unsplash.com/photo-1521572163474-6864f9cf17ab", "흰색 면 티셔츠", "면 100%", "30도 물세탁"))
        clothingList.add(ClothingItem("https://images.unsplash.com/photo-1576566588028-4147f3842f27", "검은색 후드티", "폴리에스터 혼방", "건조기 금지"))
        clothingList.add(ClothingItem("https://images.unsplash.com/photo-1542272604-780c8d52a5ce", "청바지", "데님", "단독 세탁"))
    }
}