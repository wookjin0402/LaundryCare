package com.example.laundrycare_android

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class ClothingAdapter(private val items: List<ClothingItem>) :
    RecyclerView.Adapter<ClothingAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_clothing, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvCategory.text = item.category
        holder.tvInfo.text = "${item.material} | ${item.laundryTip}"

        // Glide를 사용하여 실제 사진 표시
        // test_label 대신 기본 배경 이미지를 사용하도록 수정했습니다.
        Glide.with(holder.itemView.context)
            .load(item.imageUrl)
            .placeholder(androidx.appcompat.R.color.material_grey_300)
            .error(androidx.appcompat.R.color.material_grey_300)
            .into(holder.ivPhoto)
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivPhoto: ImageView = view.findViewById(R.id.ivItemPhoto)
        val tvCategory: TextView = view.findViewById(R.id.tvItemCategory)
        val tvInfo: TextView = view.findViewById(R.id.tvItemInfo)
    }
}