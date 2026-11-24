package com.malayspeak.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.malayspeak.app.data.audio.MalaySpeechPlayer
import com.malayspeak.app.data.local.preferences.ThemeMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import com.malayspeak.app.ui.screens.MainScreen
import com.malayspeak.app.ui.screens.progress.ProgressViewModel
import com.malayspeak.app.ui.screens.translate.TranslationViewModel
import com.malayspeak.app.ui.theme.ThemeViewModel

class MainActivity : ComponentActivity() {

    private lateinit var speechPlayer: MalaySpeechPlayer
    private val repository by lazy { (application as MalaySpeakApp).repository }

    private val translationViewModel: TranslationViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return TranslationViewModel(repository) as T
            }
        }
    }

    private val themePreferencesRepository by lazy {
        (application as MalaySpeakApp).themePreferencesRepository
    }

    // A short synchronous local read lets the first Compose frame use the stored
    // preference, so the system-controlled splash does not flash the wrong theme.
    private val initialThemeMode: ThemeMode by lazy {
        runBlocking { themePreferencesRepository.themeMode.first() }
    }

    private val themeViewModel: ThemeViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return ThemeViewModel(themePreferencesRepository, initialThemeMode) as T
            }
        }
    }

    private val progressViewModel: ProgressViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return ProgressViewModel(repository) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Must be called before super.onCreate so the splash window is installed
        // with the launch theme. It is removed automatically on first frame.
        installSplashScreen()
        super.onCreate(savedInstanceState)
        speechPlayer = MalaySpeechPlayer(applicationContext)
        setContent {
            MainScreen(
                translationViewModel = translationViewModel,
                progressViewModel = progressViewModel,
                themeViewModel = themeViewModel,
                speechPlayer = speechPlayer
            )
        }
    }

    override fun onResume() {
        super.onResume()
        if (::speechPlayer.isInitialized) {
            speechPlayer.refreshIfUnavailable()
        }
    }

    override fun onStop() {
        if (::speechPlayer.isInitialized) {
            speechPlayer.stop()
        }
        super.onStop()
    }

    override fun onDestroy() {
        if (::speechPlayer.isInitialized) {
            speechPlayer.release()
        }
        super.onDestroy()
    }
}
