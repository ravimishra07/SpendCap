package com.ravi.samstudioapp.data

import com.ravi.samstudioapp.domain.model.BankTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BankTransactionRepository(private val dao: BankTransactionDao) {
    suspend fun insert(transaction: BankTransaction) = withContext(Dispatchers.IO) {
        dao.insert(transaction)
    }
    suspend fun getAll(): List<BankTransaction> = withContext(Dispatchers.IO) {
        dao.getAll()
    }
} 