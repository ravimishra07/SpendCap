package com.ravi.samstudioapp.presentation.main

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ravi.samstudioapp.domain.model.BankTransaction
import com.ravi.samstudioapp.domain.model.ParsedSmsTransaction
import com.ravi.samstudioapp.domain.usecase.GetAllBankTransactionsUseCase
import com.ravi.samstudioapp.domain.usecase.GetBankTransactionsByDateRangeUseCase
import com.ravi.samstudioapp.domain.usecase.InsertBankTransactionUseCase
import com.ravi.samstudioapp.domain.usecase.UpdateBankTransactionUseCase
import com.ravi.samstudioapp.domain.usecase.FindExactBankTransactionUseCase
import com.ravi.samstudioapp.domain.usecase.GetExistingMessageTimesUseCase
import com.ravi.samstudioapp.utils.readAndParseSms
import com.ravi.samstudioapp.utils.MessageParser
import com.ravi.samstudioapp.ui.DateRangeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.runtime.LaunchedEffect
import java.util.Calendar
import android.util.Log
import android.os.Build

class MainViewModel(
    private val getAllTransactions: GetAllBankTransactionsUseCase,
    private val getByDateRange: GetBankTransactionsByDateRangeUseCase,
    private val insertTransaction: InsertBankTransactionUseCase,
    private val updateTransactionUseCase: UpdateBankTransactionUseCase,
    private val findExactTransaction: FindExactBankTransactionUseCase,
    private val getExistingMessageTimes: GetExistingMessageTimesUseCase
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

    // Real-time message detection state
    private val _newMessageDetected = MutableStateFlow<ParsedSmsTransaction?>(null)
    val newMessageDetected: StateFlow<ParsedSmsTransaction?> = _newMessageDetected
    


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
            try {
                insertTransaction(transaction)
                // Only reload if we're not already in a loading state
                if (!_isLoading.value) {
                    loadAllTransactions()
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error adding transaction", e)
            }
        }
    }

    fun updateTransaction(transaction: BankTransaction) {
        viewModelScope.launch(Dispatchers.Default) {
            try {
                updateTransactionUseCase(transaction)
                loadAllTransactions()
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error updating transaction", e)
            }
        }
    }

    // Add a new method to handle both add and update in a single operation
    fun saveTransaction(transaction: BankTransaction) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                // Since messageTime is now the primary key, we can directly insert/update
                // Room will handle conflicts based on the primary key
                insertTransaction(transaction)
                loadAllTransactions()
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error saving transaction", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Finds exact BankTransaction entry from database and overwrites it
     * Uses proper coroutines and threading for database operations
     * @param transaction The BankTransaction to find and overwrite
     */
    fun findAndOverwriteTransaction(transaction: BankTransaction) {
        viewModelScope.launch {
            try {
                Log.d("MainViewModel", "findAndOverwriteTransaction: Starting search for transaction with messageTime: ${transaction.messageTime}")
                Log.d("MainViewModel", "findAndOverwriteTransaction: Transaction details - Amount: ${transaction.amount}, Bank: ${transaction.bankName}, Tags: ${transaction.tags}")
                _isLoading.value = true
                
                // Use IO dispatcher for database operations
                val existingTransaction = withContext(Dispatchers.IO) {
                    findExactTransaction(transaction.messageTime)
                }
                
                if (existingTransaction != null) {
                    Log.d("MainViewModel", "findAndOverwriteTransaction: Found existing transaction with messageTime: ${existingTransaction.messageTime}")
                    Log.d("MainViewModel", "findAndOverwriteTransaction: Existing transaction - Amount: ${existingTransaction.amount}, Bank: ${existingTransaction.bankName}, Tags: ${existingTransaction.tags}")
                    
                    // Update the transaction in database using IO dispatcher
                    withContext(Dispatchers.IO) {
                        updateTransactionUseCase(transaction)
                    }
                    
                    Log.d("MainViewModel", "findAndOverwriteTransaction: Successfully overwrote transaction")
                } else {
                    Log.d("MainViewModel", "findAndOverwriteTransaction: No transaction found with messageTime ${transaction.messageTime}, inserting as new")
                    
                    // If no transaction found with this messageTime, insert as new transaction
                    withContext(Dispatchers.IO) {
                        insertTransaction(transaction)
                    }
                    Log.d("MainViewModel", "findAndOverwriteTransaction: Successfully inserted new transaction (messageTime not found)")
                }
                
                Log.d("MainViewModel", "findAndOverwriteTransaction: Reloading data...")
                // Reload transactions to reflect changes
                loadAllTransactions()
                loadSmsTransactions()
                updateFilteredSmsTransactions()
                Log.d("MainViewModel", "findAndOverwriteTransaction: Data reload completed")
                
            } catch (e: Exception) {
                Log.e("MainViewModel", "findAndOverwriteTransaction: Error finding/overwriting transaction", e)
                Log.e("MainViewModel", "findAndOverwriteTransaction: Error message: ${e.message}")
                Log.e("MainViewModel", "findAndOverwriteTransaction: Error stack trace: ${e.stackTraceToString()}")
            } finally {
                _isLoading.value = false
                Log.d("MainViewModel", "findAndOverwriteTransaction: Operation completed")
            }
        }
    }

    /**
     * Example usage function that demonstrates how to use findAndOverwriteTransaction
     * This function takes a BankTransaction and finds the exact entry to overwrite
     */
    fun processBankTransaction(transaction: BankTransaction) {
        Log.d("MainViewModel", "processBankTransaction: Processing transaction - Amount: ${transaction.amount}, Bank: ${transaction.bankName}")
        
        // Use the findAndOverwriteTransaction function with proper coroutines and threading
        findAndOverwriteTransaction(transaction)
    }

    fun syncFromSms(context: Context, onComplete: ((Int) -> Unit)? = null) {
        Log.d("SamStudio", "syncFromSms: Starting sync process")
        viewModelScope.launch {
            Log.d("SamStudio", "syncFromSms: Setting loading to true")
            _isLoading.value = true
            try {
                Log.d("SamStudio", "syncFromSms: About to read and parse SMS")
                val smsTxns = withContext(Dispatchers.IO) { 
                    Log.d("SamStudio", "syncFromSms: Calling readAndParseSms")
                    readAndParseSms(context) 
                }
                Log.d("SamStudio", "syncFromSms: SMS parsing completed, found ${smsTxns.size} transactions")
                
                Log.d("SamStudio", "syncFromSms: Mapping SMS transactions to BankTransactions")
                val newTxns = smsTxns.map { sms ->
                    BankTransaction(
                        amount = sms.amount,
                        bankName = sms.bankName,
                        messageTime = sms.messageTime,
                        tags = sms.rawMessage,
                        count = null
                    )
                }
                
                // Efficiently check for existing messageTimes in bulk
                val messageTimesToCheck = newTxns.map { it.messageTime }
                Log.d("SamStudio", "syncFromSms: Checking ${messageTimesToCheck.size} message times for duplicates")
                val existingMessageTimes = withContext(Dispatchers.IO) { 
                    getExistingMessageTimes(messageTimesToCheck) 
                }
                Log.d("SamStudio", "syncFromSms: Found ${existingMessageTimes.size} existing message times")
                
                // Filter out transactions that already exist
                val uniqueTxns = newTxns.filter { txn ->
                    !existingMessageTimes.contains(txn.messageTime)
                }
                Log.d("SamStudio", "syncFromSms: After filtering duplicates by messageTime, ${uniqueTxns.size} new transactions to insert")
                
                // Batch insert if possible, else insert one by one
                Log.d("SamStudio", "syncFromSms: Starting to insert new transactions")
                uniqueTxns.forEach { txn -> 
                    Log.d("SamStudio", "syncFromSms: Inserting transaction: ${txn.amount} from ${txn.bankName}")
                    withContext(Dispatchers.IO) { insertTransaction(txn) } 
                }
                Log.d("SamStudio", "syncFromSms: All transactions inserted, reloading data")
                
                loadAllTransactions()
                loadSmsTransactions()
                updateFilteredSmsTransactions()
                Log.d("SamStudio", "syncFromSms: Sync completed successfully")
                
                // Call completion callback with number of new transactions
                onComplete?.invoke(uniqueTxns.size)
            } catch (e: Exception) {
                Log.e("SamStudio", "syncFromSms: Error syncing SMS", e)
                Log.e("SamStudio", "syncFromSms: Error message: ${e.message}")
                Log.e("SamStudio", "syncFromSms: Error stack trace: ${e.stackTraceToString()}")
                // Call completion callback with error (-1 indicates error)
                onComplete?.invoke(-1)
            } finally {
                Log.d("SamStudio", "syncFromSms: Setting loading to false")
                _isLoading.value = false
            }
        }
        Log.d("SamStudio", "syncFromSms: Function call completed")
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

    fun loadSmsTransactions(maxDays: Int = 30) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val cutoff = now - (maxDays * 24 * 60 * 60 * 1000L)

            val allTransactions = getAllTransactions()
                .sortedByDescending { it.messageTime } // Newest first

            val recentTransactions = allTransactions.takeWhile { it.messageTime >= cutoff }

            _smsTransactions.value = recentTransactions.map {
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
    }

    fun changeDateRangeMode(newMode: DateRangeMode) {
        _dateRangeMode.value = newMode
        val cal = Calendar.getInstance()
        val end = cal.timeInMillis
        cal.add(Calendar.DAY_OF_YEAR, -(newMode.days - 1))
        val start = cal.timeInMillis
        _dateRange.value = start to end
        updateFilteredSmsTransactions()
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
    }

    fun addDummyTransactions() {
        viewModelScope.launch {
            val dummyList = listOf(
                BankTransaction(
                    amount = 100.0,
                    bankName = "HDFC",
                    tags = "Transaction successful! Your account has been credited with Rs.100",
                    messageTime = System.currentTimeMillis()
                ),
                BankTransaction(
                    amount = 200.0,
                    bankName = "ICICI",
                    tags = "Low balance alert! Your account balance is insufficient for this transaction",
                    messageTime = System.currentTimeMillis()
                ),
                BankTransaction(
                    amount = 50.0,
                    bankName = "SBI",
                    tags = "OTP verification required for transaction of Rs.50",
                    messageTime = System.currentTimeMillis()
                ),
                BankTransaction(
                    amount = 80.0,
                    bankName = "Axis",
                    tags = "Transaction limit exceeded for this operation",
                    messageTime = System.currentTimeMillis()
                ),
                BankTransaction(
                    amount = 120.0,
                    bankName = "Kotak",
                    tags = "Suspicious activity detected on your account",
                    messageTime = System.currentTimeMillis()
                )
            )
            dummyList.forEach { insertTransaction(it) }
            loadAllTransactions()
            loadSmsTransactions()
            updateFilteredSmsTransactions()
        }
    }
    

    
    /**
     * Test SMS receiver functionality
     */
    /*
    fun testSmsReceiver(context: Context) {
        Log.d("MainViewModel", "Testing SMS receiver...")
        
        // Create a test SMS intent
        val testIntent = Intent(android.provider.Telephony.Sms.Intents.SMS_RECEIVED_ACTION)
        testIntent.putExtra("pdus", arrayOf<Byte>())
        
        // Send broadcast to test receiver
        context.sendBroadcast(testIntent)
        Log.d("MainViewModel", "Test SMS broadcast sent")
    }
    */

    private fun calculateShiftedRange(currentRange: Pair<Long, Long>, mode: DateRangeMode, forward: Boolean): Pair<Long, Long> {
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

    init {
        val (start, end) = _dateRange.value
        loadTransactionsByDateRange(start, end)
    }
    

    
    /**
     * Handle new SMS message using same parsing logic
     */
    private fun handleNewSms(messageBody: String, timestamp: Long) {
        viewModelScope.launch {
            try {
                Log.d("MainViewModel", "New SMS received: $messageBody")
                // Use same parsing logic as SmsUtils.kt
                val parsedTransaction = MessageParser.parseNewMessage(messageBody, timestamp)
                if (parsedTransaction != null) {
                    Log.d("MainViewModel", "Parsed transaction: ₹${parsedTransaction.amount} from ${parsedTransaction.bankName}")
                    // Show popup for new transaction
                    _newMessageDetected.value = parsedTransaction
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error handling new SMS", e)
            }
        }
    }
    
    /**
     * Handle new SMS from MainActivity (called by ContentObserver)
     */
    fun handleNewSmsFromActivity(messageBody: String, timestamp: Long) {
        Log.d("MainViewModel", "handleNewSmsFromActivity called with: $messageBody")
        handleNewSms(messageBody, timestamp)
    }
    
    /**
     * Dismiss the new message popup
     */
    fun dismissNewMessagePopup() {
        _newMessageDetected.value = null
    }
} 