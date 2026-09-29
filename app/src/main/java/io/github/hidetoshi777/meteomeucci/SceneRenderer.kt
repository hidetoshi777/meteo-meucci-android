package io.github.hidetoshi777.meteomeucci

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Disegna la scena del widget: cielo della fascia oraria, sole o luna, nuvole,
 * pioggia, neve, lampi e vento attorno all'illustrazione del Meucci.
 * Le misure ricalcano il CSS del widget web: un "rem" vale 1/9,2 dell'altezza della scena.
 */
object SceneRenderer {

    private class Tavolozza(val alto: Int, val basso: Int, val alone: Int, val velo: Int)

    private fun tavolozza(f: Fascia) = when (f) {
        Fascia.MATTINA -> Tavolozza(0xFFF0A070.toInt(), 0xFF3A4F78.toInt(), Color.argb(122, 255, 186, 120), Color.argb(184, 42, 22, 16))
        Fascia.GIORNO -> Tavolozza(0xFF4AA3D4.toInt(), 0xFF1A4A6A.toInt(), Color.argb(97, 255, 224, 120), Color.argb(179, 12, 28, 40))
        Fascia.SERA -> Tavolozza(0xFFE07048.toInt(), 0xFF2A2048.toInt(), Color.argb(117, 255, 110, 70), Color.argb(199, 42, 16, 28))
        Fascia.NOTTE -> Tavolozza(0xFF1B2850.toInt(), 0xFF090F1D.toInt(), Color.argb(56, 148, 176, 255), Color.argb(209, 8, 12, 28))
    }

    private var edificio: Bitmap? = null

    private fun edificio(ctx: Context): Bitmap =
        edificio ?: BitmapFactory.decodeResource(ctx.resources, R.drawable.meucci).also { edificio = it }

    private fun mescola(a: Int, b: Int, t: Float): Int = Color.argb(
        (Color.alpha(a) + (Color.alpha(b) - Color.alpha(a)) * t).toInt(),
        (Color.red(a) + (Color.red(b) - Color.red(a)) * t).toInt(),
        (Color.green(a) + (Color.green(b) - Color.green(a)) * t).toInt(),
        (Color.blue(a) + (Color.blue(b) - Color.blue(a)) * t).toInt(),
    )

    fun disegna(
        ctx: Context,
        w: Int,
        h: Int,
        largo: Boolean,
        fascia: Fascia,
        tempo: Tempo?,
        ventoso: Boolean,
    ): Bitmap {
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val t = tavolozza(fascia)
        val notte = fascia == Fascia.NOTTE
        val coperto = tempo != null && tempo != Tempo.SERENO && tempo != Tempo.VARIABILE

        // Zona della scena: a sinistra nel riquadro largo, in alto in quello quadrato
        val sw = if (largo) w * 0.535f else w.toFloat()
        val sh = if (largo) h.toFloat() else h * 0.56f
        val u = min(sh / 9.2f, sw / 12f)

        // Cielo
        var alto = t.alto
        var basso = t.basso
        if (coperto && !notte) {
            alto = mescola(alto, 0xFF7D8A97.toInt(), 0.45f)
            basso = mescola(basso, 0xFF34404D.toInt(), 0.35f)
        }
        if (tempo == Tempo.TEMPORALE) {
            alto = mescola(alto, 0xFF1C2230.toInt(), 0.4f)
            basso = mescola(basso, 0xFF10141C.toInt(), 0.3f)
        }
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.shader = LinearGradient(w * 0.56f, 0f, w * 0.44f, h * 1.2f, alto, basso, Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), p)

        // Puntinato leggero in alto, come nel widget web
        p.shader = null
        val passo = 0.8125f * u
        var y = passo / 2
        while (y < h * 0.62f) {
            p.color = Color.argb((36 * (1 - y / (h * 0.62f))).toInt(), 255, 255, 255)
            var x = passo / 2
            while (x < w) {
                c.drawCircle(x, y, u / 16f, p)
                x += passo
            }
            y += passo
        }

        // Posizione di sole e luna (dal bordo destro della scena, in rem)
        val (cimaRem, destraRem) = when (fascia) {
            Fascia.MATTINA -> 2.15f to 3.2f
            Fascia.GIORNO -> 0.55f to 1.1f
            Fascia.SERA -> 2.65f to 0.5f
            Fascia.NOTTE -> 0.75f to 1.2f
        }
        val r = 1.225f * u
        val sx = sw - destraRem * u - r
        val sy = cimaRem * u + r

        p.shader = RadialGradient(sx, sy, maxOf(w, h) * 0.34f, t.alone, Color.TRANSPARENT, Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), p)
        p.shader = null

        // L'edificio, con la luce della fascia oraria
        val img = edificio(ctx)
        val rapporto = img.height.toFloat() / img.width
        val bw = if (largo) min(sw * 1.04f, h * 0.8f / rapporto) else min(w * 1.18f, sh * 0.98f / rapporto)
        val cx = if (largo) sw * 0.49f else w * 0.5f
        val fondo = if (largo) h * 0.96f else sh * 1.03f
        val pe = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        pe.colorFilter = filtroEdificio(fascia, coperto)
        c.drawBitmap(img, null, RectF(cx - bw / 2, fondo - bw * rapporto, cx + bw / 2, fondo), pe)

        // Nuvole (sopra l'edificio, come nell'originale)
        if (tempo != null && tempo != Tempo.SERENO) {
            val grigie = tempo == Tempo.PIOGGIA || tempo == Tempo.TEMPORALE || tempo == Tempo.NEVE
            val alfa = if (notte) 0.62f else 0.92f
            nuvola(c, 1f * u, 1.4f * u, u, 1f, alfa, grigie, notte)
            nuvola(c, 5.2f * u, 3.45f * u, u, 0.7f, alfa * 0.78f, grigie, notte)
            if (coperto) nuvola(c, sw - 4.6f * u, 1.1f * u, u, 0.85f, alfa * 0.9f, grigie, notte)
        }

        // Sole o luna (sopra le nuvole, velati se il cielo è coperto)
        val velato = if (coperto) 0.22f else 1f
        if (notte) luna(c, sx, sy, r, velato) else sole(c, sx, sy, r, fascia, velato)

        if (ventoso) {
            vento(c, 7.5f * u, 4.1f * u, u, 1f)
            vento(c, 2f * u, 6.15f * u, u, 0.78f)
        }
        when (tempo) {
            Tempo.PIOGGIA, Tempo.TEMPORALE -> pioggia(c, sw, u)
            Tempo.NEVE -> neve(c, sw, u)
            else -> Unit
        }
        if (tempo == Tempo.TEMPORALE) lampo(c, sw - 2.7f * u - 1.25f * u, 2.75f * u, u)
        if (tempo == Tempo.NEBBIA) {
            p.shader = LinearGradient(0f, h * 0.3f, 0f, h.toFloat(), Color.TRANSPARENT, Color.argb(150, 215, 222, 228), Shader.TileMode.CLAMP)
            c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), p)
            p.shader = null
        }

        // Velo scuro sotto il testo, perché resti leggibile su ogni cielo
        if (largo) {
            p.shader = LinearGradient(w * 0.42f, 0f, w * 0.56f, 0f, Color.TRANSPARENT, t.velo, Shader.TileMode.CLAMP)
            c.drawRect(w * 0.42f, 0f, w.toFloat(), h.toFloat(), p)
        } else {
            p.shader = LinearGradient(0f, h * 0.48f, 0f, h * 0.66f, Color.TRANSPARENT, t.velo, Shader.TileMode.CLAMP)
            c.drawRect(0f, h * 0.48f, w.toFloat(), h.toFloat(), p)
        }
        return bmp
    }

    private fun filtroEdificio(fascia: Fascia, coperto: Boolean): ColorMatrixColorFilter {
        // Traduzione dei filter CSS (brightness/saturate/sepia) del widget web
        val (sat, rgb) = when (fascia) {
            Fascia.MATTINA -> 1.08f to floatArrayOf(1.08f, 1.04f, 0.96f)
            Fascia.GIORNO -> 1.05f to floatArrayOf(1.06f, 1.06f, 1.06f)
            Fascia.SERA -> 1.15f to floatArrayOf(0.86f, 0.76f, 0.66f)
            Fascia.NOTTE -> 0.7f to floatArrayOf(0.5f, 0.53f, 0.64f)
        }
        val k = if (coperto) 0.93f else 1f
        val m = ColorMatrix()
        m.setSaturation(if (coperto) sat * 0.8f else sat)
        m.postConcat(ColorMatrix().apply { setScale(rgb[0] * k, rgb[1] * k, rgb[2] * k, 1f) })
        return ColorMatrixColorFilter(m)
    }

    private fun sole(c: Canvas, cx: Float, cy: Float, r: Float, fascia: Fascia, velato: Float) {
        val a = (255 * velato).toInt()
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        // raggi
        p.style = Paint.Style.STROKE
        p.strokeCap = Paint.Cap.ROUND
        p.strokeWidth = r * 0.22f
        p.color = Color.argb(a, 255, 209, 92)
        for (i in 0 until 8) {
            val ang = Math.toRadians(i * 45.0 + 12.0)
            c.drawLine(
                cx + (cos(ang) * r * 1.2f).toFloat(), cy + (sin(ang) * r * 1.2f).toFloat(),
                cx + (cos(ang) * r * 1.48f).toFloat(), cy + (sin(ang) * r * 1.48f).toFloat(), p,
            )
        }
        // disco
        val (chiaro, scuro, bordo) = when (fascia) {
            Fascia.MATTINA -> Triple(0xFFFFD889.toInt(), 0xFFFF9656.toInt(), 0xFFEC764E.toInt())
            Fascia.SERA -> Triple(0xFFFFC36C.toInt(), 0xFFF06A45.toInt(), 0xFFDC5742.toInt())
            else -> Triple(0xFFFFE978.toInt(), 0xFFFFB72F.toInt(), 0xFFF39A25.toInt())
        }
        p.style = Paint.Style.FILL
        p.shader = RadialGradient(cx, cy, r * 2.2f, Color.argb((140 * velato).toInt(), 255, 200, 87), Color.TRANSPARENT, Shader.TileMode.CLAMP)
        c.drawCircle(cx, cy, r * 2.2f, p)
        p.shader = LinearGradient(cx - r, cy - r, cx + r, cy + r, chiaro, scuro, Shader.TileMode.CLAMP)
        p.alpha = a
        c.drawCircle(cx, cy, r, p)
        p.shader = null
        p.style = Paint.Style.STROKE
        p.strokeWidth = r * 0.08f
        p.color = bordo
        p.alpha = a
        c.drawCircle(cx, cy, r, p)
        // riflesso
        p.color = Color.argb((87 * velato).toInt(), 255, 255, 255)
        p.strokeWidth = r * 0.12f
        c.drawArc(RectF(cx - r * 0.72f, cy - r * 0.72f, cx + r * 0.72f, cy + r * 0.72f), 195f, 55f, false, p)
        // faccina
        p.style = Paint.Style.FILL
        p.color = Color.argb(a, 111, 70, 27)
        c.drawOval(RectF(cx - r * 0.38f, cy - r * 0.34f, cx - r * 0.2f, cy - r * 0.09f), p)
        c.drawOval(RectF(cx + r * 0.2f, cy - r * 0.34f, cx + r * 0.38f, cy - r * 0.09f), p)
        p.style = Paint.Style.STROKE
        p.strokeWidth = r * 0.08f
        p.color = Color.argb(a, 135, 82, 25)
        c.drawArc(RectF(cx - r * 0.3f, cy - r * 0.05f, cx + r * 0.3f, cy + r * 0.42f), 25f, 130f, false, p)
    }

    private fun luna(c: Canvas, cx: Float, cy: Float, r: Float, velato: Float) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.shader = RadialGradient(cx, cy, r * 2.3f, Color.argb((90 * velato).toInt(), 183, 204, 255), Color.TRANSPARENT, Shader.TileMode.CLAMP)
        c.drawCircle(cx, cy, r * 2.3f, p)
        p.shader = null
        // disco chiaro spostato a sinistra dentro il disco scuro: falce con la parte in ombra a destra
        c.save()
        val bordo = Path().apply { addCircle(cx, cy, r, Path.Direction.CW) }
        c.clipPath(bordo)
        p.color = Color.argb((255 * velato).toInt(), 37, 50, 82)
        c.drawCircle(cx, cy, r, p)
        p.color = Color.argb((255 * velato).toInt(), 245, 232, 173)
        c.drawCircle(cx - r * 0.55f, cy + r * 0.05f, r, p)
        c.restore()
        p.style = Paint.Style.STROKE
        p.strokeWidth = r * 0.07f
        p.color = Color.argb((255 * velato).toInt(), 219, 209, 157)
        c.drawCircle(cx, cy, r, p)
    }

    private fun nuvola(c: Canvas, x: Float, y: Float, u: Float, scala: Float, alfa: Float, grigia: Boolean, notte: Boolean) {
        c.save()
        c.translate(x, y)
        c.scale(scala, scala)
        val w = 3.25f * u
        val h = 1.05f * u
        val forma = Path().apply {
            addRoundRect(RectF(0f, 0f, w, h), h / 2, h / 2, Path.Direction.CW)
            addCircle(0.42f * u + 0.59f * u, h - 0.2f * u - 0.59f * u, 0.59f * u, Path.Direction.CW)
            addCircle(w - 0.35f * u - 0.79f * u, h - 0.2f * u - 0.79f * u, 0.79f * u, Path.Direction.CW)
        }
        val unita = Path().apply { op(forma, Path.Op.UNION) }
        var chiaro = if (grigia) 0xFFD6DDE5.toInt() else 0xFFFFFFFF.toInt()
        var scuro = if (grigia) 0xFFAEB9C6.toInt() else 0xFFDFF1FB.toInt()
        var bordo = if (grigia) 0xFF8E9BAA.toInt() else 0xFFB7DAEB.toInt()
        if (notte) {
            chiaro = mescola(chiaro, 0xFF2A3350.toInt(), 0.35f)
            scuro = mescola(scuro, 0xFF2A3350.toInt(), 0.35f)
            bordo = mescola(bordo, 0xFF2A3350.toInt(), 0.35f)
        }
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.shader = LinearGradient(0f, -0.6f * u, 0f, h, chiaro, scuro, Shader.TileMode.CLAMP)
        p.alpha = (255 * alfa).toInt()
        c.drawPath(unita, p)
        p.shader = null
        p.style = Paint.Style.STROKE
        p.strokeWidth = u * 0.12f
        p.color = bordo
        p.alpha = (255 * alfa).toInt()
        c.drawPath(unita, p)
        // faccina
        p.style = Paint.Style.FILL
        p.color = 0xFF486477.toInt()
        p.alpha = (255 * alfa).toInt()
        val occhioY = h - 0.29f * u - 0.1f * u
        c.drawOval(RectF(1f * u, occhioY - 0.1f * u, 1.16f * u, occhioY + 0.1f * u), p)
        c.drawOval(RectF(1.58f * u, occhioY - 0.1f * u, 1.74f * u, occhioY + 0.1f * u), p)
        p.style = Paint.Style.STROKE
        p.strokeWidth = u * 0.09f
        p.color = 0xFF658397.toInt()
        p.alpha = (255 * alfa).toInt()
        c.drawArc(RectF(1.2f * u, occhioY - 0.05f * u, 1.55f * u, occhioY + 0.2f * u), 20f, 140f, false, p)
        c.restore()
    }

    private fun vento(c: Canvas, x: Float, y: Float, u: Float, scala: Float) {
        c.save()
        c.translate(x, y)
        c.scale(scala, scala)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.style = Paint.Style.STROKE
        p.strokeCap = Paint.Cap.ROUND
        p.strokeWidth = u * 0.19f
        p.color = Color.argb(235, 190, 229, 246)
        c.drawArc(RectF(0f, 0f, 3.8f * u, 1.6f * u), 200f, 140f, false, p)
        p.color = Color.argb(184, 190, 229, 246)
        c.drawArc(RectF(1.1f * u, 0.26f * u, 3.2f * u, 1.26f * u), 200f, 140f, false, p)
        p.style = Paint.Style.FILL
        p.color = 0xFF87CC65.toInt()
        c.save()
        c.rotate(28f, 3.6f * u, 0.1f * u)
        c.drawOval(RectF(3.3f * u, -0.1f * u, 3.95f * u, 0.28f * u), p)
        c.restore()
        c.restore()
    }

    private fun pioggia(c: Canvas, sw: Float, u: Float) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.color = 0xFF7DD8FF.toInt()
        p.strokeCap = Paint.Cap.ROUND
        p.strokeWidth = u * 0.18f
        val scarti = floatArrayOf(0f, 0.55f, 0.1f, 0.75f, 0.25f)
        var gx = 1.7f * u
        var gruppo = 0
        while (gx < sw - u) {
            for (i in scarti.indices) {
                val x = gx + i * u
                val y = 2.8f * u + scarti[i] * u + (gruppo % 2) * 1.6f * u
                c.drawLine(x, y, x - 0.21f * u, y + 0.7f * u, p)
                c.drawLine(x - 0.9f * u, y + 2.4f * u, x - 1.11f * u, y + 3.1f * u, p)
            }
            gx += 5.3f * u
            gruppo++
        }
    }

    private fun neve(c: Canvas, sw: Float, u: Float) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.color = Color.WHITE
        val scarti = floatArrayOf(0f, 0.55f, 0.1f, 0.75f, 0.25f)
        var gx = 1.7f * u
        var gruppo = 0
        while (gx < sw - u) {
            for (i in scarti.indices) {
                val x = gx + i * u
                val y = 2.8f * u + scarti[i] * u + (gruppo % 2) * 1.6f * u
                c.drawCircle(x, y, 0.17f * u, p)
                c.drawCircle(x - 0.5f * u, y + 2.3f * u, 0.14f * u, p)
            }
            gx += 5.3f * u
            gruppo++
        }
    }

    private fun lampo(c: Canvas, x: Float, y: Float, u: Float) {
        val w = 1.25f * u
        val h = 2.1f * u
        val punti = floatArrayOf(0.48f, 0f, 1f, 0f, 0.68f, 0.39f, 0.94f, 0.39f, 0.24f, 1f, 0.42f, 0.56f, 0.12f, 0.56f)
        val path = Path()
        for (i in punti.indices step 2) {
            val px = x + punti[i] * w
            val py = y + punti[i + 1] * h
            if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }
        path.close()
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.color = 0xFFFFE868.toInt()
        p.setShadowLayer(0.45f * u, 0f, 0f, Color.argb(178, 255, 232, 104))
        c.drawPath(path, p)
    }
}
