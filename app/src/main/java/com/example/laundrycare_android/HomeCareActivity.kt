package com.example.laundrycare_android

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

class HomeCareActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_care)

        findViewById<Button>(R.id.btnBack).setOnClickListener { finish() }

        // 아코디언 메뉴 설정
        setupAccordion(R.id.layoutPadding, R.id.tvPaddingTip, R.id.listPadding)
        setupAccordion(R.id.layoutKnit, R.id.tvKnitTip, R.id.listKnit)
        setupAccordion(R.id.layoutLeather, R.id.tvLeatherTip, R.id.listLeather)
        setupAccordion(R.id.layoutSilk, R.id.tvSilkTip, R.id.listSilk)
        setupAccordion(R.id.layoutWool, R.id.tvWoolTip, R.id.listWool)
        setupAccordion(R.id.layoutFunctional, R.id.tvFunctionalTip, R.id.listFunctional)
        setupAccordion(R.id.layoutDenim, R.id.tvDenimTip, R.id.listDenim)

        // 🌟 실크타임 업데이트를 위해 addSnapshotListener 사용
        observeWardrobeData()
    }

    private fun setupAccordion(layoutId: Int, textViewId: Int, listId: Int) {
        val layout = findViewById<LinearLayout>(layoutId)
        val textView = findViewById<TextView>(textViewId)
        val listView = findViewById<LinearLayout>(listId)

        layout.setOnClickListener {
            val isCurrentlyHidden = textView.visibility == View.GONE
            textView.visibility = if (isCurrentlyHidden) View.VISIBLE else View.GONE
            listView.visibility = if (isCurrentlyHidden) View.VISIBLE else View.GONE
        }
    }

    private fun observeWardrobeData() {
        val tvCareSummary = findViewById<TextView>(R.id.tvCareSummary)

        // 🌟 실시간 데이터 감시 시작
        db.collection("clothes").addSnapshotListener { snapshot, error ->
            if (error != null) {
                tvCareSummary.text = "데이터를 불러오는 중 오류가 발생했습니다."
                return@addSnapshotListener
            }

            if (snapshot != null) {
                // 리스트 중복 방지를 위해 기존 뷰 모두 삭제
                clearAllLists()

                var paddingCount = 0
                var knitCount = 0
                var leatherCount = 0
                var silkCount = 0
                var woolCount = 0
                var functionalCount = 0
                var denimCount = 0

                for (doc in snapshot.documents) {
                    // 공백 제거 및 소문자 변환으로 검색 정확도 향상
                    val material = doc.getString("material")?.trim() ?: ""
                    val category = doc.getString("category")?.trim() ?: ""
                    val subCategory = doc.getString("subCategory")?.trim() ?: ""
                    val fullText = material + category + subCategory

                    // 가죽 관련 키워드 검색
                    if (fullText.contains("가죽") || fullText.contains("레더") || fullText.contains("스웨이드")) {
                        leatherCount++
                        addClothToView(R.id.listLeather, doc)
                    }

                    // 패딩 관련 키워드
                    if (fullText.contains("패딩") || fullText.contains("구스") || fullText.contains("다운")) {
                        paddingCount++
                        addClothToView(R.id.listPadding, doc)
                    }

                    // 니트/캐시미어
                    if (fullText.contains("니트") || fullText.contains("스웨터") || fullText.contains("캐시미어")) {
                        knitCount++
                        addClothToView(R.id.listKnit, doc)
                    }

                    // 실크/레이온
                    if (fullText.contains("실크") || fullText.contains("견") || fullText.contains("레이온")) {
                        silkCount++
                        addClothToView(R.id.listSilk, doc)
                    }

                    // 수트/코트/울
                    if (fullText.contains("울") || fullText.contains("모") || fullText.contains("수트") || fullText.contains("코트")) {
                        woolCount++
                        addClothToView(R.id.listWool, doc)
                    }

                    // 기능성
                    if (fullText.contains("기능성") || fullText.contains("고어텍스") || fullText.contains("등산복") || fullText.contains("바람막이")) {
                        functionalCount++
                        addClothToView(R.id.listFunctional, doc)
                    }

                    // 데님
                    if (fullText.contains("데님") || fullText.contains("청바지") || fullText.contains("청자켓")) {
                        denimCount++
                        addClothToView(R.id.listDenim, doc)
                    }
                }

                updateSummaryText(tvCareSummary, paddingCount, knitCount, leatherCount, silkCount, woolCount, functionalCount, denimCount)
            }
        }
    }

    private fun clearAllLists() {
        findViewById<LinearLayout>(R.id.listPadding).removeAllViews()
        findViewById<LinearLayout>(R.id.listKnit).removeAllViews()
        findViewById<LinearLayout>(R.id.listLeather).removeAllViews()
        findViewById<LinearLayout>(R.id.listSilk).removeAllViews()
        findViewById<LinearLayout>(R.id.listWool).removeAllViews()
        findViewById<LinearLayout>(R.id.listFunctional).removeAllViews()
        findViewById<LinearLayout>(R.id.listDenim).removeAllViews()
    }

    private fun updateSummaryText(tv: TextView, p: Int, k: Int, l: Int, s: Int, w: Int, f: Int, d: Int) {
        val currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 1
        val seasonGreeting = when (currentMonth) {
            in 3..5 -> "봄철 환절기 관리가 필요합니다. 🌸"
            in 6..8 -> "여름철 장마와 습기를 대비하세요. ☔"
            in 9..11 -> "가을철 건조한 날씨에 주의하세요. 🍁"
            else -> "겨울철 방한 의류 관리가 중요합니다. ❄️"
        }

        var summaryText = "$seasonGreeting\n\n현재 내 옷장에 "
        val items = mutableListOf<String>()
        if (p > 0) items.add("패딩 $p")
        if (k > 0) items.add("니트 $k")
        if (l > 0) items.add("가죽 $l")
        if (s > 0) items.add("실크 $s")
        if (w > 0) items.add("수트 $w")
        if (f > 0) items.add("기능성 $f")
        if (d > 0) items.add("데님 $d")

        summaryText += if (items.isNotEmpty()) items.joinToString(", ") + "벌이 있습니다.\n항목을 클릭해 가이드를 확인하세요!"
        else "해당하는 특수 소재 의류가 없습니다."

        tv.text = summaryText
    }

    private fun addClothToView(containerId: Int, doc: DocumentSnapshot) {
        val container = findViewById<LinearLayout>(containerId)
        val view = LayoutInflater.from(this).inflate(R.layout.item_clothing, container, false)

        view.findViewById<TextView>(R.id.tvCategory).text = "${doc.getString("category") ?: "카테고리"} > ${doc.getString("subCategory") ?: ""}"
        view.findViewById<TextView>(R.id.tvMaterial).text = "소재: ${doc.getString("material") ?: "미상"}"
        view.findViewById<TextView>(R.id.btnItemOptions).visibility = View.GONE

        val imageUrl = doc.getString("imageUrl") ?: ""
        if (imageUrl.isNotEmpty()) {
            Glide.with(this).load(imageUrl).centerCrop().into(view.findViewById(R.id.ivClothPhoto))
        }

        container.addView(view)
    }
}