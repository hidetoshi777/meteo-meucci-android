# Meteo Meucci

Widget per la Home di Android con il plesso Meucci dell'IISS «G. Ferraris» di Acireale: ora, meteo e un cielo che cambia con la giornata.

**Scarica l'app:** https://hidetoshi777.github.io/Scuola/meteo-meucci/ (pagina con istruzioni; l'APK è servito direttamente dal sito della scuola)

## Installazione

1. Dal telefono apri il link qui sopra e scarica il file.
2. Aprilo dalle notifiche o dalla cartella Download. Android chiede di consentire l'installazione da questa fonte: consenti (vale solo per il browser o l'app File che stai usando).
3. Apri **Meteo Meucci** e tocca **Aggiungi il widget alla Home**, oppure tieni premuto su uno spazio vuoto della Home → Widget → Meteo Meucci.

Il riquadro consigliato è 4×2. Si può ridimensionare: da quadrato mette la scena sopra e il testo sotto.

## Cosa fa

- L'orologio è quello di sistema e scorre da solo, senza consumare batteria.
- Il meteo viene da [Open-Meteo](https://open-meteo.com/) e si aggiorna ogni 30 minuti circa. Senza rete resta l'ultimo dato, con l'ora in cui è stato preso.
- Il cielo segue la giornata (mattina, giorno, sera, notte) e il tempo (sole, nuvole, pioggia, neve, temporale, nebbia).
- Toccando il widget si apre la versione animata.

L'app non raccoglie dati e non chiede permessi oltre a Internet.

## Per chi lo mantiene

L'APK lo compila GitHub Actions (`.github/workflows/apk.yml`) a ogni push. Una nuova versione pubblica si fa con un tag:

```
git tag v1.1 && git push origin v1.1
```

Dopo il rilascio, copia l'APK anche sul sito della scuola (è il link che usano i colleghi) e aggiorna il numero di versione nella pagina:

```
gh release download vX.Y -p meteo-meucci.apk -O G:/Scuola/meteo-meucci/meteo-meucci.apk --clobber
```

La firma usa la chiave nei secret del repository (`KEYSTORE_B64`, `KEYSTORE_PASSWORD`): deve restare sempre la stessa, altrimenti gli aggiornamenti non si installano sopra la versione vecchia.

Realizzato dal Prof. Rossano Bella
