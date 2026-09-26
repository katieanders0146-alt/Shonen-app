package com.marverick.shonen

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseLandmark
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.atan2

class MainActivity : AppCompatActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var poseOverlay: PoseOverlayView
    private lateinit var repCountText: TextView
    private val cameraExecutor = Executors.newSingleThreadExecutor()

    private var repCount = 0
    private var isDown = false

    private val poseDetector by lazy {
        val options = PoseDetectorOptions.Builder()
            .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
            .build()
        PoseDetection.getClient(options)
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) startCamera()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        previewView = findViewById(R.id.previewView)
        poseOverlay = findViewById(R.id.poseOverlay)
        repCountText = findViewById(R.id.repCountText)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
            imageAnalysis.setAnalyzer(cameraExecutor) { processImage(it) }

            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                this, CameraSelector.DEFAULT_FRONT_CAMERA, preview, imageAnalysis
            )
        }, ContextCompat.getMainExecutor(this))
    }

    private fun processImage(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }
        val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        poseDetector.process(inputImage)
            .addOnSuccessListener { pose ->
                val rotated = imageProxy.imageInfo.rotationDegrees
                val w = if (rotated == 90 || rotated == 270) mediaImage.height else mediaImage.width
                val h = if (rotated == 90 || rotated == 270) mediaImage.width else mediaImage.height
                poseOverlay.updatePose(pose, w, h)
                countPushUpReps(pose)
            }
            .addOnCompleteListener { imageProxy.close() }
    }

    private fun countPushUpReps(pose: Pose) {
        val shoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
        val elbow = pose.getPoseLandmark(PoseLandmark.LEFT_ELBOW)
        val wrist = pose.getPoseLandmark(PoseLandmark.LEFT_WRIST)
        if (shoulder == null || elbow == null || wrist == null) return

        val angle = calculateAngle(shoulder, elbow, wrist)

        if (angle < 90f && !isDown) {
            isDown = true
        } else if (angle > 160f && isDown) {
            isDown = false
            repCount++
            runOnUiThread { repCountText.text = "Reps: $repCount" }
        }
    }

    private fun calculateAngle(a: PoseLandmark, b: PoseLandmark, c: PoseLandmark): Float {
        val radians = atan2(c.position.y - b.position.y, c.position.x - b.position.x) -
                atan2(a.position.y - b.position.y, a.position.x - b.position.x)
        var angle = abs(Math.toDegrees(radians.toDouble())).toFloat()
        if (angle > 180f) angle = 360f - angle
        return angle
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        poseDetector.close()
    }
}