package com.ravi.samstudioapp.domain.usecase

import com.ravi.samstudioapp.data.BankTransactionRepository

class MarkBankTransactionAsDeletedUseCase(private val repository: BankTransactionRepository) {
    suspend operator fun invoke(messageTime: Long) {
        repository.markAsDeleted(messageTime)
    }
} 