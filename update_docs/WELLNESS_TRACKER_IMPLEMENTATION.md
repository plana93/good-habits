# 🧘 Wellness Tracker Implementation

**Created**: 2026-01-04  
**Database Version**: 8 → 9  
**Feature**: Mood, wellness, and life quality tracking system

---

## 📋 Overview

Il **WellnessTracker** permette agli utenti di tracciare aspetti della vita quotidiana oltre all'attività fisica:
- **Mood tracking** (come ti senti)
- **Energy levels** (livelli di energia)
- **Sleep quality** (qualità del sonno)
- **Work satisfaction** (soddisfazione lavorativa)
- **Social connections** (relazioni familiari/sociali)
- **Unexpected events** (eventi imprevisti)
- E molti altri...

### Key Features

✅ **Separate from physical activity**: `countsAsActivity = false`  
✅ **Multiple response types**: Rating 0-5, Boolean, Emotion sets, Text notes  
✅ **Multiple entries per day**: Tracciabili più volte al giorno  
✅ **Optional tracking**: Non obbligatorio, non ha promemoria  
✅ **CSV export**: Esportazione completa con timestamp per analisi esterna  
✅ **Emoticon + text**: Interfaccia intuitiva con emoticon e testo descrittivo  
✅ **English templates**: Tutti i template in inglese  
✅ **Notes support**: Sempre possibile aggiungere note personali  

---

## 🏗️ Architecture

### 1. Database Schema

#### Updated `daily_session_items` Table (Version 9)

```sql
CREATE TABLE daily_session_items (
    itemId INTEGER PRIMARY KEY AUTOINCREMENT,
    sessionId INTEGER NOT NULL,
    `order` INTEGER NOT NULL,
    itemType TEXT NOT NULL,  -- 'EXERCISE', 'WORKOUT', 'WELLNESS_TRACKER'
    exerciseId INTEGER,
    workoutId INTEGER,
    trackerTemplateId INTEGER,      -- ✅ NEW: ID from JSON template
    trackerResponseJson TEXT,        -- ✅ NEW: TrackerResponse as JSON
    customReps INTEGER,
    customTime INTEGER,
    customSets INTEGER,
    customRest INTEGER,
    isCompleted INTEGER NOT NULL DEFAULT 0,
    actualReps INTEGER,
    actualTime INTEGER,
    completedAt INTEGER,
    notes TEXT NOT NULL DEFAULT '',
    parentWorkoutItemId INTEGER,
    aiData TEXT,
    countsAsActivity INTEGER NOT NULL DEFAULT 1,  -- FALSE for wellness trackers
    FOREIGN KEY(sessionId) REFERENCES daily_sessions(sessionId) ON DELETE CASCADE
);
```

#### Migration 8 → 9

```kotlin
private val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            "ALTER TABLE daily_session_items ADD COLUMN trackerTemplateId INTEGER"
        )
        database.execSQL(
            "ALTER TABLE daily_session_items ADD COLUMN trackerResponseJson TEXT"
        )
    }
}
```

---

## 📦 Data Models

### TrackerResponseType Enum

```kotlin
enum class TrackerResponseType {
    RATING_5,      // 0-5 rating scale (6 levels)
    BOOLEAN,       // Yes/No (true/false)
    EMOTION_SET,   // Multiple emotion options
    TEXT_NOTE      // Free text entry (like gratitude journal)
}
```

### WellnessTrackerTemplate

```kotlin
data class WellnessTrackerTemplate(
    val id: Int,
    val category: String,
    val name: String,
    val description: String,
    val responseType: TrackerResponseType,
    val iconName: String,
    val color: String,
    val emoticons: List<String>?,
    val labels: List<String>?,
    val customEmotions: List<CustomEmotion>?,
    val invertedRating: Boolean = false,  // For stress (lower is better)
    val isPredefined: Boolean = true
)
```

### TrackerResponse (Stored as JSON)

```kotlin
data class TrackerResponse(
    val trackerId: Int,
    val trackerName: String,
    val responseType: TrackerResponseType,
    val ratingValue: Int? = null,        // 0-5 for RATING_5
    val booleanValue: Boolean? = null,   // true/false for BOOLEAN
    val selectedEmotion: String? = null, // emotion ID for EMOTION_SET
    val textNote: String? = null,        // text for TEXT_NOTE or additional notes
    val timestamp: Long,                 // ✅ When user ENTERED this data (now)
    val referenceDate: Long              // ✅ Which day this REFERS to (can be past)
)
```

#### 📅 Date Tracking Feature

**Important**: Il sistema distingue tra due date diverse:

1. **timestamp** (Entry Date): Quando l'utente ha aggiunto questo tracker
   - Esempio: `2026-01-04 20:00` (oggi sera)
   
2. **referenceDate** (Reference Date): A quale giorno si riferisce l'emozione/evento
   - Esempio: `2026-01-02 12:00` (2 giorni fa)

**Use Case**: L'utente oggi (4 gennaio) ricorda che 2 giorni fa (2 gennaio) aveva dormito male. Può tracciare "Sleep quality = 2" con:
- `timestamp = 2026-01-04 20:00` (quando lo ha inserito)
- `referenceDate = 2026-01-02 12:00` (il giorno a cui si riferisce)

Nel CSV apparirà:
```csv
Entry Date,Entry Time,Reference Date,Days Ago,...
2026-01-04,20:00,2026-01-02,2,...
```

**Helper Functions**:
```kotlin
response.getDaysAgo()                    // Returns: 2
response.getReferenceDateDescription()   // Returns: "2 days ago"
```

---

## 📄 JSON Template Structure

**File**: `app/src/main/assets/wellness_tracker_templates.json`

### Template Examples

#### Rating Type (0-5)
```json
{
  "id": 1,
  "category": "feeling",
  "name": "How are you feeling?",
  "description": "Track your general mood",
  "responseType": "RATING_5",
  "iconName": "EmojiEmotions",
  "color": "#FFB74D",
  "emoticons": ["😢", "😟", "😐", "🙂", "😊", "😄"],
  "labels": ["Very sad", "Sad", "Neutral", "Good", "Happy", "Very happy"],
  "isPredefined": true
}
```

#### Emotion Set Type
```json
{
  "id": 18,
  "category": "unexpected",
  "name": "Unexpected events",
  "description": "Did something unexpected happen today?",
  "responseType": "EMOTION_SET",
  "iconName": "Lightbulb",
  "color": "#FF9800",
  "customEmotions": [
    {"emotion": "positive_surprise", "emoticon": "🎉", "label": "Positive surprise"},
    {"emotion": "challenge", "emoticon": "⚡", "label": "Challenge"},
    {"emotion": "opportunity", "emoticon": "🚀", "label": "Opportunity"},
    {"emotion": "setback", "emoticon": "🌧️", "label": "Setback"},
    {"emotion": "neutral_change", "emoticon": "🔄", "label": "Just different"}
  ],
  "isPredefined": true
}
```

#### Text Note Type
```json
{
  "id": 15,
  "category": "gratitude",
  "name": "Gratitude",
  "description": "What are you grateful for today?",
  "responseType": "TEXT_NOTE",
  "iconName": "FavoriteBorder",
  "color": "#E91E63",
  "emoticons": ["🙏"],
  "isPredefined": true
}
```

### All Available Trackers (18 total)

| ID | Category | Name | Type | Description |
|----|----------|------|------|-------------|
| 1 | feeling | How are you feeling? | RATING_5 | General mood |
| 2 | energy | Energy level | RATING_5 | Physical energy |
| 3 | sleep | Sleep quality | RATING_5 | Sleep satisfaction |
| 4 | stress | Stress level | RATING_5 | Stress (inverted) |
| 5 | work | Work satisfaction | RATING_5 | Job satisfaction |
| 6 | focus | Focus & concentration | RATING_5 | Mental focus |
| 7 | family | Family connection | RATING_5 | Family time quality |
| 8 | friends | Social connection | RATING_5 | Social interactions |
| 9 | love | Relationship | RATING_5 | Romantic relationship |
| 10 | me_time | Self-care time | RATING_5 | Personal time |
| 11 | diet | Nutrition quality | RATING_5 | Diet quality |
| 12 | learning | Learning & growth | RATING_5 | New knowledge |
| 13 | art | Creative expression | RATING_5 | Creativity |
| 14 | health | Physical health | RATING_5 | Body feeling |
| 15 | gratitude | Gratitude | TEXT_NOTE | Gratitude journal |
| 16 | finance | Financial wellness | RATING_5 | Finance feelings |
| 17 | life | Life satisfaction | RATING_5 | Overall life quality |
| 18 | unexpected | Unexpected events | EMOTION_SET | Surprising events |

---

## 🔧 File Manager

**Class**: `WellnessTrackerFileManager`

```kotlin
class WellnessTrackerFileManager(context: Context) {
    fun loadTrackers(): WellnessTrackerData
    fun getAllTrackers(): List<WellnessTrackerTemplate>
    fun getTrackerById(id: Int): WellnessTrackerTemplate?
    fun getTrackersByCategory(category: String): List<WellnessTrackerTemplate>
    fun getAllCategories(): List<TrackerCategory>
    fun searchTrackers(query: String): List<WellnessTrackerTemplate>
}
```

**Usage**:
```kotlin
val fileManager = WellnessTrackerFileManager(context)
val allTrackers = fileManager.getAllTrackers()
val moodTracker = fileManager.getTrackerById(1)
```

---

## 💾 CSV Export

### Export Function

```kotlin
fun generateWellnessTrackerCSV(
    trackerEntries: List<DailySessionItemWithDetails>
): String
```

### CSV Format

```csv
Entry Date,Entry Time,Reference Date,Days Ago,Tracker ID,Tracker Name,Response Type,Value,Rating (0-5),Boolean,Emotion,Notes
2026-01-04,14:30,2026-01-04,0,1,"How are you feeling?",RATING_5,4,4,,,
2026-01-04,14:32,2026-01-03,1,2,"Energy level",RATING_5,3,3,,,Low energy yesterday
2026-01-04,14:35,2026-01-04,0,15,"Gratitude",TEXT_NOTE,"Family time",,,,Family time
2026-01-04,20:15,2026-01-02,2,18,"Unexpected events",EMOTION_SET,"positive_surprise",,,positive_surprise,Got promoted 2 days ago!
```

**Column Descriptions**:
- **Entry Date/Time**: Quando l'utente ha inserito il dato (oggi)
- **Reference Date**: Il giorno a cui si riferisce l'evento/emozione
- **Days Ago**: Quanti giorni indietro rispetto alla data di inserimento
- **Value**: Il valore principale della risposta
- **Rating/Boolean/Emotion**: Valori specifici per tipo
- **Notes**: Note aggiuntive dell'utente

### Usage in Export

```kotlin
// In ExportViewModel or Repository
suspend fun exportWellnessData(): String {
    val trackerItems = dailySessionDao.getAllSessionItems()
        .filter { it.type == "WELLNESS_TRACKER" }
    return ShareHelper.generateWellnessTrackerCSV(trackerItems)
}
```

---

## 🎨 UI Components (To Be Implemented)

### Planned Components

1. **WellnessTrackerList** - Lista trackers disponibili
2. **TrackerCard** - Card singolo tracker
3. **RatingBar** - Input rating 0-5 con emoticon
4. **EmotionPicker** - Selezione emotion set
5. **ReferenceDatePicker** ⭐ - Selezione rapida data (Today, Yesterday, 2 days ago, ecc.)
6. **WellnessSection** - Sezione separata in Today screen
7. **WellnessHistoryScreen** - Storico tracker completati

### Reference Date Picker UI

```
┌─────────────────────────────────────┐
│  How are you feeling?               │
├─────────────────────────────────────┤
│  When did this happen?              │
│  ┌─────┬─────────┬────────┬───────┐ │
│  │Today│Yesterday│2 days  │3 days │ │
│  │  ✓  │         │ago     │ago    │ │
│  └─────┴─────────┴────────┴───────┘ │
│  [Pick different date...]           │
├─────────────────────────────────────┤
│  How was it?                        │
│  😢  😟  😐  🙂  😊  😄             │
│           (selected: 🙂)            │
├─────────────────────────────────────┤
│  Notes (optional)                   │
│  [Great day with family!          ] │
├─────────────────────────────────────┤
│            [Cancel] [Save]          │
└─────────────────────────────────────┘
```

### Today Screen Layout

```
┌─────────────────────────────────────┐
│  Today - January 4, 2026            │
├─────────────────────────────────────┤
│  💪 Physical Activities             │
│  ☐ Push-ups (20 reps)               │
│  ☐ Squat (3 sets)                   │
├─────────────────────────────────────┤
│  🧘 Wellness Check-in (Optional)    │
│  ☐ How are you feeling?             │
│  ☐ Energy level                     │
│  ☐ Sleep quality                    │
│  [+ Add more trackers]              │
└─────────────────────────────────────┘
```

---

## 📊 How It Works

### 1. Adding a Wellness Tracker to Today (with past reference date)

```kotlin
// User wants to track how they felt 2 days ago
val referenceDateHelper = ReferenceDateHelper
val twoDaysAgo = referenceDateHelper.getTimestampForDaysAgo(2)

// Create TrackerResponse
val response = TrackerResponse(
    trackerId = 1,
    trackerName = "How are you feeling?",
    responseType = TrackerResponseType.RATING_5,
    ratingValue = 2,
    textNote = "Was stressed about work",
    timestamp = System.currentTimeMillis(),  // ✅ NOW (when user entered it)
    referenceDate = twoDaysAgo              // ✅ 2 days ago (what it refers to)
)

// Create DailySessionItem
val item = DailySessionItem(
    sessionId = todaySessionId,
    order = nextOrder,
    itemType = SessionItemType.WELLNESS_TRACKER,
    trackerTemplateId = 1,
    trackerResponseJson = response.toJson(),
    countsAsActivity = false,
    isCompleted = false
)

dailySessionDao.insertItem(item)
```

### 2. Completing a Wellness Tracker

```kotlin
// User fills out the tracker
val updatedResponse = response.copy(
    ratingValue = 5,
    textNote = "Amazing day with family!"
)

val completedItem = item.copy(
    trackerResponseJson = updatedResponse.toJson(),
    isCompleted = true,
    completedAt = System.currentTimeMillis()
)

dailySessionDao.updateItem(completedItem)
```

### 3. Querying Wellness Data

```kotlin
// Get all wellness trackers for a session
val wellnessItems = dailySessionRelationDao
    .getSessionItemsWithDetails(sessionId)
    .filter { it.type == "WELLNESS_TRACKER" }

// Parse responses
val responses = wellnessItems.mapNotNull { item ->
    TrackerResponse.fromJson(item.trackerResponseJson ?: "")
}
```

---

## 🔍 Key Differences from Physical Activities

| Aspect | Physical Activity | Wellness Tracker |
|--------|------------------|------------------|
| `countsAsActivity` | `true` | `false` |
| Calendar visualization | ✅ Yes | ❌ No |
| Affects streak | ✅ Yes | ❌ No |
| Multiple per day | ✅ Yes | ✅ Yes |
| Mandatory | Optional | Optional |
| Export | CSV | Separate CSV |
| Template source | JSON files | JSON file |
| ItemType enum | EXERCISE/WORKOUT | WELLNESS_TRACKER |

---

## 🚀 Next Steps (UI Implementation)

1. ✅ Database schema updated (v9)
2. ✅ Data models created
3. ✅ JSON templates created (18 trackers)
4. ✅ File manager implemented
5. ✅ CSV export function added
6. ⏳ **Create UI components** (WellnessTrackerCard, RatingBar, etc.)
7. ⏳ **Update Today screen** (add separate wellness section)
8. ⏳ **Implement tracker selection** (modal/dialog per scegliere tracker)
9. ⏳ **Implement response input** (rating bar, emotion picker)
10. ⏳ **Add export button** for wellness data in Export screen
11. ⏳ **Testing** end-to-end flow

---

## 📝 Notes

- **No calendar integration**: Wellness trackers non appaiono nel calendario
- **Timestamp is critical**: `completedAt` timestamp è essenziale per CSV export
- **JSON flexibility**: TrackerResponse in JSON permette estensibilità futura
- **Template-driven**: Tutti i trackers sono definiti in JSON, facile da aggiornare
- **Privacy**: Note possono contenere dati sensibili, gestire con cautela

---

## 🎯 User Requirements Met

✅ Sezione separata nella Today screen  
✅ Domande in inglese  
✅ 18 template suggeriti (feeling, energy, sleep, stress, work, focus, family, friends, love, me_time, diet, learning, art, health, gratitude, finance, life, unexpected)  
✅ Nessuna visualizzazione nel calendario  
✅ CSV export completo con data e tempo  
✅ Opzionale (no promemoria)  
✅ Multipli inserimenti al giorno  
✅ Emoticon + testo quando necessario  
✅ Note sempre disponibili  
✅ Voce "Unexpected events" inclusa  
✅ Interfaccia semplice per votare e aggiungere  

---

**Status**: ✅ Backend Complete | ⏳ UI Pending  
**Next Action**: Implementare componenti UI e integrazione con Today screen
