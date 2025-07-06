package com.ravi.samstudioapp.domain.usecase

import android.util.Log
import com.ravi.samstudioapp.data.BankTransactionRepository
import com.ravi.samstudioapp.domain.model.BankTransaction

class FindExactBankTransactionUseCase(private val repository: BankTransactionRepository) {
    suspend operator fun invoke(messageTime: Long): BankTransaction? {
        Log.d("SamStudio", "FindExactBankTransactionUseCase: Finding transaction with messageTime=$messageTime")
        try {
            val transaction = repository.findExactTransaction(messageTime)
            Log.d("SamStudio", "FindExactBankTransactionUseCase: Found transaction: ${transaction?.messageTime}")
            return transaction
        } catch (e: Exception) {
            Log.e("SamStudio", "FindExactBankTransactionUseCase: Error finding transaction", e)
            throw e
        }
    }
} 