package com.example.laundrycare_android

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class WardrobePagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    // 🌟 '계절' 대신 우리가 사용할 '카테고리' 리스트로 완전히 교체합니다.
    private val categories = listOf("전체", "상의", "하의", "고급", "기타")

    override fun getItemCount(): Int = categories.size

    override fun createFragment(position: Int): Fragment {
        // 기존 화면 재활용: 이제 SeasonFragment로 "여름"이 아니라 "상의", "하의" 같은 카테고리 이름표가 넘어갑니다!
        return SeasonFragment.newInstance(categories[position])
    }
}