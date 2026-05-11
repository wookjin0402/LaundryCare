package com.example.laundrycare_android

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.firestore.FirebaseFirestore

class ClothDetailActivity : AppCompatActivity() {
    private lateinit var docId: String
    private var currentSeason = ""; private var currentMain = ""; private var currentSub = ""
    private var currentMaterial = ""; private var currentLaundryTip = ""; private var currentImageUrl = ""
    private lateinit var tvDetailContent: TextView; private lateinit var ivClothPhoto: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cloth_detail)

        ivClothPhoto = findViewById(R.id.ivClothPhoto); tvDetailContent = findViewById(R.id.tvDetailContent)
        val btnBack = findViewById<Button>(R.id.btnBack); val btnOptionsMenu = findViewById<TextView>(R.id.btnOptionsMenu)

        docId = intent.getStringExtra("docId") ?: ""
        currentSeason = intent.getStringExtra("season") ?: "여름"
        currentMain = intent.getStringExtra("mainCategory") ?: "상의"
        currentSub = intent.getStringExtra("subCategory") ?: "반팔"
        currentMaterial = intent.getStringExtra("material") ?: ""
        currentLaundryTip = intent.getStringExtra("laundryTip") ?: ""
        currentImageUrl = intent.getStringExtra("imageUrl") ?: ""

        updateUI()
        btnBack.setOnClickListener { finish() }
        btnOptionsMenu.setOnClickListener { showBottomSheet() }
    }

    private fun updateUI() {
        tvDetailContent.text = "[ 옷 정보 ]\n계절: $currentSeason\n분류: $currentMain ($currentSub)\n소재: $currentMaterial\n\n[ 세탁 팁 ]\n$currentLaundryTip"
        if (currentImageUrl.isNotEmpty()) Glide.with(this).load(currentImageUrl).centerCrop().into(ivClothPhoto)
    }

    private fun showBottomSheet() {
        val view = layoutInflater.inflate(R.layout.layout_bottom_sheet, null)
        val dialog = BottomSheetDialog(this); dialog.setContentView(view)
        view.findViewById<TextView>(R.id.tvEdit).setOnClickListener { dialog.dismiss(); showEditDialog() }
        view.findViewById<TextView>(R.id.tvDelete).setOnClickListener {
            FirebaseFirestore.getInstance().collection("clothes").document(docId).delete().addOnSuccessListener { finish() }
        }
        view.findViewById<TextView>(R.id.tvCancel).setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun showEditDialog() {
        val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(50, 30, 50, 10) }
        val subCategoryMap = mapOf("상의" to arrayOf("반팔", "긴팔", "아우터"), "하의" to arrayOf("반바지", "긴바지", "치마"), "고급" to arrayOf("명품", "기능성"), "기타" to arrayOf("양말", "속옷"))

        val spinnerSeason = Spinner(this).apply { adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, arrayOf("봄", "여름", "가을", "겨울")) }
        spinnerSeason.setSelection((spinnerSeason.adapter as ArrayAdapter<String>).getPosition(currentSeason))

        val spinnerMain = Spinner(this).apply { adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, subCategoryMap.keys.toTypedArray()) }
        spinnerMain.setSelection((spinnerMain.adapter as ArrayAdapter<String>).getPosition(currentMain))

        val spinnerSub = Spinner(this)
        spinnerMain.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                val selectedMain = spinnerMain.selectedItem.toString()
                val subAdapter = ArrayAdapter(this@ClothDetailActivity, android.R.layout.simple_spinner_dropdown_item, subCategoryMap[selectedMain]!!)
                spinnerSub.adapter = subAdapter
                if (selectedMain == currentMain) spinnerSub.setSelection(subAdapter.getPosition(currentSub))
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }

        layout.addView(TextView(this).apply { text = "계절" }); layout.addView(spinnerSeason)
        layout.addView(TextView(this).apply { text = "대분류" }); layout.addView(spinnerMain)
        layout.addView(TextView(this).apply { text = "소분류" }); layout.addView(spinnerSub)

        AlertDialog.Builder(this).setTitle("정보 수정").setView(layout)
            .setPositiveButton("저장") { _, _ ->
                currentSeason = spinnerSeason.selectedItem.toString()
                currentMain = spinnerMain.selectedItem.toString()
                currentSub = spinnerSub.selectedItem.toString()
                FirebaseFirestore.getInstance().collection("clothes").document(docId)
                    .update("season", currentSeason, "mainCategory", currentMain, "subCategory", currentSub)
                    .addOnSuccessListener { updateUI() }
            }.show()
    }
}