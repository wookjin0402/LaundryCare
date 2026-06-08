package com.example.laundrycare_android

import android.Manifest // 🌟 임포트 추가
import android.content.Intent
import android.content.pm.PackageManager // 🌟 임포트 추가
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth // 🌟 UID 처리를 위한 Firebase 임포트 추가
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class WasherCameraActivity : AppCompatActivity() {

    private lateinit var viewFinder: PreviewView
    private lateinit var loadingLayout: LinearLayout
    private lateinit var btnCapture: Button
    private lateinit var btnCameraBack: ImageView

    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService

    // 🌟 추가: 세탁기 등록용 비동기식 시스템 권한 수신 장치 구현
    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            startCamera()
        } else {
            Toast.makeText(this, "카메라 권한 승인이 필요합니다.", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            loadingLayout.visibility = View.VISIBLE
            btnCapture.isEnabled = false

            val file = File(cacheDir, "gallery_washer_${System.currentTimeMillis()}.jpg")
            try {
                val inputStream = contentResolver.openInputStream(uri)
                val outputStream = FileOutputStream(file)
                inputStream?.copyTo(outputStream)
                inputStream?.close()
                outputStream.close()

                simulateAiAnalysis(file.absolutePath)
            } catch (e: Exception) {
                loadingLayout.visibility = View.GONE
                btnCapture.isEnabled = true
                Toast.makeText(this, "이미지를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_washer_camera)

        viewFinder = findViewById(R.id.viewFinderWasher)
        loadingLayout = findViewById(R.id.loadingLayoutWasher)
        btnCapture = findViewById(R.id.btnCaptureWasher)
        btnCameraBack = findViewById(R.id.btnCameraBack)

        val btnSelectPhoto = findViewById<Button>(R.id.btnSelectPhoto)

        cameraExecutor = Executors.newSingleThreadExecutor()

        // 🌟 수정: 무방비 노출 상태의 즉시 호출을 중단하고 검증 절차 도입
        checkCameraPermission()

        btnCapture.setOnClickListener {
            takePhotoAndAnalyze()
        }

        btnSelectPhoto?.setOnClickListener {
            galleryLauncher.launch("image/*")
        }

        btnCameraBack.setOnClickListener {
            finish()
        }
    }

    // 🌟 추가: 권한을 정중히 검증하는 중앙 제어 체계 마련
    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(viewFinder.surfaceProvider)
            }
            imageCapture = ImageCapture.Builder().build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
            } catch (exc: Exception) {
                Toast.makeText(this, "카메라 실행 실패", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhotoAndAnalyze() {
        val imageCapture = imageCapture ?: return

        val photoFile = File(externalMediaDirs.firstOrNull(), "washer_${System.currentTimeMillis()}.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        loadingLayout.visibility = View.VISIBLE
        btnCapture.isEnabled = false

        imageCapture.takePicture(
            outputOptions, ContextCompat.getMainExecutor(this), object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    loadingLayout.visibility = View.GONE
                    btnCapture.isEnabled = true
                    Toast.makeText(baseContext, "사진 저장 실패", Toast.LENGTH_SHORT).show()
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val imagePath = photoFile.absolutePath
                    simulateAiAnalysis(imagePath)
                }
            })
    }

    private fun simulateAiAnalysis(imagePath: String) {
        cameraExecutor.execute {
            Thread.sleep(2000)

            val aiResultJson = """
                {
                    "washer": {
                        "type": "통돌이",
                        "brand": "삼성",
                        "model": "그랑데 AI"
                    }
                }
            """.trimIndent()

            // 🌟 수정: UID를 추출하여 다음 결과 화면으로 안전하게 전달
            val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: "test_uid"

            runOnUiThread {
                val intent = Intent(this@WasherCameraActivity, WasherResultActivity::class.java).apply {
                    putExtra("washer_image_path", imagePath)
                    putExtra("ai_washer_data", aiResultJson)
                    putExtra("uid", currentUid) // 🌟 UID 전달 완료
                }
                startActivity(intent)
                finish()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}