package com.example.laundrycare_android

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class SeasonFragment : Fragment() {

    private var currentTabCategory: String = "전체"
    private lateinit var rvSeasonClothing: RecyclerView
    private lateinit var adapter: ClothingAdapter
    private val clothingList = mutableListOf<ClothingItem>()

    private var currentSubCategory: String = "전체"
    private var firestoreListener: ListenerRegistration? = null

    // 🌟 다중 선택 모드 UI 요소
    private lateinit var layoutSelectionMode: LinearLayout
    private lateinit var btnSelectAll: Button
    private lateinit var btnDeleteSelected: Button

    // 🌟 시스템 뒤로가기 콜백
    private lateinit var backPressedCallback: OnBackPressedCallback

    companion object {
        fun newInstance(categoryName: String): SeasonFragment {
            val fragment = SeasonFragment()
            val args = Bundle()
            args.putString("SEASON", categoryName)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        currentTabCategory = arguments?.getString("SEASON") ?: "전체"
        val originalRoot = inflater.inflate(R.layout.fragment_season, container, false)

        // 🌟 겹침 방지: 수직으로 쌓아 리스트를 밀어내는 레이아웃 생성
        val wrapperLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            setBackgroundColor(android.graphics.Color.parseColor("#F5F5F5"))
        }

        // 🌟 상단 메뉴 바 세팅
        layoutSelectionMode = LinearLayout(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.END or android.view.Gravity.CENTER_VERTICAL
            setPadding(32, 24, 32, 24)
            setBackgroundColor(android.graphics.Color.parseColor("#FFFFFF"))
            visibility = View.GONE
            elevation = 10f
        }

        btnSelectAll = Button(requireContext()).apply {
            text = "전체 선택"
            textSize = 14f
            backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#1976D2"))
            setTextColor(android.graphics.Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                marginEnd = 16
            }
            setOnClickListener { adapter.selectAll() }
        }

        btnDeleteSelected = Button(requireContext()).apply {
            text = "선택 삭제"
            textSize = 14f
            backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#D32F2F"))
            setTextColor(android.graphics.Color.WHITE)
            setOnClickListener { deleteSelectedItems() }
        }

        layoutSelectionMode.addView(btnSelectAll)
        layoutSelectionMode.addView(btnDeleteSelected)

        // 🌟 순서대로 배치 (메뉴 바 먼저, 그 다음 원래 리스트 화면)
        wrapperLayout.addView(layoutSelectionMode)
        wrapperLayout.addView(originalRoot, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            0,
            1f // 남은 화면 공간 꽉 채우기
        ))

        return wrapperLayout
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 🌟 시스템 뒤로가기 가로채기 등록
        backPressedCallback = object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() {
                adapter.exitSelectionMode()
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, backPressedCallback)

        rvSeasonClothing = view.findViewById(R.id.rvSeasonClothing)
        rvSeasonClothing.layoutManager = LinearLayoutManager(requireContext())

        // 🌟 수정된 어댑터 연결 (콜백 연동)
        adapter = ClothingAdapter(clothingList) { isSelectionMode ->
            layoutSelectionMode.visibility = if (isSelectionMode) View.VISIBLE else View.GONE
            backPressedCallback.isEnabled = isSelectionMode
        }
        rvSeasonClothing.adapter = adapter

        // --- 기존 조원분이 만드신 칩 로직 그대로 유지 ---
        val chipGroupCategory = view.findViewById<ChipGroup>(R.id.chipGroupCategory)
        val chipGroupSubCategory = view.findViewById<ChipGroup>(R.id.chipGroupSubCategory)

        chipGroupCategory.visibility = View.GONE

        val subChips = mapOf(
            "상의" to listOf(view.findViewById<Chip>(R.id.chipTopShort), view.findViewById<Chip>(R.id.chipTopLong), view.findViewById<Chip>(R.id.chipOuter)),
            "하의" to listOf(view.findViewById<Chip>(R.id.chipBottomShort), view.findViewById<Chip>(R.id.chipBottomLong), view.findViewById<Chip>(R.id.chipSkirt)),
            "고급" to listOf(view.findViewById<Chip>(R.id.chipPremiumLuxury), view.findViewById<Chip>(R.id.chipPremiumFunc)),
            "기타" to listOf(view.findViewById<Chip>(R.id.chipEtcSocks), view.findViewById<Chip>(R.id.chipEtcUnderwear))
        )

        if (currentTabCategory == "전체") {
            chipGroupSubCategory.visibility = View.GONE
            currentSubCategory = "전체"
        } else {
            chipGroupSubCategory.visibility = View.VISIBLE
            subChips.values.flatten().forEach { it.visibility = View.GONE }
            subChips[currentTabCategory]?.forEach { it.visibility = View.VISIBLE }

            val firstChipId = subChips[currentTabCategory]?.firstOrNull()?.id
            if (firstChipId != null) {
                chipGroupSubCategory.check(firstChipId)
                currentSubCategory = getSubCategoryName(firstChipId)
            }
        }

        chipGroupSubCategory.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                currentSubCategory = getSubCategoryName(checkedIds[0])
                updateClothesList()
            }
        }
    }

    private fun getSubCategoryName(id: Int): String {
        return when (id) {
            R.id.chipTopShort -> "반팔"; R.id.chipTopLong -> "긴팔"; R.id.chipOuter -> "아우터"
            R.id.chipBottomShort -> "반바지"; R.id.chipBottomLong -> "긴바지"; R.id.chipSkirt -> "치마"
            R.id.chipPremiumLuxury -> "명품"; R.id.chipPremiumFunc -> "기능성"
            R.id.chipEtcSocks -> "양말"; R.id.chipEtcUnderwear -> "속옷"
            else -> "반팔"
        }
    }

    override fun onResume() {
        super.onResume()
        updateClothesList()
    }

    // 🌟 다중 선택 삭제 로직
    private fun deleteSelectedItems() {
        val selectedItems = clothingList.filter { it.isSelected }
        if (selectedItems.isEmpty()) {
            Toast.makeText(requireContext(), "삭제할 항목을 선택해주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        val db = FirebaseFirestore.getInstance()
        val batch = db.batch()

        for (item in selectedItems) {
            val docRef = db.collection("clothes").document(item.id)
            batch.delete(docRef)
        }

        batch.commit().addOnSuccessListener {
            Toast.makeText(requireContext(), "${selectedItems.size}개 삭제 완료", Toast.LENGTH_SHORT).show()
            adapter.exitSelectionMode()
            updateClothesList()
        }.addOnFailureListener {
            Toast.makeText(requireContext(), "삭제 실패", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateClothesList() {
        val db = FirebaseFirestore.getInstance()
        firestoreListener?.remove()

        firestoreListener = db.collection("clothes").addSnapshotListener { snapshots, e ->
            if (e != null) return@addSnapshotListener
            clothingList.clear()

            snapshots?.let {
                for (doc in it.documents) {
                    val season = doc.getString("season") ?: "여름"
                    val mainCat = doc.getString("mainCategory") ?: "상의"
                    val subCat = doc.getString("subCategory") ?: "반팔"

                    val isMatch = if (currentTabCategory == "전체") {
                        true
                    } else {
                        mainCat == currentTabCategory && subCat == currentSubCategory
                    }

                    if (isMatch) {
                        val warnings = doc.getString("warnings") ?: ""
                        clothingList.add(ClothingItem(
                            doc.id,
                            doc.getString("imageUrl") ?: "",
                            season,
                            mainCat,
                            subCat,
                            doc.getString("material") ?: "",
                            doc.getString("laundryTip") ?: "",
                            warnings
                        ))
                    }
                }
            }
            // 🌟 데이터 새로고침 시 다중 선택 모드도 안전하게 초기화
            adapter.exitSelectionMode()
            adapter.notifyDataSetChanged()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        firestoreListener?.remove()
    }
}