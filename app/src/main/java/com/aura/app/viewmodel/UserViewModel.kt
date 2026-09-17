package com.aura.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aura.app.data.local.entities.UserProfile
import com.aura.app.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.coroutines.flow.map

@HiltViewModel
class UserViewModel @Inject constructor(
    private val repository: UserRepository,
) : ViewModel() {

    val userProfile: StateFlow<UserProfile?> = repository.userProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val isProfileLoaded: StateFlow<Boolean> = repository.userProfile
    .map { true }
    .stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        false
    )

    private val _stageUpEvent = MutableSharedFlow<Int>()
    val stageUpEvent: SharedFlow<Int> = _stageUpEvent.asSharedFlow()

    init {
        viewModelScope.launch {
            repository.checkAndUpdateStreak()
        }
    }

    fun createProfile(
        name: String, 
        ageGroup: String, 
        empathy: Int, 
        confidence: Int, 
        communication: Int
    ) {
        viewModelScope.launch {
            repository.createProfile(name, ageGroup, empathy, confidence, communication)
        }
    }

    fun awardXP(amount: Int) {
        viewModelScope.launch {
            val newStage = repository.awardXP(amount)
            if (newStage != null) {
                _stageUpEvent.emit(newStage)
            }
        }
    }

    fun updateSkills(
        empathy: Int = 0,
        confidence: Int = 0,
        communication: Int = 0,
        leadership: Int = 0,
        resilience: Int = 0,
    ) {
        viewModelScope.launch {
            repository.updateSkills(empathy, confidence, communication, leadership, resilience)
        }
    }
}
