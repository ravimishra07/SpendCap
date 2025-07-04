package com.ravi.samstudioapp.domain.usecase

import com.ravi.samstudioapp.data.BankTransactionRepository
import com.ravi.samstudioapp.domain.model.BankTransaction

class GetBankTransactionsByDateRangeUseCase(private val repository: BankTransactionRepository) {
    suspend operator fun invoke(start: Long, end: Long): List<BankTransaction> {
        return repository.getByDateRange(start, end)
    }
} 