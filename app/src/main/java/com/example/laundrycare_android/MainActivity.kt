package com.example.laundrycare_android

import android.content.Intent
import android.os.Bundle
import android.widget.Toast // 🌟 토스트 메시지를 띄우기 위해 추가
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.util.Stack

class MainActivity : AppCompatActivity() {

    private val tabHistory = Stack<Int>()
    private lateinit var bottomNavigationView: BottomNavigationView

    // 홈 화면에서 받아온 백엔드의 날씨 추천 문구를 저장할 공용 변수
    var sharedWeatherRecommend: String = "날씨 정보를 불러오는 중입니다..."

    // 🌟 추가: 뒤로 가기 버튼을 누른 시간을 기록하기 위한 변수
    private var backPressedTime: Long = 0

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

        // 신호 처리 로직
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

        // 🌟 뒤로 가기 콜백 수정: 앱 종료 방어 로직 적용
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (tabHistory.size > 1) {
                    tabHistory.pop()
                    val previousTabId = tabHistory.peek()
                    bottomNavigationView.selectedItemId = previousTabId
                } else {
                    // 🌟 핵심 수정: 바로 finish() 하지 않고 2초의 유예 시간을 줍니다.
                    if (System.currentTimeMillis() - backPressedTime >= 2000) {
                        backPressedTime = System.currentTimeMillis()
                        Toast.makeText(this@MainActivity, "한 번 더 누르면 종료됩니다.", Toast.LENGTH_SHORT).show()
                    } else {
                        finish() // 2초 안에 다시 누르면 진짜 종료
                    }
                }
            }
        })
    }

    override fun onNewIntent(intent: Intent) {
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