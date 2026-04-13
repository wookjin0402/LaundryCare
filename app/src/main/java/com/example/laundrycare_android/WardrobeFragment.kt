package com.example.laundrycare_android

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
<<<<<<< HEAD

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [WardrobeFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class WardrobeFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

=======
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class WardrobeFragment : Fragment() {

    // 1. 화면(도화지)을 깔아주는 기본 함수
>>>>>>> 03eac8c735e46d8330f1d2193191c985dbc56276
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
<<<<<<< HEAD
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_wardrobe, container, false)
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment WardrobeFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            WardrobeFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
=======
        return inflater.inflate(R.layout.fragment_wardrobe, container, false)
    }

    // 🌟 2. [우리가 추가한 핵심 코드!] 도화지가 깔린 직후에 진열대를 세팅하는 함수 🌟
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 화면(XML)에서 진열대(RecyclerView) 찾아오기
        val recyclerView = view.findViewById<RecyclerView>(R.id.rvWardrobe)

        // 리스트를 위아래(세로)로 스크롤되게 방향 설정
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // 아까 고용한 직원(Adapter) 부르기! (창고 데이터도 같이 넘겨줌)
        val adapter = ClothingAdapter(ClothingRepository.itemList)
        recyclerView.adapter = adapter

        // 옷장 탭을 누르고 들어올 때마다 최신 상태로 싹 새로고침
        adapter.notifyDataSetChanged()
>>>>>>> 03eac8c735e46d8330f1d2193191c985dbc56276
    }
}