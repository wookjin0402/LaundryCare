package com.example.laundrycare_android.model

data class LaundryItem(
    val id: String = "",
    val imageUrl: String = "",

    // AI가 1차로 분석해서 준 정답
    val aiMajor: String = "", // 예: 상의
    val aiMinor: String = "", // 예: 반팔
    val aiSeason: String = "", // 예: 여름

    // 최종적으로 파이어베이스에 들어갈 데이터 (초기엔 AI값, 사용자가 바꾸면 바꾼 값)
    var finalMajor: String = "",
    var finalMinor: String = "",
    var finalSeason: String = ""
)