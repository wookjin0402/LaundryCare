package com.example.laundrycare_android

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment

class LaundryFragment : Fragment(R.layout.fragment_laundry) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 🌟 [추가] 뒤로가기 버튼 로직
        val btnBack = view.findViewById<Button>(R.id.btnBack)
        btnBack?.setOnClickListener {
            requireActivity().finish()
        }

        // 🌟 [추가] 내 세탁기 관리 화면으로 이동!
        val btnManageWasher = view.findViewById<LinearLayout>(R.id.btnManageWasher)
        btnManageWasher?.setOnClickListener {
            val intent = Intent(requireContext(), WasherListActivity::class.java)
            startActivity(intent)
        }

        // 1. 옷 바구니 열기 버튼
        val btnOpenMultiSelect = view.findViewById<Button>(R.id.btnOpenMultiSelect)
        btnOpenMultiSelect?.setOnClickListener {
            val intent = Intent(requireContext(), ClothMultiSelectActivity::class.java)
            startActivity(intent)
        }

        // 2. 나의 세탁 기록 히스토리 버튼
        val btnLaundryHistory = view.findViewById<LinearLayout>(R.id.btnLaundryHistory)
        btnLaundryHistory?.setOnClickListener {
            val intent = Intent(requireContext(), LaundryHistoryActivity::class.java)
            startActivity(intent)
        }

        val btnLaundryBatch = view.findViewById<LinearLayout>(R.id.btnLaundryBatch)
        val btnTimeRecommend = view.findViewById<LinearLayout>(R.id.btnTimeRecommend)
        val btnCourseGuide = view.findViewById<LinearLayout>(R.id.btnCourseGuide)

        // 3. 세탁 묶음 자동 분류 (조원분의 파이어베이스 코드 유지)
        btnLaundryBatch?.setOnClickListener {
            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()

            db.collection("clothes").get().addOnSuccessListener { snapshot ->
                if (snapshot.isEmpty) {
                    Toast.makeText(requireContext(), "옷장에 등록된 옷이 없습니다.", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val realClothesList = mutableListOf<Map<String, String>>()
                val displayNames = mutableListOf<String>()

                for (doc in snapshot.documents) {
                    val color = doc.getString("color") ?: "색상미상"
                    val material = doc.getString("material") ?: "소재미상"
                    val subCategory = doc.getString("subCategory") ?: "옷"
                    val displayName = "$color $material $subCategory"
                    displayNames.add(displayName)
                    realClothesList.add(mapOf("name" to displayName, "color" to color, "material" to material))
                }

                val clothesArray = displayNames.toTypedArray()
                val checkedItems = BooleanArray(clothesArray.size) { false }

                AlertDialog.Builder(requireContext())
                    .setTitle("오늘 세탁할 옷을 선택해주세요")
                    .setMultiChoiceItems(clothesArray, checkedItems) { _, which, isChecked ->
                        checkedItems[which] = isChecked
                    }
                    .setPositiveButton("분류 시작") { _, _ ->
                        val selectedClothes = realClothesList.filterIndexed { index, _ -> checkedItems[index] }
                        if (selectedClothes.isEmpty()) {
                            Toast.makeText(requireContext(), "선택된 옷이 없습니다.", Toast.LENGTH_SHORT).show()
                            return@setPositiveButton
                        }
                        val groupedByColor = selectedClothes.groupBy { it["color"] }
                        val groupedByMaterial = selectedClothes.groupBy { it["material"] }
                        var resultMessage = "🎨 [색상별 추천 묶음]\n"
                        groupedByColor.forEach { (color, list) ->
                            val names = list.joinToString(", ") { it["name"] ?: "" }
                            resultMessage += "✔️ $color: $names\n"
                        }
                        resultMessage += "\n🧵 [소재별 주의 묶음]\n"
                        groupedByMaterial.forEach { (material, list) ->
                            val names = list.joinToString(", ") { it["name"] ?: "" }
                            resultMessage += "✔️ $material: $names\n"
                        }
                        AlertDialog.Builder(requireContext()).setTitle("✨ AI 스마트 분류 결과").setMessage(resultMessage).setPositiveButton("확인", null).show()
                    }
                    .setNegativeButton("취소", null).show()
            }
        }

        // 4. 세탁 및 건조 시점 추천
        btnTimeRecommend?.setOnClickListener {
            val conditionList = arrayOf("땀을 많이 흘렸어요", "커피/음식물 얼룩이 묻었어요", "잠깐 입어서 깨끗해요")
            AlertDialog.Builder(requireContext())
                .setTitle("현재 빨랫감의 상태는 어떤가요?")
                .setSingleChoiceItems(conditionList, -1) { dialog, which ->
                    val resultMessage = when (which) {
                        0 -> "🌤 현재 맑고 건조함\n➡️ 당장 '전체 세탁' 후 '자연 건조'를 추천합니다! (건조기 절감액: 약 350원)"
                        1 -> "🚨 오염 감지\n➡️ 얼룩이 굳기 전에 '부분 세척' 후 전체 세탁하세요."
                        2 -> "💨 가벼운 착용\n➡️ 세탁기 대신 '스타일러(환기) 후 보관'을 추천합니다."
                        else -> ""
                    }
                    dialog.dismiss()
                    AlertDialog.Builder(requireContext()).setTitle("AI 맞춤 추천").setMessage(resultMessage).setPositiveButton("확인", null).show()
                }
                .setNegativeButton("취소", null).show()
        }

        // 5. 세탁 코스 가이드
        btnCourseGuide?.setOnClickListener {
            Toast.makeText(requireContext(), "세탁기 스캔은 '바구니 담기' 완료 후 진행됩니다.", Toast.LENGTH_LONG).show()
        }
    }
}