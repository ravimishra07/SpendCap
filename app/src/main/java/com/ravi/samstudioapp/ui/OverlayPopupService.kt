package com.ravi.samstudioapp.ui

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import com.ravi.samstudioapp.R

class OverlayPopupService : Service() {
    private var windowManager: WindowManager? = null
    private var popupView: View? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("OverlayPopupService", "Service started with intent: $intent")
        
        val message = intent?.getStringExtra(EXTRA_MESSAGE) ?: "New SMS received!"
        val bank = intent?.getStringExtra(EXTRA_BANK) ?: ""
        val amount = intent?.getStringExtra(EXTRA_AMOUNT) ?: ""
        
        Log.d("OverlayPopupService", "Message: $message, Bank: $bank, Amount: $amount")

        // Start as foreground service to keep it alive
        startForeground(1, createNotification())
        
        showPopup(message, bank, amount)
        
        // Auto-close service after 10 seconds
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            removePopup()
            stopSelf()
        }, 10000)
        
        return START_NOT_STICKY
    }
    
    private fun createNotification(): android.app.Notification {
        val channelId = "overlay_popup_channel"
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                channelId,
                "Transaction Alert",
                android.app.NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(android.app.NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
        return androidx.core.app.NotificationCompat.Builder(this, channelId)
            .setContentTitle("Transaction Alert")
            .setContentText("New transaction detected")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .build()
    }

    private fun showPopup(message: String, bank: String, amount: String) {
        Log.d("OverlayPopupService", "showPopup called")
        if (popupView != null) {
            Log.d("OverlayPopupService", "Popup already exists, returning")
            return // Only one popup at a time
        }

        // Check overlay permission
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            if (!android.provider.Settings.canDrawOverlays(this)) {
                Log.e("OverlayPopupService", "❌ Overlay permission not granted")
                stopSelf()
                return
            }
        }

        try {
            val inflater = LayoutInflater.from(this)
            popupView = inflater.inflate(R.layout.overlay_popup, null)
            Log.d("OverlayPopupService", "Popup view inflated successfully")

            val tvMessage = popupView!!.findViewById<TextView>(R.id.tvMessage)
            val tvBank = popupView!!.findViewById<TextView>(R.id.tvBank)
            val tvAmount = popupView!!.findViewById<TextView>(R.id.tvAmount)
            val btnClose = popupView!!.findViewById<View>(R.id.btnClose)

            tvMessage.text = message
            tvBank.text = bank
            tvAmount.text = amount

            btnClose.setOnClickListener {
                removePopup()
                stopSelf()
            }

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                else
                    WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
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
                Log.d("OverlayPopupService", "Popup removed successfully")
            }
        } catch (e: Exception) {
            Log.e("OverlayPopupService", "Error removing popup", e)
        }
    }

    override fun onDestroy() {
        removePopup()
        super.onDestroy()
    }

    companion object {
        const val EXTRA_MESSAGE = "extra_message"
        const val EXTRA_BANK = "extra_bank"
        const val EXTRA_AMOUNT = "extra_amount"
    }
} 