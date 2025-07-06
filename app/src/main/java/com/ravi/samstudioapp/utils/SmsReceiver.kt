package com.ravi.samstudioapp.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import android.telephony.SmsMessage
import android.util.Log
import com.ravi.samstudioapp.ui.OverlayPopupService
import com.ravi.samstudioapp.utils.MessageParser

class SmsReceiver : BroadcastReceiver() {
    
    companion object {
        private var onNewSmsListener: ((String, Long) -> Unit)? = null
        
        fun setOnNewSmsListener(listener: (String, Long) -> Unit) {
            onNewSmsListener = listener
        }
    }
    
    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d("SmsReceiver", "onReceive called with action: ${intent?.action}")
        
        // Check if context is available
        if (context == null) {
            Log.e("SmsReceiver", "❌ Context is null, cannot process SMS")
            return
        }
        
        if (intent?.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            Log.d("SmsReceiver", "SMS_RECEIVED_ACTION detected")
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            
            Log.d("SmsReceiver", "Number of messages: ${messages?.size}")
            
            messages?.forEach { smsMessage ->
                val messageBody = smsMessage.messageBody
                val timestamp = smsMessage.timestampMillis
                val sender = smsMessage.originatingAddress ?: "Unknown"
                
                Log.d("SmsReceiver", "=== NEW SMS RECEIVED ===")
                Log.d("SmsReceiver", "From: $sender")
                Log.d("SmsReceiver", "Time: ${java.util.Date(timestamp)}")
                Log.d("SmsReceiver", "Message: $messageBody")
                Log.d("SmsReceiver", "========================")
                
                // Parse the message using the same logic as SmsUtils.kt
                val parsedTransaction = MessageParser.parseNewMessage(messageBody, timestamp)
                
                if (parsedTransaction != null) {
                    Log.d("SmsReceiver", "✅ TRANSACTION DETECTED: ₹${parsedTransaction.amount} from ${parsedTransaction.bankName}")
                    
                    // Start overlay popup service IMMEDIATELY
                    startOverlayPopupService(context, parsedTransaction)
                    
                    // Notify listener about transaction SMS
                    onNewSmsListener?.invoke(messageBody, timestamp)
                } else {
                    Log.d("SmsReceiver", "❌ Message did not parse as a transaction")
                    Log.d("SmsReceiver", "   - Is from known bank: ${MessageParser.isBankTransaction(messageBody)}")
                    Log.d("SmsReceiver", "   - Contains amount pattern: ${MessageParser.amountRegex.containsMatchIn(messageBody)}")
                    
                    // Don't notify listener for non-transaction messages
                }
            }
        } else {
            Log.d("SmsReceiver", "Action not SMS_RECEIVED_ACTION: ${intent?.action}")
        }
    }
    
    private fun startOverlayPopupService(context: Context, transaction: com.ravi.samstudioapp.domain.model.ParsedSmsTransaction) {
        try {
            Log.d("SmsReceiver", "🚀 Starting overlay popup service...")
            
            val serviceIntent = Intent(context, OverlayPopupService::class.java).apply {
                putExtra(OverlayPopupService.EXTRA_MESSAGE, "New Transaction Detected!")
                putExtra(OverlayPopupService.EXTRA_BANK, transaction.bankName)
                putExtra(OverlayPopupService.EXTRA_AMOUNT, "₹${transaction.amount}")
            }
            
            // Use foreground service for Android O+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
            Log.d("SmsReceiver", "✅ Overlay popup service started successfully")
            
        } catch (e: Exception) {
            Log.e("SmsReceiver", "❌ Failed to start overlay popup service", e)
            Log.e("SmsReceiver", "Error details: ${e.message}")
            e.printStackTrace()
        }
    }
} 