package com.example.laundrycare_android

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
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
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraActivity : AppCompatActivity() {

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

    private var scanStep = 1
    private var clothBitmap: Bitmap? = null
    private var labelBitmap: Bitmap? = null
    private var savedClothFilePath: String = ""

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) startCamera()
        else Toast.makeText(this, "카메라 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
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

        cameraExecutor = Executors.newSingleThreadExecutor()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        btnSelectPhoto.setOnClickListener { getContent.launch("image/*") }
        btnTakePhoto.setOnClickListener { takePhoto() }
        btnRetry.setOnClickListener { resetToCameraState() }

        resetToCameraState()

        btnStartAnalysis.setOnClickListener {
            if (scanStep == 1) {
                clothBitmap?.let { bmp ->
                    val uniqueFileName = "cloth_${System.currentTimeMillis()}.jpg"
                    val file = File(cacheDir, uniqueFileName)
                    val fos = FileOutputStream(file)
                    bmp.compress(Bitmap.CompressFormat.JPEG, 100, fos)
                    fos.flush()
                    fos.close()
                    savedClothFilePath = file.absolutePath
                }
                scanStep = 2
                resetToCameraState()
            } else {
                sendImageToAI()
            }
        }
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

        imageCapture.takePicture(ContextCompat.getMainExecutor(this), object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                val buffer = image.planes[0].buffer
                val bytes = ByteArray(buffer.capacity())
                buffer.get(bytes)
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, null)
                if (scanStep == 1) clothBitmap = bitmap else labelBitmap = bitmap
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
        btnStartAnalysis.text = if (scanStep == 1) "다음: 라벨 촬영하기" else "AI 분석 시작"
    }

    private fun resetToCameraState() {
        viewFinder.visibility = View.VISIBLE
        ivCapturedImage.visibility = View.GONE
        layoutGuide.visibility = View.VISIBLE
        btnTakePhoto.visibility = View.VISIBLE
        btnSelectPhoto.visibility = View.VISIBLE
        btnRetry.visibility = View.GONE
        btnStartAnalysis.visibility = View.GONE
        val tvGuide = findViewById<TextView>(R.id.tvGuideMessage)
        tvGuide.text = if (scanStep == 1) "옷의 전체적인 형태가 보이게 촬영해주세요 (1/2)" else "옷 안쪽의 세탁 라벨을 촬영해주세요 (2/2)"
    }

    private fun sendImageToAI() {
        val bitmap = labelBitmap ?: return
        pbScanning.visibility = View.VISIBLE
        btnStartAnalysis.isEnabled = false
        btnRetry.isEnabled = false
        btnStartAnalysis.text = "AI 종합 분석 중..."

        val labelFile = File(cacheDir, "label_${System.currentTimeMillis()}.jpg")
        try {
            val fos = FileOutputStream(labelFile)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
            fos.flush()
            fos.close()
        } catch (e: Exception) { e.printStackTrace(); return }

        val clothFile = File(savedClothFilePath)
        if (!clothFile.exists()) {
            Toast.makeText(this, "옷 사진을 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
            pbScanning.visibility = View.GONE
            btnStartAnalysis.isEnabled = true
            btnRetry.isEnabled = true
            return
        }

        val client = OkHttpClient()
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("clothImage", "cloth_image.jpg", RequestBody.create("image/jpeg".toMediaTypeOrNull(), clothFile))
            .addFormDataPart("labelImage", "label_image.jpg", RequestBody.create("image/jpeg".toMediaTypeOrNull(), labelFile))
            .build()

        val request = Request.Builder()
            .url("http://34.64.101.110:3000/api/clothes/analyze")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    pbScanning.visibility = View.GONE
                    btnStartAnalysis.isEnabled = true
                    btnRetry.isEnabled = true
                    btnStartAnalysis.text = "AI 분석 시작"
                    Toast.makeText(this@CameraActivity, "서버 연결 실패", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onResponse(call: Call, response: Response) {
                val responseData = response.body?.string()
                runOnUiThread {
                    pbScanning.visibility = View.GONE
                    btnStartAnalysis.isEnabled = true
                    btnRetry.isEnabled = true
                    if (response.isSuccessful && responseData != null) {
                        val intent = Intent(this@CameraActivity, ResultActivity::class.java)
                        intent.putExtra("ai_json_data", responseData)
                        intent.putExtra("cloth_image_path", clothFile.absolutePath)
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(this@CameraActivity, "서버 에러: ${response.code}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    override fun onDestroy() { super.onDestroy(); cameraExecutor.shutdown() }
}