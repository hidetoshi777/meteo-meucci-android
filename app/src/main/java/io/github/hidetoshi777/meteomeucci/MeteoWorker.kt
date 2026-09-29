package io.github.hidetoshi777.meteomeucci

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

/**
 * Ogni 30 minuti circa: scarica il meteo e ridisegna i widget.
 * Il ridisegno avviene anche senza rete, perché il cielo cambia con l'ora.
 */
class MeteoWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        runCatching { MeteoRepo.aggiorna(applicationContext) }
        WidgetUpdater.aggiornaTutti(applicationContext)
        return Result.success()
    }

    companion object {
        private const val PERIODICO = "meteo-periodico"
        private const val SUBITO = "meteo-subito"

        fun pianifica(ctx: Context) {
            val richiesta = PeriodicWorkRequestBuilder<MeteoWorker>(30, TimeUnit.MINUTES).build()
            WorkManager.getInstance(ctx).enqueueUniquePeriodicWork(PERIODICO, ExistingPeriodicWorkPolicy.KEEP, richiesta)
        }

        fun subito(ctx: Context) {
            val richiesta = OneTimeWorkRequestBuilder<MeteoWorker>().build()
            WorkManager.getInstance(ctx).enqueueUniqueWork(SUBITO, ExistingWorkPolicy.REPLACE, richiesta)
        }

        fun annulla(ctx: Context) {
            WorkManager.getInstance(ctx).cancelUniqueWork(PERIODICO)
        }
    }
}
