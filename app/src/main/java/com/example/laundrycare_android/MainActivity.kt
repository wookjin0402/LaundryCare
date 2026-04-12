package com.example.laundrycare_android

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetDialog

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
                    true
                }

                // 🌟 [수정된 부분] 스캔 버튼 클릭 시 바로 카메라로 안 가고 선택창(다이얼로그) 띄우기! 🌟
                R.id.nav_scan -> {
                    showScanOptionDialog() // 👈 여기서 방금 만든 함수를 부릅니다!
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

    // 🌟 [수정된 부분] MainActivity(Activity)에 맞춰서 requireContext() -> this 로 전부 변경! 🌟
    private fun showScanOptionDialog() {
        // 1. 밑에서 올라오는 다이얼로그 객체 생성 (this 사용)
        val bottomSheet = BottomSheetDialog(this)

        val view = layoutInflater.inflate(R.layout.dialog_scan_option, null)

        // 2. [의류 스캔] 버튼 클릭 시
        view.findViewById<Button>(R.id.btnClothScan).setOnClickListener {
            val intent = Intent(this, CameraActivity::class.java)
            // 📦 데이터 택배 싸기: "이건 CLOTH(의류) 모드야!"
            intent.putExtra("scanType", "CLOTH")
            startActivity(intent)
            bottomSheet.dismiss() // 창 닫기
        }

        // 3. [세탁기 스캔] 버튼 클릭 시
        view.findViewById<Button>(R.id.btnMachineScan).setOnClickListener {
            val intent = Intent(this, CameraActivity::class.java)
            // 📦 데이터 택배 싸기: "이건 MACHINE(세탁기) 모드야!"
            intent.putExtra("scanType", "MACHINE")
            startActivity(intent)
            bottomSheet.dismiss() // 창 닫기
        }

        bottomSheet.setContentView(view)
        bottomSheet.show()
    }
}