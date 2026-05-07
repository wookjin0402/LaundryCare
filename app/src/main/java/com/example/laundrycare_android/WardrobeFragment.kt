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

        // 🌟 얼룩 탭과 완벽하게 똑같이 작동하는 뒤로가기 기능 🌟
        val btnBack = view.findViewById<Button>(R.id.btnBack)
        btnBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        val tabLayout = view.findViewById<TabLayout>(R.id.tabLayoutSeason)
        val viewPager = view.findViewById<ViewPager2>(R.id.viewPagerWardrobe)

        // 화면 넘김 관리자 장착
        val pagerAdapter = WardrobePagerAdapter(this)
        viewPager.adapter = pagerAdapter

        // 탭과 뷰페이저(화면)를 동기화하는 핵심 코드
        val tabTitles = arrayOf("봄", "여름", "가을", "겨울")
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = tabTitles[position]
        }.attach()
    }
}