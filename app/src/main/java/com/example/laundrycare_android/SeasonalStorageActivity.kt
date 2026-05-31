package com.example.laundrycare_android

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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

// 보관용 데이터 클래스
data class StorageItem(
    val id: String,
    val season: String,
    val mainCategory: String,
    val subCategory: String,
    val material: String,
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

    // 파이어베이스에서 '가을/겨울' 옷만 뽑아옵니다.
    private fun fetchStorageClothes() {
        val db = FirebaseFirestore.getInstance()

        db.collection("clothes")
            .whereIn("season", listOf("가을", "겨울")) // 가을, 겨울 옷 필터링
            .get()
            .addOnSuccessListener { snapshot ->
                storageList.clear()
                for (doc in snapshot) {
                    storageList.add(
                        StorageItem(
                            id = doc.id,
                            season = doc.getString("season") ?: "",
                            mainCategory = doc.getString("mainCategory") ?: "",
                            subCategory = doc.getString("subCategory") ?: "",
                            material = doc.getString("material") ?: "소재 미상",
                            imageUrl = doc.getString("imageUrl") ?: ""
                        )
                    )
                }
                adapter.notifyDataSetChanged()

                if (storageList.isEmpty()) {
                    Toast.makeText(this, "현재 보관이 필요한 가을/겨울 의류가 없습니다.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "데이터를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show()
            }
    }

    // 리스트를 만들어주는 어댑터
    inner class StorageAdapter(private val items: List<StorageItem>) : RecyclerView.Adapter<StorageAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val ivCloth: ImageView = view.findViewById(R.id.ivCloth)
            val tvCategory: TextView = view.findViewById(R.id.tvCategory)
            val tvSeason: TextView = view.findViewById(R.id.tvSeason)
            val btnTip: Button = view.findViewById(R.id.btnTip)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_storage_cloth, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]

            holder.tvCategory.text = "${item.mainCategory} > ${item.subCategory} (${item.material})"
            holder.tvSeason.text = "#${item.season}의류"

            if (item.imageUrl.isNotEmpty()) {
                Glide.with(holder.itemView.context).load(item.imageUrl).centerCrop().into(holder.ivCloth)
            }

            // 보관법 버튼 클릭 시 팝업 띄우기
            holder.btnTip.setOnClickListener {
                showStorageTipDialog(item.subCategory, item.material)
            }
        }

        override fun getItemCount() = items.size
    }

    // 소재별 맞춤형 보관 팁 로직
    private fun showStorageTipDialog(subCategory: String, material: String) {
        val title = "💡 $material 소재 보관 팁"
        val message = when {
            material.contains("니트") || subCategory.contains("니트") ->
                "니트는 습기에 매우 취약하며 옷걸이에 걸면 모양이 변형됩니다. 반드시 신문지나 습자지를 옷 사이에 끼운 뒤, 세로로 접어서 서랍에 눕혀서 보관하세요."

            subCategory.contains("패딩") || material.contains("구스") || material.contains("다운") ->
                "패딩을 옷걸이에 오래 걸어두면 충전재가 아래로 쏠립니다. 완전히 건조한 후 돌돌 말아 넉넉한 상자에 보관하거나, 압축팩을 사용해 부피를 줄이세요."

            material.contains("가죽") ->
                "가죽은 곰팡이에 가장 취약합니다. 이물질 제거 후 전용 크림을 바르고, 비닐이 아닌 '통풍이 잘되는 부직포 커버'를 씌우세요. 방습제가 가죽에 직접 닿지 않도록 주의해야 합니다."

            material.contains("모직") || subCategory.contains("코트") ->
                "반드시 드라이클리닝 후 비닐을 벗겨 하루 정도 기름기를 환기시켜야 합니다. 모직은 좀벌레가 좋아하므로 방충제(나프탈렌 등)와 함께 부직포 커버에 씌워 옷장에 보관하세요."

            material.contains("린넨") || material.contains("마") ->
                "세탁 후 풀기가 남아있으면 벌레가 생기기 쉽습니다. 깨끗이 세탁 후 직사광선을 피해 통풍이 잘 되는 서늘한 곳에 보관하세요."

            else ->
                "보관 전 세탁은 필수입니다! 오염이 묻은 채 보관하면 변색됩니다. 세탁 및 완전 건조 후, 습기 제거제와 함께 직사광선이 없는 옷장에 보관해주세요."
        }

        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("확인", null)
            .show()
    }
}