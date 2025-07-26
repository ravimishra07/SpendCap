package com.ravi.samstudioapp.presentation.screens.daily

import androidx.lifecycle.viewModelScope
import com.ravi.samstudioapp.domain.model.DailyLog
import com.ravi.samstudioapp.presentation.main.BaseViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

class DailyViewModel : BaseViewModel() {

    private val _selectedDate = MutableStateFlow(System.currentTimeMillis())
    val selectedDate: StateFlow<Long> = _selectedDate

    private val _logData = MutableStateFlow<DailyLog?>(null)
    val logData: StateFlow<DailyLog?> = _logData

    // Mock data for demonstration
    private val mockLogs = mapOf(
        getTodayTimestamp() to DailyLog(
            date = getTodayTimestamp(),
            mood = "😊 Happy",
            sleepHours = 7.5,
            notes = "Had a productive day at work. Feeling accomplished!",
            waterGlasses = 6,
            exerciseMinutes = 30,
            energyLevel = 8,
            gratitude = "Grateful for good health and supportive friends"
        ),
        getYesterdayTimestamp() to DailyLog(
            date = getYesterdayTimestamp(),
            mood = "😐 Neutral",
            sleepHours = 6.0,
            notes = "Regular day, nothing special happened.",
            waterGlasses = 4,
            exerciseMinutes = 0,
            energyLevel = 5,
            gratitude = "Grateful for a peaceful day"
        ),
        getTwoDaysAgoTimestamp() to DailyLog(
            date = getTwoDaysAgoTimestamp(),
            mood = "😃 Excited",
            sleepHours = 8.0,
            notes = "Started a new project! Very excited about the possibilities.",
            waterGlasses = 8,
            exerciseMinutes = 45,
            energyLevel = 9,
            gratitude = "Grateful for new opportunities and challenges"
        )
    )

    init {
        loadLogForDate(_selectedDate.value)
    }

    fun loadLogForDate(date: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            _selectedDate.value = date

            // Simulate network/database delay
            delay(500)

            // Get normalized date (start of day)
            val normalizedDate = normalizeToStartOfDay(date)
            _logData.value = mockLogs[normalizedDate]

            _isLoading.value = false
        }
    }

    fun selectToday() {
        loadLogForDate(System.currentTimeMillis())
    }

    fun selectYesterday() {
        val yesterday = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -1)
        }.timeInMillis
        loadLogForDate(yesterday)
    }

    fun selectDate(date: Long) {
        loadLogForDate(date)
    }

    // Override abstract methods from BaseViewModel
    override fun onDateRangeChanged(start: Long, end: Long) {
        // For daily logging, we might want to update the selected date if it's outside the range
        // For now, just keeping it simple
    }

    override fun onInitialPreferencesLoaded() {
        // Load any daily-specific preferences here
        // For now, just load today's log
        loadLogForDate(_selectedDate.value)
    }

    // Helper functions
    private fun normalizeToStartOfDay(timestamp: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    private fun getTodayTimestamp(): Long {
        return normalizeToStartOfDay(System.currentTimeMillis())
    }

    private fun getYesterdayTimestamp(): Long {
        val yesterday = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -1)
        }
        return normalizeToStartOfDay(yesterday.timeInMillis)
    }

    private fun getTwoDaysAgoTimestamp(): Long {
        val twoDaysAgo = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -2)
        }
        return normalizeToStartOfDay(twoDaysAgo.timeInMillis)
    }
}