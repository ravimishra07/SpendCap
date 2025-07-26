package com.ravi.samstudioapp.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.ravi.samstudioapp.domain.model.BankTransaction

@Database(entities = [BankTransaction::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bankTransactionDao(): BankTransactionDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_db"
                )
                    .fallbackToDestructiveMigration() // 👈 resets DB on schema change
                    .build().also { INSTANCE = it }
            }
        }
    }
}