package com.example.laundrycare_android

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

// 데이터 클래스
data class StainItem(
    val id: String,
    val stainType: String,
    val clothType: String,
    val solution: String,
    val imageUrl: String,
    val date: String,
    var isSelected: Boolean = false
)

class StainListFragment : Fragment() {

    private lateinit var rvStains: RecyclerView
    private val stainList = mutableListOf<StainItem>()
    private lateinit var adapter: StainAdapter
    private var category: String = "전체"

    private lateinit var layoutSelectionMode: LinearLayout
    private lateinit var btnSelectAll: Button
    private lateinit var btnDeleteSelected: Button

    private lateinit var backPressedCallback: OnBackPressedCallback

    companion object {
        fun newInstance(category: String): StainListFragment {
            val fragment = StainListFragment()
            val args = Bundle()
            args.putString("category", category)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        category = arguments?.getString("category") ?: "전체"
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val originalRoot = inflater.inflate(R.layout.fragment_stain_list, container, false)

        val wrapperLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            setBackgroundColor(android.graphics.Color.parseColor("#F5F5F5"))
        }

        layoutSelectionMode = LinearLayout(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.END or android.view.Gravity.CENTER_VERTICAL
            setPadding(32, 24, 32, 24)
            setBackgroundColor(android.graphics.Color.parseColor("#FFFFFF"))
            visibility = View.GONE
            elevation = 10f
        }

        btnSelectAll = Button(requireContext()).apply {
            text = "전체 선택"
            textSize = 14f
            backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#1976D2"))
            setTextColor(android.graphics.Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                marginEnd = 16
            }
            setOnClickListener { adapter.selectAll() }
        }

        btnDeleteSelected = Button(requireContext()).apply {
            text = "선택 삭제"
            textSize = 14f
            backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#D32F2F"))
            setTextColor(android.graphics.Color.WHITE)
            setOnClickListener { deleteSelectedItems() }
        }

        layoutSelectionMode.addView(btnSelectAll)
        layoutSelectionMode.addView(btnDeleteSelected)

        wrapperLayout.addView(layoutSelectionMode)
        wrapperLayout.addView(originalRoot, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            0,
            1f
        ))

        return wrapperLayout
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        backPressedCallback = object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() {
                adapter.exitSelectionMode()
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, backPressedCallback)

        rvStains = view.findViewById(R.id.rvStainsList)
        rvStains.layoutManager = LinearLayoutManager(requireContext())

        adapter = StainAdapter(stainList,
            onMoreClick = { v, item -> showPopupMenu(v, item) },
            onSelectionModeChanged = { isSelectionMode ->
                layoutSelectionMode.visibility = if (isSelectionMode) View.VISIBLE else View.GONE
                backPressedCallback.isEnabled = isSelectionMode
            }
        )
        rvStains.adapter = adapter

        loadStainData(category)
    }

    private fun deleteSelectedItems() {
        val selectedItems = stainList.filter { it.isSelected }
        if (selectedItems.isEmpty()) {
            Toast.makeText(requireContext(), "삭제할 항목을 선택해주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        val db = FirebaseFirestore.getInstance()
        val batch = db.batch()

        for (item in selectedItems) {
            val docRef = db.collection("stains").document(item.id)
            batch.delete(docRef)
        }

        batch.commit().addOnSuccessListener {
            Toast.makeText(requireContext(), "${selectedItems.size}개 삭제 완료", Toast.LENGTH_SHORT).show()
            adapter.exitSelectionMode()
            loadStainData(category)
        }.addOnFailureListener {
            Toast.makeText(requireContext(), "삭제 실패", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showPopupMenu(view: View, item: StainItem) {
        val popup = PopupMenu(requireContext(), view)
        popup.menu.add(0, 0, 0, "수정하기")
        popup.menu.add(0, 1, 1, "삭제하기")

        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                0 -> {
                    val intent = Intent(requireContext(), StainEditActivity::class.java).apply {
                        putExtra("documentId", item.id)
                    }
                    startActivity(intent)
                    true
                }
                1 -> {
                    FirebaseFirestore.getInstance().collection("stains").document(item.id).delete()
                        .addOnSuccessListener { loadStainData(category) }
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun loadStainData(selectedCategory: String) {
        var query: Query = FirebaseFirestore.getInstance().collection("stains")
            .orderBy("timestamp", Query.Direction.DESCENDING)

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
            adapter.exitSelectionMode()
            adapter.notifyDataSetChanged()
        }
    }
}

// 🌟 여기 StainAdapter가 들어있습니다.
class StainAdapter(
    private val stainList: List<StainItem>,
    private val onMoreClick: (View, StainItem) -> Unit,
    private val onSelectionModeChanged: (Boolean) -> Unit
) : RecyclerView.Adapter<StainAdapter.StainViewHolder>() {

    var isSelectionMode = false
    private var isAllSelected = false

    class StainViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cbSelect: CheckBox = view.findViewById(R.id.cbStainSelect)
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

        holder.cbSelect.visibility = if (isSelectionMode) View.VISIBLE else View.GONE
        holder.cbSelect.isChecked = item.isSelected
        holder.btnItemOptions.visibility = if (isSelectionMode) View.GONE else View.VISIBLE

        // 🌟🌟 문제 해결: 체크박스 네모 칸 터치 인식 추가 🌟🌟
        holder.cbSelect.setOnClickListener {
            item.isSelected = holder.cbSelect.isChecked
        }

        holder.itemView.setOnLongClickListener {
            if (!isSelectionMode) {
                isSelectionMode = true
                item.isSelected = true
                onSelectionModeChanged(true)
                notifyDataSetChanged()
            }
            true
        }

        holder.itemView.setOnClickListener {
            if (isSelectionMode) {
                item.isSelected = !item.isSelected
                holder.cbSelect.isChecked = item.isSelected
            } else {
                val intent = Intent(holder.itemView.context, StainDetailActivity::class.java).apply {
                    putExtra("documentId", item.id)
                }
                holder.itemView.context.startActivity(intent)
            }
        }

        holder.btnItemOptions.setOnClickListener { onMoreClick(it, item) }
    }

    fun selectAll() {
        isAllSelected = !isAllSelected
        stainList.forEach { it.isSelected = isAllSelected }
        notifyDataSetChanged()
    }

    fun exitSelectionMode() {
        if(!isSelectionMode) return
        isSelectionMode = false
        isAllSelected = false
        stainList.forEach { it.isSelected = false }
        onSelectionModeChanged(false)
        notifyDataSetChanged()
    }

    override fun getItemCount() = stainList.size
}