package com.ravi.samstudioapp.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ravi.samstudioapp.domain.model.BankTransaction

@Dao
interface BankTransactionDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(transaction: BankTransaction)

    @Query("SELECT * FROM bank_transactions WHERE deleted = 0 ORDER BY messageTime DESC")
    suspend fun getAll(): List<BankTransaction>

    @Query("SELECT * FROM bank_transactions WHERE messageTime BETWEEN :start AND :end AND deleted = 0 ORDER BY messageTime DESC")
    suspend fun getByDateRange(start: Long, end: Long): List<BankTransaction>

    @Query("SELECT * FROM bank_transactions WHERE messageTime = :messageTime LIMIT 1")
    suspend fun findExactTransaction(messageTime: Long): BankTransaction?

    @Query("SELECT messageTime FROM bank_transactions WHERE messageTime IN (:messageTimes)")
    suspend fun getExistingMessageTimes(messageTimes: List<Long>): List<Long>

    @Update
    suspend fun update(transaction: BankTransaction)

    @Query("SELECT * FROM bank_transactions WHERE amount = :amount AND bankName = :bankName AND verified = 1 LIMIT 1")
    suspend fun findVerifiedTransaction(amount: Double, bankName: String): BankTransaction?

    @Query("UPDATE bank_transactions SET deleted = 1 WHERE messageTime = :messageTime")
    suspend fun markAsDeleted(messageTime: Long)
}