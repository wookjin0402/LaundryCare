package com.example.laundrycare_android

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class DietClothingAdapter(
    private val clothingList: MutableList<ClothingItem>,
    private val onMenuClickListener: OnMenuClickListener
) : RecyclerView.Adapter<DietClothingAdapter.DietViewHolder>() {

    interface OnMenuClickListener {
        fun onExtend(item: ClothingItem, position: Int)
        fun onDelete(item: ClothingItem, position: Int)
    }

    class DietViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivPhoto: ImageView = view.findViewById(R.id.ivClothPhoto)
        val tvCategory: TextView = view.findViewById(R.id.tvCategory)
        val tvMaterial: TextView = view.findViewById(R.id.tvMaterial)
        val btnMore: TextView = view.findViewById(R.id.btnItemOptions)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DietViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_clothing, parent, false)
        return DietViewHolder(view)
    }

    override fun onBindViewHolder(holder: DietViewHolder, position: Int) {
        val item = clothingList[position]

        holder.tvCategory.text = "${item.mainCategory} > ${item.subCategory} (${item.season})"
        holder.tvMaterial.text = "소재: ${item.material}"

        Glide.with(holder.itemView.context)
            .load(item.imageUrl)
            .centerCrop()
            .into(holder.ivPhoto)

        holder.itemView.setOnClickListener { view ->
            val intent = Intent(view.context, ClothDetailActivity::class.java)
            intent.putExtra("docId", item.id)
            view.context.startActivity(intent)
        }

        holder.btnMore.setOnClickListener { view ->
            val popup = PopupMenu(view.context, view)
            popup.menu.add("기간 연장 (1년 유지)")
            // 🌟 "버리기"로 텍스트 직관적 변경
            popup.menu.add("버리기 (옷장에서도 완전 삭제)")

            popup.setOnMenuItemClickListener { menuItem ->
                when (menuItem.title) {
                    "기간 연장 (1년 유지)" -> onMenuClickListener.onExtend(item, position)
                    "버리기 (옷장에서도 완전 삭제)" -> onMenuClickListener.onDelete(item, position)
                }
                true
            }
            popup.show()
        }
    }

    override fun getItemCount(): Int = clothingList.size
}