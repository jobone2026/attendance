package com.example.camera

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun FaceDetectionOverlay(
    modifier: Modifier = Modifier,
    detectionResult: FaceDetectionResult,
    isFaceMatched: Boolean,
    isLivenessPassed: Boolean
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

    val primaryColor by animateColorAsState(
        targetValue = when {
            isLivenessPassed && isFaceMatched -> Color(0xFF107C41) // Verified Green
            detectionResult.isDetected -> Color(0xFF00E5FF)       // Tech Cyan
            else -> Color(0xFF64B5F6)                              // Guide Blue
        },
        label = "overlay_color"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        val ovalWidth = canvasWidth * 0.65f * pulseScale
        val ovalHeight = canvasHeight * 0.72f * pulseScale
        val topLeftX = (canvasWidth - ovalWidth) / 2f
        val topLeftY = (canvasHeight - ovalHeight) / 2f

        // 1. Draw subtle darkened framing vignette outside the oval target
        drawRect(
            color = Color.Black.copy(alpha = 0.35f),
            size = size
        )

        // 2. Draw Target Reticle Oval
        drawOval(
            color = primaryColor,
            topLeft = Offset(topLeftX, topLeftY),
            size = Size(ovalWidth, ovalHeight),
            style = Stroke(
                width = 3.5.dp.toPx(),
                pathEffect = if (!isLivenessPassed) PathEffect.dashPathEffect(floatArrayOf(20f, 15f), 0f) else null
            )
        )

        // 3. Draw 4 Corner Guide Reticles
        val cornerLen = 28.dp.toPx()
        val cornerStroke = 4.5.dp.toPx()

        // Top-Left corner
        drawLine(primaryColor, Offset(topLeftX, topLeftY + cornerLen), Offset(topLeftX, topLeftY), cornerStroke)
        drawLine(primaryColor, Offset(topLeftX, topLeftY), Offset(topLeftX + cornerLen, topLeftY), cornerStroke)

        // Top-Right corner
        val rightX = topLeftX + ovalWidth
        drawLine(primaryColor, Offset(rightX - cornerLen, topLeftY), Offset(rightX, topLeftY), cornerStroke)
        drawLine(primaryColor, Offset(rightX, topLeftY), Offset(rightX, topLeftY + cornerLen), cornerStroke)

        // Bottom-Left corner
        val bottomY = topLeftY + ovalHeight
        drawLine(primaryColor, Offset(topLeftX, bottomY - cornerLen), Offset(topLeftX, bottomY), cornerStroke)
        drawLine(primaryColor, Offset(topLeftX, bottomY), Offset(topLeftX + cornerLen, bottomY), cornerStroke)

        // Bottom-Right corner
        drawLine(primaryColor, Offset(rightX - cornerLen, bottomY), Offset(rightX, bottomY), cornerStroke)
        drawLine(primaryColor, Offset(rightX, bottomY - cornerLen), Offset(rightX, bottomY), cornerStroke)

        // 4. Draw Detected Face Dynamic Bounding Box & Landmarks
        if (detectionResult.isDetected && detectionResult.rawFace != null) {
            val bounds = detectionResult.boundingBox
            val imgW = detectionResult.imageWidth.toFloat().coerceAtLeast(1f)
            val imgH = detectionResult.imageHeight.toFloat().coerceAtLeast(1f)

            // Function to map image coordinates to preview canvas coordinates (with front-camera mirror support)
            fun mapPoint(x: Float, y: Float): Offset {
                val relX = (x - bounds.left) / bounds.width().toFloat().coerceAtLeast(1f)
                val relY = (y - bounds.top) / bounds.height().toFloat().coerceAtLeast(1f)
                val mappedX = if (detectionResult.isFrontCamera) {
                    topLeftX + (1f - relX) * ovalWidth
                } else {
                    topLeftX + relX * ovalWidth
                }
                val mappedY = topLeftY + relY * ovalHeight
                return Offset(mappedX, mappedY)
            }

            // Draw Real-Time Detected Landmarks (Eyes, Nose, Mouth Corners, Cheeks)
            val leftEyePt = detectionResult.leftEyePosition?.let { mapPoint(it.x, it.y) }
            val rightEyePt = detectionResult.rightEyePosition?.let { mapPoint(it.x, it.y) }
            val nosePt = detectionResult.noseBasePosition?.let { mapPoint(it.x, it.y) }
            val mouthLPt = detectionResult.mouthLeftPosition?.let { mapPoint(it.x, it.y) }
            val mouthRPt = detectionResult.mouthRightPosition?.let { mapPoint(it.x, it.y) }
            val mouthBPt = detectionResult.mouthBottomPosition?.let { mapPoint(it.x, it.y) }

            // Connective facial triangulation mesh lines
            val meshColor = Color(0xFF00E5FF).copy(alpha = 0.35f)
            val meshStroke = 1.5.dp.toPx()

            if (leftEyePt != null && rightEyePt != null) {
                drawLine(meshColor, leftEyePt, rightEyePt, meshStroke)
            }
            if (leftEyePt != null && nosePt != null) {
                drawLine(meshColor, leftEyePt, nosePt, meshStroke)
            }
            if (rightEyePt != null && nosePt != null) {
                drawLine(meshColor, rightEyePt, nosePt, meshStroke)
            }
            if (nosePt != null && mouthLPt != null) {
                drawLine(meshColor, nosePt, mouthLPt, meshStroke)
            }
            if (nosePt != null && mouthRPt != null) {
                drawLine(meshColor, nosePt, mouthRPt, meshStroke)
            }
            if (mouthLPt != null && mouthRPt != null) {
                drawLine(meshColor, mouthLPt, mouthRPt, meshStroke)
            }
            if (mouthLPt != null && mouthBPt != null && mouthRPt != null) {
                drawLine(meshColor, mouthLPt, mouthBPt, meshStroke)
                drawLine(meshColor, mouthRPt, mouthBPt, meshStroke)
            }

            // Draw Landmark Highlight Nodes
            detectionResult.landmarks.forEach { lm ->
                val pt = mapPoint(lm.positionX, lm.positionY)
                val isEye = lm.type == LandmarkType.LEFT_EYE || lm.type == LandmarkType.RIGHT_EYE
                val isNose = lm.type == LandmarkType.NOSE_BASE
                val nodeColor = when {
                    isEye -> Color(0xFF76FF03) // Bright Green for Eyes
                    isNose -> Color(0xFFFFD600) // Amber for Nose
                    else -> Color(0xFF00E5FF)  // Cyan for Mouth & Cheeks
                }

                // Outer glow
                drawCircle(
                    color = nodeColor.copy(alpha = 0.8f),
                    radius = 4.dp.toPx(),
                    center = pt
                )
                // Inner core
                drawCircle(
                    color = Color.White,
                    radius = 2.dp.toPx(),
                    center = pt
                )
            }
        }
    }
}
