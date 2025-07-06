package com.ravi.samstudioapp.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.ravi.samstudioapp.domain.model.BankTransaction

@Database(entities = [BankTransaction::class], version = 2)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bankTransactionDao(): BankTransactionDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null
        
        // Migration from version 1 to 2: Change primary key from id to messageTime
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create a new table with the new schema
                database.execSQL(
                    "CREATE TABLE bank_transactions_new (" +
                    "messageTime INTEGER NOT NULL PRIMARY KEY, " +
                    "amount REAL NOT NULL, " +
                    "bankName TEXT NOT NULL, " +
                    "tags TEXT NOT NULL, " +
                    "count INTEGER, " +
                    "category TEXT NOT NULL, " +
                    "verified INTEGER NOT NULL" +
                    ")"
                )
                
                // Copy data from old table to new table, handling potential duplicates by keeping only the first occurrence
                database.execSQL(
                    "INSERT INTO bank_transactions_new (messageTime, amount, bankName, tags, count, category, verified) " +
                    "SELECT messageTime, amount, bankName, tags, count, category, verified FROM bank_transactions " +
                    "GROUP BY messageTime"
                )
                
                // Drop the old table
                database.execSQL("DROP TABLE bank_transactions")
                
                // Rename the new table to the original name
                database.execSQL("ALTER TABLE bank_transactions_new RENAME TO bank_transactions")
            }
        }
        
        fun getInstance(context: android.content.Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: androidx.room.Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_db"
                )
                .addMigrations(MIGRATION_1_2)
                .build().also { INSTANCE = it }
            }
        }
    }
} 