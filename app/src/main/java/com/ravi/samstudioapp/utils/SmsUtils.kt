package com.ravi.samstudioapp.utils

import android.content.Context
import android.provider.Telephony
import com.ravi.samstudioapp.domain.model.ParsedSmsTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log

suspend fun readAndParseSms(context: Context): List<ParsedSmsTransaction> = withContext(Dispatchers.IO) {
    val result = mutableListOf<ParsedSmsTransaction>()
    try {
        val cursor = context.contentResolver.query(
            Telephony.Sms.Inbox.CONTENT_URI,
            arrayOf(Telephony.Sms.BODY, Telephony.Sms.DATE),
            null, null, null
        )

        val bankKeywords = mapOf(
            "federal" to "Federal Bank",
            "idfc first" to "IDFC First Bank",
            "idfc" to "IDFC First Bank",
            "hdfc" to "HDFC Bank",
            "sbi" to "SBI",
            "axis" to "Axis Bank",
            "icici" to "ICICI Bank",
            "kotak" to "Kotak Bank"
        )

        val amountRegex = Regex("(INR|Rs\\.?|rs\\.?|₹)\\s?([\\d,]+\\.?\\d*)")

        cursor?.use {
            while (it.moveToNext()) {
                val body = it.getString(0)
                val dateMillis = it.getLong(1)
                val lowerBody = body.lowercase()

                val matchedBank = bankKeywords.entries.firstOrNull { (key, _) -> key in lowerBody }?.value

                if (matchedBank != null) {
                    val match = amountRegex.find(body)
                    val amountStr = match?.groups?.get(2)?.value?.replace(",", "")
                    val amount = amountStr?.toDoubleOrNull()

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
        Log.e("SamStudio", "Error reading SMS: ", e)
    }
    result
}
