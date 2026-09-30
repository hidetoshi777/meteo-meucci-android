package io.github.hidetoshi777.meteomeucci

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Si apre toccando il widget: mostra la versione animata della scena
 * (la pagina del sito della scuola) e, al primo avvio, come aggiungere il widget.
 */
class MainActivity : Activity() {

    private lateinit var web: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sfondo = Color.rgb(13, 19, 27)
        window.statusBarColor = sfondo
        window.navigationBarColor = sfondo

        web = WebView(this).apply {
            setBackgroundColor(sfondo)
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            webViewClient = object : WebViewClient() {
                override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                    if (request.isForMainFrame) view.loadDataWithBaseURL(null, SENZA_RETE, "text/html", "utf-8", null)
                }
            }
            loadUrl(PAGINA)
        }

        val barra = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setBackgroundColor(sfondo)
            val pad = dp(16)
            setPadding(pad, dp(12), pad, dp(20))
        }
        barra.addView(TextView(this).apply {
            text = "Stile del widget"
            setTextColor(Color.rgb(245, 166, 35))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            letterSpacing = 0.06f
        }, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        barra.addView(sceltaStile(), LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
            topMargin = dp(8)
            bottomMargin = dp(12)
        })
        barra.addView(TextView(this).apply {
            text = "Per il widget: tieni premuto su uno spazio vuoto della Home → Widget → Meteo Meucci."
            setTextColor(Color.rgb(180, 194, 208))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            gravity = Gravity.CENTER
        })
        val mgr = AppWidgetManager.getInstance(this)
        if (mgr.isRequestPinAppWidgetSupported) {
            barra.addView(Button(this).apply {
                text = "Aggiungi il widget alla Home"
                isAllCaps = false
                setOnClickListener {
                    mgr.requestPinAppWidget(ComponentName(this@MainActivity, MeteoWidget::class.java), null, null)
                }
            }, LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply { topMargin = dp(10) })
        }

        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(sfondo)
            addView(web, LinearLayout.LayoutParams(MATCH_PARENT, 0, 1f))
            addView(barra, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        })

        // Chi apre l'app vuole il dato fresco anche sul widget
        MeteoWorker.pianifica(this)
        MeteoWorker.subito(this)
    }

    override fun onDestroy() {
        web.destroy()
        super.onDestroy()
    }

    /** Fila di miniature: toccandone una si cambia lo stile di tutti i widget. */
    private fun sceltaStile(): HorizontalScrollView {
        val schede = mutableMapOf<Stile, FrameLayout>()
        fun evidenzia() {
            val scelto = Stile.corrente(this)
            schede.forEach { (stile, scheda) ->
                (scheda.background as GradientDrawable).setStroke(dp(if (stile == scelto) 3 else 1), if (stile == scelto) Color.rgb(245, 166, 35) else Color.rgb(46, 58, 72))
            }
        }
        val fila = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        Stile.entries.forEach { stile ->
            val colonna = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
            }
            val scheda = FrameLayout(this).apply {
                // Gli stili scontornati si vedono su un cielo, gli altri hanno il loro sfondo
                background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(Color.rgb(74, 163, 212), Color.rgb(26, 74, 106))).apply {
                    cornerRadius = dp(10).toFloat()
                }
                clipToOutline = true
                setPadding(dp(3), dp(3), dp(3), dp(3))
                addView(ImageView(this@MainActivity).apply {
                    setImageResource(stile.immagine)
                    scaleType = if (stile.scenaIntera) ImageView.ScaleType.CENTER_CROP else ImageView.ScaleType.FIT_CENTER
                    contentDescription = stile.nome
                }, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
                setOnClickListener {
                    Stile.salva(this@MainActivity, stile)
                    WidgetUpdater.aggiornaTutti(this@MainActivity)
                    evidenzia()
                }
            }
            schede[stile] = scheda
            colonna.addView(scheda, LinearLayout.LayoutParams(dp(112), dp(66)))
            colonna.addView(TextView(this).apply {
                text = stile.nome
                setTextColor(Color.rgb(220, 228, 238))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            }, LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply { topMargin = dp(4) })
            fila.addView(colonna, LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply { marginEnd = dp(10) })
        }
        evidenzia()
        return HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            addView(fila)
        }
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    companion object {
        private const val PAGINA = "https://hidetoshi777.github.io/Scuola/professionale/admin/widget.html"
        private const val SENZA_RETE =
            "<html><body style='margin:0;height:100vh;display:flex;align-items:center;justify-content:center;" +
                "background:#0d131b;color:#b4c2d0;font-family:sans-serif;text-align:center;padding:24px;box-sizing:border-box'>" +
                "<p>Serve la connessione per vedere la scena animata.<br>Il widget sulla Home continua a funzionare con l'ultimo dato.</p>" +
                "</body></html>"
    }
}
