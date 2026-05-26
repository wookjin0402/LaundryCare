package com.example.laundrycare_android

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import org.json.JSONObject
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class LaundryResultActivity : AppCompatActivity() {

    private lateinit var tvFinalCourse: TextView
    private lateinit var cardWarning: CardView
    private lateinit var tvWarningDesc: TextView
    private lateinit var pbFinalLoading: ProgressBar
    private lateinit var btnFinishLaundry: Button
    private lateinit var rvSelectedClothes: RecyclerView

    // 비동기 처리를 위한 워커 스레드 (ANR 방지)
    private lateinit var executorService: ExecutorService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_laundry_result) // 우리가 만든 파일명으로!

        tvFinalCourse = findViewById(R.id.tvFinalCourse)
        cardWarning = findViewById(R.id.cardWarning)
        tvWarningDesc = findViewById(R.id.tvWarningDesc)
        pbFinalLoading = findViewById(R.id.pbFinalLoading)
        btnFinishLaundry = findViewById(R.id.btnFinishLaundry)
        rvSelectedClothes = findViewById(R.id.rvSelectedClothes)

        executorService = Executors.newSingleThreadExecutor()

        // 리사이클러뷰 가로 방향 설정 (선택한 옷 목록 가로 스크롤)
        rvSelectedClothes.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        // TODO: 이전 화면에서 넘겨받은 선택된 옷 데이터(이미지 URL 등)를 어댑터에 연결하는 코드 필요
        // rvSelectedClothes.adapter = SelectedClothesAdapter(선택된옷리스트)

        // 화면 켜지자마자 AI 분석 시작 (비동기)
        analyzeLaundryCourseAsync()

        // 세탁 시작 버튼 클릭 이벤트
        btnFinishLaundry.setOnClickListener {
            saveLaundryHistoryAndFinish()
        }
    }

    /**
     * 메인 스레드 멈춤 없이 워커 스레드에서 AI 서버와 통신하여 결과를 받아오는 함수
     */
    private fun analyzeLaundryCourseAsync() {
        // 분석 전 초기 상태: 로딩바 켜기, 버튼 숨기기
        pbFinalLoading.visibility = View.VISIBLE
        btnFinishLaundry.visibility = View.GONE
        cardWarning.visibility = View.GONE
        tvFinalCourse.text = "분석 중..."

        executorService.execute {
            // 백그라운드 스레드에서 무거운 작업 처리 (네트워크 통신 대기)
            Thread.sleep(2500) // 2.5초 통신 딜레이 시뮬레이션

            // AI 서버에서 반환했다고 가정하는 가짜 JSON 결과
            val aiResultJson = """
                {
                    "recommended_course": "울/섬세 코스 (30도 미온수)",
                    "has_critical_warning": true,
                    "warning_message": "선택하신 옷 중 물빠짐이 심한 '청바지'와 '흰 셔츠'가 함께 있습니다. 이염 위험이 매우 높으니 분리 세탁을 강력히 권장합니다."
                }
            """.trimIndent()

            // 분석 완료 후 화면(UI) 업데이트는 다시 메인 스레드로 돌아와서 실행
            runOnUiThread {
                updateUIWithResult(aiResultJson)
            }
        }
    }

    /**
     * 파싱한 AI 결과를 화면에 예쁘게 뿌려주는 함수
     */
    private fun updateUIWithResult(jsonString: String) {
        try {
            val jsonObject = JSONObject(jsonString)
            val course = jsonObject.optString("recommended_course", "표준 세탁")
            val hasWarning = jsonObject.optBoolean("has_critical_warning", false)
            val warningMsg = jsonObject.optString("warning_message", "")

            // 1. 로딩 끄고 버튼 켜기
            pbFinalLoading.visibility = View.GONE
            btnFinishLaundry.visibility = View.VISIBLE

            // 2. 세탁 코스 표시
            tvFinalCourse.text = course

            // 3. 치명적 주의사항이 있으면 빨간색 경고 카드 띄우기
            if (hasWarning && warningMsg.isNotEmpty()) {
                cardWarning.visibility = View.VISIBLE
                tvWarningDesc.text = warningMsg
            }

        } catch (e: Exception) {
            e.printStackTrace()
            pbFinalLoading.visibility = View.GONE
            tvFinalCourse.text = "분석 실패 (기본 표준 세탁 권장)"
        }
    }

    /**
     * 세탁 기록을 파이어베이스에 저장하고 메인으로 돌아가는 함수
     */
    private fun saveLaundryHistoryAndFinish() {
        btnFinishLaundry.isEnabled = false
        btnFinishLaundry.text = "기록 저장 중..."

        val db = FirebaseFirestore.getInstance()
        val historyData = hashMapOf(
            "course" to tvFinalCourse.text.toString(),
            "timestamp" to System.currentTimeMillis()
            // 선택된 옷들의 ID 목록 등도 여기에 함께 저장하면 좋습니다.
        )

        db.collection("laundry_history").add(historyData)
            .addOnSuccessListener {
                Toast.makeText(this, "세탁이 시작되었습니다! 기록이 저장되었습니다.", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                startActivity(intent)
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "기록 저장 실패", Toast.LENGTH_SHORT).show()
                btnFinishLaundry.isEnabled = true
                btnFinishLaundry.text = "이 코스로 세탁 시작하기"
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        executorService.shutdown() // 메모리 누수 방지
    }
}