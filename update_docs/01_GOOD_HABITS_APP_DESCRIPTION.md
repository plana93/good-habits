# 🏋️ Good Habits - App Description

**Version**: 2.0  
**Platform**: Android  
**Minimum SDK**: 24 (Android 7.0)  
**Target SDK**: 32 (Android 12L)

---

## 📱 App Overview

**Good Habits** è un'applicazione Android per il fitness che utilizza l'intelligenza artificiale per il rilevamento della posa corporea (pose detection) e il conteggio automatico degli squat.

L'app sfrutta il modello **MoveNet** di TensorFlow Lite per tracciare i movimenti del corpo in tempo reale attraverso la fotocamera, offrendo un personal trainer virtuale sempre disponibile.

---

## 🎯 Core Features

### 1. 🔢 Squat Counter (Activity: MainActivity)

**Funzionalità principale**: Conteggio automatico delle ripetizioni di squat

**Come funziona**:
1. L'utente si posiziona davanti alla camera (front o back)
2. Il sistema rileva automaticamente la postura iniziale (standing position)
3. Quando l'utente esegue uno squat, il sistema:
   - Calcola la distanza spalla-ginocchio
   - Verifica la posizione dei piedi
   - Rileva la fase di discesa (squat position)
   - Rileva il ritorno alla posizione iniziale
   - Incrementa il contatore

**Caratteristiche**:
- ✅ **Persistenza dei dati**: Il totale degli squat viene salvato e caricato automaticamente
- ✅ **Feedback visivo**: Bordi colorati indicano lo stato del rilevamento
  - 🔴 Rosso: Posizione non corretta
  - 🟡 Giallo: Rilevamento in corso
  - 🟢 Verde: Posizione corretta / Squat completato
- ✅ **Smoothing temporale**: Riduce il flickering con media mobile su 3 frame
- ✅ **Animazioni fluide**: Linee curve animate che collegano i keypoints
- ✅ **Display dual counter**: 
  - Contatore sessione corrente
  - Totale squat (persistente)

**Metriche tracciate**:
- Distanza spalla-ginocchio (sinistra e destra)
- Posizione piedi (parallelismo)
- Score di confidenza dei keypoints

**UI Elements**:
- Contatore grande e leggibile (1/25 dell'altezza dello schermo)
- Indicatore di progresso per l'attivazione iniziale
- Animazione zoom al completamento di ogni squat

---

### 2. 🎬 Track Skeleton Points (Recording Mode)

**Funzionalità**: Registrazione dei keypoints dello scheletro per analisi successive

**Come funziona**:
1. Modalità attivata tramite flag `RECORD_SKELETON` nell'intent
2. Ogni frame viene loggato con tutti i 17 keypoints MoveNet
3. I dati vengono salvati in un file CSV per analisi offline

**Use Cases**:
- Debug del sistema di rilevamento
- Creazione di dataset personalizzati
- Analisi approfondita dei movimenti
- Training di modelli custom

**Features**:
- ✅ CSV export con timestamp
- ✅ Copia file in clipboard per condivisione rapida
- ✅ Visualizzazione real-time dello scheletro
- ✅ Nessun conteggio squat (solo tracking)

---

## 🧠 AI/ML Technology Stack

### Pose Detection Model
- **Model**: MoveNet SinglePose Lightning (TFLite Float16)
- **Input**: 192x192 RGB image
- **Output**: 17 keypoints con coordinate normalizzate (x, y, confidence)
- **Inference Time**: ~30-50ms su dispositivi moderni

### Keypoints Tracked (17 total)
```
0:  Nose (naso)
1:  Left Eye (occhio sinistro)
2:  Right Eye (occhio destro)
3:  Left Ear (orecchio sinistro)
4:  Right Ear (orecchio destro)
5:  Left Shoulder (spalla sinistra)
6:  Right Shoulder (spalla destra)
7:  Left Elbow (gomito sinistro)
8:  Right Elbow (gomito destro)
9:  Left Wrist (polso sinistro)
10: Right Wrist (polso destro)
11: Left Hip (anca sinistra)
12: Right Hip (anca destra)
13: Left Knee (ginocchio sinistro)
14: Right Knee (ginocchio destro)
15: Left Ankle (caviglia sinistra)
16: Right Ankle (caviglia destra)
```

### Essential Keypoints for Squat (8 total)
Per ridurre i falsi negativi, il sistema richiede solo:
- Spalle: 5, 6
- Anche: 11, 12
- Ginocchia: 13, 14
- Caviglie: 15, 16

---

## 🎨 Visual Design

### Color Palette
- **Primary**: Verde Fluo (#39FF14) - Per keypoints e linee dello scheletro
- **Secondary**: 
  - Rosso - Posizione non rilevata
  - Giallo - Rilevamento in corso
  - Arancione - Progresso intermedio
  - Verde - Posizione corretta

### UI Components
- Contatore grande e minimalista
- Bordi animati per feedback
- Linee curve fluide per lo scheletro
- Pallini verdi fluo per i keypoints

### Animations
- **Curve di Bézier**: Per linee organiche e fluide
- **Ondulazione continua**: Basata sul tempo (sinusoidale)
- **Zoom effect**: Al completamento di ogni squat
- **Color interpolation**: Feedback progressivo

---

## 🔧 Technical Architecture

### Core Classes

#### MainActivity.kt
- Activity principale per lo squat counter
- Gestisce camera, pose detection e UI
- Implementa algoritmo di rilevamento squat

#### SquatCounter.kt
- Gestisce persistenza del contatore totale
- SharedPreferences per storage
- Metodi: increment, save, load

#### PoseLogger.kt
- Recording mode per skeleton tracking
- CSV export per analisi
- Timestamp per ogni frame

#### CameraAspectRatioHelper.kt
- Gestisce aspect ratio 16:9 della camera
- Evita allungamenti e distorsioni
- Supporta front e back camera

### Data Flow
```
Camera Frame
    ↓
TextureView.bitmap
    ↓
Preprocessing (192x192 resize)
    ↓
MoveNet Model Inference
    ↓
17 Keypoints (normalized)
    ↓
Scaling to screen coordinates
    ↓
Temporal Smoothing (3-frame buffer)
    ↓
Squat Detection Algorithm
    ↓
UI Update (counter, borders, skeleton)
```

---

## 📐 Squat Detection Algorithm

### Phase 1: Initial Position Detection
```kotlin
1. Detect essential keypoints (shoulders, hips, knees, ankles)
2. Require 8/15 consecutive stable frames
3. Progressive feedback (yellow → orange → green)
4. Save baseline metrics:
   - Shoulder-Knee distance (left & right)
   - Foot positions
```

### Phase 2: Squat Position Detection
```kotlin
while (monitoring) {
    if (shoulder_knee_distance <= baseline_squat_distance + threshold) {
        if (feet_parallel) {
            squat_detected = true
            counter++
            goto Phase 3
        }
    }
}
```

### Phase 3: Return to Standing
```kotlin
while (!monitoring) {
    if (shoulder_knee_distance ≈ baseline_standing_distance) {
        monitoring = true
        goto Phase 2
    }
}
```

### Metrics Used
- **Distance**: Euclidean distance between keypoints
- **Threshold**: ±2-10cm tolerance
- **Foot Check**: Y-coordinate difference < 2cm
- **Confidence**: Min 0.2 per keypoint

---

## 🎓 User Flow

### First Time Experience
1. Launch app → Habits screen
2. Select "SQUAT COUNTER"
3. Choose camera (front/back)
4. Position yourself → Wait for green borders
5. Start doing squats → Counter increments
6. Total squat persists across sessions

### Regular Usage
1. Launch app
2. Previous total loaded automatically
3. Select camera
4. Start workout
5. Session counter resets, total accumulates

---

## 💾 Data Storage

### SharedPreferences
```kotlin
Key: "squat_total_count"
Value: Int (total squats across all sessions)
```

### CSV Export (Recording Mode)
```csv
timestamp,keypoint_0_x,keypoint_0_y,keypoint_0_conf,...,keypoint_16_x,keypoint_16_y,keypoint_16_conf
1234567890,0.5,0.3,0.9,...,0.6,0.8,0.85
```

---

## 🔐 Permissions Required

- `CAMERA` - Per accesso alla fotocamera
- `WRITE_EXTERNAL_STORAGE` (SDK ≤ 28) - Per export CSV
- `READ_EXTERNAL_STORAGE` (SDK ≤ 32) - Per lettura file

---

## 🐛 Known Issues & Limitations

### Current Limitations
- ❌ Funziona solo in modalità portrait
- ❌ Richiede buona illuminazione
- ❌ Non supporta squat parziali (solo full squat)
- ❌ Non traccia profondità (solo 2D)

### Performance
- ✅ 30 FPS su dispositivi moderni
- ⚠️ 15-20 FPS su dispositivi entry-level
- ⚠️ Batteria: ~30-40% consumo per 30min

---

## 🚀 Future Improvements

### Planned Features
- [ ] Multi-exercise support (lunges, push-ups)
- [ ] Form analysis (postura corretta/scorretta)
- [ ] Workout history & statistics
- [ ] Challenge mode with friends
- [ ] Voice feedback
- [ ] Watch app integration

### Technical Improvements
- [ ] Model quantization for better performance
- [ ] Background processing optimization
- [ ] Cloud sync for workout data
- [ ] Offline mode improvements

---

## 📊 Performance Metrics

| Metric | Value |
|--------|-------|
| Model Size | 4.8 MB |
| Inference Time | 30-50ms |
| Frame Rate | 30 FPS |
| Memory Usage | ~150MB |
| Battery Drain | ~40%/30min |
| Squat Detection Accuracy | ~95% |

---

## 🤝 Target Audience

- 💪 Fitness enthusiasts
- 🏠 Home workout users
- 🎓 Beginners learning proper form
- 📊 Data-driven athletes
- 👴 Seniors monitoring activity

---

## 📜 Version History

### v2.0 (Current - Dec 2025)
- Split from urban camera features
- Renamed to "Good Habits"
- Enhanced squat detection algorithm
- Added persistence for total squats
- Improved visual feedback

### v1.0 (Original)
- Squat counter + Urban camera combined
- Basic pose detection
- No data persistence

---

## 📖 Related Documentation

See also:
- `SQUAT_COUNTER_GUIDE.md` - Detailed squat counter implementation
- `IMPLEMENTAZIONE_RECORDING.md` - Recording mode details
- `00_PROJECT_SPLIT_OVERVIEW.md` - Why we split the project

