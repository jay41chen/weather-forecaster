package com.weather.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weather.core.domain.DetectAndSelectCityUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val detectAndSelectCity: DetectAndSelectCityUseCase
) : ViewModel() {

    private val _isDetectingLocation = MutableStateFlow(false)
    val isDetectingLocation: StateFlow<Boolean> = _isDetectingLocation.asStateFlow()

    fun onLocationPermissionGranted() {
        viewModelScope.launch {
            _isDetectingLocation.value = true
            try {
                detectAndSelectCity()
            } finally {
                _isDetectingLocation.value = false
            }
        }
    }
}
