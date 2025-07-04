package com.ravi.samstudioapp.domain.model

data class ParsedSmsTransaction(
    val amount: Double,
    val bankName: String,
    val messageTime: Long,
    val rawMessage: String
) 