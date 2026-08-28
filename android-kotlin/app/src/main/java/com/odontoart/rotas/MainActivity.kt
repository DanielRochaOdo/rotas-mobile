package com.odontoart.rotas

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import com.odontoart.rotas.ui.RotasApp
import com.odontoart.rotas.ui.theme.RotasTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            RotasTheme {
                RotasApp()
            }
        }
    }
}
