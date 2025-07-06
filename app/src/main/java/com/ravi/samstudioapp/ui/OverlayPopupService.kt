package com.ravi.samstudioapp.ui

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.ravi.samstudioapp.MainActivity
import com.ravi.samstudioapp.R

class OverlayPopupService : Service() {
    private var windowManager: WindowManager? = null
    private var popupView: View? = null
    private var wakeLock: PowerManager.WakeLock? = null
    
    companion object {
        const val EXTRA_MESSAGE = "extra_message"
        const val EXTRA_BANK = "extra_bank"
        const val EXTRA_AMOUNT = "extra_amount"
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "overlay_popup_channel"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Log.d("OverlayPopupService", "Service created")
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("OverlayPopupService", "Service started with intent: $intent")
        
        val message = intent?.getStringExtra(EXTRA_MESSAGE) ?: "New SMS received!"
        val bank = intent?.getStringExtra(EXTRA_BANK) ?: ""
        val amount = intent?.getStringExtra(EXTRA_AMOUNT) ?: ""
        
        Log.d("OverlayPopupService", "Message: $message, Bank: $bank, Amount: $amount")

        // IMPORTANT: Start foreground service FIRST before doing anything else
        try {
            val notification = createNotification(message, bank, amount)
            startForeground(NOTIFICATION_ID, notification)
            Log.d("OverlayPopupService", "✅ Foreground service started successfully with notification")
        } catch (e: Exception) {
            Log.e("OverlayPopupService", "❌ Failed to start foreground service", e)
            stopSelf()
            return START_NOT_STICKY
        }

        // Acquire wake lock to ensure service stays alive
        acquireWakeLock()
        
        // Show popup
        showPopup(message, bank, amount)
        
        // Auto-close service after 15 seconds
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            Log.d("OverlayPopupService", "Auto-closing service after timeout")
            removePopup()
            releaseWakeLock()
            stopSelf()
        }, 15000)
        
        return START_NOT_STICKY
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Transaction Alert Service",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Shows transaction alerts as overlay popups"
                    setShowBadge(false)
                    enableLights(false)
                    enableVibration(false)
                    setSound(null, null)
                }
                
                val notificationManager = getSystemService(NotificationManager::class.java)
                notificationManager.createNotificationChannel(channel)
                Log.d("OverlayPopupService", "✅ Notification channel created successfully")
            } catch (e: Exception) {
                Log.e("OverlayPopupService", "❌ Failed to create notification channel", e)
            }
        }
    }
    
    private fun createNotification(message: String, bank: String, amount: String): android.app.Notification {
        try {
            // Create intent for when notification is tapped
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            
            val pendingIntent = PendingIntent.getActivity(
                this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Transaction Alert Active")
                .setContentText("Monitoring for new transactions")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setSilent(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setAutoCancel(false)
                .build()
            
            Log.d("OverlayPopupService", "✅ Notification created successfully")
            return notification
            
        } catch (e: Exception) {
            Log.e("OverlayPopupService", "❌ Failed to create notification", e)
            // Return a basic notification as fallback
            return NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Transaction Alert")
                .setContentText("Service is running")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build()
        }
    }
    
    private fun acquireWakeLock() {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "OverlayPopupService::WakeLock"
            ).apply {
                acquire(30000) // 30 seconds timeout
            }
            Log.d("OverlayPopupService", "✅ Wake lock acquired")
        } catch (e: Exception) {
            Log.e("OverlayPopupService", "❌ Failed to acquire wake lock", e)
        }
    }
    
    private fun releaseWakeLock() {
        try {
            wakeLock?.let {
                if (it.isHeld) {
                    it.release()
                    Log.d("OverlayPopupService", "✅ Wake lock released")
                }
            }
            wakeLock = null
        } catch (e: Exception) {
            Log.e("OverlayPopupService", "❌ Failed to release wake lock", e)
        }
    }

    private fun showPopup(message: String, bank: String, amount: String) {
        Log.d("OverlayPopupService", "showPopup called")
        if (popupView != null) {
            Log.d("OverlayPopupService", "Popup already exists, returning")
            return // Only one popup at a time
        }

        // Check overlay permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!android.provider.Settings.canDrawOverlays(this)) {
                Log.e("OverlayPopupService", "❌ Overlay permission not granted")
                stopSelf()
                return
            }
        }

        try {
            val inflater = LayoutInflater.from(this)
            popupView = inflater.inflate(R.layout.overlay_popup, null)
            Log.d("OverlayPopupService", "✅ Popup view inflated successfully")

            val tvMessage = popupView!!.findViewById<TextView>(R.id.tvMessage)
            val tvBank = popupView!!.findViewById<TextView>(R.id.tvBank)
            val tvAmount = popupView!!.findViewById<TextView>(R.id.tvAmount)
            val btnClose = popupView!!.findViewById<View>(R.id.btnClose)

            tvMessage.text = message
            tvBank.text = bank
            tvAmount.text = amount

            btnClose.setOnClickListener {
                Log.d("OverlayPopupService", "Close button clicked")
                removePopup()
                releaseWakeLock()
                stopSelf()
            }

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                else
                    WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or 
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT
            )
            params.gravity = Gravity.TOP
            params.y = 100

            windowManager?.addView(popupView, params)
            Log.d("OverlayPopupService", "✅ Popup added to window manager successfully")
            
        } catch (e: Exception) {
            Log.e("OverlayPopupService", "❌ Error showing popup", e)
            stopSelf()
        }
    }

    private fun removePopup() {
        try {
            popupView?.let {
                windowManager?.removeView(it)
                popupView = null
                Log.d("OverlayPopupService", "✅ Popup removed successfully")
            }
        } catch (e: Exception) {
            Log.e("OverlayPopupService", "❌ Error removing popup", e)
        }
    }

    override fun onDestroy() {
        Log.d("OverlayPopupService", "Service destroyed")
        removePopup()
        releaseWakeLock()
        super.onDestroy()
    }
} 