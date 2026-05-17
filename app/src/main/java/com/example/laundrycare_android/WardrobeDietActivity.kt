package com.example.laundrycare_android

import android.os.Bundle
import android.widget.Button
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

        findViewById<Button>(R.id.btnBack).setOnClickListener { finish() }

        rvDietList = findViewById(R.id.rvDietList)
        rvDietList.layoutManager = LinearLayoutManager(this)

        adapter = DietClothingAdapter(dietClothingList, this)
        rvDietList.adapter = adapter

        loadDietClothes()
    }

    private fun loadDietClothes() {
        val tvDietSummary = findViewById<TextView>(R.id.tvDietSummary)

        db.collection("clothes").get().addOnSuccessListener { snapshot ->
            dietClothingList.clear()
            val currentTime = System.currentTimeMillis()
            val timeLimitInMillis = 365L * 24 * 60 * 60 * 1000

            for (doc in snapshot.documents) {
                // 🌟 숨김 처리된 옷인지 먼저 확인 (true면 패스)
                val isDietIgnored = doc.getBoolean("isDietIgnored") ?: false

                val createdAtTimestamp = doc.getTimestamp("createdAt")
                val registeredTimeMillis = createdAtTimestamp?.toDate()?.time ?: currentTime

                // 숨김 처리 되지 않았고(&&), 1년이 지났을 때만 리스트에 추가
                if (!isDietIgnored && (currentTime - registeredTimeMillis > timeLimitInMillis)) {
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

    // 🌟 진짜 삭제가 아니라 파이어베이스에 '숨김' 꼬리표만 달아줍니다.
    override fun onDelete(item: ClothingItem, position: Int) {
        AlertDialog.Builder(this)
            .setTitle("목록에서 숨기기")
            .setMessage("이 의류를 다이어트 제안 목록에서 숨기시겠습니까?\n(내 옷장 데이터는 삭제되지 않고 안전하게 유지됩니다.)")
            .setPositiveButton("숨기기") { _, _ ->

                // delete() 대신 update()를 사용하여 isDietIgnored 값을 true로 만들어 줍니다.
                db.collection("clothes").document(item.id)
                    .update("isDietIgnored", true)
                    .addOnSuccessListener {
                        Toast.makeText(this, "다이어트 목록에서 숨겨졌습니다.", Toast.LENGTH_SHORT).show()
                        dietClothingList.removeAt(position)
                        adapter.notifyItemRemoved(position)
                        updateSummary()
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