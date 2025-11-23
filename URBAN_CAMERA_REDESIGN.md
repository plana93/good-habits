# 🎨 Urban Camera - Guida Design e Interazione

## 🌃 Stile Urban/Techno

La Urban Camera ora ha un design completamente rinnovato con palette neon techno:

### 🎨 Palette Colori
- **Deep Navy** (#151733) - Sfondo principale
- **Charcoal** (#23243A) - Sfondo secondario
- **Neon Blue** (#3DD0FF) - Accenti primari
- **Neon Orange** (#FF8B4A) - Accenti secondari
- **Peach** (#EAAE9A) - Testi e menu

### 📐 Layout Innovativo

```
┌────────────────────────────┐
│ [☰]                        │ ← Menu dropdown
│                            │
│  ╔══════════════════╗      │
│  ║                  ║      │
│  ║  VIEWFINDER      ║      │ ← Camera feed (86% width, 3:4 ratio)
│  ║  with neon       ║      │
│  ║  border          ║      │
│  ╚══════════════════╝      │
│                            │
│        ═══ ◉ ═══           │ ← Capture button neon
└────────────────────────────┘
  ║                      ║
  Knob                 Knob   ← Stagette laterali rotanti
```

## 🎮 Interazioni UI

### 📱 Menu Dropdown (Top-Left)
**Trigger**: Bottone circolare con bordo neon blu
**Animazione**: Rotazione 180° del bottone quando aperto

**Opzioni Menu**:
- 🎨 **B/W Filter** - Bianco e nero
- 🔲 **Sobel Filter** - Edge detection
- 🟦 **Pixel Filter** - Effetto pixelato
- ⭕ **No Filter** - Nessun filtro
- 🔄 **Switch Camera** - Cambia fotocamera

### 🎛️ Stagette Laterali (Knobs)

**Knob Sinistro**:
- Click → Rotazione 45° in senso orario
- Funzione: Aumenta probabilità apparizione box (+0.1)
- Range: 0.1 - 1.0

**Knob Destro**:
- Click → Rotazione 45° in senso antiorario
- Funzione: Aumenta dimensione box (+10px)
- Range: 30 - 150px

### 📸 Capture Button

**Design a tre livelli**:
1. **Outer Ring** - Anello neon blu con glow
2. **Shadow Layer** - Profondità 3D
3. **Inner Circle** - Gradiente peach con bordo arancione

**Decorazioni**: Segni neon blu/arancio attorno al pulsante

## 🎨 Effetti Urban Applicati

### Box Dinamici
I box appaiono sui joint dello scheletro con:
- Bordo sottile (2dp) monocromatico
- Dimensione configurabile tramite knob destro
- Probabilità di apparizione configurabile tramite knob sinistro

### Filtri Disponibili

1. **BLACK_WHITE**: Bianco e nero classico
2. **SOBEL**: Edge detection per effetto sketch
3. **PIXELATED**: Effetto mosaico pixelato
4. **NONE**: Nessun filtro, solo box

## 🔧 Configurazione Tecnica

### UrbanConfig.kt
```kotlin
BOX_APPEAR_PROBABILITY = 0.45f  // Modificabile con knob sinistro
BOX_SIZE = 60                   // Modificabile con knob destro
CURRENT_FILTER = FilterType     // Modificabile dal menu
```

### Layout Proporzioni
- **Viewfinder**: 86% della larghezza schermo
- **Aspect Ratio**: 3:4 (portrait ottimizzato)
- **Padding container**: 16dp
- **Corner radius**: 28dp

### Elementi Interattivi
- **Menu button**: 44x44dp con bordo circolare neon
- **Knobs**: 44x120dp con marker neon blu
- **Capture button outer**: 132dp diametro
- **Capture button inner**: 86dp diametro

## 🎯 UX Flow

### All'Avvio
1. Schermata con gradiente navy → charcoal
2. Viewfinder con bordo neon blu
3. Knobs laterali con marker neon
4. Capture button con glow effect

### Durante l'Uso
1. **Tap menu** → Dropdown appare con animazione
2. **Seleziona filtro** → Menu si chiude, filtro applicato
3. **Tap knobs** → Rotazione animata + regolazione parametri
4. **Box dinamici** → Appaiono/scompaiono sui joint in tempo reale

## 🎨 Design Principles

### Urban/Techno Aesthetic
- **Dark background** per contrasto neon
- **Rounded corners** (28dp) per modernità
- **Neon accents** (blu/arancio) per energia
- **Monospace font** nei menu per stile tech
- **Elevation & shadows** per profondità 3D

### Responsive Design
- Layout basato su constraint percentuali
- Dimensioni dinamiche che si adattano allo schermo
- Aspect ratio fisso per consistenza visiva

## 🚀 Funzionalità Aggiuntive Future

Possibili migliorie:
- [ ] Capture button per salvare screenshot
- [ ] Switch camera funzionante
- [ ] Slider per regolazione fine parametri
- [ ] Preset filtri salvabili
- [ ] Effetti particelle neon
- [ ] Animazioni box più elaborate
- [ ] Recording video con effetti

## 📱 Compatibilità

- Minimo API Level: 21 (Android 5.0)
- Layout ottimizzato per schermi 4:3 e 16:9
- ConstraintLayout per massima flessibilità
- Material Design components integrati

## 🎨 File Creati

### Drawable Resources
- `bg_gradient.xml` - Sfondo gradiente navy
- `viewfinder_bg.xml` - Bordo neon viewfinder
- `capture_outer_ring.xml` - Anello esterno capture button
- `capture_inner_circle.xml` - Cerchio interno capture button
- `toggle_knob.xml` - Design stagette laterali
- `menu_background.xml` - Sfondo menu dropdown
- `circle_neon_outline.xml` - Bordo circolare menu button
- `ic_menu_arrow.xml` - Icona freccia menu
- `neon_marks.xml` - Decorazioni neon intorno capture button

### Values Resources
- `colors.xml` - Palette colori urban theme
- `dimens.xml` - Dimensioni standard
- `styles.xml` - Stile MenuItem

### Layout
- `activity_urban_camera.xml` - Layout principale ridisegnato

---

🎨 **Enjoy your Urban Camera experience!** 🌃
