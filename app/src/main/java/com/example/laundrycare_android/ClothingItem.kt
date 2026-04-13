package com.example.laundrycare_android

// 옷 하나하나의 정보를 담는 설계도 (데이터 클래스)
data class ClothingItem(
    val category: String,   // 상의, 하의, 세탁기 등
    val material: String,   // 면, 울, 소재 정보
    val laundryTip: String, // 세탁 방법 (찬물 세탁 등)
    val imageUri: String? = null // 사진 경로 (나중에 사진 띄울 때 사용)
)

// 앱 어디서든 이 창고에 접근해서 옷을 넣고 뺄 수 있게 해주는 '공용 창고' (싱글톤 객체)
object ClothingRepository {
    // 진짜 옷들이 담기는 바구니
    val itemList = mutableListOf<ClothingItem>()

    // 바구니에 새 옷을 넣는 기능
    fun addItem(item: ClothingItem) {
        itemList.add(0, item) // 최신순으로 보여주기 위해 목록의 맨 처음에 추가
    }
}