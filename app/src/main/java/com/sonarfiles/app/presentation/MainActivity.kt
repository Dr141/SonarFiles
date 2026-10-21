package com.sonarfiles.app.presentation

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import androidx.navigation.compose.rememberNavController
import com.sonarfiles.app.core.ui.theme.SonarFilesTheme
import com.sonarfiles.app.presentation.navigation.SonarNavHost
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            SonarFilesTheme(
                darkTheme = true,
                amoledMode = true,
                dynamicColor = false // Prioriza o visual cyber-sonar moderno (True Black + Cyan)
            ) {
                val navController = rememberNavController()
                SonarNavHost(navController = navController)
            }
        }
    }
}
