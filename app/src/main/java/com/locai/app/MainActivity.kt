package com.locai.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.locai.app.ui.navigation.LocAiNavGraph
import com.locai.app.ui.theme.LocAiTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = (application as LocAiApplication).container

        setContent {
            LocAiTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    LocAiNavGraph(container = container)
                }
            }
        }
    }
}
