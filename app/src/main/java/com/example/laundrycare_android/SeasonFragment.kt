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

class SeasonFragment : Fragment() {

    private var currentSeason: String? = null
    private lateinit var rvSeasonClothing: RecyclerView
    private lateinit var adapter: ClothingAdapter
    private val clothingList = mutableListOf<ClothingItem>()

    // 현재 보고 있는 카테고리 기억 (기본값: 반팔)
    private var currentFilter: String = "반팔"

    companion object {
        fun newInstance(season: String): SeasonFragment {
            val fragment = SeasonFragment()
            val args = Bundle()
            args.putString("SEASON", season)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        currentSeason = arguments?.getString("SEASON")
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

        val chipTopShort = view.findViewById<Chip>(R.id.chipTopShort)
        val chipTopLong = view.findViewById<Chip>(R.id.chipTopLong)
        val chipOuter = view.findViewById<Chip>(R.id.chipOuter)
        val chipBottomShort = view.findViewById<Chip>(R.id.chipBottomShort)
        val chipBottomLong = view.findViewById<Chip>(R.id.chipBottomLong)
        val chipSkirt = view.findViewById<Chip>(R.id.chipSkirt)
        val chipPremiumLuxury = view.findViewById<Chip>(R.id.chipPremiumLuxury)
        val chipPremiumFunc = view.findViewById<Chip>(R.id.chipPremiumFunc)
        val chipEtcSocks = view.findViewById<Chip>(R.id.chipEtcSocks)
        val chipEtcUnderwear = view.findViewById<Chip>(R.id.chipEtcUnderwear)

        val allSubChips = listOf(
            chipTopShort, chipTopLong, chipOuter, chipBottomShort, chipBottomLong,
            chipSkirt, chipPremiumLuxury, chipPremiumFunc, chipEtcSocks, chipEtcUnderwear
        )

        chipGroupCategory.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                allSubChips.forEach { it.visibility = View.GONE }
                when (checkedIds[0]) {
                    R.id.chipTop -> {
                        chipTopShort.visibility = View.VISIBLE
                        chipTopLong.visibility = View.VISIBLE
                        chipOuter.visibility = View.VISIBLE
                        chipGroupSubCategory.check(R.id.chipTopShort)
                    }
                    R.id.chipBottom -> {
                        chipBottomShort.visibility = View.VISIBLE
                        chipBottomLong.visibility = View.VISIBLE
                        chipSkirt.visibility = View.VISIBLE
                        chipGroupSubCategory.check(R.id.chipBottomShort)
                    }
                    R.id.chipPremium -> {
                        chipPremiumLuxury.visibility = View.VISIBLE
                        chipPremiumFunc.visibility = View.VISIBLE
                        chipGroupSubCategory.check(R.id.chipPremiumLuxury)
                    }
                    R.id.chipEtc -> {
                        chipEtcSocks.visibility = View.VISIBLE
                        chipEtcUnderwear.visibility = View.VISIBLE
                        chipGroupSubCategory.check(R.id.chipEtcSocks)
                    }
                }
            }
        }

        chipGroupSubCategory.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                currentFilter = when (checkedIds[0]) {
                    R.id.chipTopShort -> "반팔"
                    R.id.chipTopLong -> "긴팔"
                    R.id.chipOuter -> "아우터"
                    R.id.chipBottomShort -> "반바지"
                    R.id.chipBottomLong -> "긴바지"
                    R.id.chipSkirt -> "치마"
                    R.id.chipPremiumLuxury -> "명품"
                    R.id.chipPremiumFunc -> "기능성"
                    R.id.chipEtcSocks -> "양말"
                    R.id.chipEtcUnderwear -> "속옷"
                    else -> "반팔"
                }
                // 🌟 카테고리를 누를 때마다 리스트 새로고침!
                updateClothesList(currentFilter)
            }
        }

        // 초기 시작 상태 (상의 -> 반팔)
        chipGroupCategory.check(R.id.chipTop)
    }

    // 🌟 화면이 다시 뜰 때(스캔 끝내고 돌아왔을 때) 최신 데이터로 새로고침
    override fun onResume() {
        super.onResume()
        updateClothesList(currentFilter)
    }

    // 🌟 임시 창고에서 현재 카테고리랑 이름이 똑같은 옷만 가져와서 띄우는 함수
    private fun updateClothesList(filter: String) {
        clothingList.clear()
        // 창고(TempWardrobeDB)에 있는 옷 중 category가 현재 필터(ex: "반팔")와 같은 것만 뽑음
        val filtered = TempWardrobeDB.myClothes.filter { it.category == filter }
        clothingList.addAll(filtered)
        adapter.notifyDataSetChanged()
    }
}