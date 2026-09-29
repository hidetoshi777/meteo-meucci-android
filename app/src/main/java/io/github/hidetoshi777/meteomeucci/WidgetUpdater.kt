package io.github.hidetoshi777.meteomeucci

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.widget.RemoteViews
import java.time.Instant
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

object WidgetUpdater {

    private const val NBSP = " "
    private val ORA_MINUTI: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    fun aggiornaTutti(ctx: Context) {
        val mgr = AppWidgetManager.getInstance(ctx)
        mgr.getAppWidgetIds(ComponentName(ctx, MeteoWidget::class.java)).forEach { aggiorna(ctx, mgr, it) }
    }

    fun aggiorna(ctx: Context, mgr: AppWidgetManager, id: Int) {
        // Misura del riquadro: in verticale conta la larghezza minima e l'altezza massima
        val opzioni = mgr.getAppWidgetOptions(id)
        val verticale = ctx.resources.configuration.orientation != Configuration.ORIENTATION_LANDSCAPE
        var wDp = opzioni.getInt(if (verticale) AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH else AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH)
        var hDp = opzioni.getInt(if (verticale) AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT else AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)
        if (wDp <= 0) wDp = 250
        if (hDp <= 0) hDp = 110

        val rv = vista(ctx, wDp, hDp, MeteoRepo.leggi(ctx), ZonedDateTime.now(ROMA))
        val apri = PendingIntent.getActivity(
            ctx, 0,
            Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        rv.setOnClickPendingIntent(R.id.radice, apri)
        mgr.updateAppWidget(id, rv)
    }

    /** Il contenuto del widget per un riquadro di wDp × hDp (usato anche dal test delle anteprime). */
    fun vista(ctx: Context, wDp: Int, hDp: Int, meteo: Meteo?, adesso: ZonedDateTime): RemoteViews {
        val largo = wDp >= hDp * 1.3f
        val densita = ctx.resources.displayMetrics.density
        val riduci = min(1f, 900f / max(wDp, hDp) / densita)
        val wPx = (wDp * densita * riduci).roundToInt().coerceAtLeast(64)
        val hPx = (hDp * densita * riduci).roundToInt().coerceAtLeast(64)

        val tempo = meteo?.let { tempoPer(it.codice) }
        val ventoso = meteo != null && (meteo.vento >= 20 || meteo.raffiche >= 35)
        val scena = SceneRenderer.disegna(ctx, wPx, hPx, largo, Fascia.per(adesso.hour), tempo?.first, ventoso)

        val rv = RemoteViews(ctx.packageName, if (largo) R.layout.widget_largo else R.layout.widget_alto)
        rv.setImageViewBitmap(R.id.scena, scena)
        if (meteo != null && tempo != null) {
            rv.setTextViewText(R.id.temp, "${meteo.temp.roundToInt()}°")
            rv.setTextViewText(R.id.cielo, tempo.second)
            rv.setTextViewText(R.id.dettagli, dettagli(meteo))
        } else {
            rv.setTextViewText(R.id.temp, "--°")
            rv.setTextViewText(R.id.cielo, "Meteo in arrivo…")
            rv.setTextViewText(R.id.dettagli, "")
        }
        return rv
    }

    private fun dettagli(m: Meteo): String = buildString {
        if (m.min != null && m.max != null) {
            append("min${NBSP}${m.min.roundToInt()}° · max${NBSP}${m.max.roundToInt()}° · ")
        }
        append("vento${NBSP}${m.vento.roundToInt()}${NBSP}km/h")
        // Se il telefono è rimasto a lungo senza rete, si dice di quando è il dato
        val eta = System.currentTimeMillis() - m.salvato
        if (m.salvato > 0 && eta > 2 * 3600 * 1000L) {
            val quando = ZonedDateTime.ofInstant(Instant.ofEpochMilli(m.salvato), ROMA)
            append(" · dato delle ${ORA_MINUTI.format(quando)}")
        }
    }
}
