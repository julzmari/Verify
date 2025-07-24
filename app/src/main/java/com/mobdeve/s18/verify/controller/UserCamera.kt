package com.mobdeve.s18.verify.controller

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.mobdeve.s18.verify.R
import com.mobdeve.s18.verify.app.VerifiApp
import com.mobdeve.s18.verify.model.UserEntry
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.*
import kotlin.Exception
import android.graphics.Matrix
import androidx.core.graphics.scale
import io.github.jan.supabase.storage.storage
import java.io.ByteArrayOutputStream
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class UserCamera : AppCompatActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var cameraExecutor: ExecutorService
    private var imageCapture: ImageCapture? = null
    private var isBackCamera = true
    private lateinit var currentTime: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_employee_cameraview)

        previewView = findViewById(R.id.previewView)
        cameraExecutor = Executors.newSingleThreadExecutor()

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(REQUIRED_PERMISSIONS)
        }

        val captureBtn = findViewById<View>(R.id.captureBtn)
        captureBtn.setOnClickListener {
            takePhoto()
        }

        val switchCamBtn = findViewById<View>(R.id.btnSwitchCam)
        switchCamBtn.setOnClickListener {
            switchCamera()
        }

        val backBtn = findViewById<View>(R.id.btnBack)
        backBtn.setOnClickListener {
            finish()
        }
    }

    private fun switchCamera() {
        isBackCamera = !isBackCamera
        startCamera()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Set 3:4 aspect ratio for preview
            val aspectRatio = AspectRatio.RATIO_4_3
            val preview = Preview.Builder()
                .setTargetAspectRatio(aspectRatio)
                .build()
                .also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

            // Configure image capture with 3:4 ratio and portrait orientation
            imageCapture = ImageCapture.Builder()
                .setTargetAspectRatio(aspectRatio)
                .setTargetRotation(previewView.display.rotation)
                .build()

            val cameraSelector = if (isBackCamera) {
                CameraSelector.DEFAULT_BACK_CAMERA
            } else {
                CameraSelector.DEFAULT_FRONT_CAMERA
            }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture
                )
            } catch (e: Exception) {
                Log.e("UserCamera", "Use case binding failed", e)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return

        imageCapture.takePicture(
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageCapturedCallback() {
                @RequiresApi(Build.VERSION_CODES.O)
                override fun onCaptureSuccess(imageProxy: ImageProxy) {
                    val bitmap = imageProxyToBitmap(imageProxy)
                    imageProxy.close()
                    showSubmissionSheet(bitmap)
                }

                override fun onError(exception: ImageCaptureException) {
                    super.onError(exception)
                    Toast.makeText(this@UserCamera, "Capture failed: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    private fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap {
        val buffer: ByteBuffer = imageProxy.planes[0].buffer
        val bytes = ByteArray(buffer.remaining()).also { buffer.get(it) }
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

        val rotationDegrees = imageProxy.imageInfo.rotationDegrees

        val matrix = Matrix().apply {
            postRotate(rotationDegrees.toFloat())

            if (!isBackCamera) {
                postScale(-1f, 1f, bitmap.width / 2f, bitmap.height / 2f)
            }
        }

        return Bitmap.createBitmap(
            bitmap,
            0, 0,
            bitmap.width, bitmap.height,
            matrix,
            true
        ).also {
            bitmap.recycle()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun showSubmissionSheet(bitmap: Bitmap) {
        val bottomSheet = PhotoSubmissionSheet(bitmap) { status, fullSizeBitmap ->
            submitPhoto(status, fullSizeBitmap)
        }
        bottomSheet.show(supportFragmentManager, "PhotoSubmissionSheet")
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun submitPhoto(status: String, bitmap: Bitmap) {
        val app = applicationContext as VerifiApp
        val supabase = app.supabase
        val userId = app.employeeID
        val companyId = app.companyID
        val username = app.username
        val location = app.location
        val latitude = app.latitude
        val longitude = app.longitude

        CoroutineScope(Dispatchers.IO).launch {
            try {

                val imageUrl = uploadImageToSupabase(bitmap, status, userId.toString())

                val newPhoto = UserEntry(
                    id = UUID.randomUUID().toString(),
                    username = username.toString(),
                    user_id = userId.toString(),
                    company_id = companyId.toString(),
                    image_url = imageUrl,
                    status = status,
                    location_name = location.toString(),
                    latitude = latitude?.toDouble() ?: 1.0,
                    longitude = longitude?.toDouble() ?: 1.0,
                    datetime = currentTime,
                )

                // Insert the photo entry into the database
                val response = supabase.postgrest["photos"]
                    .insert(newPhoto)

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@UserCamera, "Photo submitted successfully", Toast.LENGTH_SHORT).show()
                    finish() // Close the camera activity after submission
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@UserCamera, "Error submitting photo: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun uploadImageToSupabase(bitmap: Bitmap, status: String, userId: String): String {
        return try {
            val app = applicationContext as VerifiApp
            val supabase = app.supabase

            val currentLocalDateTime = LocalDateTime.now()

            val zonedDateTime = currentLocalDateTime.atZone(ZoneOffset.UTC)

            currentTime = zonedDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS"))

            val filename = "${userId}_${status}_${currentTime}.jpg"

            val resizedBitmap = bitmap.resize(1024) // Resize to 1024px width, you can adjust this

            val byteArray = resizedBitmap.toByteArray(quality = 70) // Convert to JPEG with 70% quality

            val uploadResponse = supabase.storage.from("submitted-photos").upload(
                path = filename,
                data = byteArray,
                upsert = false // Do not overwrite existing files
            )

            if (uploadResponse != null) {
                val fileUrl = supabase.storage.from("submitted-photos").publicUrl(filename)
                return fileUrl
            } else {
                throw Exception("Upload failed. No response received.")
            }

        } catch (e: Exception) {
            Log.e("UploadError", "Failed to upload image: ${e.message}")
            throw e
        }
    }

    fun Bitmap.resize(newWidth: Int): Bitmap {
        val ratio = newWidth.toFloat() / this.width
        val newHeight = (this.height * ratio).toInt()
        return this.scale(newWidth, newHeight, false)
    }

    fun Bitmap.toByteArray(quality: Int): ByteArray {
        val byteArrayOutputStream = ByteArrayOutputStream()
        this.compress(Bitmap.CompressFormat.JPEG, quality, byteArrayOutputStream)
        return byteArrayOutputStream.toByteArray()
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(this, "Camera permissions required", Toast.LENGTH_SHORT).show()
                finish()
            }
        }

    private fun allPermissionsGranted(): Boolean {
        return REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }
}