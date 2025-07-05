package com.ravi.samstudioapp.utils

import android.content.Context
import com.ravi.samstudioapp.domain.model.ParsedSmsTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log

suspend fun readAndParseSms(context: Context): List<ParsedSmsTransaction> = withContext(Dispatchers.IO) {
    val result = mutableListOf<ParsedSmsTransaction>()
    Log.d("SamStudio", "readAndParseSms: Starting SMS reading process")
    try {
        // Calculate 30 days ago timestamp
        val thirtyDaysAgo = System.currentTimeMillis() - (30 * 24 * 60 * 60 * 1000L)
        Log.d("SamStudio", "readAndParseSms: Filtering SMS from last 30 days (since: ${java.util.Date(thirtyDaysAgo)})")
        
        // Add date filter to query
        val selection = "${android.provider.Telephony.Sms.DATE} >= ?"
        val selectionArgs = arrayOf(thirtyDaysAgo.toString())
        
        Log.d("SamStudio", "readAndParseSms: Querying SMS content resolver with date filter")
        val cursor = context.contentResolver.query(
            android.provider.Telephony.Sms.Inbox.CONTENT_URI,
            arrayOf(android.provider.Telephony.Sms.BODY,
                android.provider.Telephony.Sms.DATE),
            selection, selectionArgs, "${android.provider.Telephony.Sms.DATE} DESC"
        )
        
        if (cursor == null) {
            Log.e("SamStudio", "readAndParseSms: Cursor is null - no SMS access")
            return@withContext result
        }
        
        Log.d("SamStudio", "readAndParseSms: Cursor obtained, count: ${cursor.count}")
        cursor.use {
            val amountRegex = Regex("([\\d,]+\\.?\\d*)")
            var processedCount = 0
            var matchedCount = 0
            
            while (it.moveToNext()) {
                processedCount++
                val body = it.getString(0)
                val dateMillis = it.getLong(1)
                
                if (body == null) {
                    Log.d("SamStudio", "readAndParseSms: SMS body is null, skipping")
                    continue
                }

                Log.d("SamStudio", "readAndParseSms: Processing SMS $processedCount: ${body.take(50)}...")
                
                val lowerBody = body.lowercase()
                val matchedBank = when {
                    "federal" in lowerBody -> "Federal Bank"
                    "idfc" in lowerBody -> "IDFC"
                    else -> null
                }

                if (matchedBank != null) {
                    Log.d("SamStudio", "readAndParseSms: Bank matched: $matchedBank")
                    val match = amountRegex.find(body)
                    val amount = match?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull()
                    Log.d("SamStudio", "readAndParseSms: Amount extracted: $amount")
                    
                    if (amount != null && amount < 500) {
                        Log.d("SamStudio", "readAndParseSms: Adding transaction: $amount from $matchedBank")
                        result.add(
                            ParsedSmsTransaction(
                                amount = amount,
                                bankName = matchedBank,
                                messageTime = dateMillis,
                                rawMessage = body
                            )
                        )
                        matchedCount++
                    } else {
                        Log.d("SamStudio", "readAndParseSms: Amount $amount rejected (null or >= 500)")
                    }
                } else {
                    Log.d("SamStudio", "readAndParseSms: No bank matched for SMS")
                }
            }
            Log.d("SamStudio", "readAndParseSms: Processed $processedCount SMS, matched $matchedCount transactions")
        }
    } catch (e: Exception) {
        Log.e("SamStudio", "readAndParseSms: Error reading SMS", e)
        Log.e("SamStudio", "readAndParseSms: Error message: ${e.message}")
        Log.e("SamStudio", "readAndParseSms: Error stack trace: ${e.stackTraceToString()}")
    }
    Log.d("SamStudio", "readAndParseSms: Returning ${result.size} parsed transactions")
    result
}
