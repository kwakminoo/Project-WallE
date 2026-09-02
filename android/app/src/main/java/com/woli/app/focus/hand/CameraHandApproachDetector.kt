package com.woli.app.focus.hand

import android.content.Context
import android.os.Handler
import android.os.Looper
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

    private val _isDetectionRunning = MutableStateFlow(false)
    val isDetectionRunning: StateFlow<Boolean> = _isDetectionRunning.asStateFlow()

    private var appContext: Context? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var landmarkerEngine: MediaPipeHandLandmarkerEngine? = null
    private var analysisExecutor: ExecutorService? = null
    private var running = false
    private val frameWatchdog = Handler(Looper.getMainLooper())
    private val lastAnalyzedAtNanos = AtomicLong(0L)
    private val _framesAnalyzed = MutableStateFlow(0L)
    val framesAnalyzed: StateFlow<Long> = _framesAnalyzed.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    companion object {
        private const val MIN_ANALYSIS_INTERVAL_NANOS = 100_000_000L // ~10 fps
        private const val FRAME_WATCHDOG_MS = 8_000L
    }

    fun start(context: Context, lifecycleOwner: LifecycleOwner) {
        if (!CameraHandApproachAccess.isGranted(context)) {
            _lastError.value = "카메라 권한이 없습니다"
            return
        }
        if (!HandApproachNativeSupport.isAvailable()) {
            _lastError.value = "이 기기에서 MediaPipe를 사용할 수 없습니다"
            return
        }
        if (running && _isDetectionRunning.value) return
        if (running) stop()

        val applicationContext = context.applicationContext
        appContext = applicationContext
        _lastError.value = null
        val engine = runCatching { MediaPipeHandLandmarkerEngine(applicationContext) }
            .getOrElse { error ->
                _lastError.value = error.message ?: "MediaPipe 엔진 초기화 실패"
                return
            }

        running = true
        _isDetectionRunning.value = true
        evaluator.reset()
        _isHandNear.value = false
        _framesAnalyzed.value = 0L
        landmarkerEngine = engine
        analysisExecutor = Executors.newSingleThreadExecutor()

        val future = ProcessCameraProvider.getInstance(applicationContext)
        future.addListener(
            {
                if (!running) return@addListener
                runCatching {
                    cameraProvider = future.get()
                    bindAnalysis(lifecycleOwner)
                    scheduleFrameWatchdog()
                }.onFailure { error ->
                    _lastError.value = error.message ?: "카메라 바인딩 실패"
                    stop()
                }
            },
            ContextCompat.getMainExecutor(applicationContext),
        )
    }

    fun stop() {
        if (!running) return
        running = false
        frameWatchdog.removeCallbacksAndMessages(null)
        _isDetectionRunning.value = false
        runCatching { cameraProvider?.unbindAll() }
        runCatching { landmarkerEngine?.close() }
        analysisExecutor?.shutdownNow()
        landmarkerEngine = null
        cameraProvider = null
        analysisExecutor = null
        appContext = null
        evaluator.reset()
        _isHandNear.value = false
        _framesAnalyzed.value = 0L
    }

    private fun bindAnalysis(lifecycleOwner: LifecycleOwner) {
        val provider = cameraProvider ?: return
        val executor = analysisExecutor ?: return
        val context = appContext ?: return
        val displayRotation = ContextCompat.getDisplayOrDefault(context).rotation
        val analysis = ImageAnalysis.Builder()
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .setTargetRotation(displayRotation)
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

    private fun scheduleFrameWatchdog() {
        frameWatchdog.removeCallbacksAndMessages(null)
        frameWatchdog.postDelayed({
            if (running && _framesAnalyzed.value == 0L) {
                _lastError.value = "카메라 프레임 수신 타임아웃. 다른 앱이 카메라를 쓰는지 확인해 주세요."
            }
        }, FRAME_WATCHDOG_MS)
    }

    private fun analyzeFrame(imageProxy: ImageProxy) {
        try {
            val now = System.nanoTime()
            val last = lastAnalyzedAtNanos.get()
            if (now - last < MIN_ANALYSIS_INTERVAL_NANOS) return
            if (!lastAnalyzedAtNanos.compareAndSet(last, now)) return

            val engine = landmarkerEngine ?: return
            val frame = engine.detect(imageProxy)
            _framesAnalyzed.value += 1L
            val near = evaluator.evaluate(frame)
            if (near != _isHandNear.value) {
                _isHandNear.value = near
            }
        } catch (error: Exception) {
            _lastError.value = error.message ?: "프레임 분석 실패"
        } finally {
            imageProxy.close()
        }
    }
}
