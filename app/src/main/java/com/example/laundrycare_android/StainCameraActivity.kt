package com.example.laundrycare_android

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.view.View
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
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class StainCameraActivity : AppCompatActivity() {

    private lateinit var viewFinder: PreviewView
    private lateinit var ivCapturedImage: ImageView
    private lateinit var layoutGuide: View
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
        setContentView(R.layout.activity_stain_camera)

        // 🌟 XML 파일의 버튼 아이디들과 100% 일치하도록 매핑 완료
        viewFinder = findViewById(R.id.viewFinder)
        ivCapturedImage = findViewById(R.id.ivCapturedImage)
        layoutGuide = findViewById(R.id.layoutGuide)
        pbScanning = findViewById(R.id.pbScanning)
        btnTakePhoto = findViewById(R.id.btnTakePhoto)
        btnSelectPhoto = findViewById(R.id.btnSelectPhoto)
        btnRetry = findViewById(R.id.btnRetry)
        btnStartAnalysis = findViewById(R.id.btnStartAnalysis)

        // 🌟 뒤로가기 버튼 완벽 연결
        findViewById<Button>(R.id.btnStainCameraBack).setOnClickListener {
            finish()
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
        btnStartAnalysis.setOnClickListener { sendImageToAI() }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
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

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return
        layoutGuide.visibility = View.INVISIBLE
        btnTakePhoto.isEnabled = false
        pbScanning.visibility = View.VISIBLE
        findViewById<Button>(R.id.btnStainCameraBack).visibility = View.GONE

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
                findViewById<Button>(R.id.btnStainCameraBack).visibility = View.VISIBLE
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
        findViewById<Button>(R.id.btnStainCameraBack).visibility = View.VISIBLE
    }

    private fun resetToCameraState() {
        viewFinder.visibility = View.VISIBLE
        ivCapturedImage.visibility = View.GONE
        layoutGuide.visibility = View.VISIBLE
        btnTakePhoto.visibility = View.VISIBLE
        btnSelectPhoto.visibility = View.VISIBLE
        btnRetry.visibility = View.GONE
        btnStartAnalysis.visibility = View.GONE
        findViewById<Button>(R.id.btnStainCameraBack).visibility = View.VISIBLE
    }

    private fun sendImageToAI() {
        val drawable = ivCapturedImage.drawable
        val bitmap = (drawable as? BitmapDrawable)?.bitmap

        if (bitmap == null) {
            Toast.makeText(this, "이미지를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        pbScanning.visibility = View.VISIBLE
        btnStartAnalysis.isEnabled = false
        findViewById<Button>(R.id.btnStainCameraBack).visibility = View.GONE

        val file = File(cacheDir, "temp_stain_image.jpg")
        try {
            val fos = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
            fos.flush()
            fos.close()
        } catch (e: Exception) {
            e.printStackTrace()
            pbScanning.visibility = View.GONE
            btnStartAnalysis.isEnabled = true
            findViewById<Button>(R.id.btnStainCameraBack).visibility = View.VISIBLE
            Toast.makeText(this, "이미지 저장 실패", Toast.LENGTH_SHORT).show()
            return
        }

        val intent = Intent(this, StainResultActivity::class.java)
        intent.putExtra("stain_image_path", file.absolutePath)
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}