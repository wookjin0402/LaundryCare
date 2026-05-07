package com.example.laundrycare_android

import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.util.Stack

class MainActivity : AppCompatActivity() {

    // 🌟 [핵심 무기] 하단바 메뉴를 클릭한 순서를 저장하는 '기억 바구니'
    private val tabHistory = Stack<Int>()
    private lateinit var bottomNavigationView: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bottomNavigationView = findViewById(R.id.bottomNavigationView)

        // 앱을 처음 켰을 때: 홈 화면 띄우고 기억 바구니에 '홈' 메뉴 저장
        if (savedInstanceState == null) {
            replaceFragment(HomeFragment())
            tabHistory.push(R.id.nav_home)
        }

        // 🌟 [스마트폰 뒤로가기 버튼 강제 제어]
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // 바구니에 현재 화면과 이전 화면(총 2개 이상)이 들어있다면?
                if (tabHistory.size > 1) {
                    tabHistory.pop() // 1. 지금 보고 있는 화면 기록은 바구니에서 버림
                    val previousTabId = tabHistory.peek() // 2. 바구니 맨 위에 있는 '이전 화면 메뉴 ID' 꺼내기

                    // 3. 하단바 메뉴를 이전 상태로 강제 클릭 (이렇게 하면 화면도 알아서 따라 바뀝니다!)
                    bottomNavigationView.selectedItemId = previousTabId
                } else {
                    // 바구니에 1개(홈 화면)만 남았을 때 뒤로가기를 누르면 앱 종료
                    finish()
                }
            }
        })

        // 하단바 메뉴를 눌렀을 때의 동작
        bottomNavigationView.setOnItemSelectedListener { item ->
            // 화면 교체
            when (item.itemId) {
                R.id.nav_home -> replaceFragment(HomeFragment())
                R.id.nav_stain -> replaceFragment(StainFragment())
                R.id.nav_closet -> replaceFragment(WardrobeFragment())
                R.id.nav_care -> replaceFragment(CareFragment())
                R.id.nav_laundry -> replaceFragment(LaundryFragment())
                else -> return@setOnItemSelectedListener false
            }

            // 🌟 내가 직접 터치해서 이동했을 때만 바구니에 이동한 메뉴를 추가합니다.
            // (뒤로가기 버튼을 눌러서 자동으로 이동했을 때는 중복으로 쌓이지 않게 막아줍니다.)
            if (tabHistory.isEmpty() || tabHistory.peek() != item.itemId) {
                tabHistory.push(item.itemId)
            }

            true
        }
    }

    // 안드로이드의 골치 아픈 화면 기억 기능(BackStack)은 빼버리고, 순수하게 화면만 갈아 끼웁니다.
    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            // 🌟 안드로이드 기본(android.R.anim...) 대신 우리가 만든 빠른 파일(R.anim...)로 변경!
            .setCustomAnimations(R.anim.fade_in_fast, R.anim.fade_out_fast)
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}