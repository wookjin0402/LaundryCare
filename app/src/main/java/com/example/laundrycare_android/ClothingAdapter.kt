package com.example.laundrycare_android

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.firestore.FirebaseFirestore

class ClothingAdapter(
    private val clothingList: List<ClothingItem>,
    private val onSelectionModeChanged: (Boolean) -> Unit
) : RecyclerView.Adapter<ClothingAdapter.ClothingViewHolder>() {

    var isSelectionMode = false
    private var isAllSelected = false

    class ClothingViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cbSelect: CheckBox = view.findViewById(R.id.cbClothSelect)
        val ivClothPhoto: ImageView = view.findViewById(R.id.ivClothPhoto)
        val tvCategory: TextView = view.findViewById(R.id.tvCategory)
        val tvMaterial: TextView = view.findViewById(R.id.tvMaterial)
        val btnItemOptions: TextView = view.findViewById(R.id.btnItemOptions)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClothingViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_clothing, parent, false)
        return ClothingViewHolder(view)
    }

    override fun onBindViewHolder(holder: ClothingViewHolder, position: Int) {
        val item = clothingList[position]
        holder.tvCategory.text = "${item.mainCategory} (${item.subCategory})"
        holder.tvMaterial.text = item.material

        if (item.imageUrl.isNotEmpty()) {
            Glide.with(holder.itemView.context).load(item.imageUrl).centerCrop().into(holder.ivClothPhoto)
        }

        holder.cbSelect.visibility = if (isSelectionMode) View.VISIBLE else View.GONE
        holder.cbSelect.isChecked = item.isSelected
        holder.btnItemOptions.visibility = if (isSelectionMode) View.GONE else View.VISIBLE

        // 🌟🌟 문제 해결: 체크박스 네모 칸을 직접 터치했을 때도 상태를 저장하도록 추가 🌟🌟
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
                val context = holder.itemView.context
                context.startActivity(Intent(context, ClothDetailActivity::class.java).apply {
                    putExtra("docId", item.id)
                    putExtra("imageUrl", item.imageUrl)
                    putExtra("season", item.season)
                    putExtra("mainCategory", item.mainCategory)
                    putExtra("subCategory", item.subCategory)
                    putExtra("material", item.material)
                    putExtra("laundryTip", item.laundryTip)
                    putExtra("warnings", item.warnings)
                })
            }
        }

        holder.btnItemOptions.setOnClickListener {
            showBottomSheet(holder.itemView.context, item)
        }
    }

    fun selectAll() {
        isAllSelected = !isAllSelected
        clothingList.forEach { it.isSelected = isAllSelected }
        notifyDataSetChanged()
    }

    fun exitSelectionMode() {
        if (!isSelectionMode) return
        isSelectionMode = false
        isAllSelected = false
        clothingList.forEach { it.isSelected = false }
        onSelectionModeChanged(false)
        notifyDataSetChanged()
    }

    private fun showBottomSheet(context: Context, item: ClothingItem) {
        val dialog = BottomSheetDialog(context)
        val view = LayoutInflater.from(context).inflate(R.layout.layout_bottom_sheet, null)
        dialog.setContentView(view)

        view.findViewById<TextView>(R.id.tvEdit).setOnClickListener {
            dialog.dismiss()
            val intent = Intent(context, ClothEditActivity::class.java)
            intent.putExtra("docId", item.id)
            context.startActivity(intent)
        }

        view.findViewById<TextView>(R.id.tvDelete).setOnClickListener {
            dialog.dismiss()
            AlertDialog.Builder(context).setTitle("삭제").setMessage("정말 삭제하시겠습니까?")
                .setPositiveButton("삭제") { _, _ ->
                    FirebaseFirestore.getInstance().collection("clothes").document(item.id).delete()
                }.setNegativeButton("취소", null).show()
        }

        view.findViewById<TextView>(R.id.tvCancel).setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    override fun getItemCount() = clothingList.size
}