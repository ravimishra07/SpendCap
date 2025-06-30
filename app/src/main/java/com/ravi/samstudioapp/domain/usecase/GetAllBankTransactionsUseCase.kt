package com.ravi.samstudioapp.domain.usecase

import com.ravi.samstudioapp.data.BankTransactionRepository
import com.ravi.samstudioapp.domain.model.BankTransaction

class GetAllBankTransactionsUseCase(private val repository: BankTransactionRepository) {
    suspend operator fun invoke(): List<BankTransaction> {
        return repository.getAll()
    }
} 