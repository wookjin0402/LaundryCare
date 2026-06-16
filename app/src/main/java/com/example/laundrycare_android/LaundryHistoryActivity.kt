package com.example.laundrycare_android

import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.util.*

data class LaundryHistoryItem(
    val id: String,
    val timestamp: Long,
    val washerInfo: String,
    val recommendedCourse: String,
    val warningMsg: String,
    val clothesImages: List<String>,
    var isSelected: Boolean = false
)

class LaundryHistoryActivity : AppCompatActivity() {
    private lateinit var rvLaundryHistory: RecyclerView
    private lateinit var adapter: HistoryAdapter
    private val historyList = mutableListOf<LaundryHistoryItem>()

    private lateinit var layoutSelectionMode: LinearLayout
    private lateinit var backPressedCallback: OnBackPressedCallback

    private val myUid get() = FirebaseAuth.getInstance().currentUser?.uid ?: "unknown_user"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_laundry_history)

        layoutSelectionMode = findViewById(R.id.layoutSelectionMode)
        rvLaundryHistory = findViewById(R.id.rvLaundryHistory)
        rvLaundryHistory.layoutManager = LinearLayoutManager(this)

        adapter = HistoryAdapter(historyList) { isMode ->
            layoutSelectionMode.visibility = if (isMode) View.VISIBLE else View.GONE
            backPressedCallback.isEnabled = isMode
        }
        rvLaundryHistory.adapter = adapter

        findViewById<Button>(R.id.btnSelectAll).setOnClickListener { adapter.selectAll() }
        findViewById<Button>(R.id.btnDeleteSelected).setOnClickListener { deleteSelected() }
        findViewById<ImageView>(R.id.btnBackHistory).setOnClickListener { finish() }

        backPressedCallback = object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() { adapter.exitSelectionMode() }
        }
        onBackPressedDispatcher.addCallback(this, backPressedCallback)

        fetchHistoryFromFirebase()
    }

    private fun deleteSelected() {
        val selected = historyList.filter { it.isSelected }
        if(selected.isEmpty()) return
        val db = FirebaseFirestore.getInstance()
        val batch = db.batch()
        for (item in selected) {
            val docRef = db.collection("users").document(myUid).collection("laundry_history").document(item.id)
            batch.delete(docRef)
        }
        batch.commit().addOnSuccessListener {
            Toast.makeText(this, "삭제 완료", Toast.LENGTH_SHORT).show()
            adapter.exitSelectionMode()
            fetchHistoryFromFirebase()
        }
    }

    private fun fetchHistoryFromFirebase() {
        FirebaseFirestore.getInstance().collection("users").document(myUid).collection("laundry_history")
            .orderBy("timestamp", Query.Direction.DESCENDING).get().addOnSuccessListener { documents ->
                historyList.clear()
                for (doc in documents) {
                    val item = LaundryHistoryItem(
                        doc.id,
                        doc.getLong("timestamp") ?: 0L,
                        doc.getString("washer_info") ?: "알 수 없는 기기",
                        doc.getString("recommended_course") ?: "기본 세탁 코스",
                        doc.getString("warning_msg") ?: "경고 없음",
                        doc.get("clothes_images") as? List<String> ?: listOf()
                    )
                    historyList.add(item)
                }
                adapter.notifyDataSetChanged()
            }
    }

    inner class HistoryAdapter(private val items: MutableList<LaundryHistoryItem>, private val onModeChanged: (Boolean) -> Unit) : RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {
        var isMode = false
        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val cb: CheckBox = view.findViewById(R.id.cbHistorySelect)
            val tvWasher: TextView = view.findViewById(R.id.tvHistoryWasher)
            val tvCourse: TextView = view.findViewById(R.id.tvHistoryCourse)
            val tvWarning: TextView = view.findViewById(R.id.tvHistoryWarning)
            val tvCount: TextView = view.findViewById(R.id.tvHistoryClothesCount)
            // 🌟 tvDate 및 btnMore 관련 코드 완벽히 삭제됨
        }
        override fun onCreateViewHolder(p: ViewGroup, v: Int) = ViewHolder(LayoutInflater.from(p.context).inflate(R.layout.item_laundry_history, p, false))

        override fun onBindViewHolder(h: ViewHolder, pos: Int) {
            val item = items[pos]

            h.tvWasher.text = item.washerInfo
            h.tvCourse.text = item.recommendedCourse
            h.tvWarning.text = item.warningMsg
            h.tvWarning.visibility = if(item.warningMsg == "경고 없음" || item.warningMsg.isEmpty()) View.GONE else View.VISIBLE
            h.tvCount.text = "🧺 세탁한 옷: 총 ${item.clothesImages.size}벌"

            h.cb.visibility = if(isMode) View.VISIBLE else View.GONE
            h.cb.isChecked = item.isSelected
            h.cb.setOnClickListener { item.isSelected = h.cb.isChecked }

            h.itemView.setOnLongClickListener { isMode=true; item.isSelected=true; onModeChanged(true); notifyDataSetChanged(); true }
            h.itemView.setOnClickListener { if(isMode) { item.isSelected = !item.isSelected; h.cb.isChecked = item.isSelected } else { /* 상세이동 로직 등 */ } }
        }
        fun selectAll() { items.forEach { it.isSelected = true }; notifyDataSetChanged() }
        fun exitSelectionMode() { isMode=false; items.forEach { it.isSelected = false }; onModeChanged(false); notifyDataSetChanged() }
        override fun getItemCount() = items.size
    }
}