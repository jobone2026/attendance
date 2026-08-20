package com.example.ui.screens

import androidx.compose.runtime.Composable
import com.example.ui.AttendanceUiState
import com.example.ui.MainViewModel

/**
 * LoginScreen wrapper pointing to TeacherLoginScreen with Firebase Phone Auth
 */
@Composable
fun LoginScreen(
    viewModel: MainViewModel,
    state: AttendanceUiState
) {
    TeacherLoginScreen(
        viewModel = viewModel,
        state = state
    )
}
