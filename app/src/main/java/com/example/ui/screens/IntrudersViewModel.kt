package com.example.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.IntruderRepository
import com.example.domain.model.IntruderAttempt
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class IntrudersViewModel @Inject constructor(
    private val intruderRepository: IntruderRepository
) : ViewModel() {

    val attempts: StateFlow<List<IntruderAttempt>> = intruderRepository.observeIntruderAttempts()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun deleteAttempt(attempt: IntruderAttempt) {
        viewModelScope.launch {
            intruderRepository.deleteAttempt(attempt)
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            intruderRepository.clearAllAttempts()
        }
    }
}
