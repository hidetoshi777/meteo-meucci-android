package io.github.hidetoshi777.meteomeucci

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.os.Bundle

class MeteoWidget : AppWidgetProvider() {

    override fun onUpdate(ctx: Context, mgr: AppWidgetManager, ids: IntArray) {
        // Subito con l'ultimo dato salvato, poi il lavoro in background scarica quello nuovo
        ids.forEach { WidgetUpdater.aggiorna(ctx, mgr, it) }
        MeteoWorker.pianifica(ctx)
        MeteoWorker.subito(ctx)
    }

    override fun onAppWidgetOptionsChanged(ctx: Context, mgr: AppWidgetManager, id: Int, opzioni: Bundle) {
        // Riquadro ridimensionato: la scena va ridisegnata alla nuova misura
        WidgetUpdater.aggiorna(ctx, mgr, id)
    }

    override fun onEnabled(ctx: Context) {
        MeteoWorker.pianifica(ctx)
    }

    override fun onDisabled(ctx: Context) {
        MeteoWorker.annulla(ctx)
    }
}
