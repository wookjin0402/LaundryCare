package com.example.laundrycare_android

data class ClothingItem(
    val id: String,
    val imageUrl: String,
    val season: String,        // 계절 (봄, 여름, 가을, 겨울)
    val mainCategory: String,  // 대분류 (상의, 하의, 고급, 기타)
    val subCategory: String,   // 소분류 (반팔, 긴팔, 아우터 등)
    val material: String,
    val laundryTip: String,
    val warnings: String = ""
)