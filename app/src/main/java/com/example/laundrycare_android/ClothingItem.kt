package com.example.laundrycare_android

import java.io.Serializable

data class ClothingItem(
    val imageUrl: String = "",
    val category: String = "",
    val material: String = "",
    val laundryTip: String = ""
) : Serializable

// 🌟 파이어베이스 연동 전까지 모든 옷 데이터를 쥐고 있을 임시 창고입니다!
object TempWardrobeDB {
    val myClothes = mutableListOf<ClothingItem>()
}