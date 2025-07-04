package com.ravi.samstudioapp.presentation.main

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ravi.samstudioapp.domain.model.BankTransaction
import com.ravi.samstudioapp.domain.model.ParsedSmsTransaction
import com.ravi.samstudioapp.domain.usecase.GetAllBankTransactionsUseCase
import com.ravi.samstudioapp.domain.usecase.GetBankTransactionsByDateRangeUseCase
import com.ravi.samstudioapp.domain.usecase.InsertBankTransactionUseCase
import com.ravi.samstudioapp.domain.usecase.UpdateBankTransactionUseCase
import com.ravi.samstudioapp.utils.readAndParseSms
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
            val smsTxns = readAndParseSms(context)
            smsTxns.forEach { sms ->
                val txn = com.ravi.samstudioapp.domain.model.BankTransaction(
                    amount = sms.amount,
                    bankName = sms.bankName,
                    messageTime = sms.messageTime,
                    tags = "SMS",
                    count = null
                )
                insertTransaction(txn)
            }
            loadAllTransactions()
        }
    }

    init {
        val (start, end) = _dateRange.value
        loadTransactionsByDateRange(start, end)
    }
} 