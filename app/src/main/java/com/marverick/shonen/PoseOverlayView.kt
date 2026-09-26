package com.marverick.shonen

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseLandmark

class PoseOverlayView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    private var pose: Pose? = null
    private var imageWidth = 1
    private var imageHeight = 1

    private val pointPaint = Paint().apply {
        color = Color.GREEN
        style = Paint.Style.FILL
    }

    private val linePaint = Paint().apply {
        color = Color.YELLOW
        style = Paint.Style.STROKE
        strokeWidth = 6f
    }

    fun updatePose(newPose: Pose, width: Int, height: Int) {
        pose = newPose
        imageWidth = width
        imageHeight = height
        postInvalidate()
    }

    private fun translateX(x: Float): Float = this.width - (x * this.width / imageWidth)
    private fun translateY(y: Float): Float = y * this.height / imageHeight

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val currentPose = pose ?: return

        for (landmark in currentPose.allPoseLandmarks) {
            canvas.drawCircle(translateX(landmark.position.x), translateY(landmark.position.y), 8f, pointPaint)
        }

        fun drawLine(a: Int, b: Int) {
            val start = currentPose.getPoseLandmark(a) ?: return
            val end = currentPose.getPoseLandmark(b) ?: return
            canvas.drawLine(
                translateX(start.position.x), translateY(start.position.y),
                translateX(end.position.x), translateY(end.position.y),
                linePaint
            )
        }

        drawLine(PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_ELBOW)
        drawLine(PoseLandmark.LEFT_ELBOW, PoseLandmark.LEFT_WRIST)
        drawLine(PoseLandmark.RIGHT_SHOULDER, PoseLandmark.RIGHT_ELBOW)
        drawLine(PoseLandmark.RIGHT_ELBOW, PoseLandmark.RIGHT_WRIST)
        drawLine(PoseLandmark.LEFT_SHOULDER, PoseLandmark.RIGHT_SHOULDER)
        drawLine(PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_HIP)
        drawLine(PoseLandmark.RIGHT_SHOULDER, PoseLandmark.RIGHT_HIP)
        drawLine(PoseLandmark.LEFT_HIP, PoseLandmark.RIGHT_HIP)
        drawLine(PoseLandmark.LEFT_HIP, PoseLandmark.LEFT_KNEE)
        drawLine(PoseLandmark.LEFT_KNEE, PoseLandmark.LEFT_ANKLE)
        drawLine(PoseLandmark.RIGHT_HIP, PoseLandmark.RIGHT_KNEE)
        drawLine(PoseLandmark.RIGHT_KNEE, PoseLandmark.RIGHT_ANKLE)
    }
}