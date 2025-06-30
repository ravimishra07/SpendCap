package com.ravi.samstudioapp.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ravi.samstudioapp.domain.model.BankTransaction

@Dao
interface BankTransactionDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(transaction: BankTransaction)

    @Query("SELECT * FROM bank_transactions ORDER BY messageTime DESC")
    suspend fun getAll(): List<BankTransaction>

    @Query("SELECT * FROM bank_transactions WHERE messageTime BETWEEN :start AND :end ORDER BY messageTime DESC")
    suspend fun getByDateRange(start: Long, end: Long): List<BankTransaction>
} 