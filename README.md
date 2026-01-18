# 🏋️ Good Habits - AI-Powered Fitness Tracker

<div align="center">

![Platform](https://img.shields.io/badge/Platform-Android-green.svg)
![API](https://img.shields.io/badge/API-24%2B-brightgreen.svg)
![Language](https://img.shields.io/badge/Language-Kotlin-purple.svg)
![UI](https://img.shields.io/badge/UI-Jetpack%20Compose-blue.svg)
![AI](https://img.shields.io/badge/AI-TensorFlow%20Lite-orange.svg)

**Un'app moderna per il fitness che combina intelligenza artificiale, pose detection e gestione completa degli allenamenti**

[Screenshots](#-screenshots) • [Features](#-features) • [Download](#-download) • [Tech Stack](#-tech-stack) • [Documentazione](#-documentazione)

</div>

---

## 📱 Screenshots

### Dashboard & Today Screen
<div align="center">
<table>
  <tr>
    <td><img src="screenshots/dashboard.png" width="250" alt="Dashboard"/></td>
    <td><img src="screenshots/today_screen.png" width="250" alt="Today Screen"/></td>
    <td><img src="screenshots/calendar.png" width="250" alt="Calendar"/></td>
  </tr>
  <tr>
    <td align="center"><b>Dashboard</b><br/>Statistiche e KPIs</td>
    <td align="center"><b>Today Screen</b><br/>Sessione giornaliera</td>
    <td align="center"><b>Calendar</b><br/>Streak tracking</td>
  </tr>
</table>
</div>

### AI Squat Detection
<div align="center">
<table>
  <tr>
    <td><img src="screenshots/ai_squat_detection.png" width="250" alt="AI Detection"/></td>
    <td><img src="screenshots/exercise_library.png" width="250" alt="Exercise Library"/></td>
    <td><img src="screenshots/workout_builder.png" width="250" alt="Workout Builder"/></td>
  </tr>
  <tr>
    <td align="center"><b>AI Detection</b><br/>Real-time squat counting</td>
    <td align="center"><b>Exercise Library</b><br/>Template personalizzabili</td>
    <td align="center"><b>Workout Builder</b><br/>Crea allenamenti completi</td>
  </tr>
</table>
</div>

### Wellness Tracking
<div align="center">
<table>
  <tr>
    <td><img src="screenshots/wellness_tracking.png" width="250" alt="Wellness Tracking"/></td>
    <td><img src="screenshots/export_data.png" width="250" alt="Export Data"/></td>
    <td><img src="screenshots/recovery_system.png" width="250" alt="Recovery System"/></td>
  </tr>
  <tr>
    <td align="center"><b>Wellness Tracking</b><br/>Mood & benessere</td>
    <td align="center"><b>Export Data</b><br/>CSV/JSON/TXT</td>
    <td align="center"><b>Recovery System</b><br/>Recupera giorni persi</td>
  </tr>
</table>
</div>

> **Nota**: Aggiungi i tuoi screenshot nella cartella `/screenshots/` per visualizzarli qui sopra

---

## ✨ Features

### 🤖 AI-Powered Squat Detection
- **MoveNet TensorFlow Lite**: Riconoscimento posture in tempo reale
- **Conteggio automatico**: AI squat detection con feedback visivo
- **Camera front/back**: Supporto entrambe le fotocamere
- **Calibrazione automatica**: Auto-adattamento alla postura dell'utente

### 📅 Sistema Sessioni Modulari
- **Sessioni giornaliere**: Organizzazione automatica per data
- **Multi-esercizi**: Aggiungi esercizi personalizzati o AI squat
- **Multi-allenamenti**: Template di workout completi
- **Navigazione temporale**: Scroll orizzontale tra giorni passati/presente

### 📊 Dashboard & Analytics
- **Dashboard moderna**: Overview statistiche real-time
- **Calendario integrato**: Visualizzazione streak di continuità
- **Export completo**: CSV/JSON/TXT per analisi esterne
- **Statistiche live**: Conteggi aggiornati istantaneamente

### 🧘 Wellness Tracking (NEW!)
- **18 tracker predefiniti**: Mood, energia, sonno, stress, relazioni, gratitudine
- **Tracking emotivo**: Rating 0-5, emoticon sets, note testuali
- **Reference date**: Traccia retroattivamente eventi passati
- **Separazione fisica/mentale**: Non influenza calendar e streak
- **CSV dedicato**: Export separato per analisi benessere

### 💪 Motivazione & Gamification
- **Streak tracking**: Conteggio giorni consecutivi
- **Recovery system**: Recupera sessioni mancate con 20 AI squat
- **Frasi motivazionali**: 30+ quote per giorni vuoti
- **Visual feedback**: Codifica colori per stato giorni

### 🎨 Modern UI/UX
- **Material3 Design**: Design system moderno
- **Jetpack Compose**: UI reattiva e fluida
- **Dark/Light theme**: Supporto temi dinamici
- **Conditional navigation**: UI context-sensitive

---

## 🚀 Download

### Requisiti
- **Android**: 7.0 (API 24) o superiore
- **Spazio**: ~50 MB
- **Permessi**: Camera (per AI detection)

### Installazione

#### Da Release (Consigliato)
```bash
# Scarica l'APK dalla pagina Releases
# https://github.com/plana93/good-habits/releases

# Installa via ADB
adb install good-habits-v3.0.apk
```

#### Build da Sorgente
```bash
# Clona repository
git clone https://github.com/plana93/good-habits.git
cd good-habits

# Build debug APK
./gradlew assembleDebug

# Installa
adb install app/build/outputs/apk/debug/app-debug.apk
```

---

## 🛠️ Tech Stack

### Core Technologies
- **Language**: Kotlin 100%
- **Min SDK**: API 24 (Android 7.0)
- **Target SDK**: API 32 (Android 12L)

### Architecture
- **Pattern**: MVVM + Clean Architecture
- **DI**: Manual Dependency Injection with Factory Pattern
- **Async**: Kotlin Coroutines + Flow/StateFlow
- **Navigation**: Jetpack Navigation Compose

### UI Layer
- **Framework**: Jetpack Compose
- **Design**: Material3 Design System
- **Components**: Card, FAB, Dialog, LazyVerticalGrid, HorizontalPager

### Data Layer
- **Database**: Room SQLite (v10)
- **Tables**: 7 tables with complex relations
- **Migrations**: Full migration path 1→10

### AI & ML
- **Framework**: TensorFlow Lite
- **Model**: MoveNet Lightning (optimized for mobile)
- **Processing**: GPU-accelerated when available
- **Performance**: ~30 FPS real-time detection

### Key Libraries
```gradle
// UI
implementation "androidx.compose.ui:ui:1.5.0"
implementation "androidx.compose.material3:material3:1.1.0"

// Database
implementation "androidx.room:room-runtime:2.5.2"
implementation "androidx.room:room-ktx:2.5.2"

// AI
implementation "org.tensorflow:tensorflow-lite:2.12.0"
implementation "org.tensorflow:tensorflow-lite-gpu:2.12.0"

// Navigation
implementation "androidx.navigation:navigation-compose:2.7.0"

// Camera
implementation "androidx.camera:camera-camera2:1.2.3"
implementation "androidx.camera:camera-lifecycle:1.2.3"
```

---

## 📐 Architecture Overview

```
┌──────────────────────────────────────────────────────┐
│              GOOD HABITS APP                         │
│           (Clean Architecture + MVVM)                │
└───────────────────┬──────────────────────────────────┘
                    │
    ┌───────────────┼───────────────┐
    │               │               │
┌───▼────┐    ┌─────▼──────┐   ┌───▼────┐
│   UI   │    │   DOMAIN   │   │  DATA  │
│ Layer  │    │   Layer    │   │ Layer  │
└───┬────┘    └─────┬──────┘   └───┬────┘
    │               │               │
Compose         ViewModels     Repository
Screens         + Factory      + Database
```

### Key Components

**4 Repository principali**:
- `SessionRepository` - Workout sessions (AI squat)
- `DailySessionRepository` - Daily modular sessions
- `ExerciseRepository` - Exercise templates
- `WorkoutRepository` - Workout templates

**9+ ViewModels**:
- `TodayViewModel` - Daily session management
- `DashboardViewModel` - Statistics & KPIs
- `CalendarViewModel` - Calendar & streak
- `ExportViewModel` - Data export
- E altri...

**Database Schema**:
- 7 tables con relazioni complesse
- Dual system: Legacy (workout_sessions) + Modern (daily_sessions)
- Wellness tracking integration
- Migration completa 1→10

---

## 📚 Documentazione

### Per Utenti
- **[App Overview](update_docs/00_APP_OVERVIEW.md)** - Descrizione completa features
- **[Roadmap](update_docs/02_DEVELOPMENT_ROADMAP.md)** - Sviluppi futuri

### Per Sviluppatori
- **[Technical Guide](update_docs/TECHNICAL_GUIDE.md)** - Architettura & development
- **[Build Guide](update_docs/05_BUILD_DEPLOY_GUIDE.md)** - Setup & deployment
- **[Features Reference](update_docs/FEATURES_REFERENCE.md)** - Feature implementation (coming soon)
- **[Wellness Tracking](update_docs/WELLNESS_TRACKING.md)** - Wellness system (coming soon)

### Quick Links
```bash
# Documentazione completa
cd update_docs/

# Guide principali
00_APP_OVERVIEW.md          # Overview app
TECHNICAL_GUIDE.md          # Architecture & dev
02_DEVELOPMENT_ROADMAP.md   # Roadmap
05_BUILD_DEPLOY_GUIDE.md    # Build guide
```

---

## 🎯 Use Cases

### Scenario 1: Allenamento AI Squat
```
1. Apri app → Dashboard
2. Tap FAB centrale (icona fitness)
3. Seleziona camera (front/back)
4. Start AI detection
5. Esegui squat → Conteggio automatico
6. Salva nella sessione odierna
```

### Scenario 2: Workout Personalizzato
```
1. Today Screen → Tap "+"
2. Seleziona "Add Workout"
3. Scegli template (es. "Upper Body")
4. Completa esercizi uno per uno
5. Statistiche aggiornate real-time
```

### Scenario 3: Wellness Tracking
```
1. Today Screen → Sezione Wellness
2. Tap tracker (es. "How are you feeling?")
3. Seleziona rating emoticon (0-5)
4. Aggiungi note opzionali
5. Salva → Non influenza calendario
```

### Scenario 4: Recovery Giorno Perso
```
1. Dashboard → Tap calendario
2. Tap giorno mancato (rosso)
3. Conferma recovery
4. Completa 20 AI squat
5. Giorno marcato come recuperato
```

---

## 🔧 Development

### Setup Ambiente

```bash
# Requisiti
- Android Studio Flamingo o superiore
- JDK 11+
- Android SDK 24+
- Gradle 8.0

# Setup
git clone https://github.com/plana93/good-habits.git
cd good-habits
./gradlew assembleDebug
```

### Struttura Progetto

```
app/src/main/
├── java/com/programminghut/pose_detection/
│   ├── data/
│   │   ├── dao/              # Room DAOs
│   │   ├── model/            # Data entities
│   │   ├── repository/       # Repository pattern
│   │   └── database/         # Database & migrations
│   ├── ui/
│   │   ├── activity/         # Main activities
│   │   ├── components/       # Compose components
│   │   ├── viewmodel/        # ViewModels
│   │   └── */                # Feature screens
│   ├── ml/                   # TensorFlow models
│   ├── util/                 # Utilities
│   └── SquatCounter.kt       # AI detection logic
└── assets/
    ├── exercise_templates.json
    ├── workout_templates.json
    ├── wellness_tracker_templates.json
    └── motivational_quotes.json
```

### Testing

```bash
# Unit tests
./gradlew test

# Instrumentation tests
./gradlew connectedAndroidTest

# Lint
./gradlew lint
```

---

## 🤝 Contributing

Contributi sono benvenuti! Per contribuire:

1. **Fork** il repository
2. **Crea** un branch per la tua feature (`git checkout -b feature/AmazingFeature`)
3. **Commit** le modifiche (`git commit -m 'Add some AmazingFeature'`)
4. **Push** al branch (`git push origin feature/AmazingFeature`)
5. **Apri** una Pull Request

### Guidelines

- Segui lo stile Kotlin esistente
- Scrivi test per nuove feature
- Aggiorna la documentazione
- Usa commit messages descrittivi

---

## 📊 Project Status

### Versione Corrente: 3.0 (Production Ready)

**Completato** ✅:
- AI Squat Detection
- Daily Sessions System
- Dashboard & Analytics
- Calendar & Streak Tracking
- Recovery System
- Wellness Tracking (Backend)
- Export CSV/JSON/TXT
- Material3 UI

**In Sviluppo** 🚧:
- Wellness Tracking UI
- Advanced Charts
- Social Features

**Planned** 📋:
- Wearable Integration
- Cloud Sync
- Custom Trackers
- Workout Programs

---

## 📄 License

Questo progetto è distribuito sotto licenza **MIT License**.

```
MIT License

Copyright (c) 2026 Mirco

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

---

## 👨‍💻 Author

**Mirco**
- GitHub: [@plana93](https://github.com/plana93)
- Repository: [good-habits](https://github.com/plana93/good-habits)

---

## 🙏 Acknowledgments

- **TensorFlow**: Per il framework ML
- **Google**: Per MoveNet e Jetpack Compose
- **Android Community**: Per le librerie open source
- **Material Design**: Per le guidelines UI/UX

---

## 📞 Support

Hai domande o problemi?

- 🐛 [Apri un Issue](https://github.com/plana93/good-habits/issues)
- 📧 Contatta via GitHub
- 📖 Leggi la [documentazione completa](update_docs/)

---

<div align="center">

**⭐ Se questo progetto ti è utile, lascia una stella! ⭐**

Made with ❤️ and 💪 by Mirco

</div>
