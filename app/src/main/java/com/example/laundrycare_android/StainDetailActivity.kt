package com.example.laundrycare_android

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.firestore.FirebaseFirestore

class StainDetailActivity : AppCompatActivity() {

    private lateinit var docId: String
    private var currentCategory = ""
    private var currentMaterial = ""
    private var currentStainType = ""
    private var currentSolution = ""
    private lateinit var tvDetailContent: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stain_detail)

        tvDetailContent = findViewById(R.id.tvDetailContent)
        val btnBack = findViewById<Button>(R.id.btnBack)

        // 🌟 새로 만든 점 세 개 텍스트 버튼
        val btnOptionsMenu = findViewById<TextView>(R.id.btnOptionsMenu)

        docId = intent.getStringExtra("docId") ?: ""
        currentCategory = intent.getStringExtra("category") ?: ""
        currentMaterial = intent.getStringExtra("material") ?: ""
        currentStainType = intent.getStringExtra("stainType") ?: ""
        currentSolution = intent.getStringExtra("solution") ?: ""

        updateUI()

        btnBack.setOnClickListener { finish() }

        // 🌟 점 세 개 버튼을 누르면 바텀 시트(팝업) 띄우기
        btnOptionsMenu.setOnClickListener {
            showBottomSheet()
        }
    }

    private fun updateUI() {
        tvDetailContent.text = "[ 옷 정보 ]\n종류: $currentCategory\n소재: $currentMaterial\n\n[ 얼룩 종류 ]\n$currentStainType\n\n[ 💡 해결책 ]\n$currentSolution"
    }

    // 🌟 당근마켓 스타일 바텀 시트(아래에서 올라오는 팝업) 띄우기 함수
    private fun showBottomSheet() {
        // 1단계에서 만든 팝업창 디자인을 가져옵니다.
        val bottomSheetView = layoutInflater.inflate(R.layout.layout_bottom_sheet, null)
        val bottomSheetDialog = BottomSheetDialog(this)
        bottomSheetDialog.setContentView(bottomSheetView)

        // 팝업창 안의 '수정' 버튼 눌렀을 때
        bottomSheetView.findViewById<TextView>(R.id.tvEdit).setOnClickListener {
            bottomSheetDialog.dismiss() // 팝업창 닫기
            showEditDialog() // 수정 다이얼로그 띄우기
        }

        // 팝업창 안의 '삭제' 버튼 눌렀을 때
        bottomSheetView.findViewById<TextView>(R.id.tvDelete).setOnClickListener {
            bottomSheetDialog.dismiss() // 팝업창 닫기
            showDeleteConfirmationDialog() // 삭제 경고 띄우기
        }

        // 팝업창 안의 '닫기' 버튼 눌렀을 때
        bottomSheetView.findViewById<TextView>(R.id.tvCancel).setOnClickListener {
            bottomSheetDialog.dismiss() // 그냥 팝업창만 닫기
        }

        // 세팅이 끝난 팝업창을 화면에 보여줍니다.
        bottomSheetDialog.show()
    }

    // 기존의 수정 기능 로직
    private fun showEditDialog() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 30, 50, 10)
        }
        val etCategory = EditText(this).apply { hint = "옷 종류"; setText(currentCategory) }
        val etMaterial = EditText(this).apply { hint = "소재"; setText(currentMaterial) }
        val etStain = EditText(this).apply { hint = "얼룩 종류"; setText(currentStainType) }
        layout.addView(etCategory)
        layout.addView(etMaterial)
        layout.addView(etStain)

        AlertDialog.Builder(this)
            .setTitle("정보 수정")
            .setView(layout)
            .setPositiveButton("저장") { _, _ ->
                currentCategory = etCategory.text.toString()
                currentMaterial = etMaterial.text.toString()
                currentStainType = etStain.text.toString()

                val db = FirebaseFirestore.getInstance()
                db.collection("stains").document(docId)
                    .update("category", currentCategory, "material", currentMaterial, "stainType", currentStainType)
                    .addOnSuccessListener {
                        updateUI()
                        Toast.makeText(this, "수정 완료", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("취소", null)
            .show()
    }

    // 기존의 삭제 기능 로직
    private fun showDeleteConfirmationDialog() {
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
}