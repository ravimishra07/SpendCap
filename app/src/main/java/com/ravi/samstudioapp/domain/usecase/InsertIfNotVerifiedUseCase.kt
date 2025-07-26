package com.ravi.samstudioapp.domain.usecase

import android.util.Log
import com.ravi.samstudioapp.data.BankTransactionRepository
import com.ravi.samstudioapp.domain.model.BankTransaction

class InsertIfNotVerifiedUseCase(private val repository: BankTransactionRepository) {
    suspend operator fun invoke(transaction: BankTransaction) {
        val existing = repository.findVerifiedTransaction(transaction.amount, transaction.bankName)
        if (existing == null) {
            repository.insert(transaction.copy(verified = false))
        } else {
            Log.d("InsertIfNotVerifiedUseCase", "Skipping insert: verified entry already exists")
        }
    }
}