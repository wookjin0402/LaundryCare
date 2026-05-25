package com.example.laundrycare_android

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.google.firebase.firestore.FirebaseFirestore
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class WasherCameraActivity : AppCompatActivity() {

    private lateinit var viewFinder: PreviewView
    private lateinit var ivCapturedImage: ImageView
    private lateinit var pbScanning: ProgressBar
    private lateinit var btnTakeWasherPhoto: Button

    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) startCamera()
        else Toast.makeText(this, "카메라 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_washer_camera)

        viewFinder = findViewById(R.id.viewFinder)
        ivCapturedImage = findViewById(R.id.ivCapturedImage)
        pbScanning = findViewById(R.id.pbScanning)
        btnTakeWasherPhoto = findViewById(R.id.btnTakeWasherPhoto)

        cameraExecutor = Executors.newSingleThreadExecutor()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        btnTakeWasherPhoto.setOnClickListener { takePhotoAndAnalyze() }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also { it.setSurfaceProvider(viewFinder.surfaceProvider) }
            imageCapture = ImageCapture.Builder().build()
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
            } catch(exc: Exception) {
                Toast.makeText(this, "카메라 실행 실패", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhotoAndAnalyze() {
        val imageCapture = imageCapture ?: return
        btnTakeWasherPhoto.isEnabled = false
        pbScanning.visibility = View.VISIBLE

        imageCapture.takePicture(ContextCompat.getMainExecutor(this), object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                val buffer = image.planes[0].buffer
                val bytes = ByteArray(buffer.capacity())
                buffer.get(bytes)
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, null)

                ivCapturedImage.setImageBitmap(bitmap)
                viewFinder.visibility = View.INVISIBLE
                ivCapturedImage.visibility = View.VISIBLE
                btnTakeWasherPhoto.visibility = View.GONE

                image.close()

                // 임시 파일로 저장 후 서버로 전송
                val file = File(cacheDir, "temp_washer.jpg")
                val fos = FileOutputStream(file)
                bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 100, fos)
                fos.flush()
                fos.close()

                sendToWasherAPI(file)
            }

            override fun onError(exc: ImageCaptureException) {
                pbScanning.visibility = View.GONE
                btnTakeWasherPhoto.isEnabled = true
                Toast.makeText(baseContext, "사진 촬영 실패", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun sendToWasherAPI(file: File) {
        val client = OkHttpClient()
        val requestBody = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("washerImage", file.name, RequestBody.create("image/jpeg".toMediaTypeOrNull(), file))
            .build()

        val request = Request.Builder().url("http://34.64.101.110:3000/api/washers/analyze").post(requestBody).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    pbScanning.visibility = View.GONE
                    Toast.makeText(this@WasherCameraActivity, "서버 연결 실패. 임시 UI로 진행합니다.", Toast.LENGTH_SHORT).show()
                    showInputModal("드럼 세탁기")
                }
            }

            override fun onResponse(call: Call, response: Response) {
                runOnUiThread { pbScanning.visibility = View.GONE }
                if (response.isSuccessful) {
                    val responseData = response.body?.string() ?: ""
                    val washerType = "드럼 세탁기"
                    runOnUiThread { showInputModal(washerType) }
                } else {
                    runOnUiThread {
                        Toast.makeText(this@WasherCameraActivity, "서버 분석 오류(500). 임시 UI로 진행합니다.", Toast.LENGTH_SHORT).show()
                        showInputModal("드럼 세탁기")
                    }
                }
            }
        })
    }

    private fun showInputModal(washerType: String) {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(60, 40, 60, 20)
        }

        val etBrand = EditText(this).apply { hint = "브랜드 (예: LG, 삼성)" }
        val etModel = EditText(this).apply { hint = "모델명 (예: F21VDW)" }

        layout.addView(etBrand)
        layout.addView(etModel)

        AlertDialog.Builder(this)
            .setTitle("🤖 $washerType 인식 완료!")
            .setMessage("정확한 코스 매칭을 위해 세탁기 브랜드와 모델명을 입력해주세요.")
            .setView(layout)
            .setCancelable(false)
            .setPositiveButton("등록 후 결과 확인하기") { _, _ ->
                val brand = etBrand.text.toString().ifEmpty { "LG" }
                val model = etModel.text.toString().ifEmpty { "기본모델" }

                Toast.makeText(this, "세탁기를 등록하는 중...", Toast.LENGTH_SHORT).show()
                val db = FirebaseFirestore.getInstance()

                // 🌟 파이어베이스에 새 세탁기 영구 등록!
                val newWasher = hashMapOf(
                    "name" to "우리집 세탁기",
                    "brand" to brand,
                    "model" to model,
                    "type" to washerType,
                    "timestamp" to System.currentTimeMillis()
                )

                db.collection("washers").add(newWasher).addOnSuccessListener {
                    Toast.makeText(this, "새 세탁기 등록 완료!", Toast.LENGTH_SHORT).show()

                    // 🌟 앞 화면에서 넘어온 옷 이미지 리스트를 그대로 받아서 다시 최종 화면으로 토스!
                    val clothImages = intent.getStringArrayListExtra("selected_cloth_images")

                    val intent = Intent(this, WasherResultActivity::class.java)
                    intent.putExtra("washer_type", washerType)
                    intent.putExtra("brand", brand)
                    intent.putExtra("model", model)
                    intent.putStringArrayListExtra("selected_cloth_images", clothImages) // 최종 화면으로 전달!
                    startActivity(intent)
                    finish()
                }
            }
            .setNegativeButton("다시 찍기") { _, _ ->
                finish()
            }
            .show()
    }
}