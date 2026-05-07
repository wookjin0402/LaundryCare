package com.example.laundrycare_android

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class WardrobePagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    private val seasons = listOf("봄", "여름", "가을", "겨울")

    override fun getItemCount(): Int = seasons.size

    override fun createFragment(position: Int): Fragment {
        // 각 탭마다 현재 계절이 무엇인지 글자를 담아서 SeasonFragment를 생성합니다.
        return SeasonFragment.newInstance(seasons[position])
    }
}