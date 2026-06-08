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
import com.google.firebase.auth.FirebaseAuth // 🌟 UID 가져오기 위해 추가
import com.google.firebase.firestore.FirebaseFirestore

data class SelectableCloth(
    val id: String,
    val imageUrl: String,
    val mainCategory: String,
    val subCategory: String,
    val color: String,
    val material: String,
    var isSelected: Boolean = false
)

class ClothMultiSelectActivity : AppCompatActivity() {

    private lateinit var rvClothes: RecyclerView
    private lateinit var pbLoading: ProgressBar
    private lateinit var btnNextStep: Button

    private val clothList = mutableListOf<SelectableCloth>()
    private lateinit var adapter: ClothSelectAdapter

    // 🌟 내 고유 UID 가져오기
    private val myUid get() = FirebaseAuth.getInstance().currentUser?.uid ?: "unknown_user"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cloth_multi_select)

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

        val mode = intent.getStringExtra("mode") ?: "batch"

        btnNextStep.setOnClickListener {
            val selectedClothes = clothList.filter { it.isSelected }

            if (mode == "recommend") {
                showConditionSelection(selectedClothes)
            } else {
                executeWasherMatching(selectedClothes)
            }
        }
    }

    private fun fetchClothesFromFirebase() {
        pbLoading.visibility = View.VISIBLE
        val db = FirebaseFirestore.getInstance()

        // 💡 참고: 옷장 데이터도 개인 창고(users/myUid/clothes)에 있다면 여기 경로도 나중에 맞춰주셔야 할 수 있습니다.
        // 일단 현재 옷 목록은 잘 뜨는 것으로 보이니 기존 경로를 유지했습니다.
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
                        color = doc.getString("color") ?: "미상",
                        material = doc.getString("material") ?: "일반"
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
        val mode = intent.getStringExtra("mode") ?: "batch"

        if (selectedCount > 0) {
            btnNextStep.isEnabled = true
            if (mode == "recommend") {
                btnNextStep.text = "AI 맞춤 추천 받기 (${selectedCount}벌 선택됨)"
            } else {
                btnNextStep.text = "세탁기 매칭하기 (${selectedCount}벌 선택됨)"
            }
        } else {
            btnNextStep.isEnabled = false
            btnNextStep.text = "의류를 선택해주세요 (0벌 선택됨)"
        }
    }

    private fun showConditionSelection(selectedClothes: List<SelectableCloth>) {
        val conditionList = arrayOf("땀을 많이 흘렸어요", "얼룩이 묻었어요", "깨끗해요")
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("빨랫감 상태는 어떤가요?")
            .setItems(conditionList) { _, whichCond ->
                showFinalAIResult(selectedClothes, conditionList[whichCond])
            }.show()
    }

    private fun showFinalAIResult(selectedClothes: List<SelectableCloth>, cond: String) {
        val materials = selectedClothes.joinToString { it.material }
        val colors = selectedClothes.joinToString { it.color }

        val hasSensitive = materials.contains("실크") || materials.contains("울") || materials.contains("가죽") || materials.contains("니트")
        val hasWhite = colors.contains("흰") || colors.contains("백") || colors.contains("화이트")
        val hasColor = colors.contains("검") || colors.contains("빨") || colors.contains("파") || colors.contains("블랙")

        var resultMessage = "👕 선택된 의류: 총 ${selectedClothes.size}벌\n"
        resultMessage += "💧 빨랫감 상태: $cond\n\n"

        if (hasSensitive) {
            resultMessage += "⚠️ [소재 주의] 민감한 소재(실크/울 등)가 포함되어 있습니다. 단독 세탁이나 섬세/울코스를 권장합니다.\n\n"
        }
        if (hasWhite && hasColor) {
            resultMessage += "⚠️ [이염 주의] 밝은 색 옷과 짙은 색 옷이 섞여 있습니다. 이염 방지를 위해 분리 세탁하세요.\n\n"
        }
        if (cond.contains("얼룩")) {
            resultMessage += "⚠️ [상태 맞춤] 얼룩이 있는 의류가 포함되어 있습니다. 본 세탁 전 애벌빨래를 진행해 주세요.\n\n"
        }

        val weatherGuide = intent.getStringExtra("weatherGuide") ?: "현재 날씨 기반 건조 팁을 확인 중입니다..."
        resultMessage += "💡 [오늘의 날씨 맞춤 건조 팁] 💡\n$weatherGuide"

        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("AI 통합 세탁 가이드")
            .setMessage(resultMessage)

        if (cond.contains("얼룩")) {
            builder.setPositiveButton("얼룩 지우는 법 보러가기") { _, _ ->
                val intent = Intent(this, MainActivity::class.java)
                intent.putExtra("navigate_to_fragment", "stain")
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                startActivity(intent)
                finish()
            }
            builder.setNegativeButton("닫기") { _, _ -> finish() }
        } else {
            builder.setPositiveButton("확인") { _, _ -> finish() }
        }

        builder.show()
    }

    private fun executeWasherMatching(selectedClothes: List<SelectableCloth>) {
        val selectedImages = ArrayList(selectedClothes.map { it.imageUrl })
        val selectedIds = ArrayList(selectedClothes.map { it.id })

        Toast.makeText(this, "내 세탁기 목록을 불러오는 중...", Toast.LENGTH_SHORT).show()
        val db = FirebaseFirestore.getInstance()

        // 🌟 핵심 수정 포인트: '내 개인 창고'에서 세탁기 목록 가져오기
        db.collection("users").document(myUid).collection("washers").get().addOnSuccessListener { snapshot ->
            val machineNames = mutableListOf<String>()
            val machineDocs = mutableListOf<Map<String, String>>()

            for (doc in snapshot.documents) {
                // 🌟 DB에 저장된 필드명(brand, model, type)에 맞게 데이터 추출
                val brand = doc.getString("brand") ?: "브랜드 미상"
                val model = doc.getString("model") ?: ""
                val type = doc.getString("type") ?: "세탁기"

                // 팝업에 보여질 예쁜 이름 만들기 (예: "✅ 삼성 그랑데 AI (드럼)")
                val displayName = if (model.isNotEmpty()) "✅ $brand $model ($type)" else "✅ $brand ($type)"

                machineNames.add(displayName)
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