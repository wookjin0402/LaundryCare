package com.example.laundrycare_android

import android.content.Intent // 👈 팝업 띄우려면 승차권(Intent)이 필요해서 추가했습니다!
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        // 앱을 처음 켰을 때 기본으로 '홈' 화면을 보여줌
        if (savedInstanceState == null) {
            replaceFragment(HomeFragment())
        }

        // 하단 메뉴 클릭 시 동작 설정
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    replaceFragment(HomeFragment())
                    true // 탭 선택됨(색칠) 허용
                }

                // 🌟 [우리가 수술한 스캔 버튼!] 🌟
                R.id.nav_scan -> {
                    // 조각(Fragment) 교체 대신, 영롱님의 카메라 창(Activity)을 확 띄웁니다!
                    val intent = Intent(this, CameraActivity::class.java)
                    startActivity(intent)

                    // 핵심 디테일: 카메라 창을 띄운 뒤, 하단 탭의 색칠된 표시는
                    // 원래 머물던 탭(예: 홈)에 그대로 유지하도록 false를 줍니다!
                    false
                }

                R.id.nav_wardrobe -> {
                    replaceFragment(WardrobeFragment())
                    true
                }
                R.id.nav_settings -> {
                    replaceFragment(SettingsFragment())
                    true
                }
                else -> false
            }
        }
    }

    // 화면을 교체해 주는 핵심 함수
    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.frame_layout, fragment)
            .commit()
    }
}