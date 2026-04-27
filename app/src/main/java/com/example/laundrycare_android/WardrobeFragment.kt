package com.example.laundrycare_android

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment

class WardrobeFragment : Fragment() {

    // 1. 화면(도화지)을 깔아주는 기본 함수
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_wardrobe, container, false)
    }

    // 2. 화면이 다 그려진 후 로직 처리
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // XML에서 만든 이름표로 레이아웃 찾기
        val layoutMockItem = view.findViewById<View>(R.id.layoutMockItem)

        // 쪽지(Intent)를 확인해서 "IS_SAVED"가 true인지 체크
        val isSaved = activity?.intent?.getBooleanExtra("IS_SAVED", false) ?: false

        if (isSaved) {
            // 쪽지가 왔다면 바지를 보여줍니다!
            layoutMockItem?.visibility = View.VISIBLE
        } else {
            // 평소엔 안 보입니다.
            layoutMockItem?.visibility = View.GONE
        }
    }
}