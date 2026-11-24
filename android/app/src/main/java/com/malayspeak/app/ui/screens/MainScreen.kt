package com.malayspeak.app.ui.screens

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.malayspeak.app.data.audio.MalaySpeechPlayer
import com.malayspeak.app.data.local.entity.VocabularyCard
import com.malayspeak.app.data.local.preferences.ThemeMode
import com.malayspeak.app.ui.components.AudioUnavailableDialog
import com.malayspeak.app.ui.components.BottomNavBar
import com.malayspeak.app.ui.components.GlassBackdrop
import com.malayspeak.app.ui.navigation.Screen
import com.malayspeak.app.ui.screens.home.HomeScreen
import com.malayspeak.app.ui.screens.progress.ProgressScreen
import com.malayspeak.app.ui.screens.progress.ProgressViewModel
import com.malayspeak.app.ui.screens.translate.TranslationScreen
import com.malayspeak.app.ui.screens.translate.TranslationViewModel
import com.malayspeak.app.ui.theme.MalaySpeakTheme
import com.malayspeak.app.ui.theme.ThemeViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

@Composable
fun MainScreen(
    translationViewModel: TranslationViewModel,
    progressViewModel: ProgressViewModel,
    themeViewModel: ThemeViewModel,
    speechPlayer: MalaySpeechPlayer
) {
    val onPlayAudio: (VocabularyCard) -> Unit = remember(speechPlayer) {
        { card -> speechPlayer.speak(card.id, card.malayPhrase) }
    }
    val systemInDark = isSystemInDarkTheme()
    val themeMode by themeViewModel.themeMode.collectAsStateWithLifecycle()
    val isDarkTheme = if (themeMode == ThemeMode.SYSTEM) systemInDark else themeViewModel.isDarkTheme

    LaunchedEffect(systemInDark, themeMode) {
        themeViewModel.syncWithSystemTheme(systemInDark)
    }

    MalaySpeakTheme(isDarkTheme = isDarkTheme) {
        val navController = rememberNavController()
        val onOpenWords: (String?) -> Unit = remember(translationViewModel, navController) {
            { category ->
                translationViewModel.onSearchQueryChanged("")
                translationViewModel.onCategorySelected(category ?: "ALL")
                navController.navigate(Screen.Words.route) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                }
            }
        }
        val onToggleHomeCardLearned: (VocabularyCard) -> Unit = remember(translationViewModel) {
            { card -> translationViewModel.toggleCardLearned(card.id, !card.isLearned) }
        }
        val onToggleWordLearned: (Long, Boolean) -> Unit = remember(translationViewModel) {
            translationViewModel::toggleCardLearned
        }
        val onThemeModeChange: (ThemeMode) -> Unit = remember(themeViewModel) {
            themeViewModel::setThemeMode
        }

        Box(modifier = Modifier.fillMaxSize()) {
            GlassBackdrop()

            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = Color.Transparent,
                bottomBar = { MainBottomBar(navController = navController) }
            ) { innerPadding ->
                NavHost(
                    navController = navController,
                    startDestination = Screen.Home.route,
                    modifier = Modifier.padding(innerPadding)
                ) {
                    composable(Screen.Home.route) {
                        val speakingIdState = remember(speechPlayer) {
                            speechPlayer.state.map { it.speakingId }.distinctUntilChanged()
                        }.collectAsStateWithLifecycle(initialValue = null)
                        val speakingCardId = remember(speakingIdState) { { speakingIdState.value } }
                        HomeScreen(
                            viewModel = progressViewModel,
                            onOpenWords = onOpenWords,
                            onToggleLearned = onToggleHomeCardLearned,
                            onPlayAudio = onPlayAudio,
                            speakingCardId = speakingCardId
                        )
                    }
                    composable(Screen.Words.route) {
                        val speakingIdState = remember(speechPlayer) {
                            speechPlayer.state.map { it.speakingId }.distinctUntilChanged()
                        }.collectAsStateWithLifecycle(initialValue = null)
                        val speakingCardId = remember(speakingIdState) { { speakingIdState.value } }
                        TranslationScreen(
                            viewModel = translationViewModel,
                            onToggleLearned = onToggleWordLearned,
                            onPlayAudio = onPlayAudio,
                            speakingCardId = speakingCardId
                        )
                    }
                    composable(Screen.Progress.route) {
                        ProgressScreen(
                            viewModel = progressViewModel,
                            themeMode = themeMode,
                            onThemeModeChange = onThemeModeChange
                        )
                    }
                }
            }

            SpeechUnavailableDialogHost(speechPlayer = speechPlayer)
        }
    }
}

@Composable
private fun MainBottomBar(navController: NavHostController) {
    val density = LocalDensity.current
    val imeInsets = WindowInsets.ime
    val imeVisible by remember(imeInsets, density) {
        derivedStateOf { imeInsets.getBottom(density) > 0 }
    }
    if (imeVisible) return

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val onSelect: (Screen) -> Unit = remember(navController) {
        { screen ->
            if (navController.currentDestination?.route != screen.route) {
                navController.navigate(screen.route) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        }
    }

    BottomNavBar(
        items = Screen.items,
        currentRoute = currentRoute,
        onSelect = onSelect
    )
}

@Composable
private fun SpeechUnavailableDialogHost(speechPlayer: MalaySpeechPlayer) {
    val speechState by speechPlayer.state.collectAsStateWithLifecycle()
    val message = speechState.message ?: return
    val onDismiss = remember(speechPlayer) { speechPlayer::dismissMessage }
    val onOpenSettings = remember(speechPlayer) { speechPlayer::openTtsSettings }
    val onRetry = remember(speechPlayer) { speechPlayer::refresh }

    AudioUnavailableDialog(
        message = message,
        onDismiss = onDismiss,
        onOpenSettings = onOpenSettings,
        onRetry = onRetry
    )
}
