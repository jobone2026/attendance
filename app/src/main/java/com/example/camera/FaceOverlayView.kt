package com.example.camera

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * FaceOverlayView Composable:
 * Draws bounding box, tracking brackets, landmark points (eyes, nose, mouth corners, cheeks),
 * and facial mesh lines on top of the CameraX preview based on Google ML Kit Face Detection results.
 */
@Composable
fun FaceOverlayView(
    modifier: Modifier = Modifier,
    detectionResult: FaceDetectionResult,
    isFaceMatched: Boolean = false,
    isLivenessPassed: Boolean = false,
    showMeshLines: Boolean = true,
    showLandmarkPoints: Boolean = true,
    showBoundingBox: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_trans")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val primaryThemeColor by animateColorAsState(
        targetValue = when {
            isLivenessPassed && isFaceMatched -> Color(0xFF107C41) // Success / Verified Green
            detectionResult.isDetected -> Color(0xFF00E5FF)       // Active Face Detection Cyan
            else -> Color(0xFF64B5F6)                              // Idle / Search Blue
        },
        label = "theme_color"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        val ovalWidth = canvasWidth * 0.65f * pulseScale
        val ovalHeight = canvasHeight * 0.72f * pulseScale
        val guideTopLeftX = (canvasWidth - ovalWidth) / 2f
        val guideTopLeftY = (canvasHeight - ovalHeight) / 2f

        // 1. Target Face Positioning Oval (Guided Alignment)
        drawOval(
            color = primaryThemeColor.copy(alpha = 0.85f),
            topLeft = Offset(guideTopLeftX, guideTopLeftY),
            size = Size(ovalWidth, ovalHeight),
            style = Stroke(
                width = 3.5.dp.toPx(),
                pathEffect = if (!isLivenessPassed) PathEffect.dashPathEffect(floatArrayOf(22f, 14f), 0f) else null
            )
        )

        // 2. Corner Bracket Reticles on Guide Frame
        val cornerLength = 26.dp.toPx()
        val cornerStroke = 4.dp.toPx()
        val rightX = guideTopLeftX + ovalWidth
        val bottomY = guideTopLeftY + ovalHeight

        // Top-Left
        drawLine(primaryThemeColor, Offset(guideTopLeftX, guideTopLeftY + cornerLength), Offset(guideTopLeftX, guideTopLeftY), cornerStroke)
        drawLine(primaryThemeColor, Offset(guideTopLeftX, guideTopLeftY), Offset(guideTopLeftX + cornerLength, guideTopLeftY), cornerStroke)
        // Top-Right
        drawLine(primaryThemeColor, Offset(rightX - cornerLength, guideTopLeftY), Offset(rightX, guideTopLeftY), cornerStroke)
        drawLine(primaryThemeColor, Offset(rightX, guideTopLeftY), Offset(rightX, guideTopLeftY + cornerLength), cornerStroke)
        // Bottom-Left
        drawLine(primaryThemeColor, Offset(guideTopLeftX, bottomY - cornerLength), Offset(guideTopLeftX, bottomY), cornerStroke)
        drawLine(primaryThemeColor, Offset(guideTopLeftX, bottomY), Offset(guideTopLeftX + cornerLength, bottomY), cornerStroke)
        // Bottom-Right
        drawLine(primaryThemeColor, Offset(rightX - cornerLength, bottomY), Offset(rightX, bottomY), cornerStroke)
        drawLine(primaryThemeColor, Offset(rightX, bottomY - cornerLength), Offset(rightX, bottomY), cornerStroke)

        // 3. Render ML Kit Face Detection Bounding Box & Landmarks
        if (detectionResult.isDetected && detectionResult.rawFace != null) {
            val bounds = detectionResult.boundingBox
            val faceW = bounds.width().toFloat().coerceAtLeast(1f)
            val faceH = bounds.height().toFloat().coerceAtLeast(1f)

            // Function to map image coordinates to preview canvas coordinates (with front-camera mirror support)
            fun mapPoint(x: Float, y: Float): Offset {
                val relX = (x - bounds.left) / faceW
                val relY = (y - bounds.top) / faceH
                val mappedX = if (detectionResult.isFrontCamera) {
                    guideTopLeftX + (1f - relX) * ovalWidth
                } else {
                    guideTopLeftX + relX * ovalWidth
                }
                val mappedY = guideTopLeftY + relY * ovalHeight
                return Offset(mappedX, mappedY)
            }

            // A. Draw Face Bounding Box
            if (showBoundingBox) {
                val boxTopLeft = mapPoint(bounds.left.toFloat(), bounds.top.toFloat())
                val boxBottomRight = mapPoint(bounds.right.toFloat(), bounds.bottom.toFloat())
                val boxLeft = minOf(boxTopLeft.x, boxBottomRight.x)
                val boxTop = minOf(boxTopLeft.y, boxBottomRight.y)
                val boxW = kotlin.math.abs(boxBottomRight.x - boxTopLeft.x).coerceAtLeast(ovalWidth * 0.8f)
                val boxH = kotlin.math.abs(boxBottomRight.y - boxTopLeft.y).coerceAtLeast(ovalHeight * 0.8f)

                drawRoundRect(
                    color = primaryThemeColor.copy(alpha = 0.5f),
                    topLeft = Offset(boxLeft, boxTop),
                    size = Size(boxW, boxH),
                    cornerRadius = CornerRadius(16.dp.toPx()),
                    style = Stroke(
                        width = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                    )
                )
            }

            // B. Extract Landmark Points
            val leftEyePt = detectionResult.leftEyePosition?.let { mapPoint(it.x, it.y) }
            val rightEyePt = detectionResult.rightEyePosition?.let { mapPoint(it.x, it.y) }
            val nosePt = detectionResult.noseBasePosition?.let { mapPoint(it.x, it.y) }
            val mouthLPt = detectionResult.mouthLeftPosition?.let { mapPoint(it.x, it.y) }
            val mouthRPt = detectionResult.mouthRightPosition?.let { mapPoint(it.x, it.y) }
            val mouthBPt = detectionResult.mouthBottomPosition?.let { mapPoint(it.x, it.y) }

            // C. Draw Real-time Facial Mesh Lines
            if (showMeshLines) {
                val meshColor = Color(0xFF00E5FF).copy(alpha = 0.45f)
                val meshStroke = 1.5.dp.toPx()

                if (leftEyePt != null && rightEyePt != null) drawLine(meshColor, leftEyePt, rightEyePt, meshStroke)
                if (leftEyePt != null && nosePt != null) drawLine(meshColor, leftEyePt, nosePt, meshStroke)
                if (rightEyePt != null && nosePt != null) drawLine(meshColor, rightEyePt, nosePt, meshStroke)
                if (nosePt != null && mouthLPt != null) drawLine(meshColor, nosePt, mouthLPt, meshStroke)
                if (nosePt != null && mouthRPt != null) drawLine(meshColor, nosePt, mouthRPt, meshStroke)
                if (mouthLPt != null && mouthRPt != null) drawLine(meshColor, mouthLPt, mouthRPt, meshStroke)
                if (mouthLPt != null && mouthBPt != null) drawLine(meshColor, mouthLPt, mouthBPt, meshStroke)
                if (mouthRPt != null && mouthBPt != null) drawLine(meshColor, mouthRPt, mouthBPt, meshStroke)
            }

            // D. Draw Landmark Indicator Nodes with Glow
            if (showLandmarkPoints) {
                detectionResult.landmarks.forEach { lm ->
                    val pt = mapPoint(lm.positionX, lm.positionY)
                    val isEye = lm.type == LandmarkType.LEFT_EYE || lm.type == LandmarkType.RIGHT_EYE
                    val isNose = lm.type == LandmarkType.NOSE_BASE
                    val nodeColor = when {
                        isEye -> Color(0xFF76FF03)   // Eyes: Vivid Green
                        isNose -> Color(0xFFFFD600)  // Nose: Bright Amber
                        else -> Color(0xFF00E5FF)    // Mouth & Cheeks: Cyan
                    }

                    // Outer Glow
                    drawCircle(
                        color = nodeColor.copy(alpha = 0.85f),
                        radius = 4.dp.toPx(),
                        center = pt
                    )
                    // Inner Core
                    drawCircle(
                        color = Color.White,
                        radius = 2.dp.toPx(),
                        center = pt
                    )
                }
            }
        }
    }
}
