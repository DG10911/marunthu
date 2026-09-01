package com.marunthu

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.marunthu.ui.MarunthuViewModel
import com.marunthu.ui.screens.HomeScreen
import com.marunthu.ui.screens.ResultScreen
import com.marunthu.ui.screens.ScanScreen
import com.marunthu.ui.theme.MarunthuTheme

enum class Screen { HOME, SCAN, RESULT }

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MarunthuTheme {
                val vm: MarunthuViewModel = viewModel()
                var screen by remember { mutableStateOf(Screen.HOME) }
                var hasCamera by remember { mutableStateOf(hasCameraPermission()) }

                val permLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { granted -> hasCamera = granted; if (granted) screen = Screen.SCAN }

                when (screen) {
                    Screen.HOME -> HomeScreen(
                        vm = vm,
                        onScan = {
                            if (hasCamera) screen = Screen.SCAN
                            else permLauncher.launch(Manifest.permission.CAMERA)
                        },
                    )
                    Screen.SCAN -> ScanScreen(
                        vm = vm,
                        onDone = { screen = Screen.RESULT },
                        onBack = { screen = Screen.HOME },
                    )
                    Screen.RESULT -> ResultScreen(
                        vm = vm,
                        onScanAnother = { screen = Screen.SCAN },
                        onHome = { vm.reset(); screen = Screen.HOME },
                    )
                }
            }
        }
    }

    private fun hasCameraPermission() =
        ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
}
