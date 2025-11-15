# 🎨 URBAN CAMERA - Guida Completa

## ✨ Descrizione

**Urban Camera** è una modalità artistica che trasforma la rilevazione della pose in un'esperienza visiva urban street art. Box colorate e filtrate appaiono dinamicamente intorno ai giunti del corpo, creando effetti grafici in stile cyberpunk/glitch art.

---

## 🎯 Caratteristiche Principali

### ✅ Elementi Grafici Legati allo Scheletro
- **Box Dinamiche**: Appaiono intorno ai giunti rilevati
- **Filtri Casuali**: Bianco/Nero, Sobel (edge detection), Pixelato
- **Apparizione Variabile**: Intervalli casuali e veloci
- **Colori Neon**: Palette street art personalizzabile

### ✅ Architettura Modulare
- **Facile Personalizzazione**: File `UrbanConfig.kt` per modifiche rapide
- **Preset Predefiniti**: Glitch, Neon, Minimal, Chaos
- **Espandibile**: Facile aggiungere nuovi effetti e filtri

---

## 🚀 Come Usare

### **Flusso Utente:**

```
Habits (Home)
    ↓ Click "URBAN CAMERA"
UrbanCameraSelectionActivity
    ↓ Scegli Front/Back Camera
UrbanCameraActivity
    → Effetti visual in tempo reale
```

### **UI Schermata Iniziale:**

```
┌──────────────────────────────────┐
│  Welcome to Pose Detection App!  │
│                                  │
│  ┌────────────────────────────┐  │
│  │    SQUAT COUNTER           │  │
│  └────────────────────────────┘  │
│                                  │
│  ┌────────────────────────────┐  │
│  │    RECORD SKELETON         │  │
│  └────────────────────────────┘  │
│                                  │
│  ┌────────────────────────────┐  │
│  │    URBAN CAMERA      🎨    │  │ ← NUOVO!
│  └────────────────────────────┘  │
└──────────────────────────────────┘
```

---

## 🎨 Effetti Visivi Implementati

### **1. Box Dinamiche sui Giunti**

Le box appaiono casualmente intorno ai 17 keypoint del corpo:

```
Giunti disponibili:
• 0: Naso
• 1-2: Occhi (sinistro, destro)
• 3-4: Orecchie (sinistro, destro)
• 5-6: Spalle (sinistra, destra)
• 7-8: Gomiti (sinistro, destro)
• 9-10: Polsi (sinistro, destro)
• 11-12: Fianchi (sinistro, destro)
• 13-14: Ginocchia (sinistra, destra)
• 15-16: Caviglie (sinistra, destra)
```

### **2. Filtri Immagine**

Ogni box applica **casualmente** uno di questi filtri:

#### **A) Bianco e Nero**
```kotlin
FilterType.BLACK_WHITE
```
- Converte la porzione di immagine in scala di grigi
- Effetto classico e pulito

#### **B) Sobel Effect (Edge Detection)**
```kotlin
FilterType.SOBEL_EFFECT
```
- Rileva i bordi dell'immagine
- Stile sketch/disegno tecnico
- Effetto cyberpunk

#### **C) Pixelato**
```kotlin
FilterType.PIXELATED
```
- Effetto retro 8-bit
- Dimensione pixel configurabile
- Stile glitch art

### **3. Colori Neon**

Palette di 7 colori vibranti per i bordi:

```kotlin
• Magenta:     #FF00FF
• Cyan:        #00FFFF
• Yellow:      #FFFF00
• Orange:      #FF6400
• Green Neon:  #00FF64
• Deep Pink:   #FF1493
• Lime:        #00FF00
```

---

## ⚙️ Personalizzazione Facile

### **File: `UrbanConfig.kt`**

Tutti i parametri sono in **UN UNICO FILE** per modifiche rapide:

```kotlin
// ═══════════════════════════════════════
// 📦 BOX CONFIGURATION
// ═══════════════════════════════════════

// Probabilità di apparizione (0.0 - 1.0)
UrbanConfig.BOX_APPEAR_PROBABILITY = 0.3f

// Durata in frame (30-60 FPS)
UrbanConfig.BOX_MIN_DURATION = 10
UrbanConfig.BOX_MAX_DURATION = 40

// Dimensioni in pixel
UrbanConfig.BOX_SIZE_MIN = 80f
UrbanConfig.BOX_SIZE_MAX = 200f

// Opacità (0-255)
UrbanConfig.BOX_OPACITY = 200

// Spessore bordo
UrbanConfig.BORDER_WIDTH = 8f

// ═══════════════════════════════════════
// 🦴 ACTIVE JOINTS
// ═══════════════════════════════════════

// Scegli quali giunti mostrare
UrbanConfig.ACTIVE_JOINTS = listOf(
    0,      // nose
    5, 6,   // shoulders
    9, 10,  // wrists
    13, 14  // knees
)

// ═══════════════════════════════════════
// 🎨 COLORS
// ═══════════════════════════════════════

// Aggiungi/modifica colori
UrbanConfig.BORDER_COLORS = listOf(
    Color.rgb(255, 0, 255),  // Magenta
    Color.rgb(0, 255, 255),  // Cyan
    // ... aggiungi altri colori
)
```

---

## 🎛️ Preset Predefiniti

Usa i preset per cambiare rapidamente lo stile:

### **1. Glitch Art**
```kotlin
UrbanPresets.applyGlitchPreset()
```
- Box piccole e veloci
- Alta frequenza
- Effetto frammentato

### **2. Neon City**
```kotlin
UrbanPresets.applyNeonPreset()
```
- Box grandi e lente
- Bordi spessi
- Atmosfera cyberpunk

### **3. Minimal**
```kotlin
UrbanPresets.applyMinimalPreset()
```
- Poche box
- Lunga durata
- Stile pulito

### **4. Chaos**
```kotlin
UrbanPresets.applyChaosPreset()
```
- Massima densità
- Durata brevissima
- Effetto caotico

**Come usarli:**
```kotlin
// In UrbanCameraActivity.onCreate() aggiungi:
UrbanPresets.applyNeonPreset()  // O qualsiasi altro preset
```

---

## 📁 Struttura File

```
app/src/main/java/com/programminghut/pose_detection/
├── Habits.kt                           ← Aggiunto bottone URBAN CAMERA
├── UrbanCameraSelectionActivity.kt     ← Selezione camera
├── UrbanCameraActivity.kt              ← Activity principale
└── urban/
    ├── UrbanEffectsManager.kt          ← Logica effetti grafici
    └── UrbanConfig.kt                  ← ⭐ CONFIGURAZIONE FACILE

app/src/main/res/layout/
└── activity_urban_camera.xml           ← Layout minimale

AndroidManifest.xml                     ← Activity registrate
```

---

## 🔧 Come Aggiungere Nuovi Effetti

### **Step 1: Aggiungi il tipo di filtro**

In `UrbanEffectsManager.kt`:

```kotlin
enum class FilterType {
    BLACK_WHITE,
    SOBEL_EFFECT,
    PIXELATED,
    YOUR_NEW_FILTER  // ← Aggiungi qui
}
```

### **Step 2: Implementa il filtro**

```kotlin
private fun applyFilter(bitmap: Bitmap, filter: FilterType): Bitmap {
    return when (filter) {
        FilterType.BLACK_WHITE -> applyBlackWhiteFilter(bitmap)
        FilterType.SOBEL_EFFECT -> applySobelEffect(bitmap)
        FilterType.PIXELATED -> applyPixelation(bitmap)
        FilterType.YOUR_NEW_FILTER -> applyYourNewFilter(bitmap)  // ← Aggiungi qui
    }
}

private fun applyYourNewFilter(bitmap: Bitmap): Bitmap {
    // Implementa il tuo filtro qui
    // Es: sepia, blur, invert colors, etc.
    val result = bitmap.copy(bitmap.config, true)
    // ... logica del filtro ...
    return result
}
```

### **Step 3: Parametri in Config**

In `UrbanConfig.kt`:

```kotlin
// Parametri per il tuo filtro
var YOUR_FILTER_INTENSITY = 1.0f
var YOUR_FILTER_PARAMETER = 50
```

---

## 🎨 Esempi di Personalizzazione

### **Esempio 1: Solo Mani e Testa**

```kotlin
UrbanConfig.ACTIVE_JOINTS = listOf(
    0,      // nose
    9, 10   // wrists
)
```

### **Esempio 2: Box Grandi e Lente**

```kotlin
UrbanConfig.BOX_SIZE_MIN = 200f
UrbanConfig.BOX_SIZE_MAX = 400f
UrbanConfig.BOX_MIN_DURATION = 60
UrbanConfig.BOX_MAX_DURATION = 120
UrbanConfig.BOX_APPEAR_PROBABILITY = 0.2f
```

### **Esempio 3: Palette Custom**

```kotlin
UrbanConfig.BORDER_COLORS = listOf(
    Color.rgb(255, 0, 0),    // Rosso
    Color.rgb(0, 0, 255),    // Blu
    Color.rgb(128, 0, 128)   // Viola
)
```

---

## 🚀 Roadmap Future Features

### **Fase 2: Elementi Non Legati allo Scheletro**

- [ ] **Particelle di sfondo** che reagiscono al movimento
- [ ] **Linee geometriche** che seguono la traiettoria
- [ ] **Testo generativo** con frasi urban
- [ ] **Graffiti animati** che appaiono casualmente

### **Fase 3: Effetti Temporali**

- [ ] **Palette diversa in base all'ora**
  - Alba: toni caldi (arancio, rosa)
  - Giorno: colori vivaci
  - Tramonto: toni freddi (viola, blu)
  - Notte: neon intensi

- [ ] **Effetti stagionali**
  - Estate: colori caldi e luminosi
  - Inverno: tonalità fredde

### **Fase 4: Interattività**

- [ ] **Gesture recognition**: cambia preset con gesti
- [ ] **Voice control**: comandi vocali per effetti
- [ ] **Screenshot/Recording**: salva i frame migliori

### **Fase 5: AI Generativa**

- [ ] **Style transfer** in tempo reale
- [ ] **Generative patterns** basati su pose
- [ ] **Music reactive**: effetti sincronizzati con audio

---

## 🧪 Testing

### **Checklist Test:**

- [ ] App si avvia senza crash
- [ ] Bottone "URBAN CAMERA" visibile in Habits
- [ ] Selezione camera funziona
- [ ] Box appaiono sui giunti
- [ ] Filtri (B/N, Sobel, Pixel) funzionano
- [ ] Bordi colorati visibili
- [ ] Performance fluide (30+ FPS)
- [ ] Modifiche a UrbanConfig.kt si riflettono nell'app

---

## 📊 Performance Tips

### **Ottimizzare FPS:**

```kotlin
// Riduci complessità
UrbanConfig.BOX_APPEAR_PROBABILITY = 0.2f
UrbanConfig.ACTIVE_JOINTS = listOf(5, 6, 9, 10)  // Solo 4 giunti

// Riduci dimensioni box
UrbanConfig.BOX_SIZE_MAX = 150f

// Limita FPS
UrbanConfig.MAX_FPS = 30
```

### **Gestione Memoria:**

- Le bitmap vengono automaticamente riciclate (`recycle()`)
- Box scadute vengono rimosse dalla memoria
- Solo le box visibili vengono processate

---

## 🐛 Troubleshooting

### **Problema: Box non appaiono**
**Soluzione:**
```kotlin
// Aumenta probabilità
UrbanConfig.BOX_APPEAR_PROBABILITY = 0.8f
// Abbassa threshold
UrbanConfig.MIN_JOINT_SCORE = 0.1f
```

### **Problema: App lenta**
**Soluzione:**
```kotlin
// Riduci numero box
UrbanConfig.BOX_APPEAR_PROBABILITY = 0.1f
// Usa solo alcuni giunti
UrbanConfig.ACTIVE_JOINTS = listOf(0, 9, 10)
```

### **Problema: Colori non visibili**
**Soluzione:**
```kotlin
// Aumenta opacità
UrbanConfig.BOX_OPACITY = 255
// Aumenta spessore bordo
UrbanConfig.BORDER_WIDTH = 15f
```

---

## 📝 Note Tecniche

### **Formato Dati Pose:**
```
outputFeature0: FloatArray[51]
Ogni giunto (17 totali):
  - [i*3 + 0] = Y coordinate (normalized 0-1)
  - [i*3 + 1] = X coordinate (normalized 0-1)
  - [i*3 + 2] = Confidence score (0-1)
```

### **Filtri Bitmap:**
- Ogni filtro crea una **nuova bitmap**
- Le bitmap vengono **riciclate** dopo l'uso
- Processing avviene **per frame** in tempo reale

---

## 🎉 Conclusione

Urban Camera è **completamente modulare** e **facile da estendere**. Modifica `UrbanConfig.kt` per personalizzazioni rapide, o aggiungi nuovi filtri in `UrbanEffectsManager.kt` per effetti più avanzati.

**Next Steps:**
1. Testa l'app
2. Sperimenta con i preset
3. Personalizza i parametri
4. Aggiungi i tuoi filtri custom!

---

**Versione:** 1.0  
**Data:** 11 Novembre 2025  
**Autore:** GitHub Copilot + plana93
