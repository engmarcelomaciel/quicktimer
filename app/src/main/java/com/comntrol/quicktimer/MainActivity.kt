package com.comntrol.quicktimer

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

/**
 * Tela do timer.
 * - Aberto pelo atalho (tecla Início 2x) sem timer rodando → inicia nova série.
 * - Atalho de novo com a tela visível, toque ou tecla STEM → soma o incremento.
 * - Deslizar para baixo → desfaz um incremento.
 * - Toque longo → cancela.
 * - Sair do app (tecla Início) → a contagem continua em segundo plano.
 * - Reabrir com o timer rodando em segundo plano → só mostra, sem somar.
 */
class MainActivity : ComponentActivity(), SensorEventListener {

    companion object {
        const val ACTION_SHOW = "com.comntrol.quicktimer.SHOW"
        private const val DEBOUNCE_MS = 400L
    }

    private var heartRate by mutableIntStateOf(0)
    private var sensorManager: SensorManager? = null
    private var lastAddAt = 0L

    private val permLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            startHeartRate()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        if (savedInstanceState == null) handleEntry(intent, visible = false)
        if (!TimerController.state.value.running) {
            finishAndRemoveTask()
            return
        }

        val showHr = Prefs.showHr(this)
        setContent {
            TimerScreen(
                showHr = showHr,
                heartRate = heartRate,
                hint = "Toque +" + Prefs.secondsLabel(Prefs.incSeconds(this)) + " · ↓ desfaz",
                onTap = { addIncrement() },
                onLongPress = { TimerController.cancel(this) },
                onSwipeDown = {
                    if (TimerController.subtract(this)) Haptics.vibrate(this, Haptics.TICK)
                    else Haptics.vibrate(this, Haptics.DENIED)
                }
            )
        }

        // Fecha a tela quando o timer termina ou é cancelado.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                TimerController.state.collect { if (!it.running) finishAndRemoveTask() }
            }
        }

        askPermissionsOnce()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val visible = lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
        handleEntry(intent, visible)
    }

    private fun handleEntry(intent: Intent?, visible: Boolean) {
        val running = TimerController.state.value.running
        when {
            intent?.action == ACTION_SHOW -> Unit          // toque no indicador do mostrador
            !running -> TimerController.start(this)        // nova série
            visible -> addIncrement()                      // atalho com a tela aberta
            else -> Unit                                   // voltando do segundo plano: só mostra
        }
    }

    override fun onResume() {
        super.onResume()
        if (TimerController.state.value.running) TimerService.ensureRunning(this)
    }

    override fun onStart() {
        super.onStart()
        startHeartRate()
    }

    override fun onStop() {
        stopHeartRate()
        super.onStop()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_STEM_1,
            KeyEvent.KEYCODE_STEM_2,
            KeyEvent.KEYCODE_STEM_3 -> {
                if (event?.repeatCount == 0) addIncrement()
                true
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }

    private fun addIncrement() {
        val now = SystemClock.elapsedRealtime()
        if (now - lastAddAt < DEBOUNCE_MS) return
        lastAddAt = now
        if (TimerController.add(this)) Haptics.vibrate(this, Haptics.TICK)
    }

    // --- Permissões ---

    private fun askPermissionsOnce() {
        if (Prefs.permsAsked(this)) return
        val wanted = mutableListOf<String>()
        if (Prefs.showHr(this)) wanted += Manifest.permission.BODY_SENSORS
        if (Build.VERSION.SDK_INT >= 33) wanted += Manifest.permission.POST_NOTIFICATIONS
        val missing = wanted.filter { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
        Prefs.setPermsAsked(this)
        if (missing.isNotEmpty()) permLauncher.launch(missing.toTypedArray())
    }

    // --- Frequência cardíaca ---

    private fun startHeartRate() {
        stopHeartRate()
        if (!Prefs.showHr(this)) return
        if (checkSelfPermission(Manifest.permission.BODY_SENSORS) != PackageManager.PERMISSION_GRANTED) return
        val sm = getSystemService(SENSOR_SERVICE) as SensorManager
        val sensor = sm.getDefaultSensor(Sensor.TYPE_HEART_RATE) ?: return
        sm.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        sensorManager = sm
    }

    private fun stopHeartRate() {
        sensorManager?.unregisterListener(this)
        sensorManager = null
        heartRate = 0
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || event.sensor.type != Sensor.TYPE_HEART_RATE) return
        val bpm = event.values.firstOrNull()?.toInt() ?: 0
        if (bpm > 0 && event.accuracy >= SensorManager.SENSOR_STATUS_ACCURACY_LOW) heartRate = bpm
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}

// ---------------------------------------------------------------------------
// UI
// ---------------------------------------------------------------------------

val Orange = Color(0xFFFF6B00)
private val Track = Color(0xFF2A2A2A)
private val Gray = Color(0xFF9A9A9A)
private val HeartRed = Color(0xFFFF4D5E)

@Composable
private fun TimerScreen(
    showHr: Boolean,
    heartRate: Int,
    hint: String,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    onSwipeDown: () -> Unit
) {
    val state by TimerController.state.collectAsState()
    var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }

    // Relógio da tela: 60 fps nos 10 s finais (animação fluida), ~25 fps no resto.
    LaunchedEffect(Unit) {
        while (true) {
            withFrameMillis { now = SystemClock.elapsedRealtime() }
            val left = TimerController.state.value.endAt - SystemClock.elapsedRealtime()
            if (left > 11_000L) delay(40)
        }
    }

    val remaining = (state.endAt - now).coerceAtLeast(0L)
    val progress = if (state.totalMs > 0) (remaining.toFloat() / state.totalMs).coerceIn(0f, 1f) else 0f
    val totalSec = (remaining / 1000).toInt()
    val mm = totalSec / 60
    val ss = totalSec % 60
    val centis = ((remaining % 1000) / 10).toInt()

    // Pulso nos 10 s finais: sobe e desce suavemente uma vez por segundo.
    val pulse = if (remaining in 1L..10_000L) {
        val frac = (remaining % 1000L) / 1000f
        sin(PI * frac).toFloat().coerceIn(0f, 1f)
    } else 0f
    val digitColor = lerp(Color.White, Orange, pulse)
    val digitWeight = FontWeight((300 + 400 * pulse).toInt().coerceIn(100, 900))
    val digitScale = 1f + 0.10f * pulse

    val showFlash = now < state.flashUntil

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onTap() },
                    onLongPress = { onLongPress() }
                )
            }
            .pointerInput(Unit) {
                var dragged = 0f
                detectVerticalDragGestures(
                    onDragStart = { dragged = 0f },
                    onDragEnd = { if (dragged > 50.dp.toPx()) onSwipeDown() },
                    onVerticalDrag = { change, dy ->
                        change.consume()
                        dragged += dy
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(6.dp)) {
            val stroke = 10.dp.toPx()
            val inset = stroke / 2
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(
                color = Track, startAngle = -90f, sweepAngle = 360f, useCenter = false,
                topLeft = Offset(inset, inset), size = arcSize, style = Stroke(width = stroke)
            )
            drawArc(
                color = Orange, startAngle = -90f, sweepAngle = 360f * progress, useCenter = false,
                topLeft = Offset(inset, inset), size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            BasicText(
                text = if (showFlash) state.flashText else "Série ${state.series}",
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
                modifier = Modifier.graphicsLayer {
                    scaleX = digitScale
                    scaleY = digitScale
                },
                style = TextStyle(
                    color = digitColor,
                    fontSize = 54.sp,
                    fontWeight = digitWeight,
                    fontFamily = FontFamily.SansSerif,
                    textAlign = TextAlign.Center
                )
            )
            BasicText(
                text = ".%02d".format(centis),
                style = TextStyle(
                    color = Orange,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Light,
                    textAlign = TextAlign.Center
                )
            )
            if (showHr) {
                Spacer(Modifier.height(4.dp))
                BasicText(
                    text = if (heartRate > 0) "♥ $heartRate bpm" else "♥ -- bpm",
                    style = TextStyle(
                        color = HeartRed,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                )
            }
            Spacer(Modifier.height(6.dp))
            BasicText(
                text = hint,
                style = TextStyle(color = Gray, fontSize = 10.sp, textAlign = TextAlign.Center)
            )
        }
    }
}
