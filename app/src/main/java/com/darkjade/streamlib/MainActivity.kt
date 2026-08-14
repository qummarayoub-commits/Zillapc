package com.darkjade.streamlib

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.darkjade.streamlib.ui.navigation.StreamLibNavGraph
import com.darkjade.streamlib.ui.theme.StreamLibTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = (application as StreamLibApp).container

        setContent {
            StreamLibTheme {
                StreamLibNavGraph(container = container)
            }
        }
    }
}
