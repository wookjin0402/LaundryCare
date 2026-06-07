package com.example.laundrycare_android

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ScanActivity : AppCompatActivity() {

    private lateinit var scanViewFinder: PreviewView
    private lateinit var ivScanCaptured: ImageView
    private lateinit var btnScanCapture: Button
    private lateinit var btnScanSubmit: Button
    private lateinit var pbScanLoading: ProgressBar

    // 🌟 EditText에서 Spinner로 변경 완료
    private lateinit var spinnerMainCat: Spinner
    private lateinit var spinnerSubCat: Spinner

    private lateinit var etScanColor: EditText
    private lateinit var etScanMaterial: EditText

    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService
    private var localImageFile: File? = null

    // 🌟 계층형 카테고리 데이터 정의 (백엔드와 맞춤)
    private val mainCategories = listOf("선택하세요", "상의", "하의", "아우터", "기타")
    private val subCategoryMap = mapOf(
        "선택하세요" to listOf("대분류를 먼저 선택하세요"),
        "상의" to listOf("티셔츠", "셔츠", "맨투맨", "니트"),
        "하의" to listOf("청바지", "슬랙스", "면바지", "반바지", "스커트"),
        "아우터" to listOf("패딩", "코트", "자켓", "가디건"),
        "기타" to listOf("모자", "목도리", "가방")
    )

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) startCamera()
        else Toast.makeText(this, "카메라 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan)

        findViewById<ImageView>(R.id.btnScanBack).setOnClickListener {
            finish()
        }

        scanViewFinder = findViewById(R.id.scanViewFinder)
        ivScanCaptured = findViewById(R.id.ivScanCaptured)
        btnScanCapture = findViewById(R.id.btnScanCapture)
        btnScanSubmit = findViewById(R.id.btnScanSubmit)
        pbScanLoading = findViewById(R.id.pbScanLoading)

        // 🌟 스피너 연결
        spinnerMainCat = findViewById(R.id.spinnerMainCat)
        spinnerSubCat = findViewById(R.id.spinnerSubCat)
        etScanColor = findViewById(R.id.etScanColor)
        etScanMaterial = findViewById(R.id.etScanMaterial)

        setupSpinners()

        cameraExecutor = Executors.newSingleThreadExecutor()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        btnScanCapture.setOnClickListener { takePhoto() }
        btnScanSubmit.setOnClickListener { uploadToFirebase() }
    }

    // 🌟 대분류 선택 시 소분류가 자동으로 바뀌는 로직 세팅
    private fun setupSpinners() {
        val mainAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, mainCategories)
        spinnerMainCat.adapter = mainAdapter

        spinnerMainCat.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedMain = mainCategories[position]
                val subList = subCategoryMap[selectedMain] ?: listOf("대분류를 먼저 선택하세요")
                val subAdapter = ArrayAdapter(this@ScanActivity, android.R.layout.simple_spinner_dropdown_item, subList)
                spinnerSubCat.adapter = subAdapter
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also { it.setSurfaceProvider(scanViewFinder.surfaceProvider) }
            imageCapture = ImageCapture.Builder().build()
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
            } catch (exc: Exception) {
                Toast.makeText(this, "카메라 오류", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto() {
        val capture = imageCapture ?: return

        capture.takePicture(ContextCompat.getMainExecutor(this), object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                val buffer = image.planes[0].buffer
                val bytes = ByteArray(buffer.capacity())
                buffer.get(bytes)
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, null)

                runOnUiThread {
                    ivScanCaptured.setImageBitmap(bitmap)
                    scanViewFinder.visibility = View.INVISIBLE
                    ivScanCaptured.visibility = View.VISIBLE
                    btnScanCapture.visibility = View.GONE
                }

                image.close()

                localImageFile = File(cacheDir, "scan_${System.currentTimeMillis()}.jpg")
                val fos = FileOutputStream(localImageFile)
                bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, fos)
                fos.flush()
                fos.close()

                analyzeClothWithAI(localImageFile!!)
            }
            override fun onError(exc: ImageCaptureException) {
                runOnUiThread { Toast.makeText(baseContext, "사진 촬영 실패", Toast.LENGTH_SHORT).show() }
            }
        })
    }

    private fun analyzeClothWithAI(file: File) {
        runOnUiThread {
            pbScanLoading.visibility = View.VISIBLE
            Toast.makeText(this, "AI가 옷을 분석하고 있습니다...", Toast.LENGTH_SHORT).show()
        }

        val client = OkHttpClient()
        val url = "http://34.64.101.110:3000/api/clothes/analyze"

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("image", file.name, RequestBody.create("image/jpeg".toMediaType(), file))
            .build()

        val request = Request.Builder().url(url).post(requestBody).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    pbScanLoading.visibility = View.GONE
                    Toast.makeText(this@ScanActivity, "서버 연결에 실패했습니다. (${e.message})", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseData = response.body?.string()
                runOnUiThread { pbScanLoading.visibility = View.GONE }

                if (response.isSuccessful && responseData != null) {
                    try {
                        val json = JSONObject(responseData)

                        if (json.optString("status") == "success") {
                            val mainCat = json.optString("mainCategory", "")
                            val subCat = json.optString("subCategory", "")
                            val color = json.optString("color", "")

                            val materialsArray = json.optJSONArray("extracted_materials")
                            val materialList = mutableListOf<String>()
                            if (materialsArray != null) {
                                for (i in 0 until materialsArray.length()) {
                                    materialList.add(materialsArray.getString(i))
                                }
                            }
                            val materialString = materialList.joinToString(", ")

                            runOnUiThread {
                                // 🌟 1. 대분류 스피너 자동 세팅
                                val mainIndex = mainCategories.indexOf(mainCat)
                                if (mainIndex >= 0) {
                                    spinnerMainCat.setSelection(mainIndex)

                                    // 🌟 2. 대분류에 맞춰 소분류 목록을 즉시 갱신하고 자동 세팅
                                    val subList = subCategoryMap[mainCat] ?: listOf()
                                    val subAdapter = ArrayAdapter(this@ScanActivity, android.R.layout.simple_spinner_dropdown_item, subList)
                                    spinnerSubCat.adapter = subAdapter

                                    val subIndex = subList.indexOf(subCat)
                                    if (subIndex >= 0) spinnerSubCat.setSelection(subIndex)
                                }

                                etScanColor.setText(color)
                                etScanMaterial.setText(materialString)
                                Toast.makeText(this@ScanActivity, "분석 완료! 결과가 자동으로 매핑되었습니다.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } catch (e: Exception) {
                        runOnUiThread { Toast.makeText(this@ScanActivity, "데이터 응답 분석 오류", Toast.LENGTH_SHORT).show() }
                    }
                } else {
                    runOnUiThread { Toast.makeText(this@ScanActivity, "서버 오류: ${response.code}", Toast.LENGTH_SHORT).show() }
                }
            }
        })
    }

    private fun uploadToFirebase() {
        val file = localImageFile
        if (file == null) {
            Toast.makeText(this, "옷 사진을 먼저 촬영해주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        // 🌟 스피너에서 선택된 값 가져오기
        val mainCat = spinnerMainCat.selectedItem?.toString() ?: ""
        val subCat = spinnerSubCat.selectedItem?.toString() ?: ""
        val color = etScanColor.text.toString().trim()
        val material = etScanMaterial.text.toString().trim()

        if (mainCat == "선택하세요" || subCat == "대분류를 먼저 선택하세요" || mainCat.isEmpty() || subCat.isEmpty() || color.isEmpty() || material.isEmpty()) {
            Toast.makeText(this, "모든 카테고리와 정보를 정확히 입력해주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        pbScanLoading.visibility = View.VISIBLE
        btnScanSubmit.isEnabled = false

        val storageRef = FirebaseStorage.getInstance().reference
        val imageRef = storageRef.child("clothes_images/${System.currentTimeMillis()}.jpg")

        imageRef.putFile(Uri.fromFile(file))
            .addOnSuccessListener {
                imageRef.downloadUrl.addOnSuccessListener { uri ->
                    val db = FirebaseFirestore.getInstance()
                    val clothData = hashMapOf(
                        "imageUrl" to uri.toString(),
                        "mainCategory" to mainCat,
                        "subCategory" to subCat,
                        "color" to color,
                        "material" to material,
                        "timestamp" to System.currentTimeMillis()
                    )

                    db.collection("clothes").add(clothData)
                        .addOnSuccessListener {
                            Toast.makeText(this, "옷이 성공적으로 등록되었습니다!", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                        .addOnFailureListener { resetState("DB 저장 실패") }
                }
            }
            .addOnFailureListener { resetState("이미지 업로드 실패") }
    }

    private fun resetState(msg: String) {
        pbScanLoading.visibility = View.GONE
        btnScanSubmit.isEnabled = true
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}