# 🚀 Quick Start Guide - Phase 5 & 6 Features

## 📊 Phase 5: Visualizzazioni Avanzate

### Come Accedere ai Nuovi Grafici

1. **Apri una sessione completata**
   - Vai a Dashboard → Sessioni
   - Tap su una sessione esistente

2. **Passa alla tab "Grafici"**
   - Nel SessionDetailScreen, troverai due tab in alto:
     - **Lista**: Vista tradizionale con lista ripetizioni
     - **Grafici**: Nuove visualizzazioni avanzate

3. **Esplora i Grafici**
   
   #### 🎯 Scatter Chart
   - Mostra tutti i punti delle ripetizioni
   - Colori indicano qualità (verde=ottimo, rosso=scarso)
   - Selettore metriche: Form / Depth / Media
   - **Tap su un punto** per vedere dettagli completi
   
   #### 🌡️ Heatline
   - Linea colorata progressiva
   - Identifica fasi di fatica durante sessione
   - Mostra miglior/peggior segmento

4. **Dialog Dettagli Ripetizione**
   - Appare quando tappi un punto nel grafico
   - Mostra:
     - ⏰ Timestamp esatto
     - 📊 Metriche con progress bar
     - ⚠️ Warning sulla postura
     - 🆚 Confronto con miglior rep
     - 🔄 Confronto con rep precedente

### Interpretare i Colori della Qualità

| Colore | Qualità | Score Range | Significato |
|--------|---------|-------------|-------------|
| 🟢 Verde Scuro | EXCELLENT | 90-100% | Rep perfetta! |
| 🟢 Verde Chiaro | GOOD | 75-89% | Ottima esecuzione |
| 🟡 Giallo | FAIR | 60-74% | Buona ma migliorabile |
| 🟠 Arancione | POOR | 40-59% | Attenzione alla forma |
| 🔴 Rosso | CRITICAL | 0-39% | Rep da ripetere |

### Tips per l'Analisi

✅ **Identifica pattern di fatica**
- Se l'heatline va da verde → rosso, stai perdendo qualità per fatica
- Considera pause più lunghe o volume ridotto

✅ **Trova la tua "zona verde"**
- Conta quante rep hai nella zona EXCELLENT/GOOD
- Obiettivo: mantenere 80%+ nella zona verde

✅ **Analizza le rep critiche**
- Tap sulle rep rosse per capire cosa è andato storto
- Leggi i warning per correggere la tecnica

---

## 🏋️ Phase 6: Multi-Exercise System

### Esercizi Disponibili (Preset)

#### 1. 🦵 Squat
- **Difficoltà**: Principiante
- **Muscoli**: Gambe, Glutei, Core
- **Regole**: 4 (profondità, simmetria, piedi, visibilità)
- **Tags**: legs, beginner, bodyweight

#### 2. 💪 Push-up
- **Difficoltà**: Intermedio
- **Muscoli**: Petto, Braccia, Core
- **Regole**: 3 (angolo gomiti, corpo dritto, simmetria)
- **Tags**: chest, arms, intermediate

#### 3. 🎯 Pull-up
- **Difficoltà**: Avanzato
- **Muscoli**: Schiena, Braccia, Core
- **Regole**: 3 (mento sopra sbarra, braccia estese, simmetria)
- **Tags**: back, arms, advanced

#### 4. 🚶 Lunge (Affondi)
- **Difficoltà**: Intermedio
- **Muscoli**: Gambe, Glutei, Core
- **Regole**: 2 (ginocchio 90°, busto eretto)
- **Tags**: legs, intermediate, unilateral

#### 5. 🧘 Plank
- **Difficoltà**: Principiante
- **Muscoli**: Core, Spalle
- **Regole**: 2 (corpo dritto, gomiti sotto spalle)
- **Tags**: core, beginner, isometric

### Come Funzionano le Regole

Ogni esercizio ha regole che validano l'esecuzione. Esempio Squat:

```
Regola 1: "Scendi abbastanza in basso"
- Tipo: DISTANCE_MIN
- Misura: Distanza Spalla → Ginocchio
- Target: ≥ 0.3 (30% altezza corpo)
- Peso: 1.5x (molto importante)

Regola 2: "Mantieni il corpo simmetrico"
- Tipo: SYMMETRY_LEFT_RIGHT
- Misura: Differenza sinistra/destra
- Tolleranza: ±10%
- Peso: 1.0x

Regola 3: "Mantieni i piedi allineati"
- Tipo: POSITION_ABOVE
- Misura: Allineamento caviglie
- Tolleranza: ±5%
- Peso: 0.8x

Regola 4: "Corpo completamente visibile"
- Tipo: VISIBILITY_REQUIRED
- Keypoints: Spalle, Anche, Ginocchia, Caviglie
- Confidence: > 45%
- Peso: 2.0x (critico)
```

### Score Calculation

Il **Form Score** finale è calcolato come:

```
Form Score = Σ(RuleScore × RuleWeight) / Σ(RuleWeight)
```

Ogni regola contribuisce proporzionalmente al suo peso.

### Tipi di Regole Disponibili (15)

#### Distanza
- `DISTANCE_MIN`: Distanza minima tra keypoints
- `DISTANCE_MAX`: Distanza massima
- `DISTANCE_EQUALS`: Distanza deve essere uguale

#### Angolo
- `ANGLE_MIN`: Angolo minimo tra 3 keypoints
- `ANGLE_MAX`: Angolo massimo
- `ANGLE_EQUALS`: Angolo deve essere uguale

#### Simmetria
- `SYMMETRY_LEFT_RIGHT`: Simmetria sinistra-destra
- `SYMMETRY_UP_DOWN`: Simmetria alto-basso

#### Posizione
- `POSITION_ABOVE`: Keypoint A sopra B
- `POSITION_BELOW`: Keypoint A sotto B
- `POSITION_LEFT`: Keypoint A a sinistra di B
- `POSITION_RIGHT`: Keypoint A a destra di B

#### Visibilità
- `VISIBILITY_REQUIRED`: Keypoint deve essere visibile

#### Tempo
- `TIME_MIN`: Tempo minimo movimento
- `TIME_MAX`: Tempo massimo movimento

---

## 🎨 Creazione Esercizi Custom (Roadmap)

### Workflow Previsto (da implementare):

1. **Exercise Browser**
   - Visualizza tutti gli esercizi disponibili
   - Filtra per tipo/difficoltà/gruppo muscolare
   - Tap "+" per creare nuovo

2. **Exercise Editor**
   - Inserisci nome e descrizione
   - Seleziona keypoints importanti
   - Definisci regole visualmente
   - Test in tempo reale

3. **Photo Rule Generator**
   - Scatta foto posizione START
   - Scatta foto posizione END
   - AI calcola automaticamente:
     - Distanze rilevanti
     - Angoli critici
     - Simmetrie necessarie
   - Suggerisce regole ottimali

4. **Export per LLM**
   - Copia definizione esercizio
   - Formato strutturato leggibile
   - Usa ChatGPT/Claude per:
     - Ottimizzare regole
     - Suggerire varianti
     - Generare programmi allenamento

---

## 🔧 Utilizzo Database

### Query Esercizi

```kotlin
// In un ViewModel o Repository
val exerciseDao = database.exerciseDao()

// Tutti gli esercizi
val allExercises = exerciseDao.getAllExercises().collectAsState()

// Solo squat
val squats = exerciseDao.getExercisesByType(ExerciseType.SQUAT)
    .collectAsState()

// Solo custom
val customExercises = exerciseDao.getCustomExercises()
    .collectAsState()

// Search
val searchResults = exerciseDao.searchExercisesByName("push")
    .collectAsState()
```

### Inizializzazione Preset

```kotlin
// In Application onCreate o primo avvio Activity
val presetManager = ExercisePresetManager(
    exerciseDao = database.exerciseDao(),
    context = applicationContext
)

// Popola database con preset se vuoto
lifecycleScope.launch {
    presetManager.initializePresetsIfNeeded()
}
```

### Creazione Custom Exercise

```kotlin
// Esempio: crea un "Squat Bulgaro"
lifecycleScope.launch {
    val customExerciseId = presetManager.createCustomExercise(
        name = "Bulgarian Split Squat",
        description = "Squat su una gamba con piede posteriore elevato",
        startPositionKeypoints = calibratedStartPose,
        endPositionKeypoints = calibratedEndPose,
        rules = listOf(
            ExerciseRule(
                ruleType = RuleType.ANGLE_MIN,
                keypoints = listOf(11, 13, 15), // Anca-Ginocchio-Caviglia
                targetValue = 90f,
                tolerance = 15f,
                weight = 1.5f,
                description = "Scendi fino a 90 gradi"
            ),
            // ... altre regole
        ),
        tags = listOf("legs", "advanced", "unilateral")
    )
}
```

### Export per Analisi AI

```kotlin
val exercise = exerciseDao.getExerciseById(exerciseId)
val exportText = presetManager.exportExerciseForLLM(exercise)

// Copia in clipboard
val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
val clip = ClipData.newPlainText("Exercise Definition", exportText)
clipboard.setPrimaryClip(clip)

// Incolla in ChatGPT per analisi/ottimizzazione
```

---

## 📱 Esempio Flusso Utente Completo

### Sessione con Grafici Avanzati

1. **Avvia Sessione Squat**
   - Calibrazione posizione
   - Esegui 50 ripetizioni
   - App traccia automaticamente

2. **Salvataggio Automatico**
   - Al termine, sessione salvata con tutte le rep

3. **Analisi Dettagliata**
   - Apri Dashboard → tap sulla sessione
   - Vai a tab "Grafici"
   
4. **Scatter Chart**
   - Vedi distribuzione qualità
   - 🟢 40 rep verdi (ottimo!)
   - 🟡 8 rep gialle (buone)
   - 🔴 2 rep rosse (da migliorare)

5. **Tap su Rep Rossa**
   - Dialog mostra: "Rep #37"
   - Form Score: 45% ⚠️
   - Warning: "Asimmetria destra-sinistra"
   - Confronto best: -42% form
   
6. **Heatline Rivela Pattern**
   - Prime 20 rep: 🟢 Verdi
   - Rep 21-35: 🟡 Gialle (fatica)
   - Rep 36-40: 🔴 Rosse (stanchezza)
   - Ultime 10 rep: 🟢 Verdi (recupero)

7. **Decisione**
   - Pattern chiaro: pausa necessaria a rep 35
   - Prossima sessione: pause ogni 30 rep

---

## 💡 Best Practices

### Per Ottenere Dati Ottimali

✅ **Illuminazione**
- Luce uniforme, evita contro-luce
- Migliora detection qualità

✅ **Inquadratura**
- Corpo completo visibile
- Distanza ottimale: 2-3 metri

✅ **Angolatura**
- Laterale o frontale
- Evita angoli troppo obliqui

✅ **Sfondo**
- Sfondo pulito e contrastato
- Facilita pose detection

### Per Analisi Efficace

📊 **Usa Scatter Chart per**
- Identificare outlier (rep anomale)
- Vedere distribuzione generale
- Trovare pattern

🌡️ **Usa Heatline per**
- Capire quando affatichi
- Ottimizzare volume allenamento
- Pianificare pause

🔍 **Usa Dialog Dettagli per**
- Debugging tecnica
- Capire errori specifici
- Confrontare rep buone vs cattive

---

## 🎯 Obiettivi Prossimi Sviluppi

### Priorità Alta
- [ ] Exercise Validator Core (logica validazione)
- [ ] MainActivity Refactoring (multi-exercise support)
- [ ] Exercise Selector UI (scelta esercizio pre-sessione)

### Priorità Media
- [ ] Exercise Browser (catalogo esercizi)
- [ ] Exercise Editor (creazione custom UI)

### Priorità Bassa
- [ ] Photo Rule Generator (AI-assisted creation)
- [ ] Exercise Program Builder (sequenze esercizi)
- [ ] Social Sharing (condividi preset)

---

## 📚 Risorse Aggiuntive

### Documentazione
- `PHASE5_6_IMPLEMENTATION.md`: Dettagli tecnici completi
- `ChartModels.kt`: Commenti inline sui data model
- `Exercise.kt`: Documentazione completa sistema regole

### Keypoint Reference

```
MoveNet Keypoint Indices (17 punti):
0: Naso
1: Occhio sinistro
2: Occhio destro
3: Orecchio sinistro
4: Orecchio destro
5: Spalla sinistra
6: Spalla destra
7: Gomito sinistro
8: Gomito destro
9: Polso sinistro
10: Polso destro
11: Anca sinistra
12: Anca destra
13: Ginocchio sinistro
14: Ginocchio destro
15: Caviglia sinistra
16: Caviglia destra
```

### Formule Utili

**Calcolo Angolo tra 3 Punti**
```kotlin
fun calculateAngle(p1: Point, p2: Point, p3: Point): Float {
    val v1 = Vector(p1 - p2)
    val v2 = Vector(p3 - p2)
    return acos((v1 dot v2) / (v1.length * v2.length))
}
```

**Calcolo Distanza Normalizzata**
```kotlin
fun normalizedDistance(p1: Point, p2: Point, bodyHeight: Float): Float {
    val distance = sqrt((p2.x - p1.x)² + (p2.y - p1.y)²)
    return distance / bodyHeight
}
```

---

**Happy Tracking! 💪🎯📊**
