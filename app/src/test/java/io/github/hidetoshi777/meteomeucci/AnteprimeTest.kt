package io.github.hidetoshi777.meteomeucci

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.widget.FrameLayout
import androidx.test.core.app.ApplicationProvider
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.ZonedDateTime

/**
 * Disegna il widget vero (stessi layout e stessa scena dell'app) in varie ore e condizioni
 * e salva i PNG in app/build/anteprime: la CI li pubblica come artifact, così si vede
 * com'è il widget senza un telefono sotto mano.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
class AnteprimeTest {

    @Test
    fun disegnaAnteprime() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val cartella = File("build/anteprime").apply { mkdirs() }
        val casi = listOf(
            Triple("giorno_poco_nuvoloso", 15, 2),
            Triple("mattina_sereno", 8, 0),
            Triple("sera_nuvoloso", 19, 3),
            Triple("notte_sereno", 23, 0),
            Triple("giorno_pioggia", 13, 63),
            Triple("giorno_temporale", 16, 95),
            Triple("mattina_neve", 9, 73),
            Triple("mattina_nebbia", 7, 45),
        )
        val riquadri = listOf(Triple("4x2", 320, 150), Triple("2x2", 160, 160), Triple("4x3", 320, 240))
        for ((nome, ora, codice) in casi) {
            val meteo = Meteo(23.4, codice, if (codice == 63) 24.0 else 11.0, 20.0, 19.6, 25.1, System.currentTimeMillis())
            val adesso = ZonedDateTime.of(2026, 9, 29, ora, 5, 0, 0, ROMA)
            for ((formato, wDp, hDp) in riquadri) {
                if (formato != "4x2" && nome != "giorno_poco_nuvoloso" && nome != "notte_sereno") continue
                val rv = WidgetUpdater.vista(ctx, wDp, hDp, meteo, adesso)
                val v = rv.apply(ctx, FrameLayout(ctx))
                val d = ctx.resources.displayMetrics.density
                val w = (wDp * d).toInt()
                val h = (hDp * d).toInt()
                v.measure(
                    View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY),
                )
                v.layout(0, 0, w, h)
                val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                v.draw(Canvas(bmp))
                File(cartella, "${formato}_$nome.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
            }
        }
    }
}
