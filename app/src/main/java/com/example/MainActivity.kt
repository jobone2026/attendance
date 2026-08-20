package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.AppScreen
import com.example.ui.MainViewModel
import com.example.ui.components.AppBottomBar
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                val snackbarHostState = remember { SnackbarHostState() }
                val context = LocalContext.current

                // Runtime Permissions Requester for Camera and GPS Location
                val permissionsLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) { perms ->
                    val cameraGranted = perms[Manifest.permission.CAMERA] == true
                    val locationGranted = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true
                    if (locationGranted) {
                        viewModel.refreshLocation()
                    }
                }

                LaunchedEffect(Unit) {
                    val permissions = arrayOf(
                        Manifest.permission.CAMERA,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                    val ungranted = permissions.filter {
                        ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
                    }
                    if (ungranted.isNotEmpty()) {
                        permissionsLauncher.launch(permissions)
                    }
                }

                LaunchedEffect(state.toastMessage) {
                    state.toastMessage?.let { msg ->
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        viewModel.clearToast()
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    bottomBar = {
                        if (state.currentScreen != AppScreen.LOGIN && state.currentScreen != AppScreen.ENROLL_FACE) {
                            AppBottomBar(
                                currentScreen = state.currentScreen,
                                onNavigate = { viewModel.navigateTo(it) },
                                isKannada = state.isKannadaLanguage
                            )
                        }
                    }
                ) { innerPadding ->
                    val screenModifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)

                    when (state.currentScreen) {
                        AppScreen.LOGIN -> LoginScreen(viewModel = viewModel, state = state)
                        AppScreen.ATTENDANCE -> BoxWithPadding(screenModifier) {
                            AttendanceScreen(viewModel = viewModel, state = state)
                        }
                        AppScreen.ENROLL_FACE -> BoxWithPadding(screenModifier) {
                            FaceEnrollmentScreen(viewModel = viewModel, state = state)
                        }
                        AppScreen.HISTORY -> BoxWithPadding(screenModifier) {
                            HistoryScreen(viewModel = viewModel, state = state)
                        }
                        AppScreen.SETTINGS -> BoxWithPadding(screenModifier) {
                            SettingsScreen(viewModel = viewModel, state = state)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BoxWithPadding(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    androidx.compose.foundation.layout.Box(modifier = modifier) {
        content()
    }
}
