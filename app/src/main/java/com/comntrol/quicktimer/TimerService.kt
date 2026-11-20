package com.comntrol.quicktimer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.wear.ongoing.OngoingActivity
import androidx.wear.ongoing.Status
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Mantém a contagem rodando com o app fora da tela: alerta dos 10 s, vibração final
 * e indicador de atividade em andamento no mostrador.
 */
class TimerService : Service() {

    companion object {
        private const val CHANNEL = "timer"
        private const val NOTIF_ID = 1001
        private const val PRE_ALERT_MS = 10_000L
        private const val FINAL_COUNT_MS = 5_000L

        fun ensureRunning(c: Context) {
            try {
                ContextCompat.startForegroundService(c, Intent(c, TimerService::class.java))
            } catch (e: Exception) {
                // Sem permissão de iniciar agora: a tela continua controlando o timer.
            }
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var loopJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var lastEndAt = -1L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, "Timer", NotificationManager.IMPORTANCE_LOW)
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val s = TimerController.state.value
        val type = if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0
        ServiceCompat.startForeground(this, NOTIF_ID, buildNotification(s), type)
        if (!s.running) {
            stopNow()
            return START_NOT_STICKY
        }
        if (loopJob?.isActive != true) loopJob = scope.launch { runLoop() }
        return START_NOT_STICKY
    }

    private suspend fun runLoop() {
        var preAlerted = false
        var lastBeepSec = Int.MAX_VALUE
        while (scope.isActive) {
            val s = TimerController.state.value
            if (!s.running) break

            if (s.endAt != lastEndAt) {
                lastEndAt = s.endAt
                getSystemService(NotificationManager::class.java).notify(NOTIF_ID, buildNotification(s))
                holdWakeLock(s.endAt - SystemClock.elapsedRealtime())
            }

            val left = s.endAt - SystemClock.elapsedRealtime()
            if (left <= 0) {
                Haptics.vibrate(this, Haptics.END)
                TimerController.finish(this)
                break
            }
            if (left > PRE_ALERT_MS + 500) {
                preAlerted = false
            } else if (!preAlerted && left <= PRE_ALERT_MS && left > FINAL_COUNT_MS) {
                preAlerted = true
                Haptics.vibrate(this, Haptics.PRE_ALERT)
            }

            // 5 s finais: uma vibração curta a cada segundo (00:05, 00:04 … 00:01).
            val sec = ((left + 999) / 1000).toInt()
            if (sec > FINAL_COUNT_MS / 1000) {
                lastBeepSec = Int.MAX_VALUE          // tempo somado: rearma a contagem
            } else if (sec < lastBeepSec) {
                lastBeepSec = sec
                Haptics.vibrate(this, Haptics.SECOND)
            }
            delay(50)
        }
        stopNow()
    }

    private fun holdWakeLock(ms: Long) {
        val pm = getSystemService(PowerManager::class.java)
        val wl = wakeLock ?: pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "QuickTimer:countdown")
            .also { it.setReferenceCounted(false); wakeLock = it }
        wl.acquire(ms.coerceAtLeast(0) + 10_000L)
    }

    private fun stopNow() {
        wakeLock?.let { if (it.isHeld) it.release() }
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        scope.cancel()
        wakeLock?.let { if (it.isHeld) it.release() }
        super.onDestroy()
    }

    private fun buildNotification(s: TimerController.State): Notification {
        val open = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java)
                .setAction(MainActivity.ACTION_SHOW)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val wallEnd = System.currentTimeMillis() + (s.endAt - SystemClock.elapsedRealtime())

        val builder = NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_timer)
            .setContentTitle("Timer · Série ${s.series}")
            .setContentText("Contagem regressiva")
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setShowWhen(true)
            .setWhen(wallEnd)
            .setContentIntent(open)

        OngoingActivity.Builder(applicationContext, NOTIF_ID, builder)
            .setStaticIcon(R.drawable.ic_stat_timer)
            .setTouchIntent(open)
            .setStatus(
                Status.Builder()
                    .addTemplate("Timer · Série #s#")
                    .addPart("s", Status.TextPart(s.series.toString()))
                    .build()
            )
            .build()
            .apply(applicationContext)

        return builder.build()
    }
}
