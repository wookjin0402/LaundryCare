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
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore

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

        // 🌟 뒤로가기 버튼 기능 연결
        val btnBackSelect = findViewById<ImageView>(R.id.btnBackSelect)
        btnBackSelect.setOnClickListener {
            finish()
        }

        rvClothes = findViewById(R.id.rvClothes)
        pbLoading = findViewById(R.id.pbLoading)
        btnNextStep = findViewById(R.id.btnNextStep)

        rvClothes.layoutManager = GridLayoutManager(this, 2)

        adapter = ClothSelectAdapter(clothList) { updateButtonState() }
        rvClothes.adapter = adapter

        fetchClothesFromFirebase()

        btnNextStep.setOnClickListener {
            val selectedClothes = clothList.filter { it.isSelected }

            val selectedImages = ArrayList(selectedClothes.map { it.imageUrl })
            val selectedIds = ArrayList(selectedClothes.map { it.id })

            Toast.makeText(this, "내 세탁기 목록을 불러오는 중...", Toast.LENGTH_SHORT).show()
            val db = FirebaseFirestore.getInstance()

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

                machineNames.add("➕ 새 세탁기 카메라로 등록하기")
                val machineArray = machineNames.toTypedArray()

                androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("어떤 세탁기를 사용하실 건가요?")
                    .setItems(machineArray) { _, which ->
                        if (which == machineArray.size - 1) {
                            val intent = Intent(this, WasherCameraActivity::class.java)
                            intent.putStringArrayListExtra("selected_cloth_images", selectedImages)
                            intent.putStringArrayListExtra("selected_cloth_ids", selectedIds)
                            startActivity(intent)
                        } else {
                            val selectedDoc = machineDocs[which]
                            val intent = Intent(this, LaundryResultActivity::class.java)
                            intent.putExtra("washer_type", selectedDoc["type"])
                            intent.putExtra("brand", selectedDoc["brand"])
                            intent.putExtra("model", selectedDoc["model"])
                            intent.putStringArrayListExtra("selected_cloth_images", selectedImages)
                            intent.putStringArrayListExtra("selected_cloth_ids", selectedIds)
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

            holder.cbSelect.setOnCheckedChangeListener(null)
            holder.cbSelect.isChecked = item.isSelected

            holder.cbSelect.setOnCheckedChangeListener { _, isChecked ->
                item.isSelected = isChecked
                onItemSelectionChanged()
            }

            holder.itemView.setOnClickListener {
                holder.cbSelect.isChecked = !holder.cbSelect.isChecked
            }
        }

        override fun getItemCount(): Int = items.size
    }
}