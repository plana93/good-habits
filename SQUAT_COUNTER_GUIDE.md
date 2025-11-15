# 📊 Guida Sistema Salvataggio Squat Counter

## 🎯 Funzionalità

Il sistema di conteggio squat ora include **persistenza automatica** dei dati. Ogni squat viene salvato permanentemente e il totale viene ricaricato all'avvio dell'app.

## 📁 File di Salvataggio

- **Nome file**: `squat_total_count.txt`
- **Posizione**: Directory interna dell'app (`/data/data/com.programminghut.pose_detection/files/`)
- **Contenuto**: Numero intero del totale squat effettuati
- **Formato**: Testo semplice (es. `142`)

## 🔄 Funzionamento Automatico

### Al Avvio dell'App
1. Il sistema carica automaticamente il totale degli squat dal file
2. Mostra il totale caricato con un Toast
3. Il display mostra:
   - **Sessione**: squat della sessione corrente
   - **Totale**: tutti gli squat mai effettuati

### Durante l'Uso
1. Ogni squat rilevato incrementa **sia** il contatore sessione **che** il totale
2. Il file viene **salvato immediatamente** dopo ogni squat (per sicurezza)
3. Il display viene aggiornato in tempo reale:
   ```
   Sessione: 5
   Totale: 147
   ```

### Alla Chiusura dell'App
Il salvataggio avviene **automaticamente** in questi casi:
- `onPause()` - quando l'app va in background
- `onStop()` - quando l'app viene fermata
- `onDestroy()` - quando l'app viene chiusa completamente

## 📊 Display Informazioni

### Modalità Squat Counter
```
Sessione: 12      ← squat fatti in questa sessione
Totale: 458       ← squat totali di sempre
```

### Modalità Recording
Il display mostra solo il contatore locale (nessuna persistenza in modalità recording)

## 🛠️ Classe SquatCounter

### Metodi Principali

```kotlin
// Carica il totale dal file
fun loadTotalSquats(): Int

// Salva il totale nel file
fun saveTotalSquats(): Boolean

// Incrementa e salva automaticamente
fun incrementSquat()

// Ottiene il totale
fun getTotalSquats(): Int

// Ottiene squat della sessione corrente
fun getCurrentSessionSquats(): Int

// Resetta solo la sessione corrente
fun resetSessionSquats()

// Resetta tutto (ATTENZIONE!)
fun resetAllSquats()

// Chiamato alla chiusura app
fun onAppClosing()
```

## 🔐 Sicurezza Dati

### Salvataggio Multiplo
Il sistema salva i dati in **3 momenti diversi**:
1. **Immediato** - dopo ogni squat
2. **Background** - quando si cambia app (onPause)
3. **Chiusura** - quando l'app viene chiusa (onDestroy)

Questo garantisce che i dati non vengano mai persi, anche in caso di crash improvviso.

### Gestione Errori
- Se il file non esiste al primo avvio → totale = 0
- Se il file è corrotto → totale = 0 (log errore)
- Se il salvataggio fallisce → log errore (mantiene dati in memoria)

## 📝 Log di Sistema

Il sistema genera log dettagliati per il debugging:

```
D/SquatCounter: Caricati 145 squat totali dal file
D/SquatCounter: Squat incrementato: sessione=3, totale=148
D/SquatCounter: Salvati 148 squat totali nel file: /data/data/.../squat_total_count.txt
D/MainActivity: Squat totali salvati: 148
```

## 🎮 Utilizzo

### Come Utente
1. Apri l'app in modalità "SQUAT COUNTER"
2. Seleziona la camera
3. Esegui gli squat normalmente
4. Il totale viene salvato automaticamente
5. Chiudi l'app quando vuoi
6. Al prossimo avvio, il totale sarà ancora lì!

### Reset del Contatore (Solo per sviluppatori)
Se serve resettare il totale (es. per testing):

```kotlin
// In MainActivity o tramite debug
squatCounter.resetAllSquats()
```

Oppure cancella manualmente il file:
```bash
adb shell rm /data/data/com.programminghut.pose_detection/files/squat_total_count.txt
```

## 🎯 Differenze tra Modalità

| Caratteristica | Squat Counter | Recording Mode | Urban Camera |
|---------------|---------------|----------------|--------------|
| Persistenza squat | ✅ Sì | ❌ No | ❌ No |
| Display totale | ✅ Sì | ❌ No | ❌ No |
| Salvataggio automatico | ✅ Sì | ❌ No | ❌ No |

## 🔍 Dove Trovare il File

### Da ADB
```bash
adb shell
cd /data/data/com.programminghut.pose_detection/files/
cat squat_total_count.txt
```

### Da Android Studio
Device File Explorer → data → data → com.programminghut.pose_detection → files → squat_total_count.txt

## 💡 Note Tecniche

- Il file è **privato** all'app (Android sandbox)
- Il salvataggio è **sincronizzato** (blocking I/O su thread UI)
- Ogni incremento costa ~1-2ms per il salvataggio (impatto minimo)
- Il file è **plain text** per facilità di debug
- La dimensione del file è **minima** (pochi byte)

## 🚀 Future Miglioramenti Possibili

- Aggiungere data/ora dell'ultimo squat
- Statistiche giornaliere/settimanali/mensili
- Export dati in JSON o CSV
- Backup su cloud
- Grafici di progresso
- Obiettivi e achievements
