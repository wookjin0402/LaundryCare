package com.example.laundrycare_android

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
        return inflater.inflate(R.layout.fragment_season, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        rvSeasonClothing = view.findViewById(R.id.rvSeasonClothing)
        rvSeasonClothing.layoutManager = LinearLayoutManager(requireContext())
        adapter = ClothingAdapter(clothingList)
        rvSeasonClothing.adapter = adapter

        val chipGroupCategory = view.findViewById<ChipGroup>(R.id.chipGroupCategory)
        val chipGroupSubCategory = view.findViewById<ChipGroup>(R.id.chipGroupSubCategory)

        // 화면 안의 불필요한 대분류 칩 숨김 처리
        chipGroupCategory.visibility = View.GONE

        // 소분류 칩들 연결
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
                        // 🌟 주의사항(warnings) 데이터를 Firestore에서 읽어옵니다.
                        val warnings = doc.getString("warnings") ?: ""

                        // 🌟 ClothingItem 생성자에 warnings를 추가로 넘겨줍니다.
                        // (경고: ClothingItem 데이터 클래스와 어댑터 파일도 이에 맞게 수정되어 있어야 화면에 뜹니다!)
                        clothingList.add(ClothingItem(
                            doc.id,
                            doc.getString("imageUrl") ?: "",
                            season,
                            mainCat,
                            subCat,
                            doc.getString("material") ?: "",
                            doc.getString("laundryTip") ?: "",
                            warnings // 🌟 여기에 추가되었습니다.
                        ))
                    }
                }
            }
            adapter.notifyDataSetChanged()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        firestoreListener?.remove()
    }
}