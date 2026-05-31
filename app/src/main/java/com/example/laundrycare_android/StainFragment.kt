package com.example.laundrycare_android

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

// 옷 정보가 추가된 데이터 구조
data class StainItem(
    val id: String,
    val season: String,
    val mainCategory: String,
    val subCategory: String,
    val stainType: String,
    val solution: String
)

class StainAdapter(private val stainList: List<StainItem>) : RecyclerView.Adapter<StainAdapter.StainViewHolder>() {

    class StainViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        // 🌟 수정됨: item_stain.xml에 적혀있는 진짜 ID로 변경했습니다!
        val tvStainType: TextView = view.findViewById(R.id.tvItemStainType)
        val tvStainInfo: TextView = view.findViewById(R.id.tvItemStainDate) // 기존 날짜 자리에 옷 정보를 넣습니다
        val btnItemOptions: TextView = view.findViewById(R.id.btnStainMore)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StainViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_stain, parent, false)
        return StainViewHolder(view)
    }

    override fun onBindViewHolder(holder: StainViewHolder, position: Int) {
        val item = stainList[position]
        holder.tvStainType.text = item.stainType

        // 화면에 [계절 / 상의(반팔)] 형태로 깔끔하게 표시
        holder.tvStainInfo.text = "${item.season} / ${item.mainCategory}(${item.subCategory})"

        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, StainDetailActivity::class.java).apply {
                putExtra("docId", item.id)
                putExtra("season", item.season)
                putExtra("mainCategory", item.mainCategory)
                putExtra("subCategory", item.subCategory)
                putExtra("stainType", item.stainType)
                putExtra("solution", item.solution)
            }
            context.startActivity(intent)
        }

        holder.btnItemOptions.setOnClickListener {
            val context = holder.itemView.context
            val bottomSheetDialog = BottomSheetDialog(context)

            // 🚨 주의: layout_bottom_sheet.xml 파일이 프로젝트에 있어야 작동합니다.
            val bottomSheetView = LayoutInflater.from(context).inflate(R.layout.layout_bottom_sheet, null)
            bottomSheetDialog.setContentView(bottomSheetView)

            bottomSheetView.findViewById<TextView>(R.id.tvEdit).setOnClickListener {
                bottomSheetDialog.dismiss()
                Toast.makeText(context, "수정은 상세 화면에 들어가서 진행해주세요.", Toast.LENGTH_SHORT).show()
            }

            bottomSheetView.findViewById<TextView>(R.id.tvDelete).setOnClickListener {
                bottomSheetDialog.dismiss()
                AlertDialog.Builder(context).setTitle("경고").setMessage("정말 이 기록을 삭제하시겠습니까?")
                    .setPositiveButton("삭제") { _, _ ->
                        FirebaseFirestore.getInstance().collection("stains").document(item.id).delete()
                            .addOnSuccessListener { Toast.makeText(context, "삭제되었습니다.", Toast.LENGTH_SHORT).show() }
                    }
                    .setNegativeButton("취소", null).show()
            }

            bottomSheetView.findViewById<TextView>(R.id.tvCancel).setOnClickListener { bottomSheetDialog.dismiss() }
            bottomSheetDialog.show()
        }
    }

    override fun getItemCount() = stainList.size
}

class StainFragment : Fragment() {
    private lateinit var rvStains: RecyclerView
    private val stainList = mutableListOf<StainItem>()
    private lateinit var adapter: StainAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_stain, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnScanStain = view.findViewById<FloatingActionButton>(R.id.btnScanStain)
        rvStains = view.findViewById(R.id.rvStains)
        rvStains.layoutManager = LinearLayoutManager(requireContext())
        adapter = StainAdapter(stainList)
        rvStains.adapter = adapter

        btnScanStain.setOnClickListener {
            startActivity(Intent(requireContext(), StainCameraActivity::class.java))
        }

        loadStainData()
    }

    private fun loadStainData() {
        val db = FirebaseFirestore.getInstance()
        db.collection("stains").orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null || snapshots == null) return@addSnapshotListener
                stainList.clear()
                for (doc in snapshots) {
                    val id = doc.id
                    val season = doc.getString("season") ?: "여름"
                    val mainCategory = doc.getString("mainCategory") ?: "상의"
                    val subCategory = doc.getString("subCategory") ?: "반팔"
                    val stainType = doc.getString("stainType") ?: ""
                    val solution = doc.getString("solution") ?: ""

                    stainList.add(StainItem(id, season, mainCategory, subCategory, stainType, solution))
                }
                adapter.notifyDataSetChanged()
            }
    }
}