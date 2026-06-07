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

    // 🌟 추가: 상세 화면에서 삭제하고 돌아올 때마다 데이터를 다시 불러와 리스트를 갱신합니다.
    override fun onResume() {
        super.onResume()
        loadDietClothes()
    }

    private fun loadDietClothes() {
        val tvDietSummary = findViewById<TextView>(R.id.tvDietSummary)

        db.collection("clothes").get().addOnSuccessListener { snapshot ->
            dietClothingList.clear()
            val currentTime = System.currentTimeMillis()
            val timeLimitInMillis = 365L * 24 * 60 * 60 * 1000

            for (doc in snapshot.documents) {
                val createdAtTimestamp = doc.getTimestamp("createdAt")
                val registeredTimeMillis = createdAtTimestamp?.toDate()?.time ?: currentTime

                // 숨김 검사 빼고 깔끔하게 1년 지난 옷만 필터링
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

    // 🌟 완전 삭제(Delete) 로직 적용
    override fun onDelete(item: ClothingItem, position: Int) {
        AlertDialog.Builder(this)
            .setTitle("옷 버리기")
            .setMessage("이 의류를 정말 버리시겠습니까?\n(내 옷장 데이터에서도 완전히 삭제됩니다.)")
            .setPositiveButton("버리기") { _, _ ->
                // update("isDietIgnored", true) 대신 delete() 사용!
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