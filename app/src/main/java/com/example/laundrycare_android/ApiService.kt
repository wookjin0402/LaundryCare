// ApiService.kt
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.Response

interface ApiService {
    // ... 기존 코드 ...

    // 새로 추가할 세탁 히스토리 저장 API
    @POST("/api/clothes/wash-complete")
    suspend fun saveWashHistory(@Body request: WashHistoryRequest): Response<Void>
}

// 요청 데이터 형식
data class WashHistoryRequest(
    val uid: String,
    val clothIds: List<String>,
    val washerName: String,
    val courseUsed: String
)