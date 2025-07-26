package com.ravi.samstudioapp.utils

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.telephony.SmsMessage
import android.util.Log
import com.ravi.samstudioapp.presentation.main.MainActivity
import com.ravi.samstudioapp.domain.model.BankTransaction
import com.ravi.samstudioapp.utils.MessageParser

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d("SmsReceiver", "🔔 onReceive called with action: ${intent?.action}")
        
        // Log all intent actions to see what's being received
        Log.d("SmsReceiver", "🔔 Intent action: ${intent?.action}")
        Log.d("SmsReceiver", "🔔 Context is null: ${context == null}")
        
        // Check SMS permission
        if (context != null) {
            val hasSmsPermission = context.checkSelfPermission(android.Manifest.permission.RECEIVE_SMS) == android.content.pm.PackageManager.PERMISSION_GRANTED
            Log.d("SmsReceiver", "🔔 SMS permission granted: $hasSmsPermission")
        }
        
        if (context == null || intent?.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            Log.d("SmsReceiver", "❌ Invalid context or action, returning")
            Log.d("SmsReceiver", "❌ Expected action: ${Telephony.Sms.Intents.SMS_RECEIVED_ACTION}")
            Log.d("SmsReceiver", "❌ Received action: ${intent?.action}")
            return
        }

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        Log.d("SmsReceiver", "📱 Received ${messages?.size ?: 0} SMS messages")

        messages?.forEach { sms ->
            val messageBody = sms.messageBody
            val timestamp = sms.timestampMillis
            
            Log.d("SmsReceiver", "📨 Processing SMS: ${messageBody.take(50)}...")

            val parsed = MessageParser.parseNewMessage(messageBody, timestamp)

            if (parsed != null) {
                Log.d("SmsReceiver", "✅ SMS parsed: ₹${parsed.amount} via ${parsed.bankName}")

                // Launch MainActivity with the parsed transaction
                val mainActivityIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    putExtra("NEW_SMS_DETECTED", true)
                    putExtra("MESSAGE_BODY", messageBody)
                    putExtra("MESSAGE_TIMESTAMP", timestamp)
                    putExtra("BANK_TRANSACTION", parsed)
                }
                
                try {
                    context.startActivity(mainActivityIntent)
                    Log.d("SmsReceiver", "✅ MainActivity launched with transaction data")
                } catch (e: Exception) {
                    Log.e("SmsReceiver", "❌ Failed to launch MainActivity", e)
                }
            } else {
                Log.d("SmsReceiver", "❌ SMS could not be parsed as bank transaction")
            }
        }
    }
}