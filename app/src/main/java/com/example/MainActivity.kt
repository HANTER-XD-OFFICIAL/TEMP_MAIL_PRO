package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.TempMailViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: TempMailViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var isSplashScreenVisible by remember { mutableStateOf(true) }

                    AnimatedContent(
                        targetState = isSplashScreenVisible,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(450)) togetherWith
                                    fadeOut(animationSpec = tween(350))
                        },
                        label = "AppScreenTransition"
                    ) { showSplash ->
                        if (showSplash) {
                            SplashScreen(
                                onSplashFinished = {
                                    isSplashScreenVisible = false
                                }
                            )
                        } else {
                            HomeScreen(viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }
}

