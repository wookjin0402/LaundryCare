package com.example.laundrycare_android

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore

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

        // 💡 참고: 만약 옷 데이터도 세탁기처럼 개인 창고(users/myUid/clothes)로 옮기셨다면
        // 이 경로를 db.collection("users").document(myUid).collection("clothes") 로 바꿔주셔야 합니다!
        db.collection("clothes").get().addOnSuccessListener { snapshot ->
            dietClothingList.clear()
            val currentTime = System.currentTimeMillis()
            val timeLimitInMillis = 365L * 24 * 60 * 60 * 1000 // 1년을 밀리초로 환산

            for (doc in snapshot.documents) {
                var registeredTimeMillis = currentTime // 기본값

                // 🌟 핵심 수정: 데이터 타입이 무엇이든, 혹은 아예 없든 완벽하게 시간을 추적합니다.
                if (doc.contains("createdAt")) {
                    val rawValue = doc.get("createdAt")
                    if (rawValue is com.google.firebase.Timestamp) {
                        registeredTimeMillis = rawValue.toDate().time
                    } else if (rawValue is Long) {
                        registeredTimeMillis = rawValue // 숫자로 저장된 경우도 처리
                    }
                } else {
                    // 'createdAt' 필드가 아예 없는 옛날 데이터라면? -> 아주 오래된 옷(0)으로 간주하여 리스트에 띄움!
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
        AlertDialog.Builder(this)
            .setTitle("옷 버리기")
            .setMessage("이 의류를 정말 버리시겠습니까?\n(내 옷장 데이터에서도 완전히 삭제됩니다.)")
            .setPositiveButton("버리기") { _, _ ->
                db.collection("clothes").document(item.id)
                    .delete()
                    .addOnSuccessListener {
                        Toast.makeText(this, "옷장에서도 완전히 삭제되었습니다.", Toast.LENGTH_SHORT).show()
                        dietClothingList.removeAt(position)
                        adapter.notifyItemRemoved(position)
                        updateSummary()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "삭제에 실패했습니다.", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("취소", null)
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