package com.example.ui.components

import android.annotation.SuppressLint
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.camera.FaceOverlayView
import com.example.camera.FaceDetectionResult
import com.example.domain.ml.LivenessChallenge
import com.example.domain.ml.LivenessState
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceLandmark
import java.util.concurrent.Executors

@SuppressLint("UnsafeOptInUsageError")
@Composable
fun CameraFacePreview(
    modifier: Modifier = Modifier,
    isFaceDetected: Boolean,
    detectedFace: Face? = null,
    detectionResult: FaceDetectionResult? = null,
    livenessState: LivenessState,
    similarityScore: Float,
    isFaceMatched: Boolean,
    isKannada: Boolean = false,
    onPermissionsGranted: (() -> Unit)? = null,
    onImageCaptured: (InputImage, Int, Int, Int) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    var hasCameraPermission by remember {
        mutableStateOf(isCameraPermissionGranted(context))
    }
    var hasLocationPermission by remember {
        mutableStateOf(isLocationPermissionGranted(context))
    }

    if (!hasCameraPermission) {
        CameraPermissionRequestView(
            modifier = modifier,
            isKannada = isKannada,
            onPermissionsGranted = {
                hasCameraPermission = true
                hasLocationPermission = true
                onPermissionsGranted?.invoke()
            }
        )
        return
    }

    // Pulsing scanning reticle animation
    val infiniteTransition = rememberInfiniteTransition(label = "scan_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val reticleColor by animateColorAsState(
        targetValue = when {
            livenessState.isLivenessPassed && isFaceMatched -> Color(0xFF107C41) // Green
            isFaceDetected -> Color(0xFFFFB951) // Amber
            else -> Color(0xFF64B5F6) // Light Blue
        },
        label = "reticle_color"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF0A1118))
    ) {
        // CameraX Preview View
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }

                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    try {
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.surfaceProvider = previewView.surfaceProvider
                        }

                        val imageAnalysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()

                        imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                            val mediaImage = imageProxy.image
                            if (mediaImage != null) {
                                val image = InputImage.fromMediaImage(
                                    mediaImage,
                                    imageProxy.imageInfo.rotationDegrees
                                )
                                onImageCaptured(
                                    image,
                                    imageProxy.imageInfo.rotationDegrees,
                                    imageProxy.width,
                                    imageProxy.height
                                )
                            }
                            imageProxy.close()
                        }

                        val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageAnalysis
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            }
        )

        // Real-Time Face Detection & Bounding Box Overlay
        FaceOverlayView(
            modifier = Modifier.fillMaxSize(),
            detectionResult = detectionResult ?: FaceDetectionResult(
                isDetected = isFaceDetected,
                rawFace = detectedFace,
                imageWidth = 480,
                imageHeight = 640
            ),
            isFaceMatched = isFaceMatched,
            isLivenessPassed = livenessState.isLivenessPassed,
            showBoundingBox = true,
            showMeshLines = true,
            showLandmarkPoints = true
        )

        // Top Status Badge: Biometric Similarity & Face Match Status
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp)
                .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(20.dp))
                .padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isFaceMatched) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = null,
                tint = if (isFaceMatched) Color(0xFF4CAF50) else Color(0xFFFFB951),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            val percent = (similarityScore * 100).toInt()
            val text = if (isFaceMatched) {
                if (isKannada) "ಮುಖ ದೃಢೀಕರಿಸಲಾಗಿದೆ ($percent%)" else "Face Match: $percent%"
            } else {
                if (isKannada) "ಮುಖ ಸ್ಕ್ಯಾನ್ ಆಗುತ್ತಿದೆ..." else "Scanning Face..."
            }
            Text(
                text = text,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        // Bottom Liveness Prompt Card
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp)
                .background(Color(0xFF0F1E2E).copy(alpha = 0.92f), RoundedCornerShape(16.dp))
                .border(1.dp, reticleColor.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val prompt = if (isKannada) livenessState.promptMessageKn else livenessState.promptMessage
            Text(
                text = prompt,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { livenessState.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = if (livenessState.isLivenessPassed) Color(0xFF107C41) else Color(0xFFFFB951),
                trackColor = Color.White.copy(alpha = 0.2f)
            )
        }
    }
}
