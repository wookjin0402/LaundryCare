package com.example.laundrycare_android

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.floatingactionbutton.FloatingActionButton
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

        // ❌ 뒤로가기 버튼 클릭 이벤트 완전히 삭제 완료

        val tabLayout = view.findViewById<TabLayout>(R.id.tabLayoutCategory)
        val viewPager = view.findViewById<ViewPager2>(R.id.viewPagerWardrobe)

        val pagerAdapter = WardrobePagerAdapter(this)
        viewPager.adapter = pagerAdapter

        val tabTitles = arrayOf("전체", "상의", "하의", "고급", "기타")
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = tabTitles[position]
        }.attach()

        // 🌟 플로팅 버튼(+) 클릭 시 카메라 스캔 액티비티로 이동
        val fabAddCloth = view.findViewById<FloatingActionButton>(R.id.fabAddCloth)
        fabAddCloth.setOnClickListener {
            startActivity(Intent(requireContext(), CameraActivity::class.java))
        }
    }
}