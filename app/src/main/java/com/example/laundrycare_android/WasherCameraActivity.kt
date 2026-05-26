package com.example.laundrycare_android

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class WasherCameraActivity : AppCompatActivity() {

    private lateinit var viewFinder: PreviewView
    private lateinit var loadingLayout: LinearLayout
    private lateinit var btnCapture: Button

    private var imageCapture: ImageCapture? = null
    // 🌟 PPT 핵심: 비동기 처리를 위한 Worker Thread (워커 스레드)
    private lateinit var cameraExecutor: ExecutorService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_washer_camera)

        viewFinder = findViewById(R.id.viewFinderWasher)
        loadingLayout = findViewById(R.id.loadingLayoutWasher)
        btnCapture = findViewById(R.id.btnCaptureWasher)

        cameraExecutor = Executors.newSingleThreadExecutor()

        // 카메라 실행 (권한은 이미 매니페스트에 있으므로 바로 실행)
        startCamera()

        btnCapture.setOnClickListener {
            takePhotoAndAnalyze()
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

        // 파일 저장 경로 설정
        val photoFile = File(externalMediaDirs.firstOrNull(), "washer_${System.currentTimeMillis()}.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        // 1. 촬영 버튼을 누르면 즉시 로딩 화면을 띄움 (메인 스레드 UI 업데이트)
        loadingLayout.visibility = View.VISIBLE
        btnCapture.isEnabled = false

        // 2. 비동기 백그라운드 촬영 시작
        imageCapture.takePicture(
            outputOptions, ContextCompat.getMainExecutor(this), object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    loadingLayout.visibility = View.GONE
                    btnCapture.isEnabled = true
                    Toast.makeText(baseContext, "사진 저장 실패", Toast.LENGTH_SHORT).show()
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val savedUri = Uri.fromFile(photoFile)
                    val imagePath = photoFile.absolutePath

                    // 🌟 3. 여기서 AI 서버와 비동기 통신을 수행한다고 가정 (현재는 딜레이로 시뮬레이션)
                    // 실제 구현 시 OkHttp의 enqueue() 등을 사용해 서버로 이미지를 보냅니다.
                    simulateAiAnalysis(imagePath)
                }
            })
    }

    // 🌟 서버와 통신하는 척 2초 정도 기다린 후(비동기) 결과 화면으로 넘겨주는 함수
    private fun simulateAiAnalysis(imagePath: String) {
        cameraExecutor.execute {
            // 백그라운드 스레드에서 AI 통신 중... (ANR 방지)
            Thread.sleep(2000)

            // AI 서버가 "통돌이"라고 판단해서 내려줬다고 가정하는 가짜 JSON 데이터
            val aiResultJson = """
                {
                    "washer": {
                        "type": "통돌이",
                        "brand": "삼성",
                        "model": "그랑데 AI"
                    }
                }
            """.trimIndent()

            // 분석이 끝나면 결과 화면으로 데이터 전달 (메인 스레드로 돌아옴)
            runOnUiThread {
                val intent = Intent(this@WasherCameraActivity, WasherResultActivity::class.java).apply {
                    putExtra("washer_image_path", imagePath)
                    putExtra("ai_washer_data", aiResultJson)
                }
                startActivity(intent)
                finish() // 카메라 화면 닫기
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}