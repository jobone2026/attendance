package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.AppScreen
import com.example.ui.AttendanceUiState
import com.example.ui.MainViewModel

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    state: AttendanceUiState
) {
    val scrollState = rememberScrollState()
    val isKn = state.isKannadaLanguage
    val teacher = state.currentTeacher
    val school = state.currentSchool

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        Text(
            text = if (isKn) "ಪ್ರೊಫೈಲ್ ಮತ್ತು ಶಾಲಾ ವಿವರಗಳು" else "Profile & School Details",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Teacher Profile Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0F3A5D)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isKn) (teacher?.nameKn?.ifBlank { teacher.name } ?: "") else (teacher?.name ?: ""),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "ID: ${teacher?.teacherId} • ${teacher?.subject}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Ph: +91 ${teacher?.phone}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (teacher?.isFaceEnrolled == true) Icons.Default.CheckCircle else Icons.Default.Cancel,
                            contentDescription = null,
                            tint = if (teacher?.isFaceEnrolled == true) Color(0xFF107C41) else Color(0xFFC50F1F),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (teacher?.isFaceEnrolled == true) {
                                if (isKn) "ಮುಖ ನೋಂದಣಿಯಾಗಿದೆ (128-D)" else "Face Enrolled (128-D)"
                            } else {
                                if (isKn) "ಮುಖ ನೋಂದಣಿಯಾಗಿಲ್ಲ" else "Face Not Enrolled"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    OutlinedButton(
                        onClick = { viewModel.navigateTo(AppScreen.ENROLL_FACE) },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("re_enroll_btn")
                    ) {
                        Text(if (isKn) "ಮರು-ನೋಂದಣಿ" else "Re-Enroll")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // School Information Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AccountBalance,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (isKn) "ನಿಯೋಜಿತ ಶಾಲೆಯ ಮಾಹಿತಿ" else "Assigned School Info",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                DetailRow(
                    label = if (isKn) "ಶಾಲೆಯ ಹೆಸರು" else "School Name",
                    value = if (isKn) (school?.nameKn?.ifBlank { school.name } ?: "") else (school?.name ?: "")
                )
                DetailRow(
                    label = if (isKn) "ಜಿಲ್ಲೆ" else "District",
                    value = school?.district ?: "Bengaluru Urban"
                )
                DetailRow(
                    label = if (isKn) "ಶಾಲಾ ಕೋಡ್" else "School Code",
                    value = school?.schoolId ?: ""
                )
                DetailRow(
                    label = if (isKn) "ಜಿಪಿಎಸ್ ವ್ಯಾಪ್ತಿ" else "Geofence Radius",
                    value = "${school?.radiusM ?: 200} meters (Lat: ${school?.lat}, Lng: ${school?.lng})"
                )
                DetailRow(
                    label = if (isKn) "ಚೆಕ್-ಇನ್ ಸಮಯ" else "Check-In Window",
                    value = "${school?.checkinStart} - ${school?.checkinEnd}"
                )
                DetailRow(
                    label = if (isKn) "ಚೆಕ್-ಔಟ್ ಸಮಯ" else "Check-Out Window",
                    value = "${school?.checkoutStart} - ${school?.checkoutEnd}"
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Actions Card (Sync, Language, Switch Account)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = if (isKn) "ವ್ಯವಸ್ಥಾಪಕ ಕ್ರಿಯೆಗಳು" else "System Actions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { viewModel.forceSyncCloud() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("force_sync_btn"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF107C41))
                ) {
                    Icon(imageVector = Icons.Default.Sync, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isKn) "ಕ್ಲೌಡ್ ಸಿಂಕ್ ಮಾಡಿ (Firestore)" else "Force Sync to Firestore")
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = { viewModel.toggleLanguage() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Translate, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isKn) "Switch to English" else "ಕನ್ನಡಕ್ಕೆ ಬದಲಿಸಿ")
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = { viewModel.navigateTo(AppScreen.LOGIN) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(imageVector = Icons.Default.ExitToApp, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isKn) "ಖಾತೆ ಬದಲಿಸಿ / ಲಾಗ್‌ಔಟ್" else "Switch Account / Sign Out")
                }
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1.5f)
        )
    }
}
