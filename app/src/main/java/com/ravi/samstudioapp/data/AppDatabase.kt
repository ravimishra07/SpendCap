package com.ravi.samstudioapp.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.ravi.samstudioapp.domain.model.BankTransaction

@Database(entities = [BankTransaction::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bankTransactionDao(): BankTransactionDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null
        fun getInstance(context: android.content.Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: androidx.room.Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_db"
                ).build().also { INSTANCE = it }
            }
        }
    }
} 