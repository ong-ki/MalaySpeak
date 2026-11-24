package com.malayspeak.app

import com.malayspeak.app.data.local.preferences.ThemeMode
import com.malayspeak.app.data.local.preferences.ThemePreferencesRepository
import com.malayspeak.app.ui.theme.ThemeViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.Runs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ThemeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val repository = mockk<ThemePreferencesRepository>(relaxed = true)
    private val storedMode = MutableStateFlow(ThemeMode.SYSTEM)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { repository.themeMode } returns storedMode
        coEvery { repository.setThemeMode(any()) } just Runs
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun explicitLightAndDarkSelectionUpdatesStateAndPersists() = runTest(testDispatcher) {
        val viewModel = ThemeViewModel(repository)
        advanceUntilIdle()

        viewModel.setThemeMode(ThemeMode.DARK)
        advanceUntilIdle()
        assertTrue(viewModel.isDarkTheme)
        coVerify { repository.setThemeMode(ThemeMode.DARK) }

        viewModel.setThemeMode(ThemeMode.LIGHT)
        advanceUntilIdle()
        assertFalse(viewModel.isDarkTheme)
        coVerify { repository.setThemeMode(ThemeMode.LIGHT) }
    }

    @Test
    fun systemModeFollowsDeviceAfterManualOverride() = runTest(testDispatcher) {
        val viewModel = ThemeViewModel(repository)
        advanceUntilIdle()

        viewModel.setThemeMode(ThemeMode.DARK)
        advanceUntilIdle()
        assertTrue(viewModel.isDarkTheme)

        viewModel.setThemeMode(ThemeMode.SYSTEM)
        advanceUntilIdle()

        viewModel.syncWithSystemTheme(systemInDark = false)
        assertFalse(viewModel.isDarkTheme)

        viewModel.syncWithSystemTheme(systemInDark = true)
        assertTrue(viewModel.isDarkTheme)
    }

    @Test
    fun manualTogglePersistsTheOppositeMode() = runTest(testDispatcher) {
        val viewModel = ThemeViewModel(repository)
        advanceUntilIdle()

        viewModel.setThemeMode(ThemeMode.DARK)
        advanceUntilIdle()
        assertTrue(viewModel.isDarkTheme)

        viewModel.toggleTheme()
        advanceUntilIdle()
        assertFalse(viewModel.isDarkTheme)
        coVerify { repository.setThemeMode(ThemeMode.LIGHT) }
    }
}
