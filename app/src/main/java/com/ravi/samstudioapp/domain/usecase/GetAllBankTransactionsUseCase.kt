package com.ravi.samstudioapp.domain.usecase

import android.util.Log
import com.ravi.samstudioapp.data.BankTransactionRepository
import com.ravi.samstudioapp.domain.model.BankTransaction

class GetAllBankTransactionsUseCase(private val repository: BankTransactionRepository) {
    suspend operator fun invoke(): List<BankTransaction> {
        Log.d("SamStudio", "GetAllBankTransactionsUseCase: Getting all transactions")
        try {
            val transactions = repository.getAll()
            Log.d("SamStudio", "GetAllBankTransactionsUseCase: Retrieved ${transactions.size} transactions")
            return transactions
        } catch (e: Exception) {
            Log.e("SamStudio", "GetAllBankTransactionsUseCase: Error getting transactions", e)
            throw e
        }
    }
} 