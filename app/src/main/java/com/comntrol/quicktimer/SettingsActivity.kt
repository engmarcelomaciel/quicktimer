package com.comntrol.quicktimer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.ListHeader
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text

/** Ajustes: cada item alterna entre as opções a cada toque. */
class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { SettingsScreen() } }
    }
}

@Composable
private fun SettingsScreen() {
    val ctx = LocalContext.current
    var base by remember { mutableIntStateOf(Prefs.baseSeconds(ctx)) }
    var inc by remember { mutableIntStateOf(Prefs.incSeconds(ctx)) }
    var reset by remember { mutableIntStateOf(Prefs.resetMode(ctx)) }
    var hr by remember { mutableStateOf(Prefs.showHr(ctx)) }
    var series by remember { mutableIntStateOf(Prefs.currentSeries(ctx)) }
    val listState = rememberScalingLazyListState()

    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize().background(Color.Black),
        state = listState,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item { ListHeader { Text("Ajustes do timer") } }

        item {
            SettingChip("Tempo inicial", Prefs.secondsLabel(base)) {
                base = Prefs.next(Prefs.BASE_OPTIONS_S, base)
                Prefs.setBaseSeconds(ctx, base)
            }
        }
        item {
            SettingChip("Incremento por clique", "+" + Prefs.secondsLabel(inc)) {
                inc = Prefs.next(Prefs.INC_OPTIONS_S, inc)
                Prefs.setIncSeconds(ctx, inc)
            }
        }
        item {
            SettingChip("Frequência cardíaca", if (hr) "Mostrar" else "Ocultar") {
                hr = !hr
                Prefs.setShowHr(ctx, hr)
            }
        }

        item { ListHeader { Text("Séries") } }

        item {
            SettingChip("Zerar automaticamente", Prefs.resetLabel(reset)) {
                reset = Prefs.next(Prefs.RESET_OPTIONS, reset)
                Prefs.setResetMode(ctx, reset)
            }
        }
        item {
            SettingChip("Zerar agora", "Série atual: $series") {
                Prefs.resetSeries(ctx)
                series = 0
                Haptics.vibrate(ctx, Haptics.TICK)
            }
        }
    }
}

@Composable
private fun SettingChip(label: String, value: String, onClick: () -> Unit) {
    Chip(
        onClick = onClick,
        label = { Text(label) },
        secondaryLabel = { Text(value, color = Orange) },
        colors = ChipDefaults.secondaryChipColors(),
        modifier = Modifier.fillMaxWidth()
    )
}
