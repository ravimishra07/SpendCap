package com.ravi.samstudioapp.presentation.main

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ravi.samstudioapp.domain.model.BankTransaction
import com.ravi.samstudioapp.domain.model.ParsedSmsTransaction
import com.ravi.samstudioapp.domain.usecase.GetAllBankTransactionsUseCase
import com.ravi.samstudioapp.domain.usecase.GetBankTransactionsByDateRangeUseCase
import com.ravi.samstudioapp.domain.usecase.InsertBankTransactionUseCase
import com.ravi.samstudioapp.domain.usecase.UpdateBankTransactionUseCase
import com.ravi.samstudioapp.utils.readAndParseSms
import com.ravi.samstudioapp.ui.DateRangeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.runtime.LaunchedEffect
import java.util.Calendar

class MainViewModel(
    private val getAllTransactions: GetAllBankTransactionsUseCase,
    private val getByDateRange: GetBankTransactionsByDateRangeUseCase,
    private val insertTransaction: InsertBankTransactionUseCase,
    private val updateTransaction: UpdateBankTransactionUseCase
) : ViewModel() {
    private val _transactions = MutableStateFlow<List<BankTransaction>>(emptyList())
    val transactions: StateFlow<List<BankTransaction>> = _transactions

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    // Date range state
    private val _dateRange = MutableStateFlow(getDefaultMonthRange())
    val dateRange: StateFlow<Pair<Long, Long>> = _dateRange

    // New state flows for LoadMainScreen functionality
    private val _smsTransactions = MutableStateFlow<List<ParsedSmsTransaction>>(emptyList())
    val smsTransactions: StateFlow<List<ParsedSmsTransaction>> = _smsTransactions

    private val _filteredSmsTransactions = MutableStateFlow<List<ParsedSmsTransaction>>(emptyList())
    val filteredSmsTransactions: StateFlow<List<ParsedSmsTransaction>> = _filteredSmsTransactions

    private val _dateRangeMode = MutableStateFlow(DateRangeMode.DAILY)
    val dateRangeMode: StateFlow<DateRangeMode> = _dateRangeMode

    private val _prevRange = MutableStateFlow<Pair<Long, Long>?>(null)
    val prevRange: StateFlow<Pair<Long, Long>?> = _prevRange

    // Constants for SharedPreferences
    companion object {
        const val CORE_NAME = "samstudio_prefs"
        const val RANGE_START = "date_range_start"
        const val RANGE_END = "date_range_end"
        const val RANGE_MODE = "date_range_mode"
    }

    fun setDateRange(start: Long, end: Long) {
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
        loadTransactionsByDateRange(calStart.timeInMillis, calEnd.timeInMillis)
        updateFilteredSmsTransactions()
    }

    private fun getDefaultMonthRange(): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        val end = cal.timeInMillis
        cal.set(Calendar.DAY_OF_MONTH, 1)
        val start = cal.timeInMillis
        return start to end
    }

    fun loadAllTransactions() {
        viewModelScope.launch {
            _isLoading.value = true
            _transactions.value = getAllTransactions()
            _isLoading.value = false
        }
    }

    fun loadTransactionsByDateRange(start: Long, end: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            _transactions.value = getByDateRange(start, end)
            _isLoading.value = false
        }
    }

    fun addTransaction(transaction: BankTransaction) {
        viewModelScope.launch {
            insertTransaction(transaction)
            loadAllTransactions()
        }
    }

    fun updateTransaction(transaction: BankTransaction) {
        viewModelScope.launch {
            updateTransaction(transaction)
            loadAllTransactions()
        }
    }

    fun syncFromSms(context: Context) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val smsTxns = readAndParseSms(context)
                smsTxns.forEach { sms ->
                    val txn = BankTransaction(
                        amount = sms.amount,
                        bankName = sms.bankName,
                        messageTime = sms.messageTime,
                        tags = sms.rawMessage,
                        count = null
                    )
                    insertTransaction(txn)
                }
                loadAllTransactions()
                loadSmsTransactions()
                updateFilteredSmsTransactions()
            } catch (e: Exception) {
                // Handle error
            } finally {
                _isLoading.value = false
            }
        }
    }

    // New methods for LoadMainScreen functionality

    fun loadSmsTransactions() {
        viewModelScope.launch {
            val allTransactions = getAllTransactions()
            _smsTransactions.value = allTransactions.map {
                ParsedSmsTransaction(
                    amount = it.amount,
                    bankName = it.bankName,
                    messageTime = it.messageTime,
                    rawMessage = it.tags
                )
            }
            updateFilteredSmsTransactions()
        }
    }

    private fun updateFilteredSmsTransactions() {
        val currentRange = _dateRange.value
        val filtered = _smsTransactions.value.filter {
            it.messageTime in currentRange.first..currentRange.second
        }
        _filteredSmsTransactions.value = filtered
    }

    fun loadInitialPreferences(prefs: SharedPreferences) {
        val savedStart = prefs.getLong(RANGE_START, -1L)
        val savedEnd = prefs.getLong(RANGE_END, -1L)
        val savedMode = prefs.getString(RANGE_MODE, null)
        
        if (savedStart > 0 && savedEnd > 0 && savedMode != null) {
            _dateRangeMode.value = DateRangeMode.valueOf(savedMode)
            _dateRange.value = savedStart to savedEnd
        }
        
        loadSmsTransactions()
        updateFilteredSmsTransactions()
    }

    fun shiftDateRange(forward: Boolean) {
        _prevRange.value = _dateRange.value
        val newRange = calculateShiftedRange(_dateRange.value, _dateRangeMode.value, forward)
        _dateRange.value = newRange
        updateFilteredSmsTransactions()
        saveDateRangeToPreferences()
    }

    fun changeDateRangeMode(newMode: DateRangeMode) {
        _dateRangeMode.value = newMode
        val cal = Calendar.getInstance()
        val end = cal.timeInMillis
        cal.add(Calendar.DAY_OF_YEAR, -(newMode.days - 1))
        val start = cal.timeInMillis
        _dateRange.value = start to end
        updateFilteredSmsTransactions()
        saveDateRangeToPreferences()
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
        updateFilteredSmsTransactions()
        saveDateRangeToPreferences()
    }

    fun addDummyTransactions() {
        viewModelScope.launch {
            val dummyList = listOf(
                BankTransaction(
                    amount = 100.0,
                    bankName = "HDFC",
                    tags = "Food",
                    messageTime = System.currentTimeMillis()
                ),
                BankTransaction(
                    amount = 200.0,
                    bankName = "ICICI",
                    tags = "Travel",
                    messageTime = System.currentTimeMillis()
                ),
                BankTransaction(
                    amount = 50.0,
                    bankName = "SBI",
                    tags = "Cigarette",
                    messageTime = System.currentTimeMillis()
                ),
                BankTransaction(
                    amount = 80.0,
                    bankName = "Axis",
                    tags = "Food",
                    messageTime = System.currentTimeMillis()
                ),
                BankTransaction(
                    amount = 120.0,
                    bankName = "Kotak",
                    tags = "Other",
                    messageTime = System.currentTimeMillis()
                )
            )
            dummyList.forEach { insertTransaction(it) }
            loadAllTransactions()
            loadSmsTransactions()
            updateFilteredSmsTransactions()
        }
    }

    private fun calculateShiftedRange(currentRange: Pair<Long, Long>, mode: DateRangeMode, forward: Boolean): Pair<Long, Long> {
        val (start, end) = currentRange
        val daysToShift = if (forward) mode.days else -mode.days
        
        val calStart = Calendar.getInstance().apply { timeInMillis = start }
        val calEnd = Calendar.getInstance().apply { timeInMillis = end }
        
        calStart.add(Calendar.DAY_OF_YEAR, daysToShift)
        calEnd.add(Calendar.DAY_OF_YEAR, daysToShift)
        
        return calStart.timeInMillis to calEnd.timeInMillis
    }

    private fun saveDateRangeToPreferences() {
        // This will be called from the UI with the actual SharedPreferences instance
        // We'll pass the values to the UI layer
    }

    fun getDateRangeForPreferences(): Triple<Long, Long, String> {
        return Triple(_dateRange.value.first, _dateRange.value.second, _dateRangeMode.value.name)
    }

    init {
        val (start, end) = _dateRange.value
        loadTransactionsByDateRange(start, end)
    }
} 