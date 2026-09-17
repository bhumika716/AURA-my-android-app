package com.aura.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aura.app.data.local.entities.JournalEntry
import com.aura.app.data.repository.JournalRepository
import com.aura.app.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

data class JournalUiState(
    val entries: List<JournalEntry> = emptyList(),
    val isWriting: Boolean = false,
    val isAnalyzing: Boolean = false,
    val draftText: String = "",
    val latestInsight: String? = null,
    val latestMood: String? = null,
    val averageMoodWeek: Float? = null,
    val averageMoodMonth: Float? = null,
    val totalEntries: Int = 0,
    val selectedEntry: JournalEntry? = null,
    val showEntryDetail: Boolean = false,
)

@HiltViewModel
class JournalViewModel @Inject constructor(
    private val journalRepository: JournalRepository,
    private val userRepository: UserRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(JournalUiState())
    val uiState: StateFlow<JournalUiState> = _uiState.asStateFlow()

    init {
        loadEntries()
        loadStats()
    }

    private fun loadEntries() {
        viewModelScope.launch {
            journalRepository.latestEntry.collect { entry ->
                _uiState.update {
                    it.copy(
                        latestMood = entry?.primaryMood
                    )
                }
            }
        }

        viewModelScope.launch {
            journalRepository.getRecentEntries(50).collect { entries ->
                _uiState.update {
                    it.copy(entries = entries)
                }
            }
        }
    }

    private fun loadStats() {
        viewModelScope.launch {
            val total = journalRepository.getEntryCount()

            val weekAgo = LocalDate.now().minusDays(7)
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant().toEpochMilli()

            val monthAgo = LocalDate.now().minusDays(30)
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant().toEpochMilli()

            val weekAvg = journalRepository.getAverageMoodSince(weekAgo)
            val monthAvg = journalRepository.getAverageMoodSince(monthAgo)

            _uiState.update {
                it.copy(
                    totalEntries = total,
                    averageMoodWeek = weekAvg,
                    averageMoodMonth = monthAvg,
                )
            }
        }
    }

    fun startWriting() {
        _uiState.update { it.copy(isWriting = true, draftText = "") }
    }

    fun cancelWriting() {
        _uiState.update { it.copy(isWriting = false, draftText = "") }
    }

    fun updateDraft(text: String) {
        _uiState.update { it.copy(draftText = text) }
    }

    fun submitEntry(selectedMood: String, selectedEmoji: String) {
        val text = _uiState.value.draftText.trim()
        if (text.isBlank()) return

        _uiState.update {
            it.copy(
                isAnalyzing = true,
                isWriting = false
            )
        }

        viewModelScope.launch {
            try {
                val entry = journalRepository.saveAndAnalyze(
                    rawText = text,
                    selectedMood = selectedMood,
                    selectedEmoji = selectedEmoji,
                )

                // Award XP for journaling
                userRepository.awardXP(20)

                _uiState.update {
                    it.copy(
                        isAnalyzing = false,
                        draftText = "",
                        latestInsight = entry.aiInsight,
                    )
                }

                // Refresh stats
                loadStats()

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isAnalyzing = false)
                }
            }
        }
    }

    fun dismissInsight() {
        _uiState.update { it.copy(latestInsight = null) }
    }

    fun selectEntry(entry: JournalEntry) {
        _uiState.update { it.copy(selectedEntry = entry, showEntryDetail = true) }
    }

    fun dismissEntryDetail() {
        _uiState.update { it.copy(selectedEntry = null, showEntryDetail = false) }
    }

    fun deleteEntry(entry: JournalEntry) {
        viewModelScope.launch {
            journalRepository.deleteEntry(entry)
            _uiState.update { it.copy(selectedEntry = null, showEntryDetail = false) }
            loadStats()
        }
    }
}
