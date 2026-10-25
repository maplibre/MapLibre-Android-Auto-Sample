# Step 1 — mappa GameMaps su telefono e Android Auto

## Cosa cambia

- Tema originale ispirato alla minimappa GTA: sfondo scuro, strade chiare con bordi,
  acqua blu, aree verdi, edifici e nomi delle strade. Stesso asset su telefono e auto.
- OpenFreeMap sostituisce le tile dimostrative: la mappa contiene una rete stradale
  utilizzabile come base del prototipo. Tile e font richiedono internet.
- Posizione GPS, freccia e camera che segue la direzione di marcia; pulsante Recenter.
- Permessi posizione precisa/approssimativa e servizio in primo piano per la sessione auto.
- Ciclo di vita della mappa corretto, pulizia di Presentation e VirtualDisplay anche
  alla sostituzione della superficie, rimosso il removeView sul WindowManager sbagliato.
- `compileSdk` 35 anche in `car_common`, rimosso il modulo `common` inesistente e
  la dipendenza JitPack di navigazione non usata. ID dei pacchetti mantenuti.

Questa versione mostra e segue la posizione: non calcola ancora percorsi e non dà
indicazioni vocali. Android Automotive è un target Android distinto da Android Auto;
CarPlay richiederà un progetto iOS.

## Avvio in Android Studio

1. Apri la radice del progetto e imposta il Gradle JDK a 17. Installa SDK Platform 35
   e i Build Tools richiesti da Gradle nello SDK Manager.
2. Esegui Sync Project with Gradle Files.
3. Scegli la configurazione `app`, collega il telefono con debug USB attivo e avvia.
4. Consenti la posizione, preferibilmente precisa. Concedere solo quella approssimativa
   non deve causare crash o richieste ripetute obbligatorie.
5. Attendi un fix GPS all'aperto. Senza fix non deve comparire una posizione inventata;
   la camera iniziale è solo una vista predefinita dell'Italia.
6. Trascina la mappa e premi il pulsante posizione per riprendere il follow.

Il permesso può essere riabilitato dalle impostazioni Android se viene negato
permanentemente. La preview sul telefono resta consultabile senza permesso.

## Android Auto con DHU su Windows

Segui anche la [guida ufficiale del DHU](https://developer.android.com/training/cars/testing/dhu).

1. In SDK Manager → SDK Tools installa Android Auto Desktop Head Unit Emulator.
2. Sul telefono abilita la modalità sviluppatore di Android Auto e avvia **Start head
   unit server**. Per un APK di sviluppo, verifica la relativa opzione per app da
   origini sconosciute nelle impostazioni sviluppatore di Android Auto se non compare.
3. Collega il telefono via USB e autorizza il PC. Dal terminale con `adb` nel PATH:

   ```text
   adb devices
   adb forward tcp:5277 tcp:5277
   ```

4. Avvia `desktop-head-unit.exe` dalla cartella `extras/google/auto` del tuo Android SDK.
5. Apri **GameMaps** nel launcher del DHU. Se manca il permesso, usa **Open on phone**;
   dopo averlo concesso, ritorna all'auto e premi di nuovo il pulsante se necessario.
6. Controlla la mappa, zoom, pan e **Recenter**. Quando la localizzazione auto è attiva,
   Android gestisce una notifica persistente; la sua visibilità dipende anche dalle
   impostazioni notifiche del telefono.

Se l'avvio della localizzazione in primo piano viene negato dal sistema, apri
GameMaps sul telefono e premi Recenter sul display auto mentre l'app è visibile.
Non serve concedere il permesso posizione "sempre": questo step usa un foreground
service di tipo location. Verifica questo percorso sulla tua versione Android.

## GPS su emulatore Automotive

La posizione deve provenire dal sistema: GameMaps non inventa coordinate.
Il modulo auto ora ascolta i provider Android `gps`, `network`, `fused` e
`passive`, inviando gli aggiornamenti ricevuti al puck MapLibre. Recenter
mostra un messaggio se non è ancora disponibile un fix.

- Accendi Location nelle impostazioni dell'emulatore, verifica i permessi dell'app
  (utente Automotive) e imposta un punto in Extended Controls → Location.
- Oppure, con **un solo emulatore connesso**: `adb emu geo fix 11.3426 44.4949`
  (longitudine prima della latitudine). Non impostare coordinate fittizie nel codice.
- Premi Recenter; nel Logcat filtra `CarMapContainer` e verifica
  `GPS update from ...`. Se non appare, esegui `adb shell dumpsys location`
  per controllare provider, utente e fix di sistema.
- Il provider può fornire una posizione senza bearing quando è fermo: in quel
  caso la camera segue la posizione senza ruotare, passando al course-follow
  quando arrivano aggiornamenti con direzione.
- I listener nativi si fermano con la sessione della mappa per evitare perdite.

Questa modifica non è ancora stata validata su Android Studio o emulatore.

## Verifiche da eseguire

| Scenario | Risultato atteso |
| --- | --- |
| Primo avvio con internet | Strade, acqua, edifici a zoom elevato e nomi leggibili; nessuna chiave richiesta |
| Permesso negato | Nessun crash; telefono consultabile; auto mostra richiesta permesso |
| Solo posizione approssimativa | App funzionante, con precisione ridotta |
| Permesso concesso dopo il caricamento | GPS attivato senza reinstallare l'app |
| Pan e Recenter | Esplorazione manuale, poi ripresa del follow della posizione |
| Posizione simulata in movimento / prova controllata | Freccia e camera aggiornate; valutare fluidità e direzione |
| Telefono bloccato con GameMaps visibile nell'auto | Aggiornamenti GPS mantenuti durante la sessione |
| Chiusura o disconnessione del DHU | Nessun crash, rendering e sessione di localizzazione rilasciati |
| Riconnessione / cambio dimensione superficie | Mappa ricreata, senza viste o display duplicati |
| Connessione assente, cache vuota | Tile/font possono mancare: lo stile locale non rende l'app offline |

Esegui i test di movimento con localizzazione simulata o con un passeggero che osserva
l'app. Nel DHU il GPS proviene dal telefono/host: avviare il DHU non simula da solo
un percorso. Non esiste ancora un sistema di routing in questo repository.

## Stato delle verifiche durante la modifica

- Eseguiti: parsing del JSON dello stile, controlli strutturali dei layer, parsing XML
  e TOML, controllo del diff.
- Build tentata: `:app:assembleDebug :automotive:assembleDebug :car_common:testDebugUnitTest`.
  Il wrapper si ferma con `java.net.SocketException: Network is unreachable` scaricando
  Gradle 8.9. L'ambiente non contiene inoltre l'SDK Android.
- Non eseguiti: compilazione Kotlin/Android, lint Android, test su emulatori/dispositivi,
  verifica tile/font dal renderer nativo, DHU, qualità visiva e fluidità GPS.

## Riferimenti tecnici

- [MapLibre LocationComponent](https://maplibre.org/maplibre-native/android/examples/location-component/)
- [OpenFreeMap: configurazione](https://openfreemap.org/quick_start/)
- [CarAppService: localizzazione e foreground service](https://developer.android.com/reference/androidx/car/app/CarAppService)
- [Android: permessi di localizzazione](https://developer.android.com/develop/sensors-and-location/location/permissions)
