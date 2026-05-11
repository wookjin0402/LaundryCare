package com.example.laundrycare_android

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.firestore.FirebaseFirestore

class ClothingAdapter(private val clothingList: List<ClothingItem>) : RecyclerView.Adapter<ClothingAdapter.ClothingViewHolder>() {

    class ClothingViewHolder(view: View) : RecyclerView.ViewHolder(view) {
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
        if (item.imageUrl.isNotEmpty()) Glide.with(holder.itemView.context).load(item.imageUrl).centerCrop().into(holder.ivClothPhoto)

        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            context.startActivity(Intent(context, ClothDetailActivity::class.java).apply {
                putExtra("docId", item.id); putExtra("imageUrl", item.imageUrl)
                putExtra("season", item.season); putExtra("mainCategory", item.mainCategory)
                putExtra("subCategory", item.subCategory); putExtra("material", item.material)
                putExtra("laundryTip", item.laundryTip)
            })
        }

        holder.btnItemOptions.setOnClickListener {
            showBottomSheet(holder.itemView.context, item)
        }
    }

    private fun showBottomSheet(context: android.content.Context, item: ClothingItem) {
        val dialog = BottomSheetDialog(context)
        val view = LayoutInflater.from(context).inflate(R.layout.layout_bottom_sheet, null)
        dialog.setContentView(view)

        view.findViewById<TextView>(R.id.tvEdit).setOnClickListener {
            dialog.dismiss()
            showEditDialog(context, item)
        }
        view.findViewById<TextView>(R.id.tvDelete).setOnClickListener {
            dialog.dismiss()
            FirebaseFirestore.getInstance().collection("clothes").document(item.id).delete()
        }
        view.findViewById<TextView>(R.id.tvCancel).setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun showEditDialog(context: android.content.Context, item: ClothingItem) {
        val layout = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL; setPadding(50, 30, 50, 10) }

        val subCategoryMap = mapOf(
            "상의" to arrayOf("반팔", "긴팔", "아우터"),
            "하의" to arrayOf("반바지", "긴바지", "치마"),
            "고급" to arrayOf("명품", "기능성"),
            "기타" to arrayOf("양말", "속옷")
        )

        val spinnerSeason = Spinner(context).apply { adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, arrayOf("봄", "여름", "가을", "겨울")) }
        spinnerSeason.setSelection((spinnerSeason.adapter as ArrayAdapter<String>).getPosition(item.season))

        val spinnerMain = Spinner(context).apply { adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, subCategoryMap.keys.toTypedArray()) }
        spinnerMain.setSelection((spinnerMain.adapter as ArrayAdapter<String>).getPosition(item.mainCategory))

        val spinnerSub = Spinner(context)

        // 🌟 대분류 선택에 따라 소분류 어댑터가 실시간으로 바뀜!
        spinnerMain.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                val selectedMain = spinnerMain.selectedItem.toString()
                val subAdapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, subCategoryMap[selectedMain]!!)
                spinnerSub.adapter = subAdapter
                if (selectedMain == item.mainCategory) spinnerSub.setSelection(subAdapter.getPosition(item.subCategory))
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }

        layout.addView(TextView(context).apply { text = "계절" }); layout.addView(spinnerSeason)
        layout.addView(TextView(context).apply { text = "대분류" }); layout.addView(spinnerMain)
        layout.addView(TextView(context).apply { text = "소분류" }); layout.addView(spinnerSub)

        AlertDialog.Builder(context).setTitle("정보 수정").setView(layout)
            .setPositiveButton("저장") { _, _ ->
                FirebaseFirestore.getInstance().collection("clothes").document(item.id)
                    .update("season", spinnerSeason.selectedItem.toString(),
                        "mainCategory", spinnerMain.selectedItem.toString(),
                        "subCategory", spinnerSub.selectedItem.toString())
            }.show()
    }

    override fun getItemCount() = clothingList.size
}