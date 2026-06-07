package com.example.laundrycare_android

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class StainActivity : AppCompatActivity() {

    private lateinit var btnBack: ImageView
    private lateinit var tvStainTitle: TextView
    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager2
    private lateinit var btnScanStain: FloatingActionButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stain) // 제공해주신 레이아웃과 정확히 매칭

        // 🌟 ID 매칭 완료: 제공하신 XML의 ID와 동일하게 설정
        btnBack = findViewById(R.id.btnBack)
        tvStainTitle = findViewById(R.id.tvStainTitle)
        tabLayout = findViewById(R.id.tabLayoutStain)
        viewPager = findViewById(R.id.viewPagerStain)
        btnScanStain = findViewById(R.id.btnScanStain)

        // 뒤로가기 버튼 이벤트
        btnBack.setOnClickListener { finish() }

        // 🌟 Scan 버튼 (이미지 추가 화면으로 이동)
        btnScanStain.setOnClickListener {
            val intent = Intent(this, StainCameraActivity::class.java)
            startActivity(intent)
        }

        // ViewPager2와 TabLayout 연동 (Adapter 필요)
        // 여기에 StainTabPagerAdapter 등을 연결하여 구현하시면 됩니다.
        // 예시: viewPager.adapter = StainTabPagerAdapter(this)

        // TabLayout과 ViewPager2 연결 예시
        // TabLayoutMediator(tabLayout, viewPager) { tab, position ->
        //     tab.text = "탭 이름"
        // }.attach()
    }
}