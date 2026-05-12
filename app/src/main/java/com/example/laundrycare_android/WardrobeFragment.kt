package com.example.laundrycare_android

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class WardrobeFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_wardrobe, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnBack = view.findViewById<Button>(R.id.btnBack)
        btnBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        // 🌟 새로 바꾼 카테고리 탭 아이디 연결
        val tabLayout = view.findViewById<TabLayout>(R.id.tabLayoutCategory)
        val viewPager = view.findViewById<ViewPager2>(R.id.viewPagerWardrobe)

        val pagerAdapter = WardrobePagerAdapter(this)
        viewPager.adapter = pagerAdapter

        // 🌟 기존 "봄, 여름..." 대신 대분류 카테고리로 탭 이름 교체
        val tabTitles = arrayOf("전체", "상의", "하의", "고급", "기타")
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = tabTitles[position]
        }.attach()
    }
}