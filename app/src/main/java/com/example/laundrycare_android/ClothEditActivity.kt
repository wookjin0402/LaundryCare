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
        setContentView(R.layout.activity_result)

        docId = intent.getStringExtra("docId") ?: ""
        if (docId.isEmpty()) {
            Toast.makeText(this, "옷 정보를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val btnBack = findViewById<ImageView>(R.id.btnBack)
        val btnSave = findViewById<Button>(R.id.btnSave)
        btnSave.text = "수정 내용 저장하기"

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

        loadExistingData()

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
                    finish()
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

                    val currentMain = document.getString("mainCategory") ?: "상의"
                    val currentSub = document.getString("subCategory") ?: ""

                    // 🌟 1. 자동 이벤트에 의존하지 않고, 처음부터 소분류 목록을 강제로 만들어서 채워 넣습니다. (먹통 해결)
                    val initialSubCategories = subCategoryMap[currentMain] ?: arrayOf("기타")
                    spinnerSub.adapter = ArrayAdapter(this@ClothEditActivity, android.R.layout.simple_spinner_dropdown_item, initialSubCategories)

                    // 🌟 2. DB에서 가져온 값으로 모든 스피너의 초기 세팅을 완료합니다.
                    setSpinnerToValue(spinnerSeason, document.getString("season") ?: "")
                    setSpinnerToValue(spinnerMain, currentMain)
                    setSpinnerToValue(spinnerSub, currentSub)

                    // 🌟 3. 초기 세팅이 완전히 끝난 '이후'에 리스너를 달아서, 유저가 직접 바꿀 때만 동작하게 합니다.
                    var lastMain = currentMain
                    spinnerMain.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(p0: AdapterView<*>?, p1: View?, position: Int, p3: Long) {
                            val selectedMain = spinnerMain.selectedItem.toString()
                            // 진짜로 다른 대분류를 터치해서 바꿨을 때만 소분류 갈아끼우기
                            if (selectedMain != lastMain) {
                                lastMain = selectedMain
                                val newSubCategories = subCategoryMap[selectedMain] ?: arrayOf("기타")
                                spinnerSub.adapter = ArrayAdapter(this@ClothEditActivity, android.R.layout.simple_spinner_dropdown_item, newSubCategories)
                                // 대분류가 바뀌면 소분류는 첫 번째 항목으로 초기화
                                spinnerSub.setSelection(0)
                            }
                        }
                        override fun onNothingSelected(p0: AdapterView<*>?) {}
                    }

                    // 텍스트 필드 세팅
                    etColor.setText(document.getString("color") ?: "")
                    etSize.setText(document.getString("size") ?: "")
                    etMaterial.setText(document.getString("material") ?: "")

                    // 이미지 및 주의사항 세팅
                    val ivResultPhoto = findViewById<ImageView>(R.id.ivResultPhoto)
                    Glide.with(this).load(document.getString("imageUrl") ?: "").centerCrop().into(ivResultPhoto)

                    findViewById<TextView>(R.id.tvGuideTitle).text = "의류 정보 수정"
                    findViewById<TextView>(R.id.tvWarnings).text = document.getString("warnings") ?: "특이사항 없음"
                    findViewById<TextView>(R.id.tvCareSteps).text = document.getString("careSteps") ?: "관리 정보 없음"
                    findViewById<TextView>(R.id.tvRawTags).visibility = View.GONE
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