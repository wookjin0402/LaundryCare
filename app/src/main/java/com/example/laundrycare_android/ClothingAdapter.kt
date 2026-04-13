package com.example.laundrycare_android

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

// 창고(데이터)와 진열대(화면)를 연결해주는 관리자
class ClothingAdapter(private val items: List<ClothingItem>) :
    RecyclerView.Adapter<ClothingAdapter.ViewHolder>() {

    // 1. 옷걸이(XML 디자인)를 준비하는 역할
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_clothing, parent, false)
        return ViewHolder(view)
    }

    // 2. 준비된 옷걸이에 '진짜 옷 정보'를 채워 넣는 역할
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        holder.tvCategory.text = item.category
        holder.tvInfo.text = "${item.material} | ${item.laundryTip}"
        // ※ 사진은 나중에 실제 이미지를 받아오면 이 부분에서 띄워줄 겁니다!
    }

    // 3. 창고에 옷이 총 몇 개 있는지 확인
    override fun getItemCount(): Int = items.size

    // 4. XML 파일 안에 있는 사진, 글씨 이름표를 미리 찾아두는 곳
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivPhoto: ImageView = view.findViewById(R.id.ivItemPhoto)
        val tvCategory: TextView = view.findViewById(R.id.tvItemCategory)
        val tvInfo: TextView = view.findViewById(R.id.tvItemInfo)
    }
}