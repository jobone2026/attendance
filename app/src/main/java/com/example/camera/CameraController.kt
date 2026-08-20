package com.example.camera

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PointF
import android.graphics.RectF
import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceLandmark
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraController(
    private val context: Context,
    private val onFaceDetectedCallback: ((FaceDetectionResult, InputImage) -> Unit)? = null
) {
    companion object {
        private const val TAG = "CameraController"
    }

    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var imageAnalysis: ImageAnalysis? = null
    private var preview: Preview? = null

    var lensFacing: Int = CameraSelector.LENS_FACING_FRONT
        private set

    // Google ML Kit Face Detector with High-Accuracy Mode, Landmarks, and Classifications
    val faceDetector: FaceDetector by lazy {
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .setMinFaceSize(0.15f)
            .enableTracking()
            .build()
        FaceDetection.getClient(options)
    }

    private val _detectionResult = MutableStateFlow(FaceDetectionResult())
    val detectionResult: StateFlow<FaceDetectionResult> = _detectionResult.asStateFlow()

    private var isAnalyzing = false

    /**
     * Initializes CameraX and binds Preview and ImageAnalysis use cases to lifecycle.
     */
    @SuppressLint("UnsafeOptInUsageError")
    fun startCamera(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        onReady: (() -> Unit)? = null
    ) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()

                preview = Preview.Builder()
                    .build()
                    .also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }

                imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                    .build()
                    .also { analysis ->
                        analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                            processImageProxy(imageProxy)
                        }
                    }

                bindUseCases(lifecycleOwner)
                onReady?.invoke()
            } catch (e: Exception) {
                Log.e(TAG, "Camera initialization failed", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private fun bindUseCases(lifecycleOwner: LifecycleOwner) {
        val provider = cameraProvider ?: return
        try {
            provider.unbindAll()

            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(lensFacing)
                .build()

            val prev = preview ?: return
            val analysis = imageAnalysis ?: return

            camera = provider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                prev,
                analysis
            )
        } catch (e: Exception) {
            Log.e(TAG, "Use case binding failed", e)
        }
    }

    /**
     * Captures and converts each CameraX frame to InputImage, running ML Kit Face Detection.
     */
    @SuppressLint("UnsafeOptInUsageError")
    private fun processImageProxy(imageProxy: ImageProxy) {
        if (isAnalyzing) {
            imageProxy.close()
            return
        }

        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }

        isAnalyzing = true
        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
        val width = imageProxy.width
        val height = imageProxy.height

        val image = InputImage.fromMediaImage(mediaImage, rotationDegrees)

        faceDetector.process(image)
            .addOnSuccessListener { faces ->
                val primaryFace = faces.firstOrNull()
                val result = if (primaryFace != null) {
                    mapToFaceDetectionResult(
                        face = primaryFace,
                        imageWidth = width,
                        imageHeight = height,
                        rotationDegrees = rotationDegrees,
                        isFront = lensFacing == CameraSelector.LENS_FACING_FRONT
                    )
                } else {
                    FaceDetectionResult(
                        isDetected = false,
                        imageWidth = width,
                        imageHeight = height,
                        rotationDegrees = rotationDegrees,
                        isFrontCamera = lensFacing == CameraSelector.LENS_FACING_FRONT
                    )
                }

                _detectionResult.value = result
                onFaceDetectedCallback?.invoke(result, image)
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Face detection processing error", e)
            }
            .addOnCompleteListener {
                isAnalyzing = false
                imageProxy.close()
            }
    }

    /**
     * Extracts bounding box, landmarks (eyes, nose, mouth, cheeks, ears), and orientation.
     */
    private fun mapToFaceDetectionResult(
        face: Face,
        imageWidth: Int,
        imageHeight: Int,
        rotationDegrees: Int,
        isFront: Boolean
    ): FaceDetectionResult {
        val bounds = face.boundingBox
        val normBounds = RectF(
            bounds.left.toFloat() / imageWidth.coerceAtLeast(1),
            bounds.top.toFloat() / imageHeight.coerceAtLeast(1),
            bounds.right.toFloat() / imageWidth.coerceAtLeast(1),
            bounds.bottom.toFloat() / imageHeight.coerceAtLeast(1)
        )

        val landmarksList = mutableListOf<FaceLandmarkPoint>()

        fun extract(type: Int, landmarkType: LandmarkType): PointF? {
            val lm = face.getLandmark(type)
            return if (lm != null) {
                val pt = PointF(lm.position.x, lm.position.y)
                landmarksList.add(FaceLandmarkPoint(landmarkType, pt, lm.position.x, lm.position.y))
                pt
            } else null
        }

        val leftEye = extract(FaceLandmark.LEFT_EYE, LandmarkType.LEFT_EYE)
        val rightEye = extract(FaceLandmark.RIGHT_EYE, LandmarkType.RIGHT_EYE)
        val nose = extract(FaceLandmark.NOSE_BASE, LandmarkType.NOSE_BASE)
        val mouthL = extract(FaceLandmark.MOUTH_LEFT, LandmarkType.MOUTH_LEFT)
        val mouthR = extract(FaceLandmark.MOUTH_RIGHT, LandmarkType.MOUTH_RIGHT)
        val mouthB = extract(FaceLandmark.MOUTH_BOTTOM, LandmarkType.MOUTH_BOTTOM)
        extract(FaceLandmark.LEFT_CHEEK, LandmarkType.LEFT_CHEEK)
        extract(FaceLandmark.RIGHT_CHEEK, LandmarkType.RIGHT_CHEEK)
        extract(FaceLandmark.LEFT_EAR, LandmarkType.LEFT_EAR)
        extract(FaceLandmark.RIGHT_EAR, LandmarkType.RIGHT_EAR)

        return FaceDetectionResult(
            isDetected = true,
            rawFace = face,
            boundingBox = bounds,
            normalizedBoundingBox = normBounds,
            landmarks = landmarksList,
            leftEyePosition = leftEye,
            rightEyePosition = rightEye,
            noseBasePosition = nose,
            mouthLeftPosition = mouthL,
            mouthRightPosition = mouthR,
            mouthBottomPosition = mouthB,
            leftEyeOpenProbability = face.leftEyeOpenProbability,
            rightEyeOpenProbability = face.rightEyeOpenProbability,
            smilingProbability = face.smilingProbability,
            headEulerAngleX = face.headEulerAngleX,
            headEulerAngleY = face.headEulerAngleY,
            headEulerAngleZ = face.headEulerAngleZ,
            trackingId = face.trackingId,
            imageWidth = imageWidth,
            imageHeight = imageHeight,
            rotationDegrees = rotationDegrees,
            isFrontCamera = isFront
        )
    }

    /**
     * Toggles between front and back camera lenses.
     */
    fun toggleCamera(lifecycleOwner: LifecycleOwner) {
        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
            CameraSelector.LENS_FACING_BACK
        } else {
            CameraSelector.LENS_FACING_FRONT
        }
        bindUseCases(lifecycleOwner)
    }

    /**
     * Controls torch/flashlight when using back camera.
     */
    fun setTorch(enabled: Boolean) {
        camera?.cameraControl?.enableTorch(enabled)
    }

    fun unbind() {
        cameraProvider?.unbindAll()
    }

    fun release() {
        unbind()
        cameraExecutor.shutdown()
        faceDetector.close()
    }
}
