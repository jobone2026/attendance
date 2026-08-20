package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.AttendanceRecord
import com.example.ui.AttendanceUiState
import com.example.ui.MainViewModel
import kotlinx.coroutines.launch

@Composable
fun HistoryScreen(
    viewModel: MainViewModel,
    state: AttendanceUiState
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val isKn = state.isKannadaLanguage
    val history = state.attendanceHistory
    var showCsvDialog by remember { mutableStateOf(false) }
    var csvContent by remember { mutableStateOf("") }

    val totalPresent = history.size
    val totalOnTime = history.count { it.status == "ON_TIME" }
    val totalLate = history.count { it.status == "LATE" }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // Title & Export Button Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = if (isKn) "ಹಾಜರಾತಿ ಇತಿಹಾಸ" else "Attendance History",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isKn) "ಕಳೆದ 30 ದಿನಗಳ ವಿವರ" else "Last 30 days records",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            FilledTonalButton(
                onClick = {
                    coroutineScope.launch {
                        csvContent = viewModel.repository.exportAttendanceCsv()
                        showCsvDialog = true
                    }
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("export_csv_btn")
            ) {
                Icon(imageVector = Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (isKn) "CSV ವರದಿ" else "Export CSV")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Summary Metric Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MetricCard(
                modifier = Modifier.weight(1f),
                label = if (isKn) "ಒಟ್ಟು ಹಾಜರಾತಿ" else "Present",
                value = "$totalPresent",
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
            MetricCard(
                modifier = Modifier.weight(1f),
                label = if (isKn) "ಸಮಯಕ್ಕೆ" else "On-Time",
                value = "$totalOnTime",
                containerColor = Color(0xFFE8F5E9),
                contentColor = Color(0xFF2E7D32)
            )
            MetricCard(
                modifier = Modifier.weight(1f),
                label = if (isKn) "ತಡವಾಗಿ" else "Late",
                value = "$totalLate",
                containerColor = Color(0xFFFFF3E0),
                contentColor = Color(0xFFEF6C00)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Records List
        if (history.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.EventBusy,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (isKn) "ಯಾವುದೇ ದಾಖಲೆಗಳು ಕಂಡುಬಂದಿಲ್ಲ" else "No attendance records yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(history, key = { it.recordId }) { item ->
                    AttendanceHistoryCard(record = item, isKn = isKn)
                }
            }
        }
    }

    // CSV Preview Dialog
    if (showCsvDialog) {
        AlertDialog(
            onDismissRequest = { showCsvDialog = false },
            title = { Text(if (isKn) "ಹಾಜರಾತಿ CSV ವರದಿ" else "Attendance CSV Report") },
            text = {
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Text(
                        text = csvContent.ifBlank { "No records to export" },
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        Toast.makeText(context, "CSV ready for export", Toast.LENGTH_SHORT).show()
                        showCsvDialog = false
                    }
                ) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
fun MetricCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    containerColor: Color,
    contentColor: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor.copy(alpha = 0.85f),
                maxLines = 1
            )
        }
    }
}

@Composable
fun AttendanceHistoryCard(
    record: AttendanceRecord,
    isKn: Boolean
) {
    val isOnTime = record.status == "ON_TIME"
    val statusBg = if (isOnTime) Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
    val statusColor = if (isOnTime) Color(0xFF2E7D32) else Color(0xFFEF6C00)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = record.date,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall
                    )
                }

                // Status Badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = statusBg
                ) {
                    Text(
                        text = if (isOnTime) {
                            if (isKn) "ಸಮಯಕ್ಕೆ" else "ON-TIME"
                        } else {
                            if (isKn) "ತಡವಾಗಿ" else "LATE"
                        },
                        color = statusColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = if (isKn) "ಚೆಕ್-ಇನ್" else "Check-In",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = record.checkInTime ?: "-",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Column {
                    Text(
                        text = if (isKn) "ಚೆಕ್-ಔಟ್" else "Check-Out",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = record.checkOutTime ?: "-",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Column {
                    Text(
                        text = if (isKn) "ದೂರ" else "Distance",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${record.distanceMeters} m",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))

            // Cloud Sync Status Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (record.isSynced) Icons.Default.CloudDone else Icons.Default.CloudQueue,
                        contentDescription = null,
                        tint = if (record.isSynced) Color(0xFF107C41) else Color(0xFFCA5010),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (record.isSynced) {
                            if (isKn) "ಫೈರ್‌ಸ್ಟೋರ್ ಸಿಂಕ್ ಆಗಿದೆ" else "Synced to Firestore"
                        } else {
                            if (isKn) "ಆಫ್‌ಲೈನ್ - ಸಿಂಕ್ ಬಾಕಿ ಇದೆ" else "Pending Offline Sync"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (record.isSynced) Color(0xFF107C41) else Color(0xFFCA5010),
                        fontWeight = FontWeight.Medium
                    )
                }

                Text(
                    text = "Doc: ${record.recordId}",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}
