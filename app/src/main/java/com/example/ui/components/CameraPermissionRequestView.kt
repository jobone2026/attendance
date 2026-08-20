package com.example.ui.components

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat

/**
 * Utility to check camera and location permissions.
 */
fun isCameraPermissionGranted(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED
}

fun isLocationPermissionGranted(context: Context): Boolean {
    val fine = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    val coarse = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    return fine || coarse
}

/**
 * Modern M3 Permission Request View for Camera and Location permissions.
 * Displayed when permissions are required before initializing CameraX & Geofencing services.
 */
@Composable
fun CameraPermissionRequestView(
    modifier: Modifier = Modifier,
    isKannada: Boolean = false,
    onPermissionsGranted: () -> Unit
) {
    val context = LocalContext.current

    var hasCameraPermission by remember { mutableStateOf(isCameraPermissionGranted(context)) }
    var hasLocationPermission by remember { mutableStateOf(isLocationPermissionGranted(context)) }

    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        hasCameraPermission = perms[Manifest.permission.CAMERA] == true || isCameraPermissionGranted(context)
        hasLocationPermission = (perms[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true ||
                isLocationPermissionGranted(context))

        if (hasCameraPermission && hasLocationPermission) {
            onPermissionsGranted()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF0D1B2A))
            .border(1.5.dp, Color(0xFF64B5F6).copy(alpha = 0.3f), RoundedCornerShape(24.dp))
            .padding(18.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Icon Header
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = if (isKannada) "ಅನುಮತಿಗಳು ಅಗತ್ಯವಿದೆ" else "Permissions Required",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = if (isKannada)
                    "ಕ್ಯಾಮೆರಾ ಮತ್ತು ಜಿಪಿಎಸ್ ಆಧಾರಿತ ಹಾಜರಾತಿಗಾಗಿ ಅನುಮತಿಗಳನ್ನು ನೀಡಿ"
                else
                    "Grant Camera and Location access to start biometric scanning and school geofencing.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.75f),
                textAlign = TextAlign.Center,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Permission status items
            PermissionItemRow(
                icon = Icons.Default.CameraAlt,
                title = if (isKannada) "ಕ್ಯಾಮೆರಾ ಅನುಮತಿ" else "Camera Permission",
                description = if (isKannada) "ಮುಖ ಗುರುತಿಸುವಿಕೆ ಮತ್ತು ಜೀವಂತಿಕೆ ಪರಿಶೀಲನೆ" else "Face detection & liveness check",
                isGranted = hasCameraPermission
            )

            Spacer(modifier = Modifier.height(8.dp))

            PermissionItemRow(
                icon = Icons.Default.LocationOn,
                title = if (isKannada) "ಸ್ಥಳ / ಜಿಪಿಎಸ್ ಅನುಮತಿ" else "Location Permission",
                description = if (isKannada) "ಶಾಲಾ ಜಿಯೋಫೆನ್ಸಿಂಗ್ ಪರಿಶೀಲನೆ" else "School geofence boundary validation",
                isGranted = hasLocationPermission
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Settings button (if previously denied)
                OutlinedButton(
                    onClick = {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                        context.startActivity(intent)
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("open_settings_permission_btn")
                ) {
                    Text(
                        text = if (isKannada) "ಸೆಟ್ಟಿಂಗ್ಸ್" else "Settings",
                        fontSize = 12.sp
                    )
                }

                // Grant Permission primary button
                Button(
                    onClick = {
                        permissionsLauncher.launch(
                            arrayOf(
                                Manifest.permission.CAMERA,
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier
                        .weight(1.5f)
                        .testTag("grant_permissions_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.LockOpen,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isKannada) "ಅನುಮತಿಸಿ" else "Grant Access",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionItemRow(
    icon: ImageVector,
    title: String,
    description: String,
    isGranted: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(if (isGranted) Color(0xFF107C41).copy(alpha = 0.2f) else Color(0xFFFF9800).copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isGranted) Color(0xFF4CAF50) else Color(0xFFFFB951),
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            Text(
                text = description,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.65f),
                lineHeight = 12.sp
            )
        }

        Icon(
            imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
            contentDescription = null,
            tint = if (isGranted) Color(0xFF4CAF50) else Color(0xFFFFB951),
            modifier = Modifier.size(18.dp)
        )
    }
}
