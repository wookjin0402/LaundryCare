package com.example.laundrycare_android

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore

// 파이어베이스 문서 ID를 저장하는 데이터 구조
data class WasherData(
    val documentId: String = "",
    val type: String = "",
    val brand: String = "",
    val model: String = ""
)

class WasherListActivity : AppCompatActivity() {

    private lateinit var rvWasherList: RecyclerView
    private lateinit var tvEmptyWasher: TextView
    private lateinit var btnAddWasher: Button

    // 🌟 버튼 튕김 지뢰 제거! (Button -> ImageView)
    private lateinit var btnWasherListBack: ImageView

    private val db = FirebaseFirestore.getInstance()
    private val washerList = mutableListOf<WasherData>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_washer_list)

        rvWasherList = findViewById(R.id.rvWasherList)
        tvEmptyWasher = findViewById(R.id.tvEmptyWasher)
        btnAddWasher = findViewById(R.id.btnAddWasher)
        btnWasherListBack = findViewById(R.id.btnWasherListBack)

        rvWasherList.layoutManager = LinearLayoutManager(this)

        btnWasherListBack.setOnClickListener { finish() }

        btnAddWasher.setOnClickListener {
            val intent = Intent(this, WasherCameraActivity::class.java)
            startActivity(intent)
        }

        fetchWashersFromFirebase()
    }

    override fun onResume() {
        super.onResume()
        fetchWashersFromFirebase()
    }

    private fun fetchWashersFromFirebase() {
        db.collection("washers").get()
            .addOnSuccessListener { snapshot ->
                washerList.clear()

                if (snapshot.isEmpty) {
                    tvEmptyWasher.visibility = View.VISIBLE
                    rvWasherList.visibility = View.GONE
                } else {
                    tvEmptyWasher.visibility = View.GONE
                    rvWasherList.visibility = View.VISIBLE

                    for (doc in snapshot.documents) {
                        val docId = doc.id
                        val type = doc.getString("type") ?: "알 수 없음"
                        val brand = doc.getString("brand") ?: "브랜드 미상"
                        val model = doc.getString("model") ?: ""

                        washerList.add(WasherData(docId, type, brand, model))
                    }

                    rvWasherList.adapter = WasherAdapter(
                        washerList,
                        onItemClick = { washer ->
                            // 상세 화면으로 이동하며 문서 ID 전달
                            val intent = Intent(this, WasherDetailActivity::class.java)
                            intent.putExtra("documentId", washer.documentId)
                            startActivity(intent)
                        },
                        onMoreClick = { view, washer ->
                            showPopupMenu(view, washer)
                        }
                    )
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "세탁기 목록을 불러오지 못했습니다.", Toast.LENGTH_SHORT).show()
            }
    }

    // 점 3개 메뉴를 눌렀을 때 뜨는 팝업 (수정/삭제)
    private fun showPopupMenu(view: View, washer: WasherData) {
        val popup = PopupMenu(this, view)
        popup.menu.add(0, 0, 0, "수정하기")
        popup.menu.add(0, 1, 1, "삭제하기")

        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                0 -> {
                    // 🌟 준비중 토스트 대신 실제 수정 팝업 띄우기
                    showEditWasherDialog(washer)
                    true
                }
                1 -> {
                    deleteWasher(washer.documentId)
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    // 🌟 세탁기 정보 수정 팝업 로직 추가
    private fun showEditWasherDialog(washer: WasherData) {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(60, 40, 60, 20)
        }

        val typeLabel = TextView(this).apply { text = "세탁기 형태" }
        val spinnerType = Spinner(this).apply {
            adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, arrayOf("드럼", "통돌이"))
        }
        spinnerType.setSelection(if (washer.type == "통돌이") 1 else 0)

        val brandLabel = TextView(this).apply { text = "\n브랜드" }
        val etBrand = EditText(this).apply { setText(washer.brand) }

        val modelLabel = TextView(this).apply { text = "\n모델명 (선택)" }
        val etModel = EditText(this).apply { setText(washer.model) }

        layout.addView(typeLabel)
        layout.addView(spinnerType)
        layout.addView(brandLabel)
        layout.addView(etBrand)
        layout.addView(modelLabel)
        layout.addView(etModel)

        AlertDialog.Builder(this)
            .setTitle("세탁기 정보 수정")
            .setView(layout)
            .setPositiveButton("저장") { _, _ ->
                val newType = spinnerType.selectedItem.toString()
                val newBrand = etBrand.text.toString().trim()
                val newModel = etModel.text.toString().trim()

                db.collection("washers").document(washer.documentId)
                    .update(mapOf("type" to newType, "brand" to newBrand, "model" to newModel))
                    .addOnSuccessListener {
                        Toast.makeText(this, "성공적으로 수정되었습니다.", Toast.LENGTH_SHORT).show()
                        fetchWashersFromFirebase() // 화면 새로고침
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "수정 실패", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("취소", null)
            .show()
    }

    // 파이어베이스에서 세탁기 데이터 실제 삭제
    private fun deleteWasher(documentId: String) {
        db.collection("washers").document(documentId).delete()
            .addOnSuccessListener {
                Toast.makeText(this, "세탁기가 삭제되었습니다.", Toast.LENGTH_SHORT).show()
                fetchWashersFromFirebase() // 삭제 후 리스트 새로고침
            }
            .addOnFailureListener {
                Toast.makeText(this, "삭제 실패", Toast.LENGTH_SHORT).show()
            }
    }
}

// 리사이클러뷰 어댑터
class WasherAdapter(
    private val washers: List<WasherData>,
    private val onItemClick: (WasherData) -> Unit,
    private val onMoreClick: (View, WasherData) -> Unit
) : RecyclerView.Adapter<WasherAdapter.WasherViewHolder>() {

    class WasherViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvItemWasherName)
        val tvType: TextView = view.findViewById(R.id.tvItemWasherType)
        val btnMore: TextView = view.findViewById(R.id.btnWasherMore)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WasherViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_washer, parent, false)
        return WasherViewHolder(view)
    }

    override fun onBindViewHolder(holder: WasherViewHolder, position: Int) {
        val washer = washers[position]

        val displayName = if (washer.model.isNotEmpty()) {
            "${washer.brand} ${washer.model}"
        } else {
            washer.brand
        }

        holder.tvName.text = displayName
        holder.tvType.text = washer.type

        holder.itemView.setOnClickListener { onItemClick(washer) }
        holder.btnMore.setOnClickListener { onMoreClick(holder.btnMore, washer) }
    }

    override fun getItemCount(): Int = washers.size
}