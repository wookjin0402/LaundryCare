package com.example.laundrycare_android

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore

class ClothEditActivity : AppCompatActivity() {

    private lateinit var docId: String

    private lateinit var spinnerSeason: Spinner
    private lateinit var spinnerMain: Spinner
    private lateinit var spinnerSub: Spinner
    private lateinit var etColor: EditText
    private lateinit var etSize: EditText
    private lateinit var etMaterial: EditText

    private val subCategoryMap = mapOf(
        "상의" to arrayOf("반팔", "긴팔", "아우터"),
        "하의" to arrayOf("반바지", "긴바지", "치마"),
        "고급" to arrayOf("명품", "기능성"),
        "기타" to arrayOf("양말", "속옷")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 🌟 1번 사진(ResultActivity)의 넓은 화면 레이아웃을 그대로 재활용!
        setContentView(R.layout.activity_result)

        docId = intent.getStringExtra("docId") ?: ""
        if (docId.isEmpty()) {
            Toast.makeText(this, "옷 정보를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val btnBack = findViewById<ImageView>(R.id.btnBack)
        val btnSave = findViewById<Button>(R.id.btnSave)
        btnSave.text = "수정 내용 저장하기" // 버튼 이름 변경

        spinnerSeason = findViewById(R.id.spinnerSeason)
        spinnerMain = findViewById(R.id.spinnerMain)
        spinnerSub = findViewById(R.id.spinnerSub)
        etColor = findViewById(R.id.etColor)
        etSize = findViewById(R.id.etSize)
        etMaterial = findViewById(R.id.etMaterial)

        val seasons = arrayOf("봄", "여름", "가을", "겨울")
        spinnerSeason.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, seasons)

        val mainCategories = subCategoryMap.keys.toTypedArray()
        spinnerMain.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, mainCategories)

        btnBack.setOnClickListener { finish() }

        // 1. 파이어베이스에서 기존 데이터 불러와서 화면에 채워 넣기
        loadExistingData()

        // 2. 수정된 데이터 파이어베이스에 덮어쓰기 (Update)
        btnSave.setOnClickListener {
            btnSave.isEnabled = false
            btnSave.text = "업데이트 중..."

            val updatedData = mapOf(
                "season" to spinnerSeason.selectedItem.toString(),
                "mainCategory" to spinnerMain.selectedItem.toString(),
                "subCategory" to (spinnerSub.selectedItem?.toString() ?: ""),
                "color" to etColor.text.toString(),
                "size" to etSize.text.toString(),
                "material" to etMaterial.text.toString()
            )

            FirebaseFirestore.getInstance().collection("clothes").document(docId)
                .update(updatedData)
                .addOnSuccessListener {
                    Toast.makeText(this, "수정이 완료되었습니다!", Toast.LENGTH_SHORT).show()
                    finish() // 수정 완료 후 이전 상세 화면으로 돌아감
                }
                .addOnFailureListener {
                    Toast.makeText(this, "수정 실패", Toast.LENGTH_SHORT).show()
                    btnSave.isEnabled = true
                    btnSave.text = "수정 내용 저장하기"
                }
        }
    }

    private fun loadExistingData() {
        FirebaseFirestore.getInstance().collection("clothes").document(docId).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    // 스피너 값 세팅
                    setSpinnerToValue(spinnerSeason, document.getString("season") ?: "")
                    setSpinnerToValue(spinnerMain, document.getString("mainCategory") ?: "")

                    // 대분류가 선택된 후 소분류 어댑터 세팅 및 값 지정
                    val currentSub = document.getString("subCategory") ?: ""
                    spinnerMain.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(p0: AdapterView<*>?, p1: View?, position: Int, p3: Long) {
                            val selectedMain = spinnerMain.selectedItem.toString()
                            val subCategories = subCategoryMap[selectedMain] ?: arrayOf("기타")
                            spinnerSub.adapter = ArrayAdapter(this@ClothEditActivity, android.R.layout.simple_spinner_dropdown_item, subCategories)
                            setSpinnerToValue(spinnerSub, currentSub)
                        }
                        override fun onNothingSelected(p0: AdapterView<*>?) {}
                    }

                    // 수기 입력 칸 세팅
                    etColor.setText(document.getString("color") ?: "")
                    etSize.setText(document.getString("size") ?: "")
                    etMaterial.setText(document.getString("material") ?: "")

                    // 상단 이미지 및 AI 주의사항 텍스트 그대로 불러오기 (수정 불가/열람용)
                    val ivResultPhoto = findViewById<ImageView>(R.id.ivResultPhoto)
                    Glide.with(this).load(document.getString("imageUrl") ?: "").centerCrop().into(ivResultPhoto)

                    findViewById<TextView>(R.id.tvGuideTitle).text = "의류 정보 수정"
                    findViewById<TextView>(R.id.tvWarnings).text = document.getString("warnings") ?: "특이사항 없음"
                    findViewById<TextView>(R.id.tvCareSteps).text = document.getString("careSteps") ?: "관리 정보 없음"
                    findViewById<TextView>(R.id.tvRawTags).visibility = View.GONE // 태그는 안 보여줘도 무방함
                }
            }
    }

    private fun setSpinnerToValue(spinner: Spinner, value: String) {
        val adapter = spinner.adapter
        if (adapter != null) {
            for (i in 0 until adapter.count) {
                if (adapter.getItem(i).toString() == value) {
                    spinner.setSelection(i)
                    break
                }
            }
        }
    }
}