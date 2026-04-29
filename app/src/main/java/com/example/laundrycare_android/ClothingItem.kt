package com.example.laundrycare_android

import java.io.Serializable

data class ClothingItem(
    val imageUrl: String = "",
    val category: String = "",
    val material: String = "",
    val laundryTip: String = ""
) : Serializable