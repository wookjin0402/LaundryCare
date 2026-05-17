package com.example.laundrycare_android

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment

class CareFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_care, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnSeasonalStorage = view.findViewById<Button>(R.id.btnSeasonalStorage)
        val btnHomeCare = view.findViewById<Button>(R.id.btnHomeCare)
        val btnWardrobeDiet = view.findViewById<Button>(R.id.btnWardrobeDiet)

        // 1. 계절별 보관 관리 버튼 클릭 시
        btnSeasonalStorage.setOnClickListener {
            val intent = Intent(requireContext(), SeasonalStorageActivity::class.java)
            startActivity(intent)
        }

        // 2. 특수 소재 홈케어 버튼 클릭 시
        btnHomeCare.setOnClickListener {
            val intent = Intent(requireContext(), HomeCareActivity::class.java)
            startActivity(intent)
        }

        // 3. 옷장 다이어트 버튼 클릭 시
        btnWardrobeDiet.setOnClickListener {
            val intent = Intent(requireContext(), WardrobeDietActivity::class.java)
            startActivity(intent)
        }
    }
}