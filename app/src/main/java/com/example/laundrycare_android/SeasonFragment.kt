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
import com.google.firebase.firestore.Query

class SeasonFragment : Fragment() {

    private var currentTabCategory: String = "전체"
    private lateinit var rvSeasonClothing: RecyclerView
    private lateinit var adapter: ClothingAdapter
    private val clothingList = mutableListOf<ClothingItem>()

    // 🌟 기본값을 "전체"로 설정
    private var currentSubCategory: String = "전체"
    private var firestoreListener: ListenerRegistration? = null

    private lateinit var layoutSelectionMode: LinearLayout
    private lateinit var btnSelectAll: Button
    private lateinit var btnDeleteSelected: Button
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

        val wrapperLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            setBackgroundColor(android.graphics.Color.parseColor("#F5F5F5"))
        }

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

        wrapperLayout.addView(layoutSelectionMode)
        wrapperLayout.addView(originalRoot, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            0,
            1f
        ))

        return wrapperLayout
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        backPressedCallback = object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() {
                adapter.exitSelectionMode()
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, backPressedCallback)

        rvSeasonClothing = view.findViewById(R.id.rvSeasonClothing)
        rvSeasonClothing.layoutManager = LinearLayoutManager(requireContext())

        adapter = ClothingAdapter(clothingList) { isSelectionMode ->
            layoutSelectionMode.visibility = if (isSelectionMode) View.VISIBLE else View.GONE
            backPressedCallback.isEnabled = isSelectionMode
        }
        rvSeasonClothing.adapter = adapter

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

            // 🌟 결정적 원인 제거: 강제로 첫 번째 소분류(반팔/반바지)를 선택하던 코드를 없애고 '전체'로 둡니다!
            chipGroupSubCategory.clearCheck()
            currentSubCategory = "전체"
        }

        chipGroupSubCategory.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                currentSubCategory = getSubCategoryName(checkedIds[0])
            } else {
                // 🌟 사용자가 칩 선택을 해제하면, 다시 해당 탭의 '전체 옷'을 보여줍니다.
                currentSubCategory = "전체"
            }
            updateClothesList()
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

        firestoreListener = db.collection("clothes")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null) return@addSnapshotListener
                clothingList.clear()

                snapshots?.let {
                    for (doc in it.documents) {
                        val season = doc.getString("season") ?: "여름"
                        val dbMain = doc.getString("mainCategory") ?: doc.getString("category") ?: "상의"
                        val dbSub = doc.getString("subCategory") ?: doc.getString("category") ?: "반팔"

                        // 옷의 대분류와 소분류를 합친 정보
                        val fullCategoryText = "$dbMain $dbSub"

                        // 🌟 개선된 필터링 로직!
                        val isMatch = if (currentTabCategory == "전체") {
                            true // '전체' 탭이면 무조건 통과
                        } else {
                            if (currentSubCategory == "전체") {
                                // 소분류 칩을 아무것도 안 눌렀을 때: 대분류(예: 상의) 글자만 들어가면 모두 통과!
                                fullCategoryText.contains(currentTabCategory)
                            } else {
                                // 소분류 칩(예: 반팔)을 눌렀을 때: 둘 다 포함되어야 통과!
                                fullCategoryText.contains(currentTabCategory) && fullCategoryText.contains(currentSubCategory)
                            }
                        }

                        if (isMatch) {
                            val warnings = doc.getString("warnings") ?: ""
                            clothingList.add(ClothingItem(
                                doc.id,
                                doc.getString("imageUrl") ?: "",
                                season,
                                dbMain,
                                dbSub,
                                doc.getString("material") ?: "",
                                doc.getString("laundryTip") ?: "",
                                warnings
                            ))
                        }
                    }
                }
                adapter.exitSelectionMode()
                adapter.notifyDataSetChanged()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        firestoreListener?.remove()
    }
}