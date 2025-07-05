package com.ravi.samstudioapp.domain.usecase

import android.util.Log
import com.ravi.samstudioapp.data.BankTransactionRepository
import com.ravi.samstudioapp.domain.model.BankTransaction

class InsertBankTransactionUseCase(private val repository: BankTransactionRepository) {
    suspend operator fun invoke(transaction: BankTransaction) {
        Log.d("SamStudio", "InsertBankTransactionUseCase: Inserting transaction: ${transaction.amount} from ${transaction.bankName}")
        try {
            repository.insert(transaction)
            Log.d("SamStudio", "InsertBankTransactionUseCase: Successfully inserted transaction")
        } catch (e: Exception) {
            Log.e("SamStudio", "InsertBankTransactionUseCase: Error inserting transaction", e)
            throw e
        }
    }
} 