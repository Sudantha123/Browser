package com.lightbrowser.app

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import android.support.v4.media.session.MediaSessionCompat

class MediaPlaybackService : Service() {

    private val CHANNEL_ID = "MediaBrowserChannel"
    private lateinit var mediaSession: MediaSessionCompat

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        mediaSession = MediaSessionCompat(this, "MediaService")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // App එක open කළ විගසම සරල Notification එකක් පෙන්වීම
        val notification = createMediaNotification("Browser Media", "Playing in background")
        startForeground(1, notification)
        return START_NOT_STICKY
    }

    private fun createMediaNotification(title: String, text: String): Notification {
        // Click කළ විට App එකට නැවත ඒමට
        val pendingIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play) // මෙතනට ඔබේ logo icon එක දාන්න
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            // Media Style Notification (Controls සඳහා)
            .setStyle(androidx.media.app.NotificationCompat.MediaStyle()
                .setShowActionsInCompactView(0, 1, 2)
                .setMediaSession(mediaSession.sessionToken))

        // මෙතැනට Play/Pause/Next Intents එකතු කර MainActivity එක හරහා JS run කළ හැක
        // උදා: builder.addAction(android.R.drawable.ic_media_pause, "Pause", pausePendingIntent)

        return builder.build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Media Playback",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        super.onDestroy()
        mediaSession.release()
    }
}
