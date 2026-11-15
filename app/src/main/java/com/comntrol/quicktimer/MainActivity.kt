package com.comntrol.quicktimer

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Contagem regressiva acionada pelo botão de ação (Quick button) do Galaxy Watch.
 *
 * - Ao abrir: inicia 1 minuto.
 * - Cada novo acionamento do botão com o app aberto: +1 minuto.
 *   (O sistema reentrega o app via onNewIntent; se o botão chegar como tecla
 *   STEM, também é tratado em onKeyDown.)
 * - Toque na tela: também soma +1 minuto (alternativa manual).
 * - Toque longo na tela ou deslizar para a direita: cancela e fecha.
 * - Ao zerar: vibra e fecha, voltando ao app que estava antes.
 */
class MainActivity : ComponentActivity() {

    companion object {
        private const val ONE_MINUTE_MS = 60_000L
        private const val DEBOUNCE_MS = 400L   // evita somar 2x no mesmo clique
        private const val TICK_MS = 50L
        private const val FLASH_MS = 900L
    }

    private var endAt = 0L                 // SystemClock.elapsedRealtime() do fim
    private var totalMs by mutableLongStateOf(ONE_MINUTE_MS)
    private var remainingMs by mutableLongStateOf(ONE_MINUTE_MS)
    private var flashUntil by mutableLongStateOf(0L)
    private var lastAddAt = 0L
    private var tickJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val now = SystemClock.elapsedRealtime()
        if (savedInstanceState != null && savedInstanceState.getLong("endAt") > now) {
            endAt = savedInstanceState.getLong("endAt")
            totalMs = savedInstanceState.getLong("totalMs")
        } else {
            endAt = now + ONE_MINUTE_MS
            totalMs = ONE_MINUTE_MS
        }
        lastAddAt = now

        setContent {
            TimerScreen(
                remainingMs = remainingMs,
                totalMs = totalMs,
                showFlash = SystemClock.elapsedRealtime() < flashUntil,
                onTap = { addMinute() },
                onLongPress = { closeApp(vibrate = false) }
            )
        }
        startTicking()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong("endAt", endAt)
        outState.putLong("totalMs", totalMs)
    }

    /** Botão de ação pressionado de novo com o app já aberto. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        addMinute()
    }

    /** Caso o firmware entregue o botão físico como tecla STEM. */
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_STEM_1,
            KeyEvent.KEYCODE_STEM_2,
            KeyEvent.KEYCODE_STEM_3 -> {
                if (event?.repeatCount == 0) addMinute()
                true
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }

    private fun addMinute() {
        val now = SystemClock.elapsedRealtime()
        if (now - lastAddAt < DEBOUNCE_MS) return
        lastAddAt = now

        endAt = maxOf(endAt, now) + ONE_MINUTE_MS
        totalMs += ONE_MINUTE_MS
        flashUntil = now + FLASH_MS
        remainingMs = endAt - now
        vibrate(longArrayOf(0, 40))
        if (tickJob?.isActive != true) startTicking()
    }

    private fun startTicking() {
        tickJob = lifecycleScope.launch {
            while (isActive) {
                val left = endAt - SystemClock.elapsedRealtime()
                if (left <= 0) {
                    remainingMs = 0
                    closeApp(vibrate = true)
                    break
                }
                remainingMs = left
                delay(TICK_MS)
            }
        }
    }

    private fun closeApp(vibrate: Boolean) {
        tickJob?.cancel()
        if (vibrate) vibrate(longArrayOf(0, 300, 150, 300, 150, 300))
        finishAndRemoveTask()
    }

    private fun vibrate(pattern: LongArray) {
        val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
        vibrator?.vibrate(VibrationEffect.createWaveform(pattern, -1))
    }
}

// ---------------------------------------------------------------------------
// UI — visual no estilo do cronômetro Samsung (fundo preto, arco laranja Ultra)
// ---------------------------------------------------------------------------

private val Orange = Color(0xFFFF6B00)
private val Track = Color(0xFF2A2A2A)
private val Gray = Color(0xFF9A9A9A)

@Composable
private fun TimerScreen(
    remainingMs: Long,
    totalMs: Long,
    showFlash: Boolean,
    onTap: () -> Unit,
    onLongPress: () -> Unit
) {
    val progress = if (totalMs > 0) (remainingMs.toFloat() / totalMs).coerceIn(0f, 1f) else 0f
    val totalSec = (remainingMs / 1000).toInt()
    val mm = totalSec / 60
    val ss = totalSec % 60
    val centis = ((remainingMs % 1000) / 10).toInt()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onTap() },
                    onLongPress = { onLongPress() }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // Arco de progresso
        Canvas(modifier = Modifier.fillMaxSize().padding(6.dp)) {
            val stroke = 10.dp.toPx()
            val inset = stroke / 2
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(
                color = Track, startAngle = -90f, sweepAngle = 360f, useCenter = false,
                topLeft = Offset(inset, inset), size = arcSize,
                style = Stroke(width = stroke)
            )
            drawArc(
                color = Orange, startAngle = -90f, sweepAngle = 360f * progress,
                useCenter = false, topLeft = Offset(inset, inset), size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            BasicText(
                text = if (showFlash) "+1 min" else "Timer",
                style = TextStyle(
                    color = if (showFlash) Orange else Gray,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            )
            Spacer(Modifier.height(2.dp))
            BasicText(
                text = "%02d:%02d".format(mm, ss),
                style = TextStyle(
                    color = Color.White,
                    fontSize = 54.sp,
                    fontWeight = FontWeight.Light,
                    fontFamily = FontFamily.SansSerif,
                    textAlign = TextAlign.Center
                )
            )
            BasicText(
                text = ".%02d".format(centis),
                style = TextStyle(
                    color = Orange,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Light,
                    textAlign = TextAlign.Center
                )
            )
            Spacer(Modifier.height(10.dp))
            BasicText(
                text = "Botão ou toque: +1 min",
                style = TextStyle(color = Gray, fontSize = 11.sp, textAlign = TextAlign.Center)
            )
        }
    }
}
