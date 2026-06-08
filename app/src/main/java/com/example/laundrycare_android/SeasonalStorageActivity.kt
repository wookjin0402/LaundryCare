package com.example.laundrycare_android

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

data class StorageItem(
    val id: String,
    val season: String,
    val mainCategory: String,
    val subCategory: String,
    val material: String,
    val color: String,
    val warnings: String,
    val laundryTip: String,
    val imageUrl: String
)

class SeasonalStorageActivity : AppCompatActivity() {

    private lateinit var rvStorageList: RecyclerView
    private val storageList = mutableListOf<StorageItem>()
    private lateinit var adapter: StorageAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_seasonal_storage)

        val btnBack = findViewById<ImageView>(R.id.btnBack)
        btnBack.setOnClickListener { finish() }

        rvStorageList = findViewById(R.id.rvStorageList)
        rvStorageList.layoutManager = LinearLayoutManager(this)

        adapter = StorageAdapter(storageList)
        rvStorageList.adapter = adapter

        fetchStorageClothes()
    }

    private fun fetchStorageClothes() {
        val db = FirebaseFirestore.getInstance()
        val currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 1
        val currentSeason = when (currentMonth) {
            in 3..5 -> "봄"; in 6..8 -> "여름"; in 9..11 -> "가을"; else -> "겨울"
        }

        val allSeasons = listOf("봄", "여름", "가을", "겨울")
        val targetSeasons = allSeasons.filter { it != currentSeason }

        db.collection("clothes")
            .whereIn("season", targetSeasons)
            .get()
            .addOnSuccessListener { snapshot ->
                storageList.clear()
                for (doc in snapshot) {
                    storageList.add(StorageItem(
                        id = doc.id,
                        season = doc.getString("season") ?: "",
                        mainCategory = doc.getString("mainCategory") ?: "",
                        subCategory = doc.getString("subCategory") ?: "",
                        material = doc.getString("material") ?: "소재 미상",
                        color = doc.getString("color") ?: "색상 미상",
                        warnings = doc.getString("warnings") ?: "",
                        laundryTip = doc.getString("laundryTip") ?: "",
                        imageUrl = doc.getString("imageUrl") ?: ""
                    ))
                }
                adapter.notifyDataSetChanged()
            }
    }

    inner class StorageAdapter(private val items: List<StorageItem>) : RecyclerView.Adapter<StorageAdapter.ViewHolder>() {
        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val ivCloth: ImageView = view.findViewById(R.id.ivCloth)
            val tvCategory: TextView = view.findViewById(R.id.tvCategory)
            val tvSeason: TextView = view.findViewById(R.id.tvSeason)
            val btnTip: Button = view.findViewById(R.id.btnTip)
        }
        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ViewHolder {
            val view = android.view.LayoutInflater.from(parent.context).inflate(R.layout.item_storage_cloth, parent, false)
            return ViewHolder(view)
        }
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.tvCategory.text = "${item.mainCategory} > ${item.subCategory} (${item.color})"
            holder.tvSeason.text = "#${item.season}의류"
            if (item.imageUrl.isNotEmpty()) {
                Glide.with(holder.itemView.context).load(item.imageUrl).centerCrop().into(holder.ivCloth)
            }
            holder.btnTip.setOnClickListener { showStorageTipDialog(item) }
        }
        override fun getItemCount() = items.size
    }

    // 🌟 데이터 종합 및 장기 보관 솔루션 생성 (나열 방식 삭제)
    private fun showStorageTipDialog(item: StorageItem) {
        val analysis = StringBuilder()
        analysis.append("🧥 [${item.subCategory} 장기 보관 솔루션]\n\n")

        // 1. 보관 전 상태 분석 및 처방
        analysis.append("1. 장기 보관 전 처방:\n")
        if (item.warnings.contains("세탁 금지") || item.warnings.contains("드라이")) {
            analysis.append("• 이 의류는 일반 세탁이 불가합니다. 전문 세탁소에서 케어를 받은 후 보관하세요.\n")
        } else if (item.laundryTip.isNotEmpty()) {
            analysis.append("• 권장 관리법: ${item.laundryTip}에 따라 세탁 후 100% 완전 건조 상태로 보관하세요.\n")
        } else {
            analysis.append("• 장기 보관 전 반드시 오염을 제거하고 완전히 건조하세요. 잔류 습기는 보관 중 곰팡이의 원인이 됩니다.\n")
        }

        // 2. 소재 및 색상 기반 환경 분석
        analysis.append("\n2. 최적 보관 환경 분석:\n")

        // 소재별 조합 전략
        val material = item.material
        when {
            material.contains("니트") || material.contains("울") ->
                analysis.append("• 옷걸이 사용은 금물입니다. 형태 변형 방지를 위해 종이/습자지를 끼워 서랍에 눕혀 보관하세요.\n")
            material.contains("가죽") ->
                analysis.append("• 가죽은 숨을 쉬어야 합니다. 비닐 커버를 제거하고 부직포 커버를 사용하여 통풍을 확보하세요.\n")
            material.contains("패딩") || material.contains("다운") ->
                analysis.append("• 압축팩은 충전재를 죽입니다. 넉넉한 공간에 자연스럽게 펴서 보관하세요.\n")
            else ->
                analysis.append("• 소재 특성에 맞춰 겹치지 않게 보관하세요.\n")
        }

        // 색상 기반 전략 (황변/이염 방지)
        if (item.color.contains("흰색") || item.color.contains("밝은")) {
            analysis.append("• 밝은 색상 의류입니다. 빛에 의한 황변을 막기 위해 암막 커버를 사용하세요.\n")
        } else {
            analysis.append("• 변색 방지를 위해 직사광선이 닿지 않는 서늘한 곳에 보관하세요.\n")
        }

        // 3. 최종 요약 팁
        analysis.append("\n3. 종합 제언:\n")
        analysis.append("• 보관 장소의 습도를 50% 이하로 유지하세요.\n")
        analysis.append("• 방충제는 옷감에 직접 닿지 않도록 옷장 귀퉁이에 배치하세요.\n")

        AlertDialog.Builder(this)
            .setTitle("장기 보관 솔루션")
            .setMessage(analysis.toString())
            .setPositiveButton("확인", null)
            .show()
    }
}