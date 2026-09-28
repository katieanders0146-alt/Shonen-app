package com.marverick.shonen

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
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
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseLandmark
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.atan2

class MainActivity : AppCompatActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var poseOverlay: PoseOverlayView
    private lateinit var repCountText: TextView
    private val cameraExecutor = Executors.newSingleThreadExecutor()

    private var exercise = "pushup"
    private var target = 20
    private var repCount = 0
    private var isContracted = false
    private var lastRepTime = 0L
    private var targetAnnounced = false

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

        exercise = intent.getStringExtra("exercise") ?: "pushup"
        target = intent.getIntExtra("target", 20)
        updateCounter()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun exerciseLabel(): String = when (exercise) {
        "situp" -> "Sit-ups"
        "squat" -> "Squats"
        else -> "Push-ups"
    }

    private fun updateCounter() {
        repCountText.text = "${exerciseLabel()}: $repCount / $target"
    }

    private fun onRepCounted() {
        updateCounter()
        if (repCount == target && !targetAnnounced) {
            targetAnnounced = true
            Toast.makeText(this, "${exerciseLabel()} target reached!", Toast.LENGTH_LONG).show()
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
                countReps(pose)
            }
            .addOnCompleteListener { imageProxy.close() }
    }

    private fun countReps(pose: Pose) {
        val angle: Float?
        val contractedBelow: Float
        val extendedAbove: Float

        when (exercise) {
            "squat" -> {
                angle = sideAngle(
                    pose,
                    PoseLandmark.LEFT_HIP, PoseLandmark.LEFT_KNEE, PoseLandmark.LEFT_ANKLE,
                    PoseLandmark.RIGHT_HIP, PoseLandmark.RIGHT_KNEE, PoseLandmark.RIGHT_ANKLE
                )
                contractedBelow = 110f
                extendedAbove = 160f
            }
            "situp" -> {
                angle = sideAngle(
                    pose,
                    PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_HIP, PoseLandmark.LEFT_KNEE,
                    PoseLandmark.RIGHT_SHOULDER, PoseLandmark.RIGHT_HIP, PoseLandmark.RIGHT_KNEE
                )
                contractedBelow = 80f
                extendedAbove = 120f
            }
            else -> {
                val s = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
                val e = pose.getPoseLandmark(PoseLandmark.LEFT_ELBOW)
                val w = pose.getPoseLandmark(PoseLandmark.LEFT_WRIST)
                angle = if (s != null && e != null && w != null) calculateAngle(s, e, w) else null
                contractedBelow = 90f
                extendedAbove = 160f
            }
        }

        if (angle == null) return

        val now = System.currentTimeMillis()
        if (angle < contractedBelow && !isContracted) {
            isContracted = true
        } else if (angle > extendedAbove && isContracted) {
            isContracted = false
            if (exercise == "pushup" || now - lastRepTime > 500) {
                lastRepTime = now
                repCount++
                runOnUiThread { onRepCounted() }
            }
        }
    }

    private fun sideScore(points: List<PoseLandmark?>): Float =
        if (points.any { it == null }) 0f else points.minOf { it!!.inFrameLikelihood }

    private fun sideAngle(
        pose: Pose,
        lA: Int, lB: Int, lC: Int,
        rA: Int, rB: Int, rC: Int
    ): Float? {
        val left = listOf(
            pose.getPoseLandmark(lA), pose.getPoseLandmark(lB), pose.getPoseLandmark(lC)
        )
        val right = listOf(
            pose.getPoseLandmark(rA), pose.getPoseLandmark(rB), pose.getPoseLandmark(rC)
        )
        val leftScore = sideScore(left)
        val rightScore = sideScore(right)
        if (maxOf(leftScore, rightScore) < 0.5f) return null
        val pts = if (leftScore >= rightScore) left else right
        return calculateAngle(pts[0]!!, pts[1]!!, pts[2]!!)
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