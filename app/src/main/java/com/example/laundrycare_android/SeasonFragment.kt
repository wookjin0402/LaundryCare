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
    private var currentSeason: String? = null
    private lateinit var rvSeasonClothing: RecyclerView
    private lateinit var adapter: ClothingAdapter
    private val clothingList = mutableListOf<ClothingItem>()

    private var currentMainCategory: String = "상의"
    private var currentSubCategory: String = "반팔"
    private var firestoreListener: ListenerRegistration? = null

    companion object {
        fun newInstance(season: String): SeasonFragment {
            val fragment = SeasonFragment()
            val args = Bundle()
            args.putString("SEASON", season)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
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

        // 소분류 칩들
        val subChips = mapOf(
            "상의" to listOf(view.findViewById<Chip>(R.id.chipTopShort), view.findViewById<Chip>(R.id.chipTopLong), view.findViewById<Chip>(R.id.chipOuter)),
            "하의" to listOf(view.findViewById<Chip>(R.id.chipBottomShort), view.findViewById<Chip>(R.id.chipBottomLong), view.findViewById<Chip>(R.id.chipSkirt)),
            "고급" to listOf(view.findViewById<Chip>(R.id.chipPremiumLuxury), view.findViewById<Chip>(R.id.chipPremiumFunc)),
            "기타" to listOf(view.findViewById<Chip>(R.id.chipEtcSocks), view.findViewById<Chip>(R.id.chipEtcUnderwear))
        )

        // 대분류 선택 시 로직
        chipGroupCategory.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                // 모든 소분류 칩 숨기기
                subChips.values.flatten().forEach { it.visibility = View.GONE }

                currentMainCategory = when (checkedIds[0]) {
                    R.id.chipTop -> "상의"
                    R.id.chipBottom -> "하의"
                    R.id.chipPremium -> "고급"
                    R.id.chipEtc -> "기타"
                    else -> "상의"
                }

                // 해당되는 소분류 칩만 보여주기
                subChips[currentMainCategory]?.forEach { it.visibility = View.VISIBLE }

                // 대분류 바뀔 때 첫 번째 소분류 자동 선택
                val firstSubChipId = subChips[currentMainCategory]?.firstOrNull()?.id
                if (firstSubChipId != null) {
                    chipGroupSubCategory.check(firstSubChipId)
                }
            }
        }

        // 소분류 선택 시 로직
        chipGroupSubCategory.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                currentSubCategory = when (checkedIds[0]) {
                    R.id.chipTopShort -> "반팔"; R.id.chipTopLong -> "긴팔"; R.id.chipOuter -> "아우터"
                    R.id.chipBottomShort -> "반바지"; R.id.chipBottomLong -> "긴바지"; R.id.chipSkirt -> "치마"
                    R.id.chipPremiumLuxury -> "명품"; R.id.chipPremiumFunc -> "기능성"
                    R.id.chipEtcSocks -> "양말"; R.id.chipEtcUnderwear -> "속옷"
                    else -> "반팔"
                }
                updateClothesList()
            }
        }
        chipGroupCategory.check(R.id.chipTop)
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

                    // 🌟 계절, 대분류, 소분류가 모두 일치할 때만 리스트업!
                    if (season == currentSeason && mainCat == currentMainCategory && subCat == currentSubCategory) {
                        clothingList.add(ClothingItem(
                            doc.id, doc.getString("imageUrl") ?: "", season, mainCat, subCat,
                            doc.getString("material") ?: "", doc.getString("laundryTip") ?: ""
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