object WasherMapper {
    // 세탁기 타입 변환
    fun toServerWasherType(type: String): String = when (type) {
        "드럼" -> "드럼 세탁기"
        "일반" -> "일반 세탁기"
        else -> type
    }

    // 브랜드 변환
    fun toServerBrand(brand: String): String = brand.uppercase() // lg -> LG
}