package com.example.laundrycare_android

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.firestore.FirebaseFirestore

class StainDetailActivity : AppCompatActivity() {

    private lateinit var docId: String
    private var currentSeason = ""
    private var currentMain = ""
    private var currentSub = ""
    private var currentStainType = ""
    private var currentSolution = ""
    private lateinit var tvDetailContent: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stain_detail)

        tvDetailContent = findViewById(R.id.tvDetailContent)
        val btnBack = findViewById<Button>(R.id.btnBack)
        val btnOptionsMenu = findViewById<TextView>(R.id.btnOptionsMenu)

        docId = intent.getStringExtra("docId") ?: ""
        currentSeason = intent.getStringExtra("season") ?: "여름"
        currentMain = intent.getStringExtra("mainCategory") ?: "상의"
        currentSub = intent.getStringExtra("subCategory") ?: "반팔"
        currentStainType = intent.getStringExtra("stainType") ?: ""
        currentSolution = intent.getStringExtra("solution") ?: ""

        updateUI()

        btnBack.setOnClickListener { finish() }
        btnOptionsMenu.setOnClickListener { showBottomSheet() }
    }

    private fun updateUI() {
        tvDetailContent.text = "[ 옷 정보 ]\n계절: $currentSeason\n분류: $currentMain ($currentSub)\n\n[ 얼룩 종류 ]\n$currentStainType\n\n[ 💡 해결책 ]\n$currentSolution"
    }

    private fun showBottomSheet() {
        val bottomSheetView = layoutInflater.inflate(R.layout.layout_bottom_sheet, null)
        val bottomSheetDialog = BottomSheetDialog(this)
        bottomSheetDialog.setContentView(bottomSheetView)

        bottomSheetView.findViewById<TextView>(R.id.tvEdit).setOnClickListener {
            bottomSheetDialog.dismiss()
            showEditDialog()
        }

        bottomSheetView.findViewById<TextView>(R.id.tvDelete).setOnClickListener {
            bottomSheetDialog.dismiss()
            AlertDialog.Builder(this)
                .setTitle("경고")
                .setMessage("정말 이 기록을 삭제하시겠습니까?")
                .setPositiveButton("삭제") { _, _ ->
                    FirebaseFirestore.getInstance().collection("stains").document(docId).delete()
                        .addOnSuccessListener {
                            Toast.makeText(this, "삭제되었습니다.", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                }
                .setNegativeButton("취소", null)
                .show()
        }

        bottomSheetView.findViewById<TextView>(R.id.tvCancel).setOnClickListener {
            bottomSheetDialog.dismiss()
        }

        bottomSheetDialog.show()
    }

    private fun showEditDialog() {
        val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(50, 30, 50, 10) }

        // 🌟 옷장과 완벽하게 동일한 3단계 카테고리 로직!
        val subCategoryMap = mapOf(
            "상의" to arrayOf("반팔", "긴팔", "아우터"),
            "하의" to arrayOf("반바지", "긴바지", "치마"),
            "고급" to arrayOf("명품", "기능성"),
            "기타" to arrayOf("양말", "속옷")
        )

        val tvSeason = TextView(this).apply { text = "계절 선택"; setPadding(0, 20, 0, 10) }
        val spinnerSeason = Spinner(this)
        val seasonAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, arrayOf("봄", "여름", "가을", "겨울"))
        spinnerSeason.adapter = seasonAdapter
        spinnerSeason.setSelection(seasonAdapter.getPosition(currentSeason))

        val tvMain = TextView(this).apply { text = "대분류 선택"; setPadding(0, 20, 0, 10) }
        val spinnerMain = Spinner(this)
        val mainAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, subCategoryMap.keys.toTypedArray())
        spinnerMain.adapter = mainAdapter
        spinnerMain.setSelection(mainAdapter.getPosition(currentMain))

        val tvSub = TextView(this).apply { text = "소분류 선택"; setPadding(0, 20, 0, 10) }
        val spinnerSub = Spinner(this)

        spinnerMain.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                val selectedMain = spinnerMain.selectedItem.toString()
                val subAdapter = ArrayAdapter(this@StainDetailActivity, android.R.layout.simple_spinner_dropdown_item, subCategoryMap[selectedMain]!!)
                spinnerSub.adapter = subAdapter
                if (selectedMain == currentMain) {
                    spinnerSub.setSelection(subAdapter.getPosition(currentSub))
                }
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }

        val tvStain = TextView(this).apply { text = "얼룩 종류 (직접 입력)"; setPadding(0, 20, 0, 10) }
        val etStain = EditText(this).apply { setText(currentStainType) }

        layout.addView(tvSeason)
        layout.addView(spinnerSeason)
        layout.addView(tvMain)
        layout.addView(spinnerMain)
        layout.addView(tvSub)
        layout.addView(spinnerSub)
        layout.addView(tvStain)
        layout.addView(etStain)

        AlertDialog.Builder(this)
            .setTitle("얼룩 정보 수정")
            .setView(layout)
            .setPositiveButton("저장") { _, _ ->
                currentSeason = spinnerSeason.selectedItem.toString()
                currentMain = spinnerMain.selectedItem.toString()
                currentSub = spinnerSub.selectedItem.toString()
                currentStainType = etStain.text.toString()

                val db = FirebaseFirestore.getInstance()
                db.collection("stains").document(docId)
                    .update("season", currentSeason, "mainCategory", currentMain, "subCategory", currentSub, "stainType", currentStainType)
                    .addOnSuccessListener {
                        updateUI()
                        Toast.makeText(this, "수정 완료!", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("취소", null)
            .show()
    }
}