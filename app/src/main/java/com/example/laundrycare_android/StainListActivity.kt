package com.example.laundrycare_android

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore

// 파이어베이스에서 가져올 얼룩 데이터 구조
data class StainData(
    val documentId: String = "",
    val stainType: String = "",
    val date: String = "",
    val imageUrl: String = ""
)

class StainListActivity : AppCompatActivity() {

    private lateinit var rvStainList: RecyclerView
    private lateinit var tvEmptyStain: TextView
    private lateinit var btnAddStain: Button
    private lateinit var btnStainListBack: Button

    private val db = FirebaseFirestore.getInstance()
    private val stainList = mutableListOf<StainData>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stain_list)

        rvStainList = findViewById(R.id.rvStainList)
        tvEmptyStain = findViewById(R.id.tvEmptyStain)
        btnAddStain = findViewById(R.id.btnAddStain)
        btnStainListBack = findViewById(R.id.btnStainListBack)

        rvStainList.layoutManager = LinearLayoutManager(this)
        btnStainListBack.setOnClickListener { finish() }

        btnAddStain.setOnClickListener {
            startActivity(Intent(this, StainCameraActivity::class.java))
        }

        fetchStainsFromFirebase()
    }

    override fun onResume() {
        super.onResume()
        fetchStainsFromFirebase()
    }

    private fun fetchStainsFromFirebase() {
        db.collection("stains").get()
            .addOnSuccessListener { snapshot ->
                stainList.clear()
                if (snapshot.isEmpty) {
                    tvEmptyStain.visibility = View.VISIBLE
                    rvStainList.visibility = View.GONE
                } else {
                    tvEmptyStain.visibility = View.GONE
                    rvStainList.visibility = View.VISIBLE
                    for (doc in snapshot.documents) {
                        stainList.add(StainData(
                            doc.id,
                            doc.getString("stainType") ?: "알 수 없음",
                            doc.getString("date") ?: "날짜 미상",
                            doc.getString("imageUrl") ?: ""
                        ))
                    }
                    rvStainList.adapter = StainListAdapter(stainList,
                        onItemClick = { stain ->
                            val intent = Intent(this, StainDetailActivity::class.java)
                            intent.putExtra("documentId", stain.documentId)
                            startActivity(intent)
                        },
                        onMoreClick = { view, stain -> showPopupMenu(view, stain) }
                    )
                }
            }
    }

    private fun showPopupMenu(view: View, stain: StainData) {
        val popup = PopupMenu(this, view)
        popup.menu.add(0, 0, 0, "수정하기")
        popup.menu.add(0, 1, 1, "삭제하기")

        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                0 -> {
                    // 수정하기 클릭 시 상세 화면으로 이동하면서 수정 모드 전달
                    val intent = Intent(this, StainDetailActivity::class.java)
                    intent.putExtra("documentId", stain.documentId)
                    intent.putExtra("isEditMode", true)
                    startActivity(intent)
                    true
                }
                1 -> {
                    deleteStain(stain.documentId)
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun deleteStain(documentId: String) {
        db.collection("stains").document(documentId).delete().addOnSuccessListener {
            Toast.makeText(this, "삭제 완료", Toast.LENGTH_SHORT).show()
            fetchStainsFromFirebase()
        }
    }
}

// 하나의 파일 안에 어댑터 클래스 정의
class StainListAdapter(
    private val stains: List<StainData>,
    private val onItemClick: (StainData) -> Unit,
    private val onMoreClick: (View, StainData) -> Unit
) : RecyclerView.Adapter<StainListAdapter.StainViewHolder>() {

    class StainViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivThumb: ImageView = view.findViewById(R.id.ivItemStainThumb)
        val tvType: TextView = view.findViewById(R.id.tvItemStainType)
        val tvDate: TextView = view.findViewById(R.id.tvItemStainDate)
        val btnMore: TextView = view.findViewById(R.id.btnStainMore)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StainViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_stain, parent, false)
        return StainViewHolder(view)
    }

    override fun onBindViewHolder(holder: StainViewHolder, position: Int) {
        val stain = stains[position]
        holder.tvType.text = stain.stainType
        holder.tvDate.text = "등록일: ${stain.date}"

        if (stain.imageUrl.isNotEmpty()) {
            Glide.with(holder.itemView.context).load(stain.imageUrl).into(holder.ivThumb)
        } else {
            holder.ivThumb.setBackgroundColor(android.graphics.Color.parseColor("#F0F0F0"))
        }

        holder.itemView.setOnClickListener { onItemClick(stain) }
        holder.btnMore.setOnClickListener { onMoreClick(holder.btnMore, stain) }
    }

    override fun getItemCount(): Int = stains.size
}