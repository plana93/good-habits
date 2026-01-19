# 🏋️ Good Habits

<div align="center">

![Platform](https://img.shields.io/badge/Platform-Android-green.svg)
![API](https://img.shields.io/badge/API-24%2B-brightgreen.svg)
![Language](https://img.shields.io/badge/Language-Kotlin-purple.svg)
![UI](https://img.shields.io/badge/UI-Jetpack%20Compose-blue.svg)
![AI](https://img.shields.io/badge/AI-TensorFlow%20Lite-orange.svg)

### **Il tuo smartphone ti conta gli squat. Davvero.**

*Un esperimento nato dalla passione: Kotlin, Computer Vision e AI sul palmo della mano.*

[📥 Download](#-download) • [🎯 Features](#-cosa-fa) • [🛠️ Tech](#-come-è-fatto)

</div>

---

## 🤔 Perché esiste questo progetto?

**Ti sei mai chiesto** cosa ci vuole per insegnare a uno smartphone a riconoscere il tuo corpo mentre ti alleni?

Questo progetto nasce da quella curiosità. **Non è un'app fatta da un team di sviluppatori**—è l'esplorazione personale di uno sviluppatore che voleva:

✨ **Sporcarsi le mani** con Computer Vision su Android  
✨ **Capire davvero** come funziona TensorFlow Lite  
✨ **Sperimentare** con Jetpack Compose e Kotlin moderno  
✨ **Costruire qualcosa di utile** (e che contasse gli squat al posto mio)

> *"E se il mio telefono capisse quando faccio uno squat corretto?"*  
> Spoiler: ora lo fa. E conta pure quanti ne fai.

---

## 📱 Guarda come funziona

<div align="center">
<table>
  <tr>
    <td align="center">
      <img src="screenshots/ai_squat_detection.jpg" width="250" alt="AI Detection"/>
      <br/>
      <b>🤖 AI che conta squat</b>
      <br/>
      <i>Real-time pose detection</i>
    </td>
    <td align="center">
      <img src="screenshots/today_screen.jpg" width="250" alt="Today Screen"/>
      <br/>
      <b>📅 Sessione giornaliera</b>
      <br/>
      <i>Allenamenti organizzati</i>
    </td>
    <td align="center">
      <img src="screenshots/calendar.jpg" width="250" alt="Calendar"/>
      <br/>
      <b>🔥 Streak tracking</b>
      <br/>
      <i>Motivazione visiva</i>
    </td>
  </tr>
</table>
</div>

---

## 🎯 Cosa fa?

### 🤖 **L'AI ti guarda mentre ti alleni** (non è inquietante, giuro)
- Riconosce la tua postura in **tempo reale** (30 FPS)
- Conta **automaticamente** gli squat
- Funziona con **camera frontale o posteriore**
- **Zero cloud**, tutto on-device (privacy first)

### 📊 **Traccia tutto** (ossessivamente, ma in modo carino)
- **90+ esercizi** pronti all'uso (squat, flessioni, plank, cardio, stretching...)
- Crea **workout personalizzati** o usa i template
- **Calendario** con streak di continuità
- **Export dati** in CSV/JSON/TXT (per i data nerd)

### 🧘 **Wellness tracking** (perché non sei solo muscoli)
- Traccia **mood, sonno, stress, energia**
- **18 tracker emozionali** predefiniti
- Separato dagli allenamenti (non influenza le statistiche)

---

## 🛠️ Come è fatto?

### **Tech Stack** (quello che ho voluto imparare)

```kotlin
// 🎨 UI moderna
Jetpack Compose + Material3

// 🧠 Intelligenza Artificiale
TensorFlow Lite + MoveNet (pose detection)

// 💾 Persistenza dati
Room Database + Kotlin Coroutines

// 🏗️ Architettura pulita
MVVM + Repository Pattern

// 📱 100% Kotlin nativo
Zero XML layouts, zero Java legacy
```

### **La parte interessante** (il cuore pulsante)

- **MoveNet Lightning**: modello ML ottimizzato per mobile (~4MB)
- **GPU-accelerated**: inferenza hardware quando disponibile
- **Pose tracking**: 17 keypoints del corpo umano
- **Squat logic**: algoritmo custom per validare la forma corretta

> *"TensorFlow Lite su Android non è facile. Ma quando funziona, è magia."*

---

## 📥 Download

### **Opzione 1: APK Release**
```bash
# Scarica l'ultima versione
https://github.com/plana93/good-habits/releases

# Installa sul telefono
adb install good-habits-v1.1.0-debug.apk
```

### **Opzione 2: Build da sorgente**
```bash
git clone https://github.com/plana93/good-habits.git
cd good-habits
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

**Requisiti**: Android 7.0+ (API 24) | ~50 MB spazio | Permesso camera

---

## 🧪 Cosa ho imparato (Spoiler: tanto)

### **Computer Vision è difficile**
- Calibrare soglie per riconoscere squat "giusti" vs "sbagliati"
- Gestire angolazioni camera diverse
- Ottimizzare performance per evitare lag

### **Jetpack Compose è potente**
- UI dichiarativa cambia il paradigma
- Recomposition intelligente
- State management con StateFlow

### **ML on-device ha limiti**
- Modelli compressi perdono precisione
- GPU non sempre disponibile
- Batteria soffre (ottimizzazioni necessarie)

### **Architettura conta**
- Clean Architecture salva la vita (e il refactoring)
- Repository Pattern mantiene UI disaccoppiata
- Migrations database sono insidiose

---

## 🎓 Per chi volesse sperimentare

Questo progetto è **open source** proprio per questo. Se vuoi:

- 🔬 **Esplorare** TensorFlow Lite su Android
- 🎨 **Imparare** Jetpack Compose in un progetto reale
- 🏗️ **Studiare** Clean Architecture + MVVM
- 🤖 **Sperimentare** con Computer Vision

**Clona, modifica, rompi, ripara.** È così che si impara.

### Documentazione tecnica
- [📖 Technical Guide](update_docs/TECHNICAL_GUIDE.md) - Architettura approfondita
- [🏗️ Build Guide](update_docs/05_BUILD_DEPLOY_GUIDE.md) - Setup sviluppo
- [📋 Roadmap](update_docs/02_DEVELOPMENT_ROADMAP.md) - Prossimi step

---

## 🤝 Contributi benvenuti

Hai un'idea? Hai trovato un bug? Vuoi aggiungere un esercizio?

1. **Fork** il repo
2. **Sperimenta** nel tuo branch
3. **Proponi** una Pull Request

Nessuna formalità eccessiva, solo codice pulito e voglia di imparare.

---

## 📄 Licenza

**MIT License** - Fai quello che vuoi, ma cita la fonte 🙏

---

<div align="center">

### **⭐ Se ti incuriosisce, lascia una stella ⭐**

*Built with passion, curiosity, and too much caffeine ☕*

**Mirco** • [@plana93](https://github.com/plana93) • 2026

</div>

