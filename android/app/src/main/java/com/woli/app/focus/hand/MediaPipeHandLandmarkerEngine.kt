package com.woli.app.focus.hand

import android.content.Context
import androidx.camera.core.ImageProxy
import com.google.mediapipe.framework.image.MediaImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.ImageProcessingOptions
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
            .build()
        handLandmarker = HandLandmarker.createFromOptions(context, options)
    }

    fun detect(imageProxy: ImageProxy): HandApproachFrame {
        val mediaImage = imageProxy.image ?: return emptyFrame()
        val mpImage = MediaImageBuilder(mediaImage).build()
        val processingOptions = ImageProcessingOptions.builder()
            .setRotationDegrees(imageProxy.imageInfo.rotationDegrees)
            .build()
        return frameFromResult(handLandmarker.detect(mpImage, processingOptions))
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
