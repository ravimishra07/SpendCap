package com.ravi.samstudioapp.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.telephony.SmsMessage
import android.util.Log
import com.ravi.samstudioapp.MainActivity
import com.ravi.samstudioapp.domain.model.BankTransaction
import com.ravi.samstudioapp.utils.MessageParser

class SmsReceiver : BroadcastReceiver() {

    companion object {
        private var onNewSmsListener: ((String, Long) -> Unit)? = null

        fun setOnNewSmsListener(listener: (String, Long) -> Unit) {
            onNewSmsListener = listener
        }
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent?.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)

        messages?.forEach { sms ->
            val messageBody = sms.messageBody
            val timestamp = sms.timestampMillis

            val parsed = MessageParser.parseNewMessage(messageBody, timestamp)

            if (parsed != null) {
                Log.d("SmsReceiver", "✅ SMS parsed: ₹${parsed.amount} via ${parsed.bankName}")

                // Create BankTransaction from parsed message
                val bankTransaction = BankTransaction(
                    amount = parsed.amount,
                    bankName = parsed.bankName,
                    tags = parsed.rawMessage, // Use raw message as tags
                    messageTime = parsed.messageTime,
                    category = "SMS", // Mark as SMS transaction
                    verified = true // Mark as verified since it's from SMS
                )

                // Launch MainActivity with BankTransaction
                val launchIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    putExtra("from_sms", true)
                    putExtra("bank_transaction", bankTransaction) // Pass BankTransaction object
                }
                context.startActivity(launchIntent)

                // ✅ Optionally trigger listener
                onNewSmsListener?.invoke(messageBody, timestamp)
            }
        }
    }
}