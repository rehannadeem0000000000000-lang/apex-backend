package com.apexbuilder.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.apexbuilder.app.ui.ApexBuilderScreen
import com.apexbuilder.app.ui.theme.ApexBuilderTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val app = application as ApexBuilderApplication
        val factory = BuilderViewModelFactory(app, app.container)
        setContent {
            ApexBuilderTheme {
                val viewModel: BuilderViewModel = viewModel(factory = factory)
                ApexBuilderScreen(viewModel = viewModel)
            }
        }
    }
}