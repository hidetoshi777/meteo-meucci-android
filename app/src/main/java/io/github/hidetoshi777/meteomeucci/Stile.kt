package io.github.hidetoshi777.meteomeucci

import android.content.Context

/**
 * Stili del plesso nel widget.
 * - scenaIntera = false: l'edificio è scontornato e il cielo lo disegna l'app (cambia con ora e meteo).
 * - scenaIntera = true: l'immagine ha il suo sfondo (carta blu, notte al neon) e l'app ci aggiunge
 *   solo pioggia, neve, lampi e nebbia. notturna = è già una scena di notte, non va scurita la sera.
 */
enum class Stile(
    val chiave: String,
    val nome: String,
    val immagine: Int,
    val scenaIntera: Boolean,
    val notturna: Boolean = false,
) {
    ORIGINALE("originale", "Originale", R.drawable.meucci, false),
    ACQUERELLO("acquerello", "Acquerello", R.drawable.meucci_acquerello, false),
    MATTONCINI("mattoncini", "Mattoncini", R.drawable.meucci_mattoncini, false),
    PROGETTO("progetto", "Progetto tecnico", R.drawable.meucci_progetto, true),
    CYBERPUNK("cyberpunk", "Cyberpunk", R.drawable.meucci_cyberpunk, true, notturna = true);

    companion object {
        private const val PREF = "impostazioni"
        private const val CHIAVE = "stile"

        fun corrente(ctx: Context): Stile {
            val salvato = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).getString(CHIAVE, null)
            return entries.firstOrNull { it.chiave == salvato } ?: ORIGINALE
        }

        fun salva(ctx: Context, stile: Stile) {
            ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putString(CHIAVE, stile.chiave).apply()
        }
    }
}
