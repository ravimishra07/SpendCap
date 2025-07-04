package com.ravi.samstudioapp.utils

import android.content.Context
import com.ravi.samstudioapp.domain.model.ParsedSmsTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun readAndParseSms(context: Context): List<ParsedSmsTransaction> = withContext(Dispatchers.IO) {
    val result = mutableListOf<ParsedSmsTransaction>()
    try {
        val cursor = context.contentResolver.query(
            android.provider.Telephony.Sms.Inbox.CONTENT_URI,
            arrayOf(android.provider.Telephony.Sms.BODY,
                android.provider.Telephony.Sms.DATE),
            null, null, null
        )
        cursor?.use {
            val amountRegex = Regex("([\\d,]+\\.?\\d*)")
            while (it.moveToNext()) {
                val body = it.getString(0)
                val dateMillis = it.getLong(1)

                val lowerBody = body.lowercase()
                val matchedBank = when {
                    "federal" in lowerBody -> "Federal Bank"
                    "idfc" in lowerBody -> "IDFC"
                    else -> null
                }

                if (matchedBank != null) {
                    val match = amountRegex.find(body)
                    val amount = match?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull()
                    if (amount != null && amount < 500) {
                        result.add(
                            ParsedSmsTransaction(
                                amount = amount,
                                bankName = matchedBank,
                                messageTime = dateMillis,
                                rawMessage = body
                            )
                        )
                    }
                }
            }
        }
    } catch (e: Exception) {
        android.util.Log.e("SamStudio", "Error reading SMS: ", e)
    }
    result
}
