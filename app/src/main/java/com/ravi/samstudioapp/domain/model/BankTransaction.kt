package com.ravi.samstudioapp.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(
    tableName = "bank_transactions",
    indices = [Index(value = ["amount", "bankName", "messageTime"], unique = true)]
)
data class BankTransaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val amount: Double,
    val bankName: String,
    val tags: String = "",
    val messageTime: Long,
    val count: Int? = null,
    val category: String = "Other",
    val verified: Boolean = false
) 