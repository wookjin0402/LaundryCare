package com.example.laundrycare_android

import android.content.Intent
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.util.Stack

class MainActivity : AppCompatActivity() {

    private val tabHistory = Stack<Int>()
    private lateinit var bottomNavigationView: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bottomNavigationView = findViewById(R.id.bottomNavigationView)

        // (이전에 있던 테스트용 버튼 코드는 깔끔하게 삭제되었습니다)

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

        if (savedInstanceState == null) {
            val navigateTo = intent.getStringExtra("navigate_to")
            val targetId = when (navigateTo) {
                "closet" -> R.id.nav_closet
                "stain" -> R.id.nav_stain
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

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(R.anim.fade_in_fast, R.anim.fade_out_fast)
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}