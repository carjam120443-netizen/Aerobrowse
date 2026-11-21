package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.browser.BrowserViewModel
import com.example.ui.screens.BrowserScreen
import com.example.ui.theme.AeroBrowseTheme

class MainActivity : ComponentActivity() {

    private val browserViewModel: BrowserViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val preferences by browserViewModel.preferences.collectAsStateWithLifecycle()
            AeroBrowseTheme(
                preset = preferences.aeroTheme,
                glassIntensity = preferences.glassIntensity,
            ) {
                BrowserScreen(viewModel = browserViewModel)
            }
        }
    }

    override fun onDestroy() {
        if (isFinishing) {
            browserViewModel.clearSessionIfConfiguredOnExit()
        }
        super.onDestroy()
    }
}
