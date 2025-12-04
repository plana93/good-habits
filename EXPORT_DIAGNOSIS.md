# Export e salvataggio video — Diagnosi dettagliata

Data: 30 novembre 2025

Questo file riassume cosa stai cercando di ottenere, cosa è già stato implementato nell'app, i sintomi osservati (file MP4 con durata 0.0, nessuna anteprima), e una diagnosi dettagliata delle possibili cause con passi di raccolta dati e azioni raccomandate.

## Obiettivo

- Permettere all'utente di caricare foto/video o usare la camera live, applicare la stessa pipeline di filtri adattivi (sovrapposti alla preview) e quindi esportare il risultato come un singolo file MP4 a 30 fps.
- Quando si salva un singolo frame (PHOTO), il file salvato deve sempre includere i filtri attivi.
- Per i video: due strategie principali — esportazione diretta (encoder -> muxer o MediaRecorder) oppure salvare la sequenza di frame e fare lo "stitching" in post-processing. Mostrare una barra di progresso durante l'operazione perché può essere lenta.

## Cosa è già stato implementato nel codice

- Modalità di playback per media caricati (image/video) che riutilizza la pipeline di rilevamento + filtri (`UrbanCameraActivityRefactored.kt`).
- Pulsante `PHOTO` in playback che ora salva il `Bitmap` con i filtri applicati (salvataggio in `Pictures/UrbanCamera` o tramite MediaStore su Android Q+).
- Tre strade di esportazione MP4 implementate:
  - CPU path: conversione ARGB -> NV12 e feed a `MediaCodec` + `MediaMuxer` (funzione `exportFramesToMp4`).
  - EGL/input-surface: creare encoder con input-surface e renderizzare bitmap via EGL + GLES (funzione `exportFramesToMp4UsingInputSurface`).
  - MediaRecorder + EGL: creare `MediaRecorder` con surface, renderizzare frames e lasciar che `MediaRecorder` scriva il file (funzione `exportFramesWithMediaRecorder`).
- Fallbacks: se CPU path fallisce, prova EGL path, poi MediaRecorder.
- Diagnostica aggiunta: `debugDumpFrames()` scrive `info.txt` e `frame_0.jpg` nella cartella app files per ispezione. Logging esteso per `Muxer started`, `Wrote sample`, contatori sample, PTS min/max e file size/duration (MediaRecorder).
- Nuova opzione "Save frames -> Stitch (slow)": salva i singoli JPEG in app files quindi richiama l'EGL encoder per effettuare lo stitching. Mostra overlay con `ProgressBar` durante salvataggio e stitching.

## Sintomi osservati

- Il file MP4 viene creato ma risulta avere durata 0.0s e nessuna anteprima riproducibile.
- In alcuni casi il file ha dimensione molto piccola (o 0 bytes).
- Pulsante PHOTO a volte era invisibile in playback (già corretto, ora visibile e salva con filtri).

## Dove guardare nel codice (file / funzioni)

- `app/src/main/java/com/programminghut/pose_detection/UrbanCameraActivityRefactored.kt`
  - `exportFramesToMp4(frames, fps)` — CPU NV12 -> MediaCodec -> MediaMuxer
  - `exportFramesToMp4UsingInputSurface(frames, fps)` — EGL -> encoder input surface
  - `exportFramesWithMediaRecorder(frames, fps)` — MediaRecorder with EGL rendering
  - `exportFramesSequenceAndStitch(frames, fps)` — salva jpeg e poi richiama `exportFramesToMp4UsingInputSurface`
  - `debugDumpFrames(frames)` — scrive `info.txt` e `frame_0.jpg`
  - UI: `activity_urban_camera_refactored.xml` (spinner `exporterSpinner`, overlay `exportProgressOverlay`)

## Principali difficoltà tecniche (dettagliate)

1) Il muxer non parte ("Muxer was never started")
   - Sintomo: non si vede il log `INFO_OUTPUT_FORMAT_CHANGED` dall'encoder; `MediaMuxer.start()` non viene mai chiamato.
   - Possibili cause:
     - L'encoder non ha prodotto il `INFO_OUTPUT_FORMAT_CHANGED` (problema interno codec o di configurazione).
     - Configurazione `MediaFormat` non compatibile con il codec HW (es. `KEY_COLOR_FORMAT` errato).
   - Controlli/diagnostica:
     - Log cercare `INFO_OUTPUT_FORMAT_CHANGED`, `Encoder output format changed`, `Muxer started`.
     - Verificare che `codec.outputFormat` sia valido prima di `muxer.addTrack`.
   - Mitigazioni:
     - Usare l'input-surface (COLOR_FormatSurface) anziché feedare YUV da CPU.
     - Provare MediaRecorder / CameraX Recorder se encoder HW ha limiti.

2) L'encoder non produce output (0 samplesWritten)
   - Sintomo: viene chiamata la sequenza di feed, ma non ci sono buffer emessi da `dequeueOutputBuffer` con `size>0`.
   - Possibili cause:
     - Formato colore inviato non corretto o stride/align richiesto dal codec.
     - Input YUV creato in modo errato (RGB->NV12 conversion bug o politica di subsampling sbagliata).
     - Immissione di buffer con `presentationTimeUs` errati (PTS in ordine non crescente) che possono confondere il encoder.
   - Controlli/diagnostica:
     - Salvare il primo frame con `debugDumpFrames` e aprirlo per vedere se l'immagine è valida.
     - Log dei `bufferInfo` (size, flags, presentationTimeUs) e contatore samples.
   - Mitigazioni:
     - Preferire input-surface + GL texture upload (già implementato come fallback): evita conversione CPU YUV.
     - Testare su più dispositivi e codec software (se disponibile) per isolamento.

3) PTS / timestamp sbagliati (tutti 0 o identici)
   - Sintomo: file scritto, ma strumenti come MediaStore / Gallery visualizzano durata 0.0s.
   - Possibili cause:
     - PTS calcolati con unità sbagliata (usare microsecondi per `presentationTimeUs`, nanosecondi per `eglPresentationTimeANDROID`).
     - Overflow o uso di valori non crescenti (PTS deve essere monotonicamente crescente).
   - Controlli/diagnostica:
     - Log firstPtsUs / lastPtsUs che sono stati aggiunti; vedere se sono 0 o uguali.
     - `ffprobe` sul file generato per capire se ci sono sample e quali PTS hanno.
   - Mitigazioni:
     - Calcolare PTS come `i * 1_000_000 / fps` (microsecondi) per MediaCodec/MediaMuxer e `i * 1_000_000_000 / fps` (nanosecondi) per `eglPresentationTimeANDROID`.

4) MediaRecorder scrive file ma `stop()` non finalizza correttamente (file corrotto / duration null)
   - Sintomo: temp file creato ma `MediaMetadataRetriever` non riesce a leggere la durata o file troppo piccolo.
   - Possibili cause:
     - `stop()` lancia eccezione interna (spesso causata da frame mancanti o problemi HW) e il file non è ben finalizzato.
     - Permessi/MediaStore: copy/insert nel MediaStore fatto prima che il file sia completamente flushato.
   - Controlli/diagnostica:
     - Log exceptions attorno a `mediaRecorder.stop()`; sono già catturate e loggate.
     - Controllare file size sul device dopo stop (adb shell ls -l …).
   - Mitigazioni:
     - Provare CameraX Recorder (più alto livello e spesso più stabile su device moderni).
     - Aggiungere retry/attesa breve dopo `stop()` prima di copiare il file su MediaStore.

5) Device-specific codec quirks e formati di colore
   - Sintomo: funziona su alcuni dispositivi ma non su altri.
   - Causa tipica: encoder hardware supporta solo alcuni color-format/stride/height-alignment; CPU YUV feeding può fallire su encoder che si aspettano stride allineati o piano separati.
   - Mitigazioni:
     - Usare input-surface (COLOR_FormatSurface) + texture upload.
     - Forzare MediaRecorder o CameraX Recorder su dispositivi problematici.

6) Problemi di concorrenza / drain dell'encoder
   - Sintomo: encode -> queueInput -> ma `dequeueOutputBuffer` ritorna `INFO_TRY_AGAIN_LATER` sempre.
   - Controlli:
     - Incrementare tempi di timeout su `dequeueOutputBuffer` (testare 10_000 ms) e loggare ripetizioni.
     - Assicurarsi che il thread di encoding possa girare e non sia bloccato.

## Dati diagnostici da raccogliere (priorità alta)

1. Logcat filtrato (tag `UrbanCamera`, e facoltativamente `MediaCodec|MediaMuxer|MediaRecorder`) durante un export di prova, salvato in un file.
   - Comando consigliato:
     ```bash
     adb logcat -c
     adb logcat -v time | egrep "UrbanCamera|MediaCodec|MediaMuxer|MediaRecorder" > ~/urbancamera_export_log.txt
     # Esegui l'export dall'app, poi interrompi con Ctrl+C
     ```

2. Il file MP4 prodotto (o almeno la sua dimensione). Copialo con `adb pull` e controlla con `ffprobe`:
   ```bash
   adb pull /sdcard/Android/data/<tuo.package>/files/UrbanCamera/export_*.mp4 ~/export_rec.mp4
   ls -lh ~/export_rec.mp4
   ffprobe -v error -show_entries format=duration -of default=noprint_wrappers=1:nokey=1 ~/export_rec.mp4
   ```

3. Cartella `debug_frames_*` creata da `debugDumpFrames` (almeno `frame_0.jpg` e `info.txt`) — utile per verificare se i frame che stiamo codificando sono validi.
   ```bash
   adb shell ls -l /sdcard/Android/data/<tuo.package>/files | grep debug_frames
   adb pull /sdcard/Android/data/<tuo.package>/files/debug_frames_YYYYMMDD_HHMMSS ./debug_frames_local
   ```

4. Estratti dei log con le righe che contengono: `Muxer started`, `Wrote sample`, `Skipping write`, `Muxer was never started`, `MediaRecorder output temp file size`, e eventuali stacktrace.

## Passi di riproduzione (breve)

1. Avvia l'app su device collegato via USB. Assicurati che i filtri siano attivi (se desideri salvarli sul frame).
2. Vai in playback mode e scegli l'opzione di export (consiglio: MediaRecorder o "Save frames -> Stitch (slow)").
3. Premi EXPORT/VIDEO; registra logcat come indicato.
4. Al termine copia il file MP4 e i log locali e inviali per analisi.

## Priorità dei fix raccomandati

1. Raccogliere log + `frame_0.jpg` e `ffprobe` (immediato): senza questi non si può diagnosticare esattamente il problema.
2. Se `Muxer never started` -> forzare e testare EGL input-surface come percorso principale e non fallback. Già presente nel codice: provare direttamente questa opzione.
3. Se encoder non produce output -> sospetto conversione NV12 errata; preferire input-surface (evita YUV CPU conversion).
4. Se MediaRecorder `stop()` fallisce regolarmente -> implementare CameraX Recorder (più robusto e moderno) come fallback e usare MediaRecorder solo come ultima risorsa.
5. Aggiungere retry/attesa prima di copiare il file su MediaStore e controllare il risultato di `MediaMetadataRetriever` prima di procedere.

## Possibili azioni che posso fare ora (se vuoi che proceda)

- Implementare il fallback CameraX Recorder e agganciarlo allo spinner (richiede Gradle sync, abbiamo già le dipendenze aggiunte).
- Migliorare la gestione dei PTS (forzare PTS monotoni e loggare ogni PTS inviato).
- Fornire una patch che renda l'EGL path il percorso predefinito e rimuova la conversione NV12 CPU path dove non necessaria.
- Aggiungere un check post-copy che usa `MediaMetadataRetriever` per confermare che il file esportato ha durata > 0 e riprovare se necessario.

---

Se vuoi, procedo subito con l'implementazione del fallback CameraX Recorder (opzione consigliata per stabilità), oppure aspetti prima i log e i file sintetici che hai prodotto: con i log potrò fare un fix mirato (probabilmente molto più veloce). Dimmi come preferisci procedere.

*** Fine del documento
