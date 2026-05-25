package com.example.laundrycare_android

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore

// 1. 옷 데이터를 담을 그릇 (선택 여부 Boolean 추가)
data class SelectableCloth(
    val id: String,
    val imageUrl: String,
    val mainCategory: String,
    val subCategory: String,
    val color: String,
    var isSelected: Boolean = false
)

class ClothMultiSelectActivity : AppCompatActivity() {

    private lateinit var rvClothes: RecyclerView
    private lateinit var pbLoading: ProgressBar
    private lateinit var btnNextStep: Button

    private val clothList = mutableListOf<SelectableCloth>()
    private lateinit var adapter: ClothSelectAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cloth_multi_select)

        rvClothes = findViewById(R.id.rvClothes)
        pbLoading = findViewById(R.id.pbLoading)
        btnNextStep = findViewById(R.id.btnNextStep)

        rvClothes.layoutManager = LinearLayoutManager(this)
        adapter = ClothSelectAdapter(clothList) { updateButtonState() }
        rvClothes.adapter = adapter

        fetchClothesFromFirebase()

        // 🌟 다음 단계 버튼 로직: 카메라로 직행하지 않고 '등록된 세탁기 목록'을 먼저 띄웁니다!
        btnNextStep.setOnClickListener {
            val selectedClothes = clothList.filter { it.isSelected }
            val selectedImages = ArrayList(selectedClothes.map { it.imageUrl })

            Toast.makeText(this, "내 세탁기 목록을 불러오는 중...", Toast.LENGTH_SHORT).show()
            val db = FirebaseFirestore.getInstance()

            // 파이어베이스에서 내 세탁기 목록 가져오기
            db.collection("washers").get().addOnSuccessListener { snapshot ->
                val machineNames = mutableListOf<String>()
                val machineDocs = mutableListOf<Map<String, String>>()

                for (doc in snapshot.documents) {
                    val name = doc.getString("name") ?: "내 세탁기"
                    val brand = doc.getString("brand") ?: "LG"
                    val model = doc.getString("model") ?: "기본모델"
                    val type = doc.getString("type") ?: "드럼 세탁기"

                    machineNames.add("✅ $name ($brand $model)")
                    machineDocs.add(mapOf("brand" to brand, "model" to model, "type" to type))
                }

                // 리스트 맨 마지막에 '새로 등록하기' 버튼 추가
                machineNames.add("➕ 새 세탁기 카메라로 등록하기")
                val machineArray = machineNames.toTypedArray()

                androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("어떤 세탁기를 사용하실 건가요?")
                    .setItems(machineArray) { _, which ->
                        if (which == machineArray.size - 1) {
                            // 📷 새 세탁기 등록 -> 카메라 화면으로 이동
                            val intent = Intent(this, WasherCameraActivity::class.java)
                            intent.putStringArrayListExtra("selected_cloth_images", selectedImages)
                            startActivity(intent)
                        } else {
                            // 🚀 기존 세탁기 선택 -> 카메라 건너뛰고 결과 화면으로 직행!
                            val selectedDoc = machineDocs[which]
                            val intent = Intent(this, WasherResultActivity::class.java)
                            intent.putExtra("washer_type", selectedDoc["type"])
                            intent.putExtra("brand", selectedDoc["brand"])
                            intent.putExtra("model", selectedDoc["model"])
                            intent.putStringArrayListExtra("selected_cloth_images", selectedImages)
                            startActivity(intent)
                        }
                    }
                    .show()
            }.addOnFailureListener {
                Toast.makeText(this, "세탁기 목록을 불러오지 못했습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun fetchClothesFromFirebase() {
        pbLoading.visibility = View.VISIBLE
        val db = FirebaseFirestore.getInstance()

        db.collection("clothes").get()
            .addOnSuccessListener { documents ->
                pbLoading.visibility = View.GONE
                clothList.clear()

                for (doc in documents) {
                    val cloth = SelectableCloth(
                        id = doc.id,
                        imageUrl = doc.getString("imageUrl") ?: "",
                        mainCategory = doc.getString("mainCategory") ?: "알 수 없음",
                        subCategory = doc.getString("subCategory") ?: "알 수 없음",
                        color = doc.getString("color") ?: "미상"
                    )
                    clothList.add(cloth)
                }
                adapter.notifyDataSetChanged()

                if (clothList.isEmpty()) {
                    Toast.makeText(this, "옷장에 등록된 옷이 없습니다.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                pbLoading.visibility = View.GONE
                Toast.makeText(this, "데이터를 불러오는데 실패했습니다.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateButtonState() {
        val selectedCount = clothList.count { it.isSelected }
        if (selectedCount > 0) {
            btnNextStep.isEnabled = true
            btnNextStep.text = "세탁기 매칭하기 (${selectedCount}벌 선택됨)"
        } else {
            btnNextStep.isEnabled = false
            btnNextStep.text = "세탁기 매칭하기 (0벌 선택됨)"
        }
    }

    // 2. 리스트를 그려주는 내부 어댑터 클래스
    inner class ClothSelectAdapter(
        private val items: List<SelectableCloth>,
        private val onItemSelectionChanged: () -> Unit
    ) : RecyclerView.Adapter<ClothSelectAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val ivThumb: ImageView = view.findViewById(R.id.ivClothThumb)
            val tvCategory: TextView = view.findViewById(R.id.tvCategory)
            val tvColor: TextView = view.findViewById(R.id.tvColor)
            val cbSelect: CheckBox = view.findViewById(R.id.cbSelect)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_cloth_selectable, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.tvCategory.text = "${item.mainCategory} - ${item.subCategory}"
            holder.tvColor.text = "색상: ${item.color}"

            if (item.imageUrl.isNotEmpty()) {
                Glide.with(holder.itemView.context).load(item.imageUrl).into(holder.ivThumb)
            }

            // 체크박스 꼬임 방지 처리
            holder.cbSelect.setOnCheckedChangeListener(null)
            holder.cbSelect.isChecked = item.isSelected

            holder.cbSelect.setOnCheckedChangeListener { _, isChecked ->
                item.isSelected = isChecked
                onItemSelectionChanged()
            }

            // 리스트 전체 영역 클릭 시에도 체크박스 토글되게 편의성 추가
            holder.itemView.setOnClickListener {
                holder.cbSelect.isChecked = !holder.cbSelect.isChecked
            }
        }

        override fun getItemCount(): Int = items.size
    }
}