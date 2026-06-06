package com.example.laundrycare_android

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

// 🌟 기존 얼룩 데이터 클래스 복구
data class StainItem(
    val id: String,
    val stainType: String,
    val clothType: String,
    val solution: String,
    val imageUrl: String,
    val date: String
)

// 🌟 기존 어댑터 복구
class StainAdapter(private val stainList: List<StainItem>) : RecyclerView.Adapter<StainAdapter.StainViewHolder>() {

    class StainViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivThumb: ImageView = view.findViewById(R.id.ivItemStainThumb)
        val tvStainType: TextView = view.findViewById(R.id.tvItemStainType)
        val tvStainInfo: TextView = view.findViewById(R.id.tvItemStainDate)
        val btnItemOptions: TextView = view.findViewById(R.id.btnStainMore)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StainViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_stain, parent, false)
        return StainViewHolder(view)
    }

    override fun onBindViewHolder(holder: StainViewHolder, position: Int) {
        val item = stainList[position]
        holder.tvStainType.text = item.stainType
        holder.tvStainInfo.text = "${item.date} / ${item.clothType}"

        if (item.imageUrl.isNotEmpty()) {
            Glide.with(holder.itemView.context).load(item.imageUrl).into(holder.ivThumb)
        } else {
            holder.ivThumb.setBackgroundColor(android.graphics.Color.parseColor("#F0F0F0"))
        }

        holder.itemView.setOnClickListener {
            val intent = Intent(holder.itemView.context, StainDetailActivity::class.java).apply {
                putExtra("documentId", item.id)
            }
            holder.itemView.context.startActivity(intent)
        }

        holder.btnItemOptions.setOnClickListener {
            val context = holder.itemView.context
            val bottomSheetDialog = BottomSheetDialog(context)
            val bottomSheetView = LayoutInflater.from(context).inflate(R.layout.layout_bottom_sheet, null)
            bottomSheetDialog.setContentView(bottomSheetView)

            bottomSheetView.findViewById<TextView>(R.id.tvEdit).setOnClickListener {
                bottomSheetDialog.dismiss()
                val intent = Intent(context, StainDetailActivity::class.java).apply {
                    putExtra("documentId", item.id)
                    putExtra("isEditMode", true)
                }
                context.startActivity(intent)
            }

            bottomSheetView.findViewById<TextView>(R.id.tvDelete).setOnClickListener {
                bottomSheetDialog.dismiss()
                AlertDialog.Builder(context).setTitle("경고").setMessage("삭제하시겠습니까?")
                    .setPositiveButton("삭제") { _, _ ->
                        FirebaseFirestore.getInstance().collection("stains").document(item.id).delete()
                    }.setNegativeButton("취소", null).show()
            }
            bottomSheetDialog.show()
        }
    }
    override fun getItemCount() = stainList.size
}

// 🌟 각 탭 안에서 동작하는 리스트 프래그먼트
class StainListFragment : Fragment() {

    private lateinit var rvStains: RecyclerView
    private val stainList = mutableListOf<StainItem>()
    private lateinit var adapter: StainAdapter
    private var category: String = "전체"

    companion object {
        fun newInstance(category: String): StainListFragment {
            val fragment = StainListFragment()
            val args = Bundle()
            args.putString("category", category) // "음식물", "화장품" 등 꼬리표 부착
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        category = arguments?.getString("category") ?: "전체"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_stain_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvStains = view.findViewById(R.id.rvStainsList)
        rvStains.layoutManager = LinearLayoutManager(requireContext())
        adapter = StainAdapter(stainList)
        rvStains.adapter = adapter

        // 카테고리 이름으로 데이터 필터링 시작
        loadStainData(category)
    }

    private fun loadStainData(selectedCategory: String) {
        var query: Query = FirebaseFirestore.getInstance().collection("stains")
            .orderBy("timestamp", Query.Direction.DESCENDING)

        // 🌟 선택된 탭이 "전체"가 아니면 해당 카테고리만 가져오도록 쿼리 추가
        if (selectedCategory != "전체") {
            query = query.whereEqualTo("stainType", selectedCategory)
        }

        query.addSnapshotListener { snapshots, _ ->
            if (snapshots == null) return@addSnapshotListener
            stainList.clear()
            for (doc in snapshots) {
                stainList.add(
                    StainItem(
                        doc.id,
                        doc.getString("stainType") ?: "",
                        doc.getString("clothType") ?: "",
                        doc.getString("solution") ?: "",
                        doc.getString("imageUrl") ?: "",
                        doc.getString("date") ?: ""
                    )
                )
            }
            adapter.notifyDataSetChanged()
        }
    }
}