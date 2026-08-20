package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.AppScreen
import com.example.ui.AttendanceUiState
import com.example.ui.MainViewModel
import com.example.ui.components.CameraFacePreview

@Composable
fun FaceEnrollmentScreen(
    viewModel: MainViewModel,
    state: AttendanceUiState
) {
    val isKn = state.isKannadaLanguage
    val currentStep = state.enrollmentStep
    val samples = state.enrollmentSamples
    val isFaceReady = state.isFaceDetected && state.livenessState.isFaceCentered

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top App Bar with back button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { viewModel.navigateTo(AppScreen.ATTENDANCE) },
                modifier = Modifier.testTag("back_from_enrollment_btn")
            ) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = if (isKn) "ಮುಖ ನೋಂದಣಿ" else "Face Biometric Enrollment",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isKn) "ಮಾದರಿ $currentStep / 3" else "Sample $currentStep of 3",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Step Indicator Dots
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            for (i in 1..3) {
                val isCompleted = i < currentStep
                val isCurrent = i == currentStep
                val dotColor = when {
                    isCompleted -> Color(0xFF107C41)
                    isCurrent -> MaterialTheme.colorScheme.primary
                    else -> Color.LightGray
                }
                Box(
                    modifier = Modifier
                        .padding(horizontal = 6.dp)
                        .size(if (isCurrent) 14.dp else 10.dp)
                        .clip(CircleShape)
                        .background(dotColor)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Instructions banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = when (currentStep) {
                        1 -> Icons.Default.Face
                        2 -> Icons.Default.TurnLeft
                        3 -> Icons.Default.TurnRight
                        else -> Icons.Default.Check
                    },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = if (isKn) state.enrollmentStatusMessageKn else state.enrollmentStatusMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Camera Preview
        CameraFacePreview(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            isFaceDetected = state.isFaceDetected,
            detectedFace = state.detectedFace,
            detectionResult = state.detectionResult,
            livenessState = state.livenessState,
            similarityScore = state.faceSimilarityScore,
            isFaceMatched = true,
            isKannada = isKn,
            onPermissionsGranted = {
                viewModel.refreshLocation()
            },
            onImageCaptured = { img, rot, w, h ->
                viewModel.processCameraImage(img, rot, w, h)
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Capture Button
        Button(
            onClick = { viewModel.captureEnrollmentSample() },
            enabled = isFaceReady && !state.isSubmitting,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("capture_face_sample_btn"),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F3A5D))
        ) {
            if (state.isSubmitting) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            } else {
                Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                val btnText = if (currentStep == 3) {
                    if (isKn) "ಅಂತಿಮ ಮಾದರಿ ಸೆರೆಹಿಡಿಯಿರಿ & ಉಳಿಸಿ" else "Capture & Save Master Embedding"
                } else {
                    if (isKn) "ಮಾದರಿ $currentStep ಸೆರೆಹಿಡಿಯಿರಿ" else "Capture Sample $currentStep"
                }
                Text(
                    text = btnText,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
