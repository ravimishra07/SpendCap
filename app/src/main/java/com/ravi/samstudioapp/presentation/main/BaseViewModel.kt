package com.ravi.samstudioapp.presentation.main

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ravi.samstudioapp.ui.DateRangeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Calendar

abstract class BaseViewModel : ViewModel() {
    
    // Loading state
    protected val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    // Date range state
    protected val _dateRange = MutableStateFlow(getDefaultMonthRange())
    val dateRange: StateFlow<Pair<Long, Long>> = _dateRange

    protected val _dateRangeMode = MutableStateFlow(DateRangeMode.DAILY)
    val dateRangeMode: StateFlow<DateRangeMode> = _dateRangeMode

    protected val _prevRange = MutableStateFlow<Pair<Long, Long>?>(null)
    val prevRange: StateFlow<Pair<Long, Long>?> = _prevRange

    // Constants for SharedPreferences
    companion object {
        const val CORE_NAME = "samstudio_prefs"
        const val RANGE_START = "date_range_start"
        const val RANGE_END = "date_range_end"
        const val RANGE_MODE = "date_range_mode"
    }

    protected fun setDateRange(start: Long, end: Long) {
        val calStart = Calendar.getInstance().apply { timeInMillis = start }
        val calEnd = Calendar.getInstance().apply { timeInMillis = end }
        val isSameDay = calStart.get(Calendar.YEAR) == calEnd.get(Calendar.YEAR) &&
                calStart.get(Calendar.DAY_OF_YEAR) == calEnd.get(Calendar.DAY_OF_YEAR)
        if (isSameDay) {
            // Set start to 00:00:00.000
            calStart.set(Calendar.HOUR_OF_DAY, 0)
            calStart.set(Calendar.MINUTE, 0)
            calStart.set(Calendar.SECOND, 0)
            calStart.set(Calendar.MILLISECOND, 0)
            // Set end to 23:59:59.999
            calEnd.set(Calendar.HOUR_OF_DAY, 23)
            calEnd.set(Calendar.MINUTE, 59)
            calEnd.set(Calendar.SECOND, 59)
            calEnd.set(Calendar.MILLISECOND, 999)
        }
        _dateRange.value = calStart.timeInMillis to calEnd.timeInMillis
        onDateRangeChanged(calStart.timeInMillis, calEnd.timeInMillis)
    }

    private fun getDefaultMonthRange(): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        val end = cal.timeInMillis
        cal.set(Calendar.DAY_OF_MONTH, 1)
        val start = cal.timeInMillis
        return start to end
    }

    fun loadInitialPreferences(prefs: SharedPreferences) {
        val savedStart = prefs.getLong(RANGE_START, -1L)
        val savedEnd = prefs.getLong(RANGE_END, -1L)
        val savedMode = prefs.getString(RANGE_MODE, null)
        
        if (savedStart > 0 && savedEnd > 0 && savedMode != null) {
            _dateRangeMode.value = DateRangeMode.valueOf(savedMode)
            _dateRange.value = savedStart to savedEnd
        }
        
        onInitialPreferencesLoaded()
    }

    fun shiftDateRange(forward: Boolean) {
        _prevRange.value = _dateRange.value
        val newRange = calculateShiftedRange(_dateRange.value, _dateRangeMode.value, forward)
        _dateRange.value = newRange
        onDateRangeChanged(newRange.first, newRange.second)
    }

    fun changeDateRangeMode(newMode: DateRangeMode) {
        _dateRangeMode.value = newMode
        val cal = Calendar.getInstance()
        val end = cal.timeInMillis
        cal.add(Calendar.DAY_OF_YEAR, -(newMode.days - 1))
        val start = cal.timeInMillis
        _dateRange.value = start to end
        onDateRangeChanged(start, end)
    }

    fun setDateRangeFromPicker(start: Long, end: Long) {
        _prevRange.value = _dateRange.value
        
        // Patch: expand single-day range to full day
        val calStart = Calendar.getInstance().apply { timeInMillis = start }
        val calEnd = Calendar.getInstance().apply { timeInMillis = end }
        val isSameDay = calStart.get(Calendar.YEAR) == calEnd.get(Calendar.YEAR) &&
                calStart.get(Calendar.DAY_OF_YEAR) == calEnd.get(Calendar.DAY_OF_YEAR)
        
        if (isSameDay) {
            calStart.set(Calendar.HOUR_OF_DAY, 0)
            calStart.set(Calendar.MINUTE, 0)
            calStart.set(Calendar.SECOND, 0)
            calStart.set(Calendar.MILLISECOND, 0)
            calEnd.set(Calendar.HOUR_OF_DAY, 23)
            calEnd.set(Calendar.MINUTE, 59)
            calEnd.set(Calendar.SECOND, 59)
            calEnd.set(Calendar.MILLISECOND, 999)
        }
        
        val newStart = calStart.timeInMillis
        val newEnd = calEnd.timeInMillis
        _dateRange.value = newStart to newEnd
        onDateRangeChanged(newStart, newEnd)
    }

    protected fun calculateShiftedRange(currentRange: Pair<Long, Long>, mode: DateRangeMode, forward: Boolean): Pair<Long, Long> {
        val (start, end) = currentRange
        val daysToShift = if (forward) mode.days else -mode.days
        
        val calStart = Calendar.getInstance().apply { timeInMillis = start }
        val calEnd = Calendar.getInstance().apply { timeInMillis = end }
        
        calStart.add(Calendar.DAY_OF_YEAR, daysToShift)
        calEnd.add(Calendar.DAY_OF_YEAR, daysToShift)
        
        return calStart.timeInMillis to calEnd.timeInMillis
    }

    fun getDateRangeForPreferences(): Triple<Long, Long, String> {
        return Triple(_dateRange.value.first, _dateRange.value.second, _dateRangeMode.value.name)
    }

    // Abstract methods that subclasses must implement
    protected abstract fun onDateRangeChanged(start: Long, end: Long)
    protected abstract fun onInitialPreferencesLoaded()
} 