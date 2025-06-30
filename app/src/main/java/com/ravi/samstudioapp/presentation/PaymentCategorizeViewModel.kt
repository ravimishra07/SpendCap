package com.ravi.samstudioapp.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ravi.samstudioapp.domain.model.BankTransaction
import com.ravi.samstudioapp.domain.usecase.InsertBankTransactionUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class SaveState {
    object Idle : SaveState()
    object Loading : SaveState()
    object Success : SaveState()
    data class Error(val message: String) : SaveState()
}

class PaymentCategorizeViewModel(
    private val insertUseCase: InsertBankTransactionUseCase
) : ViewModel() {
    private val _saveState = MutableStateFlow<SaveState>(SaveState.Idle)
    val saveState: StateFlow<SaveState> = _saveState

    fun saveTransaction(transaction: BankTransaction) {
        _saveState.value = SaveState.Loading
        viewModelScope.launch {
            try {
                insertUseCase(transaction)
                _saveState.value = SaveState.Success
            } catch (e: Exception) {
                _saveState.value = SaveState.Error(e.message ?: "Unknown error")
            }
        }
    }
} 