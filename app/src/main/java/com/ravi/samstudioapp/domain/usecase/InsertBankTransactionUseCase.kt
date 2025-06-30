package com.ravi.samstudioapp.domain.usecase

import com.ravi.samstudioapp.data.BankTransactionRepository
import com.ravi.samstudioapp.domain.model.BankTransaction

class InsertBankTransactionUseCase(private val repository: BankTransactionRepository) {
    suspend operator fun invoke(transaction: BankTransaction) {
        repository.insert(transaction)
    }
} 