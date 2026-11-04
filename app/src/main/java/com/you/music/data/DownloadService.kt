package com.you.music.data

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/** Download chalte waqt app ko zinda rakhta hai aur notification me progress dikhata hai */
class DownloadService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var job: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        nm().createNotificationChannel(
            NotificationChannel(CHANNEL, "Downloads", NotificationManager.IMPORTANCE_LOW)
        )
        startForeground(NOTIF_ID, notification(Downloads.progress.value))

        if (job == null) {
            job = scope.launch {
                Downloads.progress.collect { map ->
                    if (map.isEmpty()) {
                        stopForeground(STOP_FOREGROUND_REMOVE)
                        stopSelf()
                    } else {
                        nm().notify(NOTIF_ID, notification(map))
                    }
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun nm(): NotificationManager =
        getSystemService(NotificationManager::class.java)

    private fun notification(map: Map<String, Float>): Notification {
        val count = map.size
        val avg = if (count == 0) 0f else map.values.average().toFloat()
        return NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(
                if (count <= 1) "Gaana download ho raha hai"
                else "$count gaane download ho rahe hain"
            )
            .setProgress(100, (avg * 100).toInt(), count == 0)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL = "downloads"
        private const val NOTIF_ID = 42
    }
}
