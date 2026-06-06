package com.example.laundrycare_android

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class StainPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
    // 탭 종류 세팅
    private val categories = arrayOf("전체", "음식물", "화장품", "생활/기타")

    override fun getItemCount(): Int = categories.size

    override fun createFragment(position: Int): Fragment {
        // 선택된 탭 이름("전체", "음식물" 등)을 넘겨주며 알맹이 프래그먼트 생성
        return StainListFragment.newInstance(categories[position])
    }
}