package com.woli.app.focus.hand

import android.content.Context
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong

/** Background front-camera analysis without Preview; on-device hand proximity only. */
class CameraHandApproachDetector {
    private val evaluator = HandApproachEvaluator()
    private val _isHandNear = MutableStateFlow(false)
    val isHandNear: StateFlow<Boolean> = _isHandNear.asStateFlow()

    private var cameraProvider: ProcessCameraProvider? = null
    private var landmarkerEngine: MediaPipeHandLandmarkerEngine? = null
    private var analysisExecutor: ExecutorService? = null
    private var running = false
    private val lastAnalyzedAtNanos = AtomicLong(0L)

    companion object {
        private const val MIN_ANALYSIS_INTERVAL_NANOS = 100_000_000L // ~10 fps
    }

    fun start(context: Context, lifecycleOwner: LifecycleOwner) {
        if (running || !CameraHandApproachAccess.isGranted(context)) return
        if (!HandApproachNativeSupport.isAvailable()) return

        val appContext = context.applicationContext
        val engine = runCatching { MediaPipeHandLandmarkerEngine(appContext) }.getOrNull() ?: return

        running = true
        evaluator.reset()
        _isHandNear.value = false
        landmarkerEngine = engine
        analysisExecutor = Executors.newSingleThreadExecutor()

        val future = ProcessCameraProvider.getInstance(appContext)
        future.addListener(
            {
                if (!running) return@addListener
                cameraProvider = future.get()
                bindAnalysis(lifecycleOwner)
            },
            ContextCompat.getMainExecutor(appContext),
        )
    }

    fun stop() {
        if (!running) return
        running = false
        runCatching { cameraProvider?.unbindAll() }
        runCatching { landmarkerEngine?.close() }
        analysisExecutor?.shutdownNow()
        landmarkerEngine = null
        cameraProvider = null
        analysisExecutor = null
        evaluator.reset()
        _isHandNear.value = false
    }

    private fun bindAnalysis(lifecycleOwner: LifecycleOwner) {
        val provider = cameraProvider ?: return
        val executor = analysisExecutor ?: return
        val analysis = ImageAnalysis.Builder()
            .setResolutionSelector(
                ResolutionSelector.Builder()
                    .setResolutionStrategy(
                        ResolutionStrategy(
                            Size(640, 480),
                            ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER,
                        ),
                    )
                    .build(),
            )
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
        analysis.setAnalyzer(executor, ::analyzeFrame)

        val selector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
            .build()

        provider.unbindAll()
        provider.bindToLifecycle(lifecycleOwner, selector, analysis)
    }

    private fun analyzeFrame(imageProxy: ImageProxy) {
        try {
            val now = System.nanoTime()
            val last = lastAnalyzedAtNanos.get()
            if (now - last < MIN_ANALYSIS_INTERVAL_NANOS) return
            if (!lastAnalyzedAtNanos.compareAndSet(last, now)) return

            val engine = landmarkerEngine ?: return
            val frame = engine.detect(imageProxy)
            val near = evaluator.evaluate(frame)
            if (near != _isHandNear.value) {
                _isHandNear.value = near
            }
        } catch (_: Exception) {
            // ponytail: skip bad frame; upgrade path is structured error logging.
        } finally {
            imageProxy.close()
        }
    }
}
