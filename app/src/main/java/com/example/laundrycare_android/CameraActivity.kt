package com.example.laundrycare_android

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
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
    private lateinit var btnAddLabel: Button
    // 🌟 추가된 뒤로가기 버튼 변수
    private lateinit var btnCameraBack: Button

    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService

    private var scanStep = 1
    private var clothBitmap: Bitmap? = null
    private var currentCapturedBitmap: Bitmap? = null

    private val labelBitmaps = mutableListOf<Bitmap>()
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
        btnAddLabel = findViewById(R.id.btnAddLabel)

        // 🌟 뒤로가기 버튼 클릭 이벤트 연결
        btnCameraBack = findViewById(R.id.btnCameraBack)
        btnCameraBack.setOnClickListener { finish() }

        cameraExecutor = Executors.newSingleThreadExecutor()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        btnSelectPhoto.setOnClickListener { getContent.launch("image/*") }
        btnTakePhoto.setOnClickListener { takePhoto() }

        btnRetry.setOnClickListener {
            currentCapturedBitmap = null
            resetToCameraState()
        }

        btnAddLabel.setOnClickListener {
            currentCapturedBitmap?.let { labelBitmaps.add(it) }
            currentCapturedBitmap = null
            resetToCameraState()
            Toast.makeText(this, "라벨이 추가되었습니다. 계속 촬영해주세요.", Toast.LENGTH_SHORT).show()
        }

        resetToCameraState()

        btnStartAnalysis.setOnClickListener {
            if (scanStep == 1) {
                clothBitmap = currentCapturedBitmap
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
                currentCapturedBitmap = null
                resetToCameraState()
            } else {
                currentCapturedBitmap?.let { labelBitmaps.add(it) }
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
        // 사진 찍을 때 뒤로가기 버튼 숨기기
        btnCameraBack.visibility = View.GONE

        imageCapture.takePicture(ContextCompat.getMainExecutor(this), object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                val buffer = image.planes[0].buffer
                val bytes = ByteArray(buffer.capacity())
                buffer.get(bytes)
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, null)

                // 🌟 가로 사진 회전 처리 (90도 회전)
                val matrix = Matrix()
                matrix.postRotate(90f)
                val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                currentCapturedBitmap = rotatedBitmap

                runOnUiThread {
                    ivCapturedImage.setImageBitmap(rotatedBitmap)
                    pbScanning.visibility = View.GONE
                    btnTakePhoto.isEnabled = true
                    showCapturedState()
                }
                image.close()
            }
            override fun onError(exc: ImageCaptureException) {
                runOnUiThread {
                    pbScanning.visibility = View.GONE
                    btnTakePhoto.isEnabled = true
                    layoutGuide.visibility = View.VISIBLE
                    btnCameraBack.visibility = View.VISIBLE
                    Toast.makeText(baseContext, "사진 촬영 실패", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    private fun showCapturedState() {
        viewFinder.visibility = View.INVISIBLE
        ivCapturedImage.visibility = View.VISIBLE
        layoutGuide.visibility = View.INVISIBLE
        // 사진 확인 화면에서도 뒤로가기 유지
        btnCameraBack.visibility = View.VISIBLE

        btnTakePhoto.visibility = View.GONE
        btnSelectPhoto.visibility = View.GONE

        btnRetry.visibility = View.VISIBLE
        btnStartAnalysis.visibility = View.VISIBLE

        if (scanStep == 1) {
            btnAddLabel.visibility = View.GONE
            btnStartAnalysis.text = "다음: 라벨 촬영하기"
        } else {
            if (labelBitmaps.size < 4) {
                btnAddLabel.visibility = View.VISIBLE
                btnStartAnalysis.text = "총 ${labelBitmaps.size + 1}장으로 분석 시작"
            } else {
                btnAddLabel.visibility = View.GONE
                btnStartAnalysis.text = "최대 5장 촬영 완료 - 분석 시작"
                Toast.makeText(this, "최대 5장까지 촬영할 수 있습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun resetToCameraState() {
        viewFinder.visibility = View.VISIBLE
        ivCapturedImage.visibility = View.GONE
        layoutGuide.visibility = View.VISIBLE
        btnCameraBack.visibility = View.VISIBLE

        btnTakePhoto.visibility = View.VISIBLE
        btnSelectPhoto.visibility = View.VISIBLE

        btnRetry.visibility = View.GONE
        btnAddLabel.visibility = View.GONE
        btnStartAnalysis.visibility = View.GONE

        val tvGuide = findViewById<TextView>(R.id.tvGuideMessage)
        if (scanStep == 1) {
            tvGuide.text = "옷의 전체적인 형태가 보이게 촬영해주세요 (1/2)"
        } else {
            tvGuide.text = "라벨의 모든 면을 촬영해주세요\n(최대 5장 / 현재 ${labelBitmaps.size}장 보관 중)"
            if(labelBitmaps.isNotEmpty()) btnSelectPhoto.visibility = View.GONE
        }
    }

    private fun sendImageToAI() {
        if (labelBitmaps.isEmpty()) return

        pbScanning.visibility = View.VISIBLE
        btnStartAnalysis.isEnabled = false
        btnRetry.isEnabled = false
        btnAddLabel.isEnabled = false
        btnCameraBack.visibility = View.GONE
        btnStartAnalysis.text = "AI 종합 분석 중..."

        val clothFile = File(savedClothFilePath)
        if (!clothFile.exists()) {
            showRetryDialog("옷 사진을 찾을 수 없습니다.")
            return
        }

        val client = OkHttpClient()
        val requestBodyBuilder = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("clothImage", "cloth_image.jpg", RequestBody.create("image/jpeg".toMediaTypeOrNull(), clothFile))

        labelBitmaps.forEachIndexed { index, bitmap ->
            val labelFile = File(cacheDir, "label_${System.currentTimeMillis()}_$index.jpg")
            try {
                val fos = FileOutputStream(labelFile)
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
                fos.flush()
                fos.close()
                requestBodyBuilder.addFormDataPart("labelImage", "label_image_$index.jpg", RequestBody.create("image/jpeg".toMediaTypeOrNull(), labelFile))
            } catch (e: Exception) { e.printStackTrace() }
        }

        val request = Request.Builder()
            .url("http://34.64.101.110:3000/api/clothes/analyze")
            .post(requestBodyBuilder.build())
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread { showRetryDialog("서버에 연결할 수 없습니다.") }
            }
            override fun onResponse(call: Call, response: Response) {
                val responseData = response.body?.string()
                runOnUiThread {
                    pbScanning.visibility = View.GONE
                    if (response.isSuccessful && responseData != null) {
                        val intent = Intent(this@CameraActivity, ResultActivity::class.java)
                        intent.putExtra("ai_json_data", responseData)
                        intent.putExtra("cloth_image_path", clothFile.absolutePath)
                        startActivity(intent)
                        finish()
                    } else {
                        showRetryDialog("대상이 구겨져 있거나 손상되어 스캔이 어렵습니다.")
                    }
                }
            }
        })
    }

    private fun showRetryDialog(errorMessage: String) {
        pbScanning.visibility = View.GONE
        btnStartAnalysis.isEnabled = true
        btnRetry.isEnabled = true
        btnAddLabel.isEnabled = true
        btnCameraBack.visibility = View.VISIBLE

        AlertDialog.Builder(this)
            .setTitle("분석 실패")
            .setMessage("$errorMessage\n\n대상을 다시 명확하게 촬영해 주세요.")
            .setPositiveButton("처음부터 다시 스캔하기") { dialog, _ ->
                scanStep = 1
                clothBitmap = null
                currentCapturedBitmap = null
                labelBitmaps.clear()
                savedClothFilePath = ""
                resetToCameraState()
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}