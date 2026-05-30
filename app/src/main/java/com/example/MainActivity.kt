package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.ui.screens.MainOrchestrationScreen
import com.example.viewmodel.UjianViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: UjianViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MainOrchestrationScreen(viewModel = viewModel)
        }
    }
}
