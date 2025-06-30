package com.ravi.samstudioapp.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.ravi.samstudioapp.domain.model.BankTransaction

@Database(entities = [BankTransaction::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bankTransactionDao(): BankTransactionDao
} 