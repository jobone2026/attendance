package com.example.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.AppScreen

@Composable
fun AppBottomBar(
    currentScreen: AppScreen,
    onNavigate: (AppScreen) -> Unit,
    isKannada: Boolean = false
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp
    ) {
        NavigationBarItem(
            modifier = Modifier.testTag("nav_attendance_tab"),
            selected = currentScreen == AppScreen.ATTENDANCE,
            onClick = { onNavigate(AppScreen.ATTENDANCE) },
            icon = {
                Icon(
                    imageVector = if (currentScreen == AppScreen.ATTENDANCE) Icons.Filled.Face else Icons.Outlined.Face,
                    contentDescription = stringResource(R.string.nav_attendance),
                    modifier = Modifier.size(24.dp)
                )
            },
            label = { Text(if (isKannada) "ಹಾಜರಾತಿ" else "Attendance") }
        )

        NavigationBarItem(
            modifier = Modifier.testTag("nav_history_tab"),
            selected = currentScreen == AppScreen.HISTORY,
            onClick = { onNavigate(AppScreen.HISTORY) },
            icon = {
                Icon(
                    imageVector = if (currentScreen == AppScreen.HISTORY) Icons.Filled.History else Icons.Outlined.History,
                    contentDescription = stringResource(R.string.nav_history),
                    modifier = Modifier.size(24.dp)
                )
            },
            label = { Text(if (isKannada) "ಇತಿಹಾಸ" else "History") }
        )

        NavigationBarItem(
            modifier = Modifier.testTag("nav_profile_tab"),
            selected = currentScreen == AppScreen.SETTINGS,
            onClick = { onNavigate(AppScreen.SETTINGS) },
            icon = {
                Icon(
                    imageVector = if (currentScreen == AppScreen.SETTINGS) Icons.Filled.AccountBalance else Icons.Outlined.AccountBalance,
                    contentDescription = stringResource(R.string.nav_profile),
                    modifier = Modifier.size(24.dp)
                )
            },
            label = { Text(if (isKannada) "ಶಾಲೆ/ವಿವರ" else "School") }
        )
    }
}
