package com.example.laundrycare_android

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class StainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stain)

        val btnBack = findViewById<ImageView>(R.id.btnBack)
        btnBack.setOnClickListener {
            finish()
        }

        // 🌟 탭 레이아웃과 뷰페이저 세팅
        val tabLayout = findViewById<TabLayout>(R.id.tabLayoutStain)
        val viewPager = findViewById<ViewPager2>(R.id.viewPagerStain)

        // 액티비티 전용 뷰페이저 어댑터 연결
        val pagerAdapter = StainPagerAdapter(this)
        viewPager.adapter = pagerAdapter

        // 🌟 카테고리 이름 배열 및 탭 결합
        val tabTitles = arrayOf("전체", "음식물", "화장품", "생활/기타")
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = tabTitles[position]
        }.attach()

        // 🌟 플로팅 버튼(+) 클릭 시 카메라 화면으로 이동
        val btnScanStain = findViewById<FloatingActionButton>(R.id.btnScanStain)
        btnScanStain.setOnClickListener {
            val intent = Intent(this, StainCameraActivity::class.java)
            startActivity(intent)
        }
    }

    // 🌟 액티비티 내부에서만 쓰는 뷰페이저 어댑터 클래스
    inner class StainPagerAdapter(activity: AppCompatActivity) : FragmentStateAdapter(activity) {
        private val categories = arrayOf("전체", "음식물", "화장품", "생활/기타")

        override fun getItemCount(): Int = categories.size

        override fun createFragment(position: Int): Fragment {
            // 아까 새로 만든 StainListFragment를 카테고리별로 생성해서 던져줌!
            return StainListFragment.newInstance(categories[position])
        }
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }
}