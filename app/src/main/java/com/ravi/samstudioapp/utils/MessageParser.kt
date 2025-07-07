package com.ravi.samstudioapp.utils

import android.util.Log
import com.ravi.samstudioapp.domain.model.BankTransaction

object MessageParser {
    
    // Same bank keywords as in SmsUtils.kt
    private val bankKeywords = mapOf(
        "federal" to "Federal Bank",
        "idfc first" to "IDFC First Bank",
        "idfc" to "IDFC First Bank",
        "hdfc" to "HDFC Bank",
        "sbi" to "SBI",
        "axis" to "Axis Bank",
        "icici" to "ICICI Bank",
        "kotak" to "Kotak Bank"
    )
    
    // Same amount regex as in SmsUtils.kt
    val amountRegex = Regex("(INR|Rs\\.?|rs\\.?|₹)\\s?([\\d,]+\\.?\\d*)")
    
    /**
     * Parse a new SMS message using the same logic as SmsUtils.kt
     */
    fun parseNewMessage(messageBody: String, timestamp: Long): BankTransaction? {
        try {
            val lowerBody = messageBody.lowercase()
            Log.d("MessageParser", "🔍 Parsing message: ${messageBody.take(100)}...")
            
            // Check if it's from a known bank
            val matchedBank = bankKeywords.entries.firstOrNull { (key, _) -> 
                key in lowerBody 
            }?.value
            
            if (matchedBank != null) {
                Log.d("MessageParser", "🏦 Bank detected: $matchedBank")
                
                // Extract amount using same regex
                val match = amountRegex.find(messageBody)
                val amountStr = match?.groups?.get(2)?.value?.replace(",", "")
                val amount = amountStr?.toDoubleOrNull()
                
                Log.d("MessageParser", "💰 Amount extracted: $amount")
                
                if (amount != null) {
                    Log.d("MessageParser", "✅ Valid transaction: ₹$amount from $matchedBank")
                    
                    return BankTransaction(
                        messageTime = timestamp,
                        amount = amount,
                        bankName = matchedBank,
                        tags = messageBody
                    )
                } else {
                    if (true) {
                        Log.d("MessageParser", "❌ Amount could not be parsed")
                    } else {
                        Log.d("MessageParser", "❌ Amount ₹$amount is >= 500 (threshold)")
                    }
                }
            } else {
                Log.d("MessageParser", "❌ No known bank detected in message")
                Log.d("MessageParser", "   Available banks: ${bankKeywords.keys.joinToString(", ")}")
            }
            
            return null
        } catch (e: Exception) {
            Log.e("MessageParser", "Error parsing message: $messageBody", e)
            return null
        }
    }
    
    /**
     * Check if a message is a bank transaction (without parsing amount)
     */
    fun isBankTransaction(messageBody: String): Boolean {
        val lowerBody = messageBody.lowercase()
        return bankKeywords.entries.any { (key, _) -> key in lowerBody }
    }
} 