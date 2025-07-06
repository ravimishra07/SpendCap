package com.ravi.samstudioapp.domain.usecase

import android.util.Log
import com.ravi.samstudioapp.data.BankTransactionRepository
import com.ravi.samstudioapp.domain.model.BankTransaction

class FindExactBankTransactionUseCase(private val repository: BankTransactionRepository) {
    suspend operator fun invoke(id: Int): BankTransaction? {
        Log.d("SamStudio", "FindExactBankTransactionUseCase: Finding transaction with id=$id")
        try {
            val transaction = repository.findExactTransaction(id)
            Log.d("SamStudio", "FindExactBankTransactionUseCase: Found transaction: ${transaction?.id}")
            return transaction
        } catch (e: Exception) {
            Log.e("SamStudio", "FindExactBankTransactionUseCase: Error finding transaction", e)
            throw e
        }
    }
} 