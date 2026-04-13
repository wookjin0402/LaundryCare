package com.example.laundrycare_android

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraActivity : AppCompatActivity() {

    private lateinit var viewFinder: PreviewView
    private lateinit var ivCapturedImage: ImageView
    private lateinit var layoutGuide: View // 전체 가이드(어두운 배경+괄호) 덩어리
    private lateinit var pbScanning: ProgressBar
    private lateinit var btnTakePhoto: Button
    private lateinit var btnSelectPhoto: Button
    private lateinit var btnRetry: Button
    private lateinit var btnStartAnalysis: Button

    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) { startCamera() }
        else { Toast.makeText(this, "카메라 권한이 필요합니다.", Toast.LENGTH_SHORT).show() }
    }

    private val getContent = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            ivCapturedImage.setImageURI(uri)
            showCapturedState()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        viewFinder = findViewById(R.id.viewFinder)
        ivCapturedImage = findViewById(R.id.ivCapturedImage)
        layoutGuide = findViewById(R.id.layoutGuide)
        pbScanning = findViewById(R.id.pbScanning)
        btnTakePhoto = findViewById(R.id.btnTakePhoto)
        btnSelectPhoto = findViewById(R.id.btnSelectPhoto)
        btnRetry = findViewById(R.id.btnRetry)
        btnStartAnalysis = findViewById(R.id.btnStartAnalysis)
        val scanType = intent.getStringExtra("scanType")
        val tvGuide = findViewById<TextView>(R.id.tvGuideMessage) // XML에서 달아준 이름표

        if (scanType == "MACHINE") {
            tvGuide.text = "세탁기 외관이나 모델명이 보이게 촬영해주세요"
        } else {
            tvGuide.text = "세탁물을 [   ] 칸 안에 맞춰주세요"
        }
        cameraExecutor = Executors.newSingleThreadExecutor()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        btnSelectPhoto.setOnClickListener { getContent.launch("image/*") }
        btnTakePhoto.setOnClickListener { takePhoto() }
        btnRetry.setOnClickListener { resetToCameraState() }

        // 🌟 [여기에 추가!] 분석 시작 버튼 누르면 ResultActivity로 택배 싸서 보내기! 🌟
        btnStartAnalysis.setOnClickListener {
            val intent = Intent(this, ResultActivity::class.java)

            // 메인에서 받은 택배(의류/세탁기)를 결과 화면으로 다시 토스!
            val currentScanType = getIntent().getStringExtra("scanType")
            intent.putExtra("scanType", currentScanType)

            startActivity(intent)
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
            } catch(exc: Exception) {
                Toast.makeText(this, "카메라 실행 실패", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return

        // 촬영 버튼 누르면 가이드 화면 끄고, 로딩 바 켜기
        layoutGuide.visibility = View.INVISIBLE
        btnTakePhoto.isEnabled = false
        pbScanning.visibility = View.VISIBLE

        imageCapture.takePicture(ContextCompat.getMainExecutor(this), object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                val buffer = image.planes[0].buffer
                val bytes = ByteArray(buffer.capacity())
                buffer.get(bytes)
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, null)

                ivCapturedImage.setImageBitmap(bitmap)
                image.close()

                pbScanning.visibility = View.GONE
                btnTakePhoto.isEnabled = true
                showCapturedState()
            }

            override fun onError(exc: ImageCaptureException) {
                pbScanning.visibility = View.GONE
                btnTakePhoto.isEnabled = true
                layoutGuide.visibility = View.VISIBLE
                Toast.makeText(baseContext, "사진 촬영 실패", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showCapturedState() {
        viewFinder.visibility = View.INVISIBLE
        ivCapturedImage.visibility = View.VISIBLE
        layoutGuide.visibility = View.INVISIBLE

        btnTakePhoto.visibility = View.GONE
        btnSelectPhoto.visibility = View.GONE
        btnRetry.visibility = View.VISIBLE
        btnStartAnalysis.visibility = View.VISIBLE
    }

    private fun resetToCameraState() {
        viewFinder.visibility = View.VISIBLE
        ivCapturedImage.visibility = View.GONE
        layoutGuide.visibility = View.VISIBLE

        btnTakePhoto.visibility = View.VISIBLE
        btnSelectPhoto.visibility = View.VISIBLE
        btnRetry.visibility = View.GONE
        btnStartAnalysis.visibility = View.GONE
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}