package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.AppScreen
import com.example.ui.AttendanceUiState
import com.example.ui.MainViewModel
import com.example.ui.components.CameraFacePreview
import java.text.SimpleDateFormat
import java.util.*

/**
 * Main AttendanceScreen:
 * Displays real-time geofence boundary telemetry, ML Kit biometric face verification,
 * and a Check-In / Check-Out button that strictly activates ONLY when both location
 * (inside school boundary with no mock GPS) and face biometric verification are successful.
 */
@Composable
fun AttendanceScreen(
    viewModel: MainViewModel,
    state: AttendanceUiState
) {
    val scrollState = rememberScrollState()
    val isKn = state.isKannadaLanguage
    val teacher = state.currentTeacher
    val school = state.currentSchool
    val todayRec = state.todayRecord
    val geofence = state.geofenceResult
    val liveness = state.livenessState

    val todayFormatted = remember {
        SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault()).format(Date())
    }

    // Strict Validation Conditions
    val isLocationValid = geofence != null && geofence.isInside && !geofence.isMockLocation
    val isFaceValid = state.isFaceMatched && liveness.isLivenessPassed
    val isMockDetected = geofence?.isMockLocation == true

    // Button Activation Criteria: strictly requires BOTH location and face verification success
    val canCheckIn = isLocationValid && isFaceValid && todayRec?.checkInTime == null
    val canCheckOut = isLocationValid && isFaceValid && todayRec?.checkInTime != null && todayRec.checkOutTime == null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- 1. Teacher & School Header Profile Card ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("attendance_header_card"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.75f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.School,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (isKn) (teacher?.nameKn?.ifBlank { teacher.name } ?: "ಶಿಕ್ಷಕರು") else (teacher?.name ?: "Teacher"),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = if (isKn) (school?.nameKn?.ifBlank { school.name } ?: "ಸರ್ಕಾರಿ ಶಾಲೆ") else (school?.name ?: "Govt School"),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                                maxLines = 1
                            )
                        }
                    }

                    // Language toggle button
                    FilledTonalButton(
                        onClick = { viewModel.toggleLanguage() },
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("toggle_language_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isKn) "English" else "ಕನ್ನಡ",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f))
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = todayFormatted,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )

                    // Offline Sync Status Pill
                    Surface(
                        onClick = { viewModel.syncOfflineRecords() },
                        shape = RoundedCornerShape(8.dp),
                        color = if (state.isSyncing)
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        else if (state.unsyncedCount > 0)
                            Color(0xFFE65100).copy(alpha = 0.15f)
                        else
                            Color(0xFF107C41).copy(alpha = 0.15f),
                        modifier = Modifier.testTag("sync_status_badge")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            if (state.isSyncing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(10.dp),
                                    strokeWidth = 1.5.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isKn) "ಸಿಂಕ್ ಆಗುತ್ತಿದೆ..." else "Syncing...",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            } else if (state.unsyncedCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFE65100))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isKn) "${state.unsyncedCount} ಕಾಯುತ್ತಿದೆ" else "${state.unsyncedCount} Pending Sync",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFE65100),
                                    fontWeight = FontWeight.Bold
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF107C41))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isKn) "ಆಫ್‌ಲೈನ್ ಸಕ್ರಿಯ (ಸಿಂಕ್ ಆಗಿದೆ)" else "Offline-Ready",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF107C41),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Check if teacher needs to enroll face first
        if (teacher != null && !teacher.isFaceEnrolled) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .testTag("enroll_face_warning_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.FaceRetouchingNatural,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isKn) "ಮುಖ ನೋಂದಣಿ ಅಗತ್ಯವಿದೆ" else "Biometric Enrollment Required",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = if (isKn) "ಹಾಜರಾತಿ ಗುರುತಿಸಲು ಒಮ್ಮೆ ಮುಖ ನೋಂದಣಿ ಮಾಡಿ" else "Register 3 face angles for high-speed offline verification",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f)
                        )
                    }
                    Button(
                        onClick = { viewModel.navigateTo(AppScreen.ENROLL_FACE) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("enroll_face_action_btn")
                    ) {
                        Text(if (isKn) "ನೋಂದಣಿ" else "Enroll")
                    }
                }
            }
        }

        // --- 2. Real-Time Geofence Telemetry Card ---
        RealtimeGeofenceCard(
            geofence = geofence,
            school = school,
            isKannada = isKn,
            onRefreshLocation = { viewModel.refreshLocation() },
            onPresetSelected = { inside, mock -> viewModel.setLocationPreset(inside, mock) }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // --- 3. Live ML Camera Biometric Scanning View ---
        CameraFacePreview(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp),
            isFaceDetected = state.isFaceDetected,
            detectedFace = state.detectedFace,
            detectionResult = state.detectionResult,
            livenessState = state.livenessState,
            similarityScore = state.faceSimilarityScore,
            isFaceMatched = state.isFaceMatched,
            isKannada = isKn,
            onPermissionsGranted = {
                viewModel.refreshLocation()
            },
            onImageCaptured = { img, rot, w, h ->
                viewModel.processCameraImage(img, rot, w, h)
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // --- 4. Dual-Lock Verification Badges (Location + Biometrics + Time) ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("verification_status_row"),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Geofence / Location Status
            ValidationPill(
                modifier = Modifier.weight(1f),
                title = if (isKn) "1. ಜಿಯೋಫೆನ್ಸ್" else "1. Geofence",
                subtitle = if (isMockDetected) {
                    if (isKn) "ನಕಲಿ GPS!" else "Mock GPS!"
                } else if (geofence != null) {
                    if (geofence.isInside) "${geofence.distanceMeters}m (OK)" else "${geofence.distanceMeters}m"
                } else {
                    if (isKn) "ಪಡೆಯಲಾಗುತ್ತಿದೆ" else "Locating..."
                },
                isValid = isLocationValid,
                icon = if (isMockDetected) Icons.Default.Warning else Icons.Default.Place,
                isError = isMockDetected || (geofence != null && !geofence.isInside)
            )

            // Face Biometric Match
            ValidationPill(
                modifier = Modifier.weight(1f),
                title = if (isKn) "2. ಮುಖ ದೃಢೀಕರಣ" else "2. Face Match",
                subtitle = if (state.isFaceMatched) {
                    "${(state.faceSimilarityScore * 100).toInt()}% " + (if (isKn) "ಸರಿಹೊಂದಿದೆ" else "Matched")
                } else if (state.isFaceDetected) {
                    "${(state.faceSimilarityScore * 100).toInt()}% " + (if (isKn) "ಪರಿಶೀಲನೆ" else "Checking")
                } else {
                    if (isKn) "ಮುಖ ತೋರಿಸಿ" else "No Face"
                },
                isValid = state.isFaceMatched,
                icon = Icons.Default.Face,
                isError = state.isFaceDetected && !state.isFaceMatched
            )

            // Anti-Spoof Liveness Status
            ValidationPill(
                modifier = Modifier.weight(1f),
                title = if (isKn) "3. ಜೀವಂತಿಕೆ" else "3. Liveness",
                subtitle = if (liveness.isLivenessPassed) {
                    if (isKn) "ದೃಢೀಕರಿಸಲಾಗಿದೆ" else "Passed"
                } else if (state.isFaceDetected) {
                    if (isKn) "ಕಣ್ಣು ಮಿಟುಕಿಸಿ" else "Blink/Smile"
                } else {
                    if (isKn) "ಕಾಯುತ್ತಿದೆ" else "Waiting"
                },
                isValid = liveness.isLivenessPassed,
                icon = Icons.Default.VerifiedUser,
                isError = false
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // --- 5. Today's Attendance Record Summary Card (if logged) ---
        if (todayRec != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("today_status_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (todayRec.status == "ON_TIME") Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = if (todayRec.status == "ON_TIME") Color(0xFF2E7D32) else Color(0xFFEF6C00),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isKn) "ಇಂದಿನ ಹಾಜರಾತಿ ಸ್ಥಿತಿ" else "Today's Attendance Status",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall,
                                color = if (todayRec.status == "ON_TIME") Color(0xFF1B5E20) else Color(0xFFE65100)
                            )
                        }
                        Surface(
                            color = if (todayRec.status == "ON_TIME") Color(0xFF2E7D32) else Color(0xFFEF6C00),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (todayRec.status == "ON_TIME") {
                                    if (isKn) "ಸಮಯಕ್ಕೆ ಸರಿಯಾಗಿ" else "ON TIME"
                                } else {
                                    if (isKn) "ತಡವಾಗಿ" else "LATE"
                                },
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${if (isKn) "ಚೆಕ್-ಇನ್" else "Check-In"}: ${todayRec.checkInTime ?: "-"}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${if (isKn) "ಚೆಕ್-ಔಟ್" else "Check-Out"}: ${todayRec.checkOutTime ?: "-"}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // --- 6. Main Action Button: Check-In / Check-Out ---
        // ACTIVATION RULE: Button activates ONLY when both location (inside geofence) and face verification are successful!
        if (todayRec?.checkInTime == null) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = { viewModel.submitCheckIn() },
                    enabled = canCheckIn && !state.isSubmitting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("mark_checkin_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0F3A5D),
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    if (state.isSubmitting) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Icon(imageVector = Icons.Default.Fingerprint, contentDescription = null)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (isKn) "ಚೆಕ್-ಇನ್ ದೃಢೀಕರಿಸಿ" else "Confirm Check-In",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Helper guidance text when Check-In is disabled
                if (!canCheckIn) {
                    Spacer(modifier = Modifier.height(6.dp))
                    val pendingReason = when {
                        !isLocationValid && isMockDetected -> if (isKn) "⚠️ ನಕಲಿ GPS ಪತ್ತೆಯಾಗಿದೆ! ನೈಜ ಸ್ಥಳ ಸಕ್ರಿಯಗೊಳಿಸಿ." else "⚠️ Mock GPS detected! Disable mock location."
                        !isLocationValid -> if (isKn) "📍 ಶಾಲೆಯ ಗಡಿಯೊಳಗೆ ಬನ್ನಿ (${school?.radiusM ?: 200}m)" else "📍 Move within ${school?.radiusM ?: 200}m of school geofence"
                        !state.isFaceMatched -> if (isKn) "👤 ಮುಖವನ್ನು ಕ್ಯಾಮರಾದೆದುರು ನೇರವಾಗಿ ಇರಿಸಿ" else "👤 Align face with camera to match teacher profile"
                        !liveness.isLivenessPassed -> if (isKn) "👁️ ಜೀವಂತಿಕೆಗಾಗಿ ಒಮ್ಮೆ ಕಣ್ಣು ಮಿಟುಕಿಸಿ / ನಗಿರಿ" else "👁️ Blink or smile for liveness verification"
                        else -> ""
                    }
                    Text(
                        text = pendingReason,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        } else if (todayRec.checkOutTime == null) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = { viewModel.submitCheckOut() },
                    enabled = canCheckOut && !state.isSubmitting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("mark_checkout_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFB57000),
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    if (state.isSubmitting) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Icon(imageVector = Icons.Default.Logout, contentDescription = null)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (isKn) "ಚೆಕ್-ಔಟ್ ದೃಢೀಕರಿಸಿ" else "Confirm Check-Out",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Helper guidance text when Check-Out is disabled
                if (!canCheckOut) {
                    Spacer(modifier = Modifier.height(6.dp))
                    val pendingReason = when {
                        !isLocationValid -> if (isKn) "📍 ಚೆಕ್-ಔಟ್‌ಗಾಗಿ ಶಾಲಾ ಗಡಿಯೊಳಗೆ ಇರಬೇಕು" else "📍 Stay inside school geofence for check-out"
                        !isFaceValid -> if (isKn) "👤 ಚೆಕ್-ಔಟ್ ಮಾಡಲು ಮುಖ ದೃಢೀಕರಿಸಿ" else "👤 Face verification required for checkout"
                        else -> ""
                    }
                    Text(
                        text = pendingReason,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        } else {
            OutlinedButton(
                onClick = { viewModel.navigateTo(AppScreen.HISTORY) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("view_history_button"),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(imageVector = Icons.Default.History, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isKn) "ಇಂದಿನ ಹಾಜರಾತಿ ಮುಕ್ತಾಯ • ಇತಿಹಾಸ ವೀಕ್ಷಿಸಿ" else "Attendance Complete • View History",
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

/**
 * Real-Time Geofence Telemetry Card displaying distance, coordinates, and perimeter status.
 */
@Composable
fun RealtimeGeofenceCard(
    geofence: com.example.domain.location.GeofenceResult?,
    school: com.example.data.model.School?,
    isKannada: Boolean,
    onRefreshLocation: () -> Unit,
    onPresetSelected: (inside: Boolean, isMock: Boolean) -> Unit
) {
    val isInside = geofence?.isInside == true
    val isMock = geofence?.isMockLocation == true
    val distance = geofence?.distanceMeters ?: -1
    val radius = school?.radiusM ?: 200

    val cardBorderColor = when {
        isMock -> Color(0xFFD32F2F)
        isInside -> Color(0xFF2E7D32)
        geofence != null -> Color(0xFFE65100)
        else -> MaterialTheme.colorScheme.outlineVariant
    }

    val statusBadgeColor = when {
        isMock -> Color(0xFFD32F2F)
        isInside -> Color(0xFF2E7D32)
        else -> Color(0xFFE65100)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, cardBorderColor.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .testTag("realtime_geofence_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isInside) Icons.Default.GpsFixed else Icons.Default.GpsNotFixed,
                        contentDescription = null,
                        tint = statusBadgeColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isKannada) "ನೈಜ-ಸಮಯದ ಜಿಯೋಫೆನ್ಸ್ ಸ್ಥಿತಿ" else "Real-Time Geofence Status",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Refresh Location button
                IconButton(
                    onClick = onRefreshLocation,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("refresh_location_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh GPS",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Main Geofence Status Indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = when {
                            isMock -> if (isKannada) "⚠️ ನಕಲಿ ಸ್ಥಳ ಪತ್ತೆಯಾಗಿದೆ!" else "⚠️ Mock GPS Detected!"
                            isInside -> if (isKannada) "✅ ಶಾಲೆಯ ಗಡಿಯಲ್ಲಿದೆ (${distance}m)" else "✅ Inside School Perimeter (${distance}m)"
                            distance >= 0 -> if (isKannada) "❌ ಗಡಿಯ ಹೊರಗೆ (${distance}m ದೂರ)" else "❌ Outside Boundary (${distance}m away)"
                            else -> if (isKannada) "ಜಿಪಿಎಸ್ ಸಿಗ್ನಲ್‌ಗಾಗಿ ಹುಡುಕಲಾಗುತ್ತಿದೆ..." else "Acquiring GPS fix..."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = statusBadgeColor
                    )
                    Text(
                        text = "${if (isKannada) "ಅನುಮತಿಸಲಾದ ಗಡಿ" else "Allowed Radius"}: ${radius}m • ${school?.name ?: ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Quick Test Presets (Inside, Outside, Mock) for instant validation
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedButton(
                    onClick = { onPresetSelected(true, false) },
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("preset_inside_btn")
                ) {
                    Text(
                        text = if (isKannada) "ಗಡಿಯೊಳಗೆ" else "Inside (22m)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                OutlinedButton(
                    onClick = { onPresetSelected(false, false) },
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("preset_outside_btn")
                ) {
                    Text(
                        text = if (isKannada) "ಹೊರಗೆ" else "Outside (890m)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                OutlinedButton(
                    onClick = { onPresetSelected(true, true) },
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("preset_mock_btn")
                ) {
                    Text(
                        text = if (isKannada) "ನಕಲಿ GPS" else "Mock GPS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

/**
 * Visual validation checklist pill with icons and status colors.
 */
@Composable
fun ValidationPill(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    isValid: Boolean,
    icon: ImageVector,
    isError: Boolean = false
) {
    val bgColor = when {
        isError -> Color(0xFFFFEBEE)
        isValid -> Color(0xFFE8F5E9)
        else -> Color(0xFFF5F5F5)
    }
    val contentColor = when {
        isError -> Color(0xFFC62828)
        isValid -> Color(0xFF2E7D32)
        else -> Color(0xFF616161)
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = contentColor,
                maxLines = 1
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                color = contentColor.copy(alpha = 0.85f),
                maxLines = 1
            )
        }
    }
}
