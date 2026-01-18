# 🏋️ Good Habits App - Overview

**Version**: 3.0  
**Platform**: Android  
**Minimum SDK**: 24 (Android 7.0)  
**Target SDK**: 32 (Android 12L)  
**Status**: Production Ready ✅

---

## 📱 App Description

**Good Habits** è un'applicazione moderna per il fitness e benessere che combina intelligenza artificiale, pose detection e gestione completa degli allenamenti. L'app offre un'esperienza completamente personalizzabile per tracciare progressi fisici, gestire sessioni di allenamento, monitorare il proprio stato emotivo e mantenere costanza nelle abitudini positive.

### 🎯 Caratteristiche Principali

#### 🤖 AI-Powered Squat Detection
- **MoveNet TensorFlow Lite**: Riconoscimento posture in tempo reale
- **Conteggio automatico**: Squat AI detection con feedback visivo
- **Conteggio manuale**: eserici posso essere tracciati inserendo manualmente le ripetizioni o i secondi di tenuta
- **Camera front/back**: Supporto entrambe le fotocamere
- **Calibrazione automatica**: Auto-adattamento alla postura dell'utente (SquatAI)

#### 📅 Sistema Sessioni Modulari
- **Sessioni giornaliere**: Organizzazione automatica per data
- **Multi-esercizi**: Supporto esercizi personalizzati e AI squat
- **Multi-allenamenti**: Template di workout completi (più esercizi raggruppati)
- **Navigazione temporale**: Scroll orizzontale tra giorni passati/presente

#### 📊 Dashboard & Analytics
- **Dashboard moderna**: Overview statistiche
- **Calendario integrato**: Visualizzazione streak di continuità e giorni mancati
- **Export CSV**: Dati completi per analisi esterne
- **Statistiche real-time**: Conteggi aggiornati istantaneamente, ogni volta che aggiungo un esercizio o un allamento si deve adattare il calendario con la streak e il conteggio dei squat totati che deve essere l'esercizio core. 

#### 💡 Motivazione & Gamification
- **Frasi motivazionali**: 30+ quote per giorni vuoti 
- **Streak tracking**: Conteggio giorni consecutivi (per gli esercizi o allenamentei)
- **Recovery system**: Sistema recupero sessioni mancate tramite 20 AI squat
- **Recovery execution**: Eseguito nel giorno corrente, segna giorno passato come recuperato
- **Streak contribution**: Recovery contribuisce alla streak e ai totali squat
- **Calendar integration**: Toccare giorni mancati avvia direttamente procedura recovery
- **Visual feedback**: Codifica colori per stato giorni (normale/mancato/recuperato)

#### 🧘 Wellness Tracking System
- **18 Tracker predefiniti**: Mood, energia, sonno, stress, relazioni, gratitudine e altro
- **Sezione separata**: Dedicata al benessere mentale/emotivo (non fisica)
- **Flessibilità giornaliera**: Possibilità di tracciare più volte al giorno
- **Multiple response types**: 
  - Rating 0-5 con emoticon e label descrittive
  - Boolean (Sì/No)
  - Emotion Sets (selezione emozioni specifiche)
  - Note testuali libere
- **Template JSON**: 18 tracker in inglese modificabili senza rebuild app
- **Export dedicato**: CSV separato per analisi dati benessere
- **Privacy-first**: Note opzionali, nessun obbligo, nessuna pressione
- **Non impatta calendario**: `countsAsActivity = false` - separazione netta da attività fisica
- **No streak pressure**: Benessere mentale senza gamification forzata

---

## 🏗️ Architettura App

### 📱 Schermate Principali

1. **Dashboard** 
   - Overview statistiche con grid layout
   - Quick actions: calendario e export
   - Navigazione central FAB

2. **Today/History**
   - Gestione sessione giornaliera
   - HorizontalPager per navigazione giorni
   - Add esercizi/workout (solo per oggi)
   - Sezione Wellness separata per tracking emotivo/mentale

3. **Exercises & Workouts**
   - Librerie template esercizi e workout
   - Creazione elementi personalizzati
   - Integrazione con sessioni giornaliere

4. **AI Squat**
   - Camera selection (front/back)
   - Pose detection real-time
   - Conteggio automatico con salvataggio

### 🔧 Tecnologie Utilizzate

- **UI**: Jetpack Compose + Material3 Design
- **Navigation**: Navigation Compose con conditional rendering
- **Database**: Room + SQLite con relazioni complesse
- **AI**: TensorFlow Lite + MoveNet pose detection
- **Architecture**: MVVM + Repository pattern
- **Async**: Kotlin Coroutines + StateFlow/Flow
- **Data Management**: JSON templates per wellness trackers
- **Export**: CSV generation per analytics esterni

---

## ✅ Funzionalità Implementate

### Core Features (100% Complete)
- ✅ AI Squat detection con MoveNet
- ✅ Sistema sessioni giornaliere modulari  
- ✅ Dashboard con statistiche real-time
- ✅ Calendario con streak tracking
- ✅ Export CSV completo
- ✅ Navigation con conditional UI
- ✅ Material3 design system

### Advanced Features (100% Complete)
- ✅ Horizontal day navigation
- ✅ Temporal restrictions (add solo oggi)
- ✅ Motivational quotes per giorni vuoti
- ✅ Recovery system per sessioni mancate (20 AI squat)
- ✅ Recovery execution nel giorno corrente
- ✅ Calendar-recovery integration (tap giorni mancati)
- ✅ Calendar-dashboard integration
- ✅ Multi-camera support (front/back)

### Wellness Tracking Features (Backend Complete 100% | UI Pending)
- ✅ Database schema v9 con supporto wellness trackers
- ✅ 18 tracker predefiniti in JSON (mood, energia, stress, relazioni, etc.)
- ✅ WellnessTrackerFileManager per gestione templates
- ✅ Multiple response types (Rating 0-5, Boolean, Emotion Set, Text Note)
- ✅ CSV export dedicato per wellness data
- ✅ Separazione netta da attività fisica (`countsAsActivity = false`)
- ✅ Support multiple entries per day (flessibilità tracking)
- ⏳ UI Components (WellnessSection, TrackerCard, RatingBarInput)
- ⏳ TrackerEntryDialog con response inputs
- ⏳ Integration nella Today screen

### UI/UX Enhancements (100% Complete)
- ✅ Conditional bottom bar (hide in exercises/workouts)
- ✅ Central FAB con context-sensitive icons
- ✅ Full-screen calendar/export dialogs
- ✅ StatCard grid layout per dashboard
- ✅ Route-based conditional rendering

---

## 🎨 Design Philosophy

### Material3 First
- **Dynamic theming**: Adattamento automatic color scheme
- **Typography scale**: Gerarchia tipografica consistente  
- **Component library**: Card, FAB, Dialog, Navigation
- **Responsive layout**: Grid e flex layouts

### User Experience
- **Progressive disclosure**: Funzionalità avanzate in dialogs
- **Contextual actions**: FAB cambia in base alla schermata
- **Visual feedback**: Stati chiari con iconografie
- **Temporal logic**: Restrizioni intuitive (add solo oggi)

---

## 🚀 Performance & Scalability

### Database Optimization
- **Efficient queries**: Query ottimizzate con indices
- **Lazy loading**: Dati caricati on-demand
- **Caching**: Repository pattern con caching locale

### AI Performance
- **Model efficiency**: MoveNet Lite per mobile
- **Frame optimization**: Processing ottimizzato per 30 FPS
- **Memory management**: Gestione automatica bitmap

### Code Quality
- **SOLID principles**: Architettura modulare e testabile
- **Type safety**: Kotlin con null safety completo
- **Error handling**: Gestione errori robusta

---

## 📈 Metrics & Analytics

L'app traccia le seguenti metriche:

### Physical Activity Metrics
- **Sessioni totali**: Conteggio allenamenti completati
- **Esercizi per giorno**: Numero elementi sessione odierna
- **Streak giorni**: Giorni consecutivi con attività (include recovery)
- **Squat AI**: Ripetizioni automatiche con timestamp
- **Recovery sessions**: Sessioni recuperate tramite 20 AI squat vs perse
- **Recovery tracking**: Giorni passati marcati come recuperati

### Wellness Metrics (Backend Ready)
- **Tracker entries**: Numero totale tracciamenti wellness completati
- **Daily wellness check-ins**: Frequenza utilizzo tracker giornalieri
- **Response distribution**: Distribuzione valori per tipo (rating, emotion, etc.)
- **Note richness**: Percentuale entries con note testuali
- **Tracker variety**: Diversità tracker utilizzati
- **Timestamp analysis**: Pattern orari di utilizzo (mattina/sera)

---

