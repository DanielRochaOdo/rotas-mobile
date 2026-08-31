package com.odontoart.rotas

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import com.odontoart.rotas.ui.ExactRotasRoot
import com.odontoart.rotas.ui.theme.RotasTheme
import com.odontoart.rotas.update.AppUpdateManager

class MainActivity : ComponentActivity() {
    private val appUpdateManager = AppUpdateManager()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            RotasTheme {
                ExactRotasRoot()
            }
        }

        appUpdateManager.checkForUpdate(this)
    }

    override fun onResume() {
        super.onResume()
        appUpdateManager.resumePendingInstall(this)
    }
}
