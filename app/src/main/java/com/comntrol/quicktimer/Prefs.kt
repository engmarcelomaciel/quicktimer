package com.comntrol.quicktimer

import android.content.Context
import android.content.SharedPreferences
import java.util.Calendar

/** Ajustes persistentes e contador de séries. */
object Prefs {
    private const val FILE = "quicktimer"

    val BASE_OPTIONS_S = listOf(15, 30, 45, 60, 90, 120, 180, 300)
    val INC_OPTIONS_S = listOf(15, 30, 60, 120)

    /** Zeramento automático: >0 = horas sem uso, RESET_MIDNIGHT = virada do dia, RESET_NEVER = nunca. */
    const val RESET_NEVER = 0
    const val RESET_MIDNIGHT = -1
    val RESET_OPTIONS = listOf(1, 2, 4, 8, 12, RESET_MIDNIGHT, RESET_NEVER)

    private fun sp(c: Context): SharedPreferences =
        c.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    // --- Tempo ---
    fun baseSeconds(c: Context): Int = sp(c).getInt("base_s", 60)
    fun setBaseSeconds(c: Context, v: Int) = sp(c).edit().putInt("base_s", v).apply()

    fun incSeconds(c: Context): Int = sp(c).getInt("inc_s", 60)
    fun setIncSeconds(c: Context, v: Int) = sp(c).edit().putInt("inc_s", v).apply()

    // --- Frequência cardíaca ---
    fun showHr(c: Context): Boolean = sp(c).getBoolean("show_hr", true)
    fun setShowHr(c: Context, v: Boolean) = sp(c).edit().putBoolean("show_hr", v).apply()

    fun permsAsked(c: Context): Boolean = sp(c).getBoolean("perms_asked", false)
    fun setPermsAsked(c: Context) = sp(c).edit().putBoolean("perms_asked", true).apply()

    // --- Séries ---
    fun resetMode(c: Context): Int = sp(c).getInt("reset_mode", RESET_MIDNIGHT)
    fun setResetMode(c: Context, v: Int) = sp(c).edit().putInt("reset_mode", v).apply()

    /** Registra atividade agora (conta para o zeramento por inatividade). */
    fun touch(c: Context) = sp(c).edit().putLong("last_activity", System.currentTimeMillis()).apply()

    fun resetSeries(c: Context) = sp(c).edit().putInt("series", 0).apply()

    /** Série atual, já aplicando o zeramento automático. */
    fun currentSeries(c: Context): Int {
        applyAutoReset(c)
        return sp(c).getInt("series", 0)
    }

    /** Inicia uma nova série e devolve o número dela. */
    fun nextSeries(c: Context): Int {
        val n = currentSeries(c) + 1
        sp(c).edit().putInt("series", n).apply()
        touch(c)
        return n
    }

    private fun applyAutoReset(c: Context) {
        val p = sp(c)
        val last = p.getLong("last_activity", 0L)
        if (last == 0L) return
        val now = System.currentTimeMillis()
        val expired = when (val mode = resetMode(c)) {
            RESET_NEVER -> false
            RESET_MIDNIGHT -> !sameDay(last, now)
            else -> now - last > mode * 3_600_000L
        }
        if (expired) p.edit().putInt("series", 0).apply()
    }

    private fun sameDay(a: Long, b: Long): Boolean {
        val ca = Calendar.getInstance().apply { timeInMillis = a }
        val cb = Calendar.getInstance().apply { timeInMillis = b }
        return ca.get(Calendar.YEAR) == cb.get(Calendar.YEAR) &&
            ca.get(Calendar.DAY_OF_YEAR) == cb.get(Calendar.DAY_OF_YEAR)
    }

    // --- Rótulos ---
    fun secondsLabel(s: Int): String {
        if (s < 60) return "$s s"
        val m = s / 60
        val r = s % 60
        return if (r == 0) "$m min" else "$m min $r s"
    }

    fun resetLabel(mode: Int): String = when (mode) {
        RESET_NEVER -> "Nunca"
        RESET_MIDNIGHT -> "À meia-noite"
        else -> "Após $mode h sem uso"
    }

    fun <T> next(list: List<T>, current: T): T {
        val i = list.indexOf(current)
        return list[(i + 1) % list.size]
    }
}
