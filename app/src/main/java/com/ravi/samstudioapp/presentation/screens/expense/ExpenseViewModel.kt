package com.ravi.samstudioapp.presentation.screens.expense

import android.content.Context
import android.util.Log
import androidx.lifecycle.viewModelScope
import com.ravi.samstudioapp.domain.model.BankTransaction
import com.ravi.samstudioapp.domain.usecase.FindExactBankTransactionUseCase
import com.ravi.samstudioapp.domain.usecase.GetAllBankTransactionsUseCase
import com.ravi.samstudioapp.domain.usecase.GetBankTransactionsByDateRangeUseCase
import com.ravi.samstudioapp.domain.usecase.GetExistingMessageTimesUseCase
import com.ravi.samstudioapp.domain.usecase.InsertBankTransactionUseCase
import com.ravi.samstudioapp.domain.usecase.InsertIfNotVerifiedUseCase
import com.ravi.samstudioapp.domain.usecase.MarkBankTransactionAsDeletedUseCase
import com.ravi.samstudioapp.domain.usecase.UpdateBankTransactionUseCase
import com.ravi.samstudioapp.presentation.main.BaseViewModel
import com.ravi.samstudioapp.utils.MessageParser
import com.ravi.samstudioapp.utils.readAndParseSms
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ExpenseViewModel(
    private val getAllTransactions: GetAllBankTransactionsUseCase,
    private val getByDateRange: GetBankTransactionsByDateRangeUseCase,
    private val insertTransaction: InsertBankTransactionUseCase,
    private val updateTransactionUseCase: UpdateBankTransactionUseCase,
    private val findExactTransaction: FindExactBankTransactionUseCase,
    private val getExistingMessageTimes: GetExistingMessageTimesUseCase,
    private val insertIfNotVerifiedUseCase: InsertIfNotVerifiedUseCase,
    private val markAsDeletedUseCase: MarkBankTransactionAsDeletedUseCase
) : BaseViewModel() {
    private val _transactions = MutableStateFlow<List<BankTransaction>>(emptyList())
    val transactions: StateFlow<List<BankTransaction>> = _transactions

    // New state flows for LoadMainScreen functionality
    private val _smsTransactions = MutableStateFlow<List<BankTransaction>>(emptyList())
    val smsTransactions: StateFlow<List<BankTransaction>> = _smsTransactions

    private val _filteredSmsTransactions = MutableStateFlow<List<BankTransaction>>(emptyList())
    val filteredSmsTransactions: StateFlow<List<BankTransaction>> = _filteredSmsTransactions

    // Real-time message detection state
    private val _newMessageDetected = MutableStateFlow<BankTransaction?>(null)
    val newMessageDetected: StateFlow<BankTransaction?> = _newMessageDetected

    // Override abstract methods from BaseViewModel
    override fun onDateRangeChanged(start: Long, end: Long) {
        loadTransactionsByDateRange(start, end)
        updateFilteredSmsTransactions()
    }

    override fun onInitialPreferencesLoaded() {
        loadSmsTransactions()
        updateFilteredSmsTransactions()
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
                _isLoading.value = true

                // Use IO dispatcher for database operations
                val existingTransaction = withContext(Dispatchers.IO) {
                    findExactTransaction(transaction.messageTime)
                }

                if (existingTransaction != null) {

                    // Update the transaction in database using IO dispatcher
                    withContext(Dispatchers.IO) {
                        updateTransactionUseCase(transaction)
                    }

                } else {
                    // If no transaction found with this messageTime, insert as new transaction
                    withContext(Dispatchers.IO) {
                        insertTransaction(transaction)
                    }
                }

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

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val smsTxns = withContext(Dispatchers.IO) {
                    readAndParseSms(context)
                }

                val newTxns = smsTxns.map { sms ->
                    BankTransaction(
                        amount = sms.amount,
                        bankName = sms.bankName,
                        messageTime = sms.messageTime,
                        tags = sms.tags,
                        count = null
                    )
                }

                // Efficiently check for existing messageTimes in bulk
                val messageTimesToCheck = newTxns.map { it.messageTime }
                val existingMessageTimes = withContext(Dispatchers.IO) {
                    getExistingMessageTimes(messageTimesToCheck)
                }

                // Filter out transactions that already exist
                val uniqueTxns = newTxns.filter { txn ->
                    !existingMessageTimes.contains(txn.messageTime)
                }
                uniqueTxns.forEach { txn ->
                    withContext(Dispatchers.IO) { insertIfNotVerifiedUseCase(txn) }
                }
                loadAllTransactions()
                loadSmsTransactions()
                updateFilteredSmsTransactions()

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
            allTransactions.filter { it.deleted }.forEach { Log.d("MainViewModel", "DELETED Txn: ${it.messageTime}, deleted: ${it.deleted}") }
            val nonDeleted = allTransactions.filter { !it.deleted }
            _smsTransactions.value = nonDeleted.map {
                BankTransaction(
                    messageTime = it.messageTime,
                    amount = it.amount,
                    bankName = it.bankName,
                    tags = it.tags,
                    count = it.count,
                    category = it.category,
                    verified = it.verified
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
            allTransactions.filter { it.deleted }.forEach { Log.d("MainViewModel", "DELETED Txn: ${it.messageTime}, deleted: ${it.deleted}") }
            val recentTransactions = allTransactions.takeWhile { it.messageTime >= cutoff }
            val nonDeleted = recentTransactions.filter { !it.deleted }
            _smsTransactions.value = nonDeleted.map {
                BankTransaction(
                    messageTime = it.messageTime,
                    amount = it.amount,
                    bankName = it.bankName,
                    tags = it.tags,
                    count = it.count,
                    category = it.category,
                    verified = it.verified
                )
            }

            updateFilteredSmsTransactions()
        }
    }

    private fun updateFilteredSmsTransactions() {
        val currentRange = _dateRange.value
        val filtered = _smsTransactions.value.filter {
            it.messageTime in currentRange.first..currentRange.second && !it.deleted
        }
        _filteredSmsTransactions.value = filtered
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
     * Handle new SMS message using same parsing logic
     */
    private fun handleNewSms(messageBody: String, timestamp: Long) {
        viewModelScope.launch {
            try {
                Log.d("MainViewModel", "🔄 handleNewSms called with: ${messageBody.take(50)}...")
                // Use same parsing logic as SmsUtils.kt
                val parsedTransaction = MessageParser.parseNewMessage(messageBody, timestamp)
                if (parsedTransaction != null) {
                    Log.d("MainViewModel", "✅ Parsed transaction: ₹${parsedTransaction.amount} from ${parsedTransaction.bankName}")
                    // Show popup for new transaction
                    _newMessageDetected.value = parsedTransaction
                    Log.d("MainViewModel", "🎉 Popup should now be visible!")
                } else {
                    Log.d("MainViewModel", "❌ Could not parse transaction from SMS")
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

    /**
     * Test method to manually trigger popup
     */
    fun testPopup() {
        Log.d("MainViewModel", "🧪 Testing popup with dummy transaction")
        val testTransaction = BankTransaction(
            messageTime = System.currentTimeMillis(),
            amount = 100.0,
            bankName = "HDFC",
            tags = "Test transaction for ₹100 from HDFC"
        )
        _newMessageDetected.value = testTransaction
        Log.d("MainViewModel", "🧪 Test popup triggered!")
    }

    fun markTransactionAsDeleted(messageTime: Long) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    markAsDeletedUseCase(messageTime)
                }
                loadSmsTransactions() // Instantly refresh the list after delete
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error marking transaction as deleted", e)
            }
        }
    }

    init {
        val (start, end) = _dateRange.value
        loadTransactionsByDateRange(start, end)
    }
}