package com.ravi.samstudioapp.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ravi.samstudioapp.domain.usecase.GetAllBankTransactionsUseCase
import com.ravi.samstudioapp.domain.usecase.GetBankTransactionsByDateRangeUseCase
import com.ravi.samstudioapp.domain.usecase.InsertBankTransactionUseCase
import com.ravi.samstudioapp.domain.usecase.UpdateBankTransactionUseCase

class MainViewModelFactory(
    private val getAllTransactions: GetAllBankTransactionsUseCase,
    private val getByDateRange: GetBankTransactionsByDateRangeUseCase,
    private val insertTransaction: InsertBankTransactionUseCase,
    private val updateTransaction: UpdateBankTransactionUseCase
) : ViewModelProvider.Factory {
    
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(
                getAllTransactions,
                getByDateRange,
                insertTransaction,
                updateTransaction
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
} 