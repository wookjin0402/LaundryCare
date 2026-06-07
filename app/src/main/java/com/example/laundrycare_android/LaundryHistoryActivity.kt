package com.example.laundrycare_android

import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth // 🌟 실제 로그인 유저 인증 임포트
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
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

    // 🌟 현재 로그인한 실제 유저의 고유 UID를 실시간으로 가져옵니다!
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
            // 🌟 수정: '모두의 창고'가 아닌 '내 개인 창고'에서 삭제
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
        // 🌟 수정: '모두의 창고'가 아닌 '내 개인 창고'에서 데이터 가져오기
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
            val tvDate: TextView = view.findViewById(R.id.tvHistoryDate)
            val tvWasher: TextView = view.findViewById(R.id.tvHistoryWasher)
            val tvCourse: TextView = view.findViewById(R.id.tvHistoryCourse)
            val tvWarning: TextView = view.findViewById(R.id.tvHistoryWarning)
            val tvCount: TextView = view.findViewById(R.id.tvHistoryClothesCount)
            val btnMore: TextView = view.findViewById(R.id.btnMore)
        }
        override fun onCreateViewHolder(p: ViewGroup, v: Int) = ViewHolder(LayoutInflater.from(p.context).inflate(R.layout.item_laundry_history, p, false))
        override fun onBindViewHolder(h: ViewHolder, pos: Int) {
            val item = items[pos]
            h.tvDate.text = SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.KOREA).format(Date(item.timestamp))
            h.tvWasher.text = item.washerInfo
            h.tvCourse.text = item.recommendedCourse
            h.tvWarning.text = item.warningMsg
            h.tvWarning.visibility = if(item.warningMsg == "경고 없음" || item.warningMsg.isEmpty()) View.GONE else View.VISIBLE
            h.tvCount.text = "🧺 세탁한 옷: 총 ${item.clothesImages.size}벌"

            h.cb.visibility = if(isMode) View.VISIBLE else View.GONE
            h.cb.isChecked = item.isSelected
            h.cb.setOnClickListener { item.isSelected = h.cb.isChecked }
            h.itemView.setOnLongClickListener { isMode=true; item.isSelected=true; onModeChanged(true); notifyDataSetChanged(); true }
            h.itemView.setOnClickListener { if(isMode) { item.isSelected = !item.isSelected; h.cb.isChecked = item.isSelected } else { /* 상세이동 */ } }
            h.btnMore.visibility = if(isMode) View.GONE else View.VISIBLE
        }
        fun selectAll() { items.forEach { it.isSelected = true }; notifyDataSetChanged() }
        fun exitSelectionMode() { isMode=false; items.forEach { it.isSelected = false }; onModeChanged(false); notifyDataSetChanged() }
        override fun getItemCount() = items.size
    }
}