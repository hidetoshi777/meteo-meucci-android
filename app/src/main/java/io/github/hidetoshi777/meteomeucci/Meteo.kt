package io.github.hidetoshi777.meteomeucci

import android.content.Context
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.ZoneId

val ROMA: ZoneId = ZoneId.of("Europe/Rome")

/** Fasce della giornata: stesse soglie del widget web (js/school-clock.js). */
enum class Fascia {
    MATTINA, GIORNO, SERA, NOTTE;

    companion object {
        fun per(ora: Int): Fascia = when (ora) {
            in 5..11 -> MATTINA
            in 12..17 -> GIORNO
            in 18..20 -> SERA
            else -> NOTTE
        }
    }
}

enum class Tempo { SERENO, VARIABILE, NUVOLOSO, NEBBIA, PIOGGIA, NEVE, TEMPORALE }

/** Codici WMO di Open-Meteo → categoria per la scena ed etichetta da mostrare. */
fun tempoPer(codice: Int): Pair<Tempo, String> = when (codice) {
    0 -> Tempo.SERENO to "Sereno"
    1, 2 -> Tempo.VARIABILE to "Poco nuvoloso"
    3 -> Tempo.NUVOLOSO to "Nuvoloso"
    45, 48 -> Tempo.NEBBIA to "Nebbia"
    in 51..57 -> Tempo.PIOGGIA to "Pioviggine"
    in 61..67 -> Tempo.PIOGGIA to "Pioggia"
    in 71..77 -> Tempo.NEVE to "Neve"
    in 80..82 -> Tempo.PIOGGIA to "Rovesci"
    85, 86 -> Tempo.NEVE to "Rovesci di neve"
    in 95..99 -> Tempo.TEMPORALE to "Temporale"
    else -> Tempo.VARIABILE to "Meteo variabile"
}

data class Meteo(
    val temp: Double,
    val codice: Int,
    val vento: Double,
    val raffiche: Double,
    val min: Double?,
    val max: Double?,
    val salvato: Long,
)

object MeteoRepo {
    private const val URL_METEO =
        "https://api.open-meteo.com/v1/forecast?latitude=37.61&longitude=15.16" +
            "&current=temperature_2m,weather_code,wind_speed_10m,wind_gusts_10m" +
            "&daily=temperature_2m_max,temperature_2m_min" +
            "&timezone=Europe%2FRome&forecast_days=1"
    private const val PREF = "meteo"

    /** Scarica e salva il meteo; se la rete non c'è resta valido l'ultimo dato salvato. */
    fun aggiorna(ctx: Context): Boolean {
        val conn = URL(URL_METEO).openConnection() as HttpURLConnection
        conn.connectTimeout = 10_000
        conn.readTimeout = 10_000
        try {
            if (conn.responseCode != 200) return false
            val testo = conn.inputStream.bufferedReader().use { it.readText() }
            if (analizza(testo, 0L) == null) return false
            ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit()
                .putString("json", testo)
                .putLong("salvato", System.currentTimeMillis())
                .commit()
            return true
        } finally {
            conn.disconnect()
        }
    }

    fun leggi(ctx: Context): Meteo? {
        val p = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        val testo = p.getString("json", null) ?: return null
        return analizza(testo, p.getLong("salvato", 0L))
    }

    private fun analizza(testo: String, salvato: Long): Meteo? = try {
        val j = JSONObject(testo)
        val cur = j.getJSONObject("current")
        val giorno = j.optJSONObject("daily")
        fun primo(nome: String): Double? =
            giorno?.optJSONArray(nome)?.optDouble(0)?.takeUnless { it.isNaN() }
        Meteo(
            temp = cur.getDouble("temperature_2m"),
            codice = cur.getInt("weather_code"),
            vento = cur.optDouble("wind_speed_10m", 0.0),
            raffiche = cur.optDouble("wind_gusts_10m", 0.0),
            min = primo("temperature_2m_min"),
            max = primo("temperature_2m_max"),
            salvato = salvato,
        )
    } catch (e: Exception) {
        null
    }
}
