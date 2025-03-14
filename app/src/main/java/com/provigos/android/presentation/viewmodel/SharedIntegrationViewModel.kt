package com.provigos.android.presentation.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

class SharedIntegrationViewModel: ViewModel() {

    private val _navigateIntegrationScreen = MutableStateFlow<String?>(null)
    val navigateIntegrationScreen: SharedFlow<String?> get() = _navigateIntegrationScreen

    private val _preferencesUpdated = MutableStateFlow(false)
    val preferencesUpdated: StateFlow<Boolean> get() = _preferencesUpdated

    fun setIntegrationSettings(destination: String) {
        _navigateIntegrationScreen.tryEmit(destination)
    }

    fun resetIntegration() {
        _navigateIntegrationScreen.tryEmit(null)
    }

    fun notifyPreferencesChanged() {
        _preferencesUpdated.tryEmit(true)
    }

    fun resetPreferencesChanged() {
        _preferencesUpdated.value = false
    }


}