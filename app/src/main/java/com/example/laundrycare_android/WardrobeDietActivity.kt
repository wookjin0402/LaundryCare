package com.example.laundrycare_android

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Locale

class WardrobeDietActivity : AppCompatActivity(), DietClothingAdapter.OnMenuClickListener {

    private lateinit var rvDietList: RecyclerView
    private lateinit var adapter: DietClothingAdapter
    private val dietClothingList = mutableListOf<ClothingItem>()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wardrobe_diet)

        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }

        rvDietList = findViewById(R.id.rvDietList)
        rvDietList.layoutManager = LinearLayoutManager(this)

        adapter = DietClothingAdapter(dietClothingList, this)
        rvDietList.adapter = adapter

        loadDietClothes()
    }

    override fun onResume() {
        super.onResume()
        loadDietClothes()
    }

    private fun loadDietClothes() {
        val tvDietSummary = findViewById<TextView>(R.id.tvDietSummary)

        db.collection("clothes").get().addOnSuccessListener { snapshot ->
            dietClothingList.clear()
            val currentTime = System.currentTimeMillis()
            val timeLimitInMillis = 365L * 24 * 60 * 60 * 1000 // 1년을 밀리초로 환산

            for (doc in snapshot.documents) {
                var registeredTimeMillis = currentTime // 기본값

                // 🌟 핵심 수정: 데이터 타입(Timestamp, Long, String) 모두 커버하는 날짜 파싱 로직
                if (doc.contains("createdAt")) {
                    val rawValue = doc.get("createdAt")
                    when (rawValue) {
                        is com.google.firebase.Timestamp -> {
                            registeredTimeMillis = rawValue.toDate().time
                        }
                        is Long -> {
                            registeredTimeMillis = rawValue
                        }
                        is String -> {
                            // 문자열로 날짜가 저장된 경우 (예: "2025-06-09" 또는 "2025-06-09 14:30:00")
                            try {
                                val format = if (rawValue.length > 10) {
                                    SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                                } else {
                                    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                }
                                val date = format.parse(rawValue)
                                if (date != null) {
                                    registeredTimeMillis = date.time
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                } else if (doc.contains("date")) { // 혹시 date라는 필드명으로 String 저장했을 경우를 위한 대비
                    val dateString = doc.getString("date")
                    if (dateString != null) {
                        try {
                            val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                            val date = format.parse(dateString)
                            if (date != null) {
                                registeredTimeMillis = date.time
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                } else {
                    // 날짜 필드가 아예 없으면 1년 넘은 걸로 취급
                    registeredTimeMillis = 0L
                }

                // 현재 시간과 등록 시간의 차이가 1년(timeLimitInMillis)보다 크면 리스트에 추가
                if (currentTime - registeredTimeMillis > timeLimitInMillis) {
                    dietClothingList.add(ClothingItem(
                        doc.id,
                        doc.getString("imageUrl") ?: "",
                        doc.getString("season") ?: "",
                        doc.getString("category") ?: "카테고리 없음",
                        doc.getString("subCategory") ?: "",
                        doc.getString("material") ?: "",
                        doc.getString("laundryTip") ?: ""
                    ))
                }
            }

            tvDietSummary.text = "1년 이상 입지 않은 옷이 총 ${dietClothingList.size}벌 발견되었습니다."
            adapter.notifyDataSetChanged()

            if (dietClothingList.isEmpty()) {
                tvDietSummary.text = "현재 다이어트 대상 의류가 없습니다."
            }
        }
    }

    override fun onExtend(item: ClothingItem, position: Int) {
        db.collection("clothes").document(item.id)
            .update("createdAt", com.google.firebase.Timestamp.now())
            .addOnSuccessListener {
                Toast.makeText(this, "보관 기간이 1년 연장되었습니다.", Toast.LENGTH_SHORT).show()
                dietClothingList.removeAt(position)
                adapter.notifyItemRemoved(position)
                updateSummary()
            }
    }

    override fun onDelete(item: ClothingItem, position: Int) {
        // 🌟 1차 팝업 (삭제 확인)
        AlertDialog.Builder(this)
            .setTitle("옷 버리기")
            .setMessage("이 의류를 정말 버리시겠습니까?\n(내 옷장 데이터에서도 완전히 삭제됩니다.)")
            .setPositiveButton("버리기") { _, _ ->

                // 실제 파이어베이스 삭제 로직
                db.collection("clothes").document(item.id).delete()
                    .addOnSuccessListener {
                        dietClothingList.removeAt(position)
                        adapter.notifyItemRemoved(position)
                        updateSummary()

                        // 🌟 2차 팝업 (안내 메시지 띄우기)
                        showDisposalRecommendationDialog()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "삭제에 실패했습니다.", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("취소", null)
            .show()
    }

    // 🌟 추가된 2차 안내 팝업 함수
    private fun showDisposalRecommendationDialog() {
        AlertDialog.Builder(this)
            .setTitle("옷 비우기 완료")
            .setMessage("옷장 다이어트에 성공하셨네요!\n가까운 의류수거함에 버리시거나 중고거래를 추천드려요!")
            .setPositiveButton("확인", null)
            .show()
    }

    private fun updateSummary() {
        val tvDietSummary = findViewById<TextView>(R.id.tvDietSummary)
        if (dietClothingList.isEmpty()) {
            tvDietSummary.text = "현재 다이어트 대상 의류가 없습니다."
        } else {
            tvDietSummary.text = "1년 이상 입지 않은 옷이 총 ${dietClothingList.size}벌 발견되었습니다."
        }
    }
}