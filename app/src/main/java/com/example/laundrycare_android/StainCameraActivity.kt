package com.example.laundrycare_android

import android.Manifest // 🌟 임포트 추가
import android.content.Intent
import android.content.pm.PackageManager // 🌟 임포트 추가
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.drawable.BitmapDrawable
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
import com.google.firebase.auth.FirebaseAuth // 🌟 UID 처리를 위한 Firebase 임포트 추가
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class StainCameraActivity : AppCompatActivity() {
    private lateinit var viewFinder: PreviewView
    private lateinit var ivCapturedImage: ImageView
    private lateinit var pbScanning: ProgressBar
    private var imageCapture: ImageCapture? = null

    // 🌟 추가: 시스템과 통신하여 결과를 수신받는 비동기식 카메라 권한 팝업 런처
    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            startCamera()
        } else {
            Toast.makeText(this, "카메라 권한이 거부되어 이전 화면으로 돌아갑니다.", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            ivCapturedImage.setImageURI(uri)
            showCapturedState()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stain_camera)

        viewFinder = findViewById(R.id.viewFinder)
        ivCapturedImage = findViewById(R.id.ivCapturedImage)
        pbScanning = findViewById(R.id.pbScanning)

        findViewById<ImageView>(R.id.btnStainCameraBack).setOnClickListener { finish() }
        findViewById<Button>(R.id.btnTakePhoto).setOnClickListener { takePhoto() }
        findViewById<Button>(R.id.btnStartAnalysis).setOnClickListener { sendImageToAI() }
        findViewById<Button>(R.id.btnRetry).setOnClickListener { resetToCameraState() }

        findViewById<Button>(R.id.btnSelectPhoto).setOnClickListener {
            galleryLauncher.launch("image/*")
        }

        // 🌟 수정: 묻지마 실행을 배제하고, 권한 유무 체크 로직으로 변경 호출
        checkCameraPermission()
    }

    // 🌟 추가: 실행 전 정중하게 안전벨트 유무 파악
    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun takePhoto() {
        imageCapture?.takePicture(ContextCompat.getMainExecutor(this), object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                val buffer = image.planes[0].buffer
                val bytes = ByteArray(buffer.capacity())
                buffer.get(bytes)
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, null)
                val matrix = Matrix().apply { postRotate(image.imageInfo.rotationDegrees.toFloat()) }
                val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                runOnUiThread { ivCapturedImage.setImageBitmap(rotated); showCapturedState() }
                image.close()
            }
            override fun onError(e: ImageCaptureException) { Toast.makeText(baseContext, "촬영 실패", Toast.LENGTH_SHORT).show() }
        })
    }

    private fun showCapturedState() {
        viewFinder.visibility = View.INVISIBLE
        ivCapturedImage.visibility = View.VISIBLE
        findViewById<View>(R.id.layoutGuide).visibility = View.INVISIBLE
        findViewById<Button>(R.id.btnTakePhoto).visibility = View.GONE
        findViewById<Button>(R.id.btnSelectPhoto).visibility = View.GONE
        findViewById<Button>(R.id.btnRetry).visibility = View.VISIBLE
        findViewById<Button>(R.id.btnStartAnalysis).visibility = View.VISIBLE
    }

    private fun resetToCameraState() {
        viewFinder.visibility = View.VISIBLE
        ivCapturedImage.visibility = View.GONE
        findViewById<View>(R.id.layoutGuide).visibility = View.VISIBLE
        findViewById<Button>(R.id.btnTakePhoto).visibility = View.VISIBLE
        findViewById<Button>(R.id.btnSelectPhoto).visibility = View.VISIBLE
        findViewById<Button>(R.id.btnRetry).visibility = View.GONE
        findViewById<Button>(R.id.btnStartAnalysis).visibility = View.GONE
    }

    private fun sendImageToAI() {
        val bitmap = (ivCapturedImage.drawable as? BitmapDrawable)?.bitmap ?: return
        pbScanning.visibility = View.VISIBLE

        val file = File(cacheDir, "stain_${System.currentTimeMillis()}.jpg")
        bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 100, FileOutputStream(file))

        // 🌟 수정: UID를 추출하여 다음 결과 화면으로 안전하게 전달
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: "test_uid"

        val intent = Intent(this, StainResultActivity::class.java).apply {
            putExtra("stain_image_path", file.absolutePath)
            putExtra("uid", currentUid) // 전달 완료
        }
        startActivity(intent)
        finish()
    }

    private fun startCamera() {
        ProcessCameraProvider.getInstance(this).addListener({
            val provider = ProcessCameraProvider.getInstance(this).get()
            val preview = Preview.Builder().build().also { it.setSurfaceProvider(viewFinder.surfaceProvider) }
            imageCapture = ImageCapture.Builder().build()
            provider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture)
        }, ContextCompat.getMainExecutor(this))
    }
}