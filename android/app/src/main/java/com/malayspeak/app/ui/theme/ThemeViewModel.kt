package com.malayspeak.app.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.malayspeak.app.data.local.preferences.ThemeMode
import com.malayspeak.app.data.local.preferences.ThemePreferencesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ThemeViewModel(
    private val themePreferencesRepository: ThemePreferencesRepository,
    initialThemeMode: ThemeMode = ThemeMode.SYSTEM
) : ViewModel() {

    // Compose mutableStateOf boolean for instant reactive recomposition.
    // Initialising from the persisted value prevents a light->dark first-frame
    // flash when the in-app preference differs from the system night mode.
    var isDarkTheme by mutableStateOf(initialThemeMode == ThemeMode.DARK)
        private set

    private var hasManualPreference = initialThemeMode != ThemeMode.SYSTEM

    val themeMode: StateFlow<ThemeMode> = themePreferencesRepository.themeMode
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = initialThemeMode
        )

    init {
        viewModelScope.launch {
            themePreferencesRepository.themeMode.collect { mode ->
                when (mode) {
                    ThemeMode.LIGHT -> {
                        isDarkTheme = false
                        hasManualPreference = true
                    }
                    ThemeMode.DARK -> {
                        isDarkTheme = true
                        hasManualPreference = true
                    }
                    ThemeMode.SYSTEM -> {
                        hasManualPreference = false
                    }
                }
            }
        }
    }

    /**
     * Called on composition with current system dark theme state.
     * Only applies if user has not explicitly picked LIGHT or DARK.
     */
    fun syncWithSystemTheme(systemInDark: Boolean) {
        if (!hasManualPreference && themeMode.value == ThemeMode.SYSTEM) {
            isDarkTheme = systemInDark
        }
    }

    /**
     * Toggles between Light and Dark mode, updating memory state instantly
     * and persisting the new choice to DataStore.
     */
    fun toggleTheme() {
        val newTheme = !isDarkTheme
        isDarkTheme = newTheme
        hasManualPreference = true
        viewModelScope.launch {
            val mode = if (newTheme) ThemeMode.DARK else ThemeMode.LIGHT
            themePreferencesRepository.setThemeMode(mode)
        }
    }

    /** Explicit three-way selection used by the appearance card. */
    fun setThemeMode(mode: ThemeMode) {
        when (mode) {
            ThemeMode.LIGHT -> isDarkTheme = false
            ThemeMode.DARK -> isDarkTheme = true
            ThemeMode.SYSTEM -> hasManualPreference = false
        }
        if (mode != ThemeMode.SYSTEM) {
            hasManualPreference = true
        }
        viewModelScope.launch {
            themePreferencesRepository.setThemeMode(mode)
        }
    }
}
