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
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ScanActivity : AppCompatActivity() {

    private lateinit var scanViewFinder: PreviewView
    private lateinit var ivScanCaptured: ImageView
    private lateinit var btnScanCapture: Button
    private lateinit var btnScanSubmit: Button
    private lateinit var pbScanLoading: ProgressBar

    private lateinit var etScanMainCat: EditText
    private lateinit var etScanSubCat: EditText
    private lateinit var etScanColor: EditText
    private lateinit var etScanMaterial: EditText

    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService
    private var localImageFile: File? = null

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) startCamera()
        else Toast.makeText(this, "카메라 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan)

        // 🌟 핵심 해결 부분: Button에서 ImageView로 수정 완료!
        findViewById<ImageView>(R.id.btnScanBack).setOnClickListener {
            finish()
        }

        scanViewFinder = findViewById(R.id.scanViewFinder)
        ivScanCaptured = findViewById(R.id.ivScanCaptured)
        btnScanCapture = findViewById(R.id.btnScanCapture)
        btnScanSubmit = findViewById(R.id.btnScanSubmit)
        pbScanLoading = findViewById(R.id.pbScanLoading)

        etScanMainCat = findViewById(R.id.etScanMainCat)
        etScanSubCat = findViewById(R.id.etScanSubCat)
        etScanColor = findViewById(R.id.etScanColor)
        etScanMaterial = findViewById(R.id.etScanMaterial)

        cameraExecutor = Executors.newSingleThreadExecutor()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        btnScanCapture.setOnClickListener { takePhoto() }
        btnScanSubmit.setOnClickListener { uploadToFirebase() }
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

                ivScanCaptured.setImageBitmap(bitmap)
                scanViewFinder.visibility = View.INVISIBLE
                ivScanCaptured.visibility = View.VISIBLE
                btnScanCapture.visibility = View.GONE

                image.close()

                localImageFile = File(cacheDir, "scan_${System.currentTimeMillis()}.jpg")
                val fos = FileOutputStream(localImageFile)
                bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, fos)
                fos.flush()
                fos.close()
            }
            override fun onError(exc: ImageCaptureException) {
                Toast.makeText(baseContext, "사진 촬영 실패", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun uploadToFirebase() {
        val file = localImageFile
        if (file == null) {
            Toast.makeText(this, "옷 사진을 먼저 촬영해주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        val mainCat = etScanMainCat.text.toString().trim()
        val subCat = etScanSubCat.text.toString().trim()
        val color = etScanColor.text.toString().trim()
        val material = etScanMaterial.text.toString().trim()

        if (mainCat.isEmpty() || subCat.isEmpty() || color.isEmpty() || material.isEmpty()) {
            Toast.makeText(this, "모든 정보를 입력해주세요.", Toast.LENGTH_SHORT).show()
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