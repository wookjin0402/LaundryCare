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

class StainFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_stain, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tabLayout = view.findViewById<TabLayout>(R.id.tabLayoutStain)
        val viewPager = view.findViewById<ViewPager2>(R.id.viewPagerStain)

        val pagerAdapter = StainPagerAdapter(this)
        viewPager.adapter = pagerAdapter

        val tabTitles = arrayOf("전체", "음식물", "화장품", "생활/기타")

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = tabTitles[position]
        }.attach()

        view.findViewById<FloatingActionButton>(R.id.btnScanStain).setOnClickListener {
            startActivity(Intent(requireContext(), StainCameraActivity::class.java))
        }
    }
}