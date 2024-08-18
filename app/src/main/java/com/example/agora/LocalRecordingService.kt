package com.example.agora

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

/**
 * @author Perry Lance
 * @since 2024-08-19 Created
 */
class LocalRecordingService : Service() {

    private val CHANNEL_ID = "agora_example_audio_channel"
    private val NOTIFICATION_ID = 987654321

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIFICATION_ID, getDefaultNotification())
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun getDefaultNotification(): Notification {
        val intent = Intent(this, AgoraAudioActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Audio Service", NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Agora Audio")
            .setContentText("Agora is recording your audio...")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setLargeIcon(BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher))
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
    }
}