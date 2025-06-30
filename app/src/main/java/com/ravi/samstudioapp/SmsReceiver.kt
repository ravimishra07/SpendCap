package com.ravi.samstudioapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import android.util.Log

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            val sms = messages.lastOrNull() ?: return
            val body = sms.messageBody
            val regex = Regex("""Rs\\s*([\d,.]+).*on\\s*([\d-]+\\s*[\d:]+).*-(\w+\\s*Bank)""", RegexOption.IGNORE_CASE)
            val match = regex.find(body)
            if (match != null) {
                val amount = match.groupValues[1]
                val messageTime = match.groupValues[2]
                val bank = match.groupValues[3]
                val amt = amount.replace(",", "").toDoubleOrNull() ?: return
                if (amt < 500) {
                    val launchIntent = Intent(context, PaymentCategorizeActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        putExtra("amount", amt)
                        putExtra("bankName", bank)
                        putExtra("messageTime", messageTime)
                        putExtra("rawMessage", body)
                    }
                    context.startActivity(launchIntent)
                }
            }
        }
    }
} 