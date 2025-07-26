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

    suspend fun getByDateRange(start: Long, end: Long): List<BankTransaction> = withContext(Dispatchers.IO) {
        dao.getByDateRange(start, end)
    }

    suspend fun findExactTransaction(messageTime: Long): BankTransaction? = withContext(Dispatchers.IO) {
        dao.findExactTransaction(messageTime)
    }

    suspend fun getExistingMessageTimes(messageTimes: List<Long>): List<Long> = withContext(Dispatchers.IO) {
        dao.getExistingMessageTimes(messageTimes)
    }

    suspend fun update(transaction: BankTransaction) = withContext(Dispatchers.IO) {
        dao.update(transaction)
    }

    suspend fun findVerifiedTransaction(amount: Double, bankName: String): BankTransaction? = withContext(Dispatchers.IO) {
        dao.findVerifiedTransaction(amount, bankName)
    }

    suspend fun insertIfNotVerified(transaction: BankTransaction) = withContext(Dispatchers.IO) {
        val existing = dao.findVerifiedTransaction(transaction.amount, transaction.bankName)
        if (existing == null) {
            dao.insert(transaction.copy(verified = false))
        } else {
            // Skipping insert since a verified version exists
        }
    }
}