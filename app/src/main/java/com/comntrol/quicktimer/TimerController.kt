package com.comntrol.quicktimer

import android.content.Context
import android.os.Build
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Estado único do timer, compartilhado entre a tela e o serviço. Tempos em elapsedRealtime. */
object TimerController {

    data class State(
        val running: Boolean = false,
        val endAt: Long = 0L,
        val totalMs: Long = 0L,
        val series: Int = 0,
        val flashText: String = "",
        val flashUntil: Long = 0L
    )

    private const val FLASH_MS = 900L

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state

    private fun now() = SystemClock.elapsedRealtime()

    fun start(ctx: Context) {
        val c = ctx.applicationContext
        val base = Prefs.baseSeconds(c) * 1000L
        val series = Prefs.nextSeries(c)
        _state.value = State(running = true, endAt = now() + base, totalMs = base, series = series)
    }

    fun add(ctx: Context): Boolean {
        val c = ctx.applicationContext
        val s = _state.value
        if (!s.running) return false
        val incS = Prefs.incSeconds(c)
        val inc = incS * 1000L
        val t = now()
        _state.value = s.copy(
            endAt = maxOf(s.endAt, t) + inc,
            totalMs = s.totalMs + inc,
            flashText = "+" + Prefs.secondsLabel(incS),
            flashUntil = t + FLASH_MS
        )
        Prefs.touch(c)
        return true
    }

    /** Desfaz um incremento. Recusa se sobrar menos de 1 s. */
    fun subtract(ctx: Context): Boolean {
        val c = ctx.applicationContext
        val s = _state.value
        if (!s.running) return false
        val incS = Prefs.incSeconds(c)
        val inc = incS * 1000L
        val t = now()
        val left = s.endAt - t
        if (left - inc < 1000L) return false
        _state.value = s.copy(
            endAt = s.endAt - inc,
            totalMs = maxOf(s.totalMs - inc, left - inc),
            flashText = "−" + Prefs.secondsLabel(incS),
            flashUntil = t + FLASH_MS
        )
        Prefs.touch(c)
        return true
    }

    fun cancel(ctx: Context) {
        _state.value = _state.value.copy(running = false)
        Prefs.touch(ctx.applicationContext)
    }

    /** Chamado pelo serviço quando chega a zero. */
    fun finish(ctx: Context) {
        _state.value = _state.value.copy(running = false)
        Prefs.touch(ctx.applicationContext)
    }
}

object Haptics {
    val TICK = longArrayOf(0, 40)
    val DENIED = longArrayOf(0, 20, 60, 20)
    val PRE_ALERT = longArrayOf(0, 120, 100, 120)
    val SECOND = longArrayOf(0, 90)
    val END = longArrayOf(0, 300, 150, 300, 150, 300)

    fun vibrate(c: Context, pattern: LongArray) {
        val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (c.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            c.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
        vibrator?.vibrate(VibrationEffect.createWaveform(pattern, -1))
    }
}
