package com.example.laundrycare_android

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// 세탁 기록을 담을 그릇
data class LaundryHistoryItem(
    val id: String,
    val timestamp: Long,
    val washerInfo: String,
    val recommendedCourse: String,
    val warningMsg: String,
    val clothesImages: List<String>
)

class LaundryHistoryActivity : AppCompatActivity() {

    private lateinit var rvLaundryHistory: RecyclerView
    private lateinit var pbHistoryLoading: ProgressBar
    private val historyList = mutableListOf<LaundryHistoryItem>()
    private lateinit var adapter: HistoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_laundry_history)

        rvLaundryHistory = findViewById(R.id.rvLaundryHistory)
        pbHistoryLoading = findViewById(R.id.pbHistoryLoading)

        rvLaundryHistory.layoutManager = LinearLayoutManager(this)
        adapter = HistoryAdapter(historyList)
        rvLaundryHistory.adapter = adapter

        fetchHistoryFromFirebase()
    }

    private fun fetchHistoryFromFirebase() {
        pbHistoryLoading.visibility = View.VISIBLE
        val db = FirebaseFirestore.getInstance()

        // 최신 세탁 기록이 맨 위로 오도록 시간 역순(DESCENDING) 정렬
        db.collection("laundry_history")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                pbHistoryLoading.visibility = View.GONE
                historyList.clear()

                for (doc in documents) {
                    val images = doc.get("clothes_images") as? List<String> ?: listOf()
                    val item = LaundryHistoryItem(
                        id = doc.id,
                        timestamp = doc.getLong("timestamp") ?: 0L,
                        washerInfo = doc.getString("washer_info") ?: "알 수 없는 기기",
                        recommendedCourse = doc.getString("recommended_course") ?: "기본 세탁 코스",
                        warningMsg = doc.getString("warning_msg") ?: "경고 없음",
                        clothesImages = images
                    )
                    historyList.add(item)
                }
                adapter.notifyDataSetChanged()

                if (historyList.isEmpty()) {
                    Toast.makeText(this, "기록된 세탁 히스토리가 없습니다.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                pbHistoryLoading.visibility = View.GONE
                Toast.makeText(this, "기록을 불러오지 못했습니다.", Toast.LENGTH_SHORT).show()
            }
    }

    // 리스트를 그려주는 내부 어댑터 클래스
    inner class HistoryAdapter(private val items: List<LaundryHistoryItem>) :
        RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvDate: TextView = view.findViewById(R.id.tvHistoryDate)
            val tvWasher: TextView = view.findViewById(R.id.tvHistoryWasher)
            val tvCourse: TextView = view.findViewById(R.id.tvHistoryCourse)
            val tvWarning: TextView = view.findViewById(R.id.tvHistoryWarning)
            val tvCount: TextView = view.findViewById(R.id.tvHistoryClothesCount)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_laundry_history, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]

            // 타임스탬프 값을 읽기 좋은 날짜 형식으로 변환
            val sdf = SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.KOREA)
            holder.tvDate.text = sdf.format(Date(item.timestamp))

            holder.tvWasher.text = item.washerInfo
            // 🌟 문제의 오타(item.course)가 삭제되고 정상적인 변수로 수정되었습니다!
            holder.tvCourse.text = item.recommendedCourse
            holder.tvWarning.text = item.warningMsg
            holder.tvCount.text = "🧺 세탁한 옷: 총 ${item.clothesImages.size}벌"

            // 경고 내용이 없으면 경고창 레이아웃 숨기기
            if (item.warningMsg == "경고 없음" || item.warningMsg.isEmpty()) {
                holder.tvWarning.visibility = View.GONE
            } else {
                holder.tvWarning.visibility = View.VISIBLE
            }
        }

        override fun getItemCount(): Int = items.size
    }
}