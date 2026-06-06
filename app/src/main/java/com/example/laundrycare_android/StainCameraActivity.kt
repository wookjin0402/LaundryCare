package com.example.laundrycare_android

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.drawable.BitmapDrawable
import android.net.Uri // 🌟 갤러리 이미지 URI 처리를 위해 추가
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts // 🌟 갤러리 런처 처리를 위해 추가
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class StainCameraActivity : AppCompatActivity() {
    private lateinit var viewFinder: PreviewView
    private lateinit var ivCapturedImage: ImageView
    private lateinit var pbScanning: ProgressBar
    private var imageCapture: ImageCapture? = null

    // 🌟 1. 갤러리에서 사진을 선택했을 때 결과를 받아오는 런처 설정
    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            // 사진을 성공적으로 골라오면 화면에 띄우고 상태 변경
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

        // 🌟 2. 갤러리 버튼 클릭 이벤트 추가 (이 부분이 빠져 있었습니다!)
        findViewById<Button>(R.id.btnSelectPhoto).setOnClickListener {
            galleryLauncher.launch("image/*")
        }

        startCamera()
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

        val intent = Intent(this, StainResultActivity::class.java).apply {
            putExtra("stain_image_path", file.absolutePath)
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