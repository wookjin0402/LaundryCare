package com.example.laundrycare_android

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

data class WasherData(
    val documentId: String = "",
    val type: String = "",
    val brand: String = "",
    val model: String = "",
    val imageUrl: String = "",
    var isSelected: Boolean = false
)

class WasherListActivity : AppCompatActivity() {

    private lateinit var rvWasherList: RecyclerView
    private lateinit var tvEmptyWasher: TextView
    private lateinit var btnAddWasher: Button
    private lateinit var btnWasherListBack: ImageView

    private val db = FirebaseFirestore.getInstance()
    private val washerList = mutableListOf<WasherData>()

    private lateinit var adapter: WasherAdapter

    private lateinit var layoutSelectionMode: LinearLayout
    private lateinit var btnSelectAll: Button
    private lateinit var btnDeleteSelected: Button
    private lateinit var backPressedCallback: OnBackPressedCallback

    // 🌟 UID 가져오기 (이제 리스트 불러오기와 삭제에 핵심으로 사용됩니다)
    private val myUid get() = FirebaseAuth.getInstance().currentUser?.uid ?: "unknown_user"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_washer_list)

        rvWasherList = findViewById(R.id.rvWasherList)
        tvEmptyWasher = findViewById(R.id.tvEmptyWasher)
        btnAddWasher = findViewById(R.id.btnAddWasher)
        btnWasherListBack = findViewById(R.id.btnWasherListBack)

        layoutSelectionMode = findViewById(R.id.layoutSelectionMode)
        btnSelectAll = findViewById(R.id.btnSelectAll)
        btnDeleteSelected = findViewById(R.id.btnDeleteSelected)

        rvWasherList.layoutManager = LinearLayoutManager(this)

        backPressedCallback = object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() {
                adapter.exitSelectionMode()
            }
        }
        onBackPressedDispatcher.addCallback(this, backPressedCallback)

        btnWasherListBack.setOnClickListener { finish() }

        btnAddWasher.setOnClickListener {
            val intent = Intent(this, WasherCameraActivity::class.java)
            startActivity(intent)
        }

        btnSelectAll.setOnClickListener { adapter.selectAll() }
        btnDeleteSelected.setOnClickListener { deleteSelectedItems() }

        adapter = WasherAdapter(
            washerList,
            onItemClick = { washer ->
                val intent = Intent(this, WasherDetailActivity::class.java)
                intent.putExtra("documentId", washer.documentId)
                startActivity(intent)
            },
            onMoreClick = { view, washer ->
                showPopupMenu(view, washer)
            },
            onSelectionModeChanged = { isSelectionMode ->
                layoutSelectionMode.visibility = if (isSelectionMode) View.VISIBLE else View.GONE
                backPressedCallback.isEnabled = isSelectionMode
            }
        )
        rvWasherList.adapter = adapter

        fetchWashersFromFirebase()
    }

    override fun onResume() {
        super.onResume()
        fetchWashersFromFirebase()
    }

    private fun deleteSelectedItems() {
        val selectedItems = washerList.filter { it.isSelected }
        if (selectedItems.isEmpty()) {
            Toast.makeText(this, "삭제할 세탁기를 선택해주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        val batch = db.batch()
        for (item in selectedItems) {
            // 🌟 핵심 수정: 유저의 개인 폴더(users/uid/washers)에서 정확히 삭제하도록 경로 변경
            val docRef = db.collection("users").document(myUid).collection("washers").document(item.documentId)
            batch.delete(docRef)
        }

        batch.commit().addOnSuccessListener {
            Toast.makeText(this, "${selectedItems.size}개의 세탁기가 삭제되었습니다.", Toast.LENGTH_SHORT).show()
            adapter.exitSelectionMode()
            fetchWashersFromFirebase()
        }.addOnFailureListener {
            Toast.makeText(this, "삭제에 실패했습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun fetchWashersFromFirebase() {
        // 🌟 핵심 수정: 방금 등록한 세탁기를 제대로 불러오도록 유저 개인 폴더 경로로 변경
        db.collection("users").document(myUid).collection("washers")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
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
                        val imageUrl = doc.getString("imageUrl") ?: ""

                        washerList.add(WasherData(docId, type, brand, model, imageUrl))
                    }
                    adapter.exitSelectionMode()
                    adapter.notifyDataSetChanged()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "세탁기 목록을 불러오지 못했습니다.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showPopupMenu(view: View, washer: WasherData) {
        val popup = PopupMenu(this, view)
        popup.menu.add(0, 0, 0, "수정하기")
        popup.menu.add(0, 1, 1, "삭제하기")

        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                0 -> {
                    val intent = Intent(this, WasherEditActivity::class.java)
                    intent.putExtra("documentId", washer.documentId)
                    startActivity(intent)
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

    private fun deleteWasher(documentId: String) {
        // 🌟 핵심 수정: 단일 삭제도 유저 개인 폴더 경로에서 수행
        db.collection("users").document(myUid).collection("washers").document(documentId).delete()
            .addOnSuccessListener {
                Toast.makeText(this, "세탁기가 삭제되었습니다.", Toast.LENGTH_SHORT).show()
                fetchWashersFromFirebase()
            }
            .addOnFailureListener {
                Toast.makeText(this, "삭제 실패", Toast.LENGTH_SHORT).show()
            }
    }
}

class WasherAdapter(
    private val washers: List<WasherData>,
    private val onItemClick: (WasherData) -> Unit,
    private val onMoreClick: (View, WasherData) -> Unit,
    private val onSelectionModeChanged: (Boolean) -> Unit
) : RecyclerView.Adapter<WasherAdapter.WasherViewHolder>() {

    var isSelectionMode = false
    private var isAllSelected = false

    class WasherViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cbSelect: CheckBox = view.findViewById(R.id.cbWasherSelect)
        val tvName: TextView = view.findViewById(R.id.tvItemWasherName)
        val tvType: TextView = view.findViewById(R.id.tvItemWasherType)
        val btnMore: TextView = view.findViewById(R.id.btnWasherMore)
        val ivThumb: ImageView = view.findViewById(R.id.ivItemWasherThumb)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WasherViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_washer, parent, false)
        return WasherViewHolder(view)
    }

    override fun onBindViewHolder(holder: WasherViewHolder, position: Int) {
        val washer = washers[position]

        val displayName = if (washer.model.isNotEmpty()) "${washer.brand} ${washer.model}" else washer.brand
        holder.tvName.text = displayName
        holder.tvType.text = washer.type

        if (washer.imageUrl.isNotEmpty()) {
            Glide.with(holder.itemView.context).load(washer.imageUrl).into(holder.ivThumb)
        } else {
            holder.ivThumb.setBackgroundColor(android.graphics.Color.parseColor("#E0E0E0"))
        }

        holder.cbSelect.visibility = if (isSelectionMode) View.VISIBLE else View.GONE
        holder.cbSelect.isChecked = washer.isSelected
        holder.btnMore.visibility = if (isSelectionMode) View.GONE else View.VISIBLE

        holder.cbSelect.setOnClickListener {
            washer.isSelected = holder.cbSelect.isChecked
        }

        holder.itemView.setOnLongClickListener {
            if (!isSelectionMode) {
                isSelectionMode = true
                washer.isSelected = true
                onSelectionModeChanged(true)
                notifyDataSetChanged()
            }
            true
        }

        holder.itemView.setOnClickListener {
            if (isSelectionMode) {
                washer.isSelected = !washer.isSelected
                holder.cbSelect.isChecked = washer.isSelected
            } else {
                onItemClick(washer)
            }
        }

        holder.btnMore.setOnClickListener { onMoreClick(holder.btnMore, washer) }
    }

    fun selectAll() {
        isAllSelected = !isAllSelected
        washers.forEach { it.isSelected = isAllSelected }
        notifyDataSetChanged()
    }

    fun exitSelectionMode() {
        if (!isSelectionMode) return
        isSelectionMode = false
        isAllSelected = false
        washers.forEach { it.isSelected = false }
        onSelectionModeChanged(false)
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = washers.size
}