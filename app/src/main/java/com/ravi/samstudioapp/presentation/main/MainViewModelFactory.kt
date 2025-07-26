package com.ravi.samstudioapp.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ravi.samstudioapp.domain.usecase.GetAllBankTransactionsUseCase
import com.ravi.samstudioapp.domain.usecase.GetBankTransactionsByDateRangeUseCase
import com.ravi.samstudioapp.domain.usecase.InsertBankTransactionUseCase
import com.ravi.samstudioapp.domain.usecase.UpdateBankTransactionUseCase
import com.ravi.samstudioapp.domain.usecase.FindExactBankTransactionUseCase
import com.ravi.samstudioapp.domain.usecase.GetExistingMessageTimesUseCase
import com.ravi.samstudioapp.domain.usecase.InsertIfNotVerifiedUseCase
import com.ravi.samstudioapp.domain.usecase.MarkBankTransactionAsDeletedUseCase
import com.ravi.samstudioapp.presentation.screens.expense.ExpenseViewModel

class MainViewModelFactory(
    private val getAllTransactions: GetAllBankTransactionsUseCase,
    private val getByDateRange: GetBankTransactionsByDateRangeUseCase,
    private val insertTransaction: InsertBankTransactionUseCase,
    private val updateTransaction: UpdateBankTransactionUseCase,
    private val findExactTransaction: FindExactBankTransactionUseCase,
    private val getExistingMessageTimes: GetExistingMessageTimesUseCase,
    private val insertIfNotVerifiedUseCase: InsertIfNotVerifiedUseCase,
    private val markAsDeletedUseCase: MarkBankTransactionAsDeletedUseCase
) : ViewModelProvider.Factory {
    
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ExpenseViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ExpenseViewModel(
                getAllTransactions,
                getByDateRange,
                insertTransaction,
                updateTransaction,
                findExactTransaction,
                getExistingMessageTimes,
                insertIfNotVerifiedUseCase,
                markAsDeletedUseCase
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
} 