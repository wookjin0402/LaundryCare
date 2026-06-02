package com.example.laundrycare_android

import android.content.Intent // 🌟 이 부분이 추가되었습니다!
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.util.Stack

class MainActivity : AppCompatActivity() {

    private val tabHistory = Stack<Int>()
    private lateinit var bottomNavigationView: BottomNavigationView

    // 🌟 홈 화면에서 받아온 백엔드의 날씨 추천 문구를 저장할 공용 변수
    var sharedWeatherRecommend: String = "날씨 정보를 불러오는 중입니다..."

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bottomNavigationView = findViewById(R.id.bottomNavigationView)

        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> replaceFragment(HomeFragment())
                R.id.nav_stain -> replaceFragment(StainFragment())
                R.id.nav_closet -> replaceFragment(WardrobeFragment())
                R.id.nav_care -> replaceFragment(CareFragment())
                R.id.nav_laundry -> replaceFragment(LaundryFragment())
                else -> return@setOnItemSelectedListener false
            }

            if (tabHistory.isEmpty() || tabHistory.peek() != item.itemId) {
                tabHistory.push(item.itemId)
            }
            true
        }

        // 🌟 신호 처리 로직
        if (savedInstanceState == null) {
            val navigateToFragment = intent.getStringExtra("navigate_to_fragment")
            val navigateTo = intent.getStringExtra("navigate_to")

            val targetId = when {
                navigateToFragment == "stain" -> R.id.nav_stain
                navigateTo == "closet" -> R.id.nav_closet
                navigateTo == "stain" -> R.id.nav_stain
                else -> R.id.nav_home
            }
            bottomNavigationView.selectedItemId = targetId
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (tabHistory.size > 1) {
                    tabHistory.pop()
                    val previousTabId = tabHistory.peek()
                    bottomNavigationView.selectedItemId = previousTabId
                } else {
                    finish()
                }
            }
        })
    }

    override fun onNewIntent(intent: android.content.Intent) { // 🌟 타입 명시적으로 수정
        super.onNewIntent(intent)
        setIntent(intent)
        val navigateToFragment = intent.getStringExtra("navigate_to_fragment")
        if (navigateToFragment == "stain") {
            bottomNavigationView.selectedItemId = R.id.nav_stain
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(R.anim.fade_in_fast, R.anim.fade_out_fast)
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}