package com.example.laundrycare_android

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
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

data class StainItem(val id: String, val category: String, val material: String, val stainType: String, val solution: String)

class StainAdapter(private val stainList: List<StainItem>) : RecyclerView.Adapter<StainAdapter.StainViewHolder>() {

    class StainViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvStainType: TextView = view.findViewById(R.id.tvStainType)
        val tvStainInfo: TextView = view.findViewById(R.id.tvStainInfo)
        val btnItemOptions: TextView = view.findViewById(R.id.btnItemOptions) // 🌟 점 세 개 버튼
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StainViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_stain, parent, false)
        return StainViewHolder(view)
    }

    override fun onBindViewHolder(holder: StainViewHolder, position: Int) {
        val item = stainList[position]
        holder.tvStainType.text = item.stainType
        holder.tvStainInfo.text = "옷 종류: ${item.category} / 소재: ${item.material}"

        // 1. 네모칸 전체를 클릭하면 상세 화면(StainDetailActivity)으로 이동
        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, StainDetailActivity::class.java).apply {
                putExtra("docId", item.id)
                putExtra("category", item.category)
                putExtra("material", item.material)
                putExtra("stainType", item.stainType)
                putExtra("solution", item.solution)
            }
            context.startActivity(intent)
        }

        // 2. 🌟 점 세 개(⋮) 버튼을 클릭하면 당근마켓 바텀 시트 띄우기
        holder.btnItemOptions.setOnClickListener {
            val context = holder.itemView.context
            val bottomSheetDialog = BottomSheetDialog(context)
            val bottomSheetView = LayoutInflater.from(context).inflate(R.layout.layout_bottom_sheet, null)
            bottomSheetDialog.setContentView(bottomSheetView)

            // 수정 버튼 클릭 시
            bottomSheetView.findViewById<TextView>(R.id.tvEdit).setOnClickListener {
                bottomSheetDialog.dismiss()

                // 수정용 팝업(다이얼로그) 띄우기
                val layout = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL; setPadding(50, 30, 50, 10) }
                val etCategory = EditText(context).apply { hint = "옷 종류"; setText(item.category) }
                val etMaterial = EditText(context).apply { hint = "소재"; setText(item.material) }
                val etStain = EditText(context).apply { hint = "얼룩 종류"; setText(item.stainType) }
                layout.addView(etCategory)
                layout.addView(etMaterial)
                layout.addView(etStain)

                AlertDialog.Builder(context)
                    .setTitle("정보 수정")
                    .setView(layout)
                    .setPositiveButton("저장") { _, _ ->
                        val db = FirebaseFirestore.getInstance()
                        db.collection("stains").document(item.id)
                            .update(
                                "category", etCategory.text.toString(),
                                "material", etMaterial.text.toString(),
                                "stainType", etStain.text.toString()
                            )
                            .addOnSuccessListener {
                                Toast.makeText(context, "수정 완료", Toast.LENGTH_SHORT).show()
                            }
                    }
                    .setNegativeButton("취소", null)
                    .show()
            }

            // 삭제 버튼 클릭 시
            bottomSheetView.findViewById<TextView>(R.id.tvDelete).setOnClickListener {
                bottomSheetDialog.dismiss()
                AlertDialog.Builder(context).setTitle("경고").setMessage("정말 이 기록을 삭제하시겠습니까?")
                    .setPositiveButton("삭제") { _, _ ->
                        FirebaseFirestore.getInstance().collection("stains").document(item.id).delete()
                            .addOnSuccessListener {
                                Toast.makeText(context, "삭제되었습니다.", Toast.LENGTH_SHORT).show()
                            }
                    }
                    .setNegativeButton("취소", null).show()
            }

            // 닫기 버튼 클릭 시
            bottomSheetView.findViewById<TextView>(R.id.tvCancel).setOnClickListener {
                bottomSheetDialog.dismiss()
            }

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
                    val category = doc.getString("category") ?: ""
                    val material = doc.getString("material") ?: ""
                    val stainType = doc.getString("stainType") ?: ""
                    val solution = doc.getString("solution") ?: ""
                    stainList.add(StainItem(id, category, material, stainType, solution))
                }
                adapter.notifyDataSetChanged() // 데이터가 수정/삭제되면 화면을 즉시 새로고침합니다.
            }
    }
}