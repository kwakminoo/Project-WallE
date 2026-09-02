package com.woli.app.focus.hand

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.ImageProxy
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult

/** Isolated MediaPipe usage so unsupported ABIs never touch HandLandmarker. */
internal class MediaPipeHandLandmarkerEngine(context: Context) : AutoCloseable {
    private val handLandmarker: HandLandmarker

    init {
        val options = HandLandmarker.HandLandmarkerOptions.builder()
            .setBaseOptions(
                BaseOptions.builder()
                    .setModelAssetPath("hand_landmarker.task")
                    .build(),
            )
            .setRunningMode(RunningMode.IMAGE)
            .setNumHands(1)
            // ponytail: Jump2 전면 8MP·640p 분석에서 손 검출률 우선
            .setMinHandDetectionConfidence(0.4f)
            .setMinHandPresenceConfidence(0.4f)
            .setMinTrackingConfidence(0.4f)
            .build()
        handLandmarker = HandLandmarker.createFromOptions(context, options)
    }

    fun detect(imageProxy: ImageProxy): HandApproachFrame {
        val bitmap = imageProxy.toFrontCameraBitmap() ?: return emptyFrame()
        val mpImage = BitmapImageBuilder(bitmap).build()
        return frameFromResult(handLandmarker.detect(mpImage))
    }

    override fun close() {
        handLandmarker.close()
    }

    private fun frameFromResult(result: HandLandmarkerResult?): HandApproachFrame {
        val landmarks = result?.landmarks()?.firstOrNull().orEmpty()
        if (landmarks.isEmpty()) return emptyFrame()

        var minX = 1f
        var minY = 1f
        var maxX = 0f
        var maxY = 0f
        landmarks.forEach { point ->
            minX = minOf(minX, point.x())
            minY = minOf(minY, point.y())
            maxX = maxOf(maxX, point.x())
            maxY = maxOf(maxY, point.y())
        }
        return HandApproachFrame.fromBoundingBox(
            handDetected = true,
            minX = minX,
            minY = minY,
            maxX = maxX,
            maxY = maxY,
        )
    }

    private fun emptyFrame() = HandApproachFrame(
        handDetected = false,
        handAreaRatio = 0f,
        overlapsCenter = false,
    )
}

/** MediaPipe 공식 CameraX 샘플과 동일: RGBA → 회전 → 전면 미러. */
private fun ImageProxy.toFrontCameraBitmap(): Bitmap? {
    if (planes.isEmpty()) return null
    val bitmapBuffer = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    planes[0].buffer.rewind()
    bitmapBuffer.copyPixelsFromBuffer(planes[0].buffer)

    val matrix = Matrix().apply {
        postRotate(imageInfo.rotationDegrees.toFloat())
        postScale(-1f, 1f, width.toFloat(), height.toFloat())
    }
    return Bitmap.createBitmap(bitmapBuffer, 0, 0, width, height, matrix, true)
}
