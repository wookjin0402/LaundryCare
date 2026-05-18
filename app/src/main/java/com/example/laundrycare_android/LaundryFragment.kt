package com.example.laundrycare_android

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment

class LaundryFragment : Fragment(R.layout.fragment_laundry) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 뒤로가기는 원래 버튼이 맞으니 그대로 둡니다.
        val btnBack = view.findViewById<Button>(R.id.btnBack)
        btnBack?.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        // 🌟 에러의 원인 해결! Button -> LinearLayout으로 정확하게 바꿔서 연결했습니다.
        val btnLaundryBatch = view.findViewById<LinearLayout>(R.id.btnLaundryBatch)
        val btnTimeRecommend = view.findViewById<LinearLayout>(R.id.btnTimeRecommend)
        val btnCourseGuide = view.findViewById<LinearLayout>(R.id.btnCourseGuide)

        // 1. 세탁 묶음 자동 분류 (진짜 파이어베이스 데이터 연동!)
        btnLaundryBatch.setOnClickListener {
            Toast.makeText(requireContext(), "옷장 데이터를 불러오는 중...", Toast.LENGTH_SHORT).show()

            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()

            // 파이어베이스에서 진짜 옷 데이터 싹 다 가져오기
            db.collection("clothes").get().addOnSuccessListener { snapshot ->
                if (snapshot.isEmpty) {
                    Toast.makeText(requireContext(), "옷장에 등록된 옷이 없습니다.", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                // 진짜 옷 리스트와 화면에 띄울 이름 리스트 만들기
                val realClothesList = mutableListOf<Map<String, String>>()
                val displayNames = mutableListOf<String>()

                for (doc in snapshot.documents) {
                    // DB에 저장된 색상, 소재, 종류 가져오기 (없으면 '미상' 처리)
                    val color = doc.getString("color") ?: "색상미상"
                    val material = doc.getString("material") ?: "소재미상"
                    val subCategory = doc.getString("subCategory") ?: "옷"

                    val displayName = "$color $material $subCategory" // 예: "검은색 면 반팔"
                    displayNames.add(displayName)

                    realClothesList.add(
                        mapOf(
                            "name" to displayName,
                            "color" to color,
                            "material" to material
                        )
                    )
                }

                // 팝업창에 띄울 체크박스 배열 세팅
                val clothesArray = displayNames.toTypedArray()
                val checkedItems = BooleanArray(clothesArray.size) { false }

                AlertDialog.Builder(requireContext())
                    .setTitle("오늘 세탁할 옷을 선택해주세요")
                    .setMultiChoiceItems(clothesArray, checkedItems) { _, which, isChecked ->
                        checkedItems[which] = isChecked
                    }
                    .setPositiveButton("분류 시작") { _, _ ->
                        // 사용자가 체크한 옷들만 골라내기
                        val selectedClothes =
                            realClothesList.filterIndexed { index, _ -> checkedItems[index] }

                        if (selectedClothes.isEmpty()) {
                            Toast.makeText(requireContext(), "선택된 옷이 없습니다.", Toast.LENGTH_SHORT)
                                .show()
                            return@setPositiveButton
                        }

                        // 🌟 마법의 groupBy 함수로 진짜 끼리끼리 묶어버리기!
                        val groupedByColor = selectedClothes.groupBy { it["color"] }
                        val groupedByMaterial = selectedClothes.groupBy { it["material"] }

                        // 묶인 결과를 예쁜 텍스트로 만들기
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

                        // 최종 결과 팝업 띄우기
                        AlertDialog.Builder(requireContext())
                            .setTitle("✨ AI 스마트 분류 결과")
                            .setMessage(resultMessage)
                            .setPositiveButton("확인", null)
                            .show()
                    }
                    .setNegativeButton("취소", null)
                    .show()

            }.addOnFailureListener {
                Toast.makeText(requireContext(), "데이터를 불러오는데 실패했습니다.", Toast.LENGTH_SHORT).show()
            }
        }

        // 2. 세탁 및 건조 시점 추천
        btnTimeRecommend.setOnClickListener {
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

                    AlertDialog.Builder(requireContext())
                        .setTitle("AI 맞춤 추천")
                        .setMessage(resultMessage)
                        .setPositiveButton("확인", null)
                        .show()
                }
                .setNegativeButton("취소", null)
                .show()
        }

        // 3. 세탁 코스 가이드 (진짜 파이어베이스 연동 + 2대 등록 제한 로직 포함)
        btnCourseGuide.setOnClickListener {
            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()

            // 1. 파이어베이스에서 저장된 세탁기 목록 가져오기
            db.collection("washers").get().addOnSuccessListener { snapshot ->
                val machineNames = mutableListOf<String>()

                for (doc in snapshot.documents) {
                    val name = doc.getString("name") ?: "알 수 없는 기기"
                    machineNames.add("등록된 기기: $name")
                }

                // 2. 새 세탁기 등록하기 버튼을 리스트 맨 마지막에 추가
                machineNames.add("➕ 새 세탁기 등록하기")

                val machineArray = machineNames.toTypedArray()

                AlertDialog.Builder(requireContext())
                    .setTitle("사용할 세탁기를 선택해주세요")
                    .setItems(machineArray) { _, which ->
                        if (which == machineArray.size - 1) {
                            // 새 세탁기 등록하기를 눌렀을 때: 2대 제한 체크 로직!
                            db.collection("washers").count()
                                .get(com.google.firebase.firestore.AggregateSource.SERVER)
                                .addOnSuccessListener { countSnapshot ->
                                    if (countSnapshot.count >= 2) {
                                        Toast.makeText(
                                            requireContext(),
                                            "세탁기는 최대 2대까지만 등록할 수 있습니다.",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    } else {
                                        // 2대 미만이면 임시로 세탁기 하나를 DB에 추가해 둠 (승엽이가 나중에 카메라 스캔으로 바꿀 부분)
                                        val dummyWasher = hashMapOf(
                                            "name" to "임시 등록 세탁기",
                                            "timestamp" to System.currentTimeMillis()
                                        )
                                        db.collection("washers").add(dummyWasher)
                                            .addOnSuccessListener {
                                                Toast.makeText(
                                                    requireContext(),
                                                    "새 세탁기가 등록되었습니다! (테스트용)",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                    }
                                }
                        } else {
                            // 기존 세탁기를 선택했을 때 코스 안내 (기존 로직 유지)
                            val selectedMachine = machineNames[which].replace("등록된 기기: ", "")
                            AlertDialog.Builder(requireContext())
                                .setTitle("최적 코스 안내")
                                .setMessage(
                                    "기기: $selectedMachine\n\n" +
                                            "옷감 손상을 막기 위해 해당 기기의\n" +
                                            "👉 울/섬세 코스 + 냉수(20도) + 약한 탈수\n" +
                                            "설정을 권장합니다."
                                )
                                .setPositiveButton("세탁기로 전송") { _, _ ->
                                    Toast.makeText(
                                        requireContext(),
                                        "세탁기에 코스를 전송했습니다 (시연용)",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                .show()
                        }
                    }
                    .show()
            }.addOnFailureListener {
                Toast.makeText(requireContext(), "세탁기 정보를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}