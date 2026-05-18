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

    // 🌟 기존 currentSeason 대신, 상단 탭에서 넘어온 '대분류 카테고리(전체, 상의 등)'를 저장합니다.
    private var currentTabCategory: String = "전체"
    private lateinit var rvSeasonClothing: RecyclerView
    private lateinit var adapter: ClothingAdapter
    private val clothingList = mutableListOf<ClothingItem>()

    private var currentSubCategory: String = "전체"
    private var firestoreListener: ListenerRegistration? = null

    private var hasShownSeasonGuide = false

    companion object {
        fun newInstance(categoryName: String): SeasonFragment {
            val fragment = SeasonFragment()
            val args = Bundle()
            // WardrobePagerAdapter와 안전하게 데이터를 주고받기 위해 키 값은 "SEASON"을 그대로 사용합니다.
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

        // 🌟 핵심: 상단 탭이 이미 '대분류' 역할을 하므로, 화면 안의 불필요한 대분류 칩은 숨김 처리합니다.
        chipGroupCategory.visibility = View.GONE

        // 소분류 칩들 연결
        val subChips = mapOf(
            "상의" to listOf(view.findViewById<Chip>(R.id.chipTopShort), view.findViewById<Chip>(R.id.chipTopLong), view.findViewById<Chip>(R.id.chipOuter)),
            "하의" to listOf(view.findViewById<Chip>(R.id.chipBottomShort), view.findViewById<Chip>(R.id.chipBottomLong), view.findViewById<Chip>(R.id.chipSkirt)),
            "고급" to listOf(view.findViewById<Chip>(R.id.chipPremiumLuxury), view.findViewById<Chip>(R.id.chipPremiumFunc)),
            "기타" to listOf(view.findViewById<Chip>(R.id.chipEtcSocks), view.findViewById<Chip>(R.id.chipEtcUnderwear))
        )

        // 상단 탭 종류에 따른 화면 설정
        if (currentTabCategory == "전체") {
            // '전체' 탭일 때는 소분류 칩도 숨깁니다.
            chipGroupSubCategory.visibility = View.GONE
            currentSubCategory = "전체"
        } else {
            // 다른 탭일 경우 소분류 칩을 보여줍니다.
            chipGroupSubCategory.visibility = View.VISIBLE

            // 모든 소분류 칩을 일단 다 숨김
            subChips.values.flatten().forEach { it.visibility = View.GONE }

            // 현재 탭(예: "상의")에 해당하는 소분류 칩만 띄움
            subChips[currentTabCategory]?.forEach { it.visibility = View.VISIBLE }

            // 화면을 켰을 때 첫 번째 칩(예: "반팔")을 자동으로 선택되게 함
            val firstChipId = subChips[currentTabCategory]?.firstOrNull()?.id
            if (firstChipId != null) {
                chipGroupSubCategory.check(firstChipId)
                currentSubCategory = getSubCategoryName(firstChipId)
            }
        }

        // 소분류 칩 클릭 시 로직
        chipGroupSubCategory.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                currentSubCategory = getSubCategoryName(checkedIds[0])
                updateClothesList() // 리스트 새로고침
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

    private fun checkSeasonalStorage(list: List<ClothingItem>) {
        // 1. 현재 '월' 가져오기
        val currentMonth = java.util.Calendar.getInstance().get(java.util.Calendar.MONTH) + 1

        // 2. 현재 계절 판별
        val currentSeason = when (currentMonth) {
            in 3..5 -> "봄"
            in 6..8 -> "여름"
            in 9..11 -> "가을"
            else -> "겨울"
        }

        // 3. 현재 계절과 안 맞는 옷만 골라내기 (사계절 옷은 제외)
        val outOfSeasonClothes = list.filter { it.season != currentSeason && it.season != "사계절" && it.season.isNotEmpty() }

        // 4. 안 맞는 옷이 있다면 팝업(Dialog) 띄우기!
        if (outOfSeasonClothes.isNotEmpty()) {
            android.app.AlertDialog.Builder(requireContext())
                .setTitle("💡 $currentSeason 맞춤 옷장 정리 팁!")
                .setMessage("지금 옷장에 현재 계절($currentSeason)과 맞지 않는 옷이 ${outOfSeasonClothes.size}벌 있어요!\n\n의류 손상을 막기 위해 제습제와 함께 통풍이 잘 되는 보관함에 따로 정리해 두는 것을 추천합니다.")
                .setPositiveButton("확인", null)
                .show()
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

                    // 🌟 필터링 변경: 계절(season) 검사를 빼고, '전체' 탭이거나 대/소분류가 일치할 때만 통과
                    val isMatch = if (currentTabCategory == "전체") {
                        true
                    } else {
                        mainCat == currentTabCategory && subCat == currentSubCategory
                    }

                    if (isMatch) {
                        clothingList.add(ClothingItem(
                            doc.id,
                            doc.getString("imageUrl") ?: "",
                            season, // 옷장 필터링엔 안 쓰지만 상세 정보 뷰를 위해 데이터는 그대로 넘깁니다.
                            mainCat,
                            subCat,
                            doc.getString("material") ?: "",
                            doc.getString("laundryTip") ?: ""
                        ))
                    }
                }
                // 🌟 2단계 코드 삽입: 전체 탭이고, 옷이 1개라도 있고, 아직 팝업을 안 띄웠을 때만 실행!
                if (currentTabCategory == "전체" && clothingList.isNotEmpty() && !hasShownSeasonGuide) {
                    hasShownSeasonGuide = true
                    checkSeasonalStorage(clothingList)
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