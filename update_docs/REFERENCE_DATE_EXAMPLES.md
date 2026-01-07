# 📅 Reference Date Feature - Examples

**Feature**: Retroactive Wellness Tracking  
**Purpose**: Track emotions/events from past days while maintaining data integrity

---

## 🎯 Problem Statement

**User Scenario**:
> "Oggi (4 gennaio) mi sono ricordato che 2 giorni fa (2 gennaio) avevo dormito malissimo. Voglio tracciarlo nel mio wellness journal, ma voglio che i dati riflettano il giorno corretto."

**Solution**: 
Separare la **data di inserimento** (quando l'utente ha aggiunto il dato) dalla **data di riferimento** (il giorno a cui si riferisce l'evento).

---

## 📊 Data Structure

### Two Timestamps

```kotlin
data class TrackerResponse(
    // ... other fields ...
    val timestamp: Long,        // When user ENTERED this
    val referenceDate: Long     // What day this REFERS to
)
```

### Example 1: Real-time tracking (same day)

```kotlin
// User tracks mood NOW about TODAY
TrackerResponse(
    trackerId = 1,
    trackerName = "How are you feeling?",
    ratingValue = 4,
    timestamp = 1704384000000,     // Jan 4, 2026 14:00
    referenceDate = 1704384000000  // Jan 4, 2026 14:00 (same)
)
```

**CSV Output**:
```csv
Entry Date,Entry Time,Reference Date,Days Ago,Tracker Name,Value
2026-01-04,14:00,2026-01-04,0,"How are you feeling?",4
```

---

### Example 2: Retroactive tracking (past day)

```kotlin
// User tracks TODAY about 2 DAYS AGO
TrackerResponse(
    trackerId = 3,
    trackerName = "Sleep quality",
    ratingValue = 1,  // Very bad sleep
    textNote = "Couldn't sleep because of stress",
    timestamp = 1704384000000,     // Jan 4, 2026 14:00 (NOW)
    referenceDate = 1704211200000  // Jan 2, 2026 12:00 (2 days ago)
)
```

**CSV Output**:
```csv
Entry Date,Entry Time,Reference Date,Days Ago,Tracker Name,Value,Notes
2026-01-04,14:00,2026-01-02,2,"Sleep quality",1,"Couldn't sleep because of stress"
```

---

### Example 3: Multiple entries for same reference date

```kotlin
// User adds multiple trackers about yesterday, all entered today

// Entry 1: Sleep quality (yesterday)
TrackerResponse(
    timestamp = 1704384000000,     // Jan 4, 2026 14:00
    referenceDate = 1704297600000, // Jan 3, 2026 12:00 (yesterday)
    trackerId = 3,
    ratingValue = 2
)

// Entry 2: Energy level (yesterday) - added 2 hours later
TrackerResponse(
    timestamp = 1704391200000,     // Jan 4, 2026 16:00 (2 hours later)
    referenceDate = 1704297600000, // Jan 3, 2026 12:00 (same: yesterday)
    trackerId = 2,
    ratingValue = 1
)
```

**CSV Output**:
```csv
Entry Date,Entry Time,Reference Date,Days Ago,Tracker Name,Value
2026-01-04,14:00,2026-01-03,1,"Sleep quality",2
2026-01-04,16:00,2026-01-03,1,"Energy level",1
```

**Analysis**: Il CSV mostra che entrambe le entry si riferiscono a ieri, ma sono state aggiunte in momenti diversi oggi.

---

## 🎨 UI Quick Date Selector

### Quick Options (0-7 days ago)

```
ReferenceDateHelper.getQuickDateOptions(7)
```

**Returns**:
```kotlin
[
    DateOption(label = "Today",        daysAgo = 0, timestamp = 1704384000000),
    DateOption(label = "Yesterday",    daysAgo = 1, timestamp = 1704297600000),
    DateOption(label = "2 days ago",   daysAgo = 2, timestamp = 1704211200000),
    DateOption(label = "3 days ago",   daysAgo = 3, timestamp = 1704124800000),
    DateOption(label = "4 days ago",   daysAgo = 4, timestamp = 1704038400000),
    DateOption(label = "5 days ago",   daysAgo = 5, timestamp = 1703952000000),
    DateOption(label = "6 days ago",   daysAgo = 6, timestamp = 1703865600000),
    DateOption(label = "7 days ago",   daysAgo = 7, timestamp = 1703779200000)
]
```

### UI Layout

```
┌─────────────────────────────────────────────────────┐
│ How are you feeling?                                │
│                                                     │
│ When did this happen?                               │
│ ┌───────┬─────────┬─────────┬─────────┬─────────┐  │
│ │ Today │Yesterday│2 days   │3 days   │4 days   │  │
│ │   ✓   │         │ago      │ago      │ago      │  │
│ └───────┴─────────┴─────────┴─────────┴─────────┘  │
│                                                     │
│ Or pick a specific date...                          │
│ [📅 Calendar Picker]                                │
└─────────────────────────────────────────────────────┘
```

---

## 🔍 Real-World Use Cases

### Use Case 1: Evening Reflection

**Scenario**: L'utente la sera riflette sulla giornata

```kotlin
// 8 PM: User reflects on today
val response = TrackerResponse(
    trackerId = 1,
    trackerName = "How are you feeling?",
    ratingValue = 4,
    timestamp = 1704398400000,     // Jan 4, 2026 20:00
    referenceDate = 1704398400000  // Jan 4, 2026 20:00 (same day)
)
```

**CSV**: `Days Ago = 0` (today)

---

### Use Case 2: Forgot to Track Yesterday

**Scenario**: L'utente si dimentica di tracciare ieri, lo fa oggi

```kotlin
// Today: User remembers yesterday's stress
val response = TrackerResponse(
    trackerId = 4,
    trackerName = "Stress level",
    ratingValue = 5,  // Very stressed
    textNote = "Deadline at work",
    timestamp = 1704384000000,     // Jan 4, 2026 14:00 (now)
    referenceDate = 1704297600000  // Jan 3, 2026 12:00 (yesterday)
)
```

**CSV**: `Days Ago = 1` (yesterday)

---

### Use Case 3: Weekly Review

**Scenario**: Domenica l'utente riflette sull'intera settimana passata

```kotlin
// Sunday: User reviews Monday's work satisfaction
val response = TrackerResponse(
    trackerId = 5,
    trackerName = "Work satisfaction",
    ratingValue = 3,
    textNote = "Productive week start",
    timestamp = 1704556800000,     // Jan 6, 2026 (Sunday)
    referenceDate = 1704038400000  // Jan 1, 2026 (Monday, 5 days ago)
)
```

**CSV**: `Days Ago = 5` (5 days ago)

---

### Use Case 4: Unexpected Event Recall

**Scenario**: L'utente ricorda un evento inaspettato di 3 giorni fa

```kotlin
// Today: User remembers unexpected positive event from 3 days ago
val response = TrackerResponse(
    trackerId = 18,
    trackerName = "Unexpected events",
    selectedEmotion = "positive_surprise",
    textNote = "Got a call from an old friend!",
    timestamp = 1704384000000,     // Jan 4, 2026 14:00 (now)
    referenceDate = 1704124800000  // Jan 1, 2026 12:00 (3 days ago)
)
```

**CSV**: 
```csv
Entry Date,Entry Time,Reference Date,Days Ago,Tracker Name,Emotion,Notes
2026-01-04,14:00,2026-01-01,3,"Unexpected events","positive_surprise","Got a call from an old friend!"
```

---

## 📈 CSV Analysis Examples

### Example CSV Export

```csv
Entry Date,Entry Time,Reference Date,Days Ago,Tracker ID,Tracker Name,Response Type,Value,Rating (0-5),Boolean,Emotion,Notes
2026-01-04,14:00,2026-01-04,0,1,"How are you feeling?",RATING_5,4,4,,,Happy today
2026-01-04,14:05,2026-01-03,1,3,"Sleep quality",RATING_5,2,2,,,Bad sleep yesterday
2026-01-04,14:10,2026-01-02,2,2,"Energy level",RATING_5,1,1,,,Low energy 2 days ago
2026-01-04,20:00,2026-01-04,0,17,"Life satisfaction",RATING_5,5,5,,,Great day overall
2026-01-05,09:00,2026-01-05,0,3,"Sleep quality",RATING_5,5,5,,,Slept well last night
```

### Analysis Questions

1. **When was the data entered?**
   - Look at `Entry Date` and `Entry Time`
   - Shows user's tracking habits (morning vs evening)

2. **What period does it cover?**
   - Look at `Reference Date` and `Days Ago`
   - Shows if user tracks retroactively

3. **Is the user consistent?**
   - Compare `Entry Date` patterns
   - Daily entries = consistent

4. **How far back do they track?**
   - Check `Days Ago` values
   - Most should be 0-1, occasional 2-7

---

## 🛠️ Helper Functions Usage

### Get Days Ago

```kotlin
val response = TrackerResponse(
    timestamp = 1704384000000,     // Jan 4, 2026
    referenceDate = 1704211200000  // Jan 2, 2026
    // ...
)

response.getDaysAgo()  // Returns: 2
```

### Get Description

```kotlin
response.getReferenceDateDescription()  // Returns: "2 days ago"
```

### Format for Display

```kotlin
ReferenceDateHelper.formatDateOption(response.referenceDate)
// If 0 days ago: "Today"
// If 1 day ago: "Yesterday"
// If >1 day ago: "Jan 2, 2026"
```

---

## ✅ Benefits

1. **Data Integrity**: Distingue tra quando è stato inserito e quando è successo
2. **Flexibility**: Utente può tracciare eventi passati senza perdere accuratezza
3. **Analysis**: CSV permette analisi temporale dettagliata
4. **User Experience**: Quick date selector rende facile selezionare date recenti
5. **Honest Tracking**: Utente non è forzato a mentire sulla data per tracciare eventi passati

---

## 🎯 UI/UX Best Practices

### Default Behavior
- **Default to "Today"** - La maggior parte delle volte l'utente traccia il presente
- **Quick access to yesterday** - Secondo caso più comune
- **Show 7 days** - Bilancio tra utilità e semplicità

### Visual Feedback
```
"Added today about yesterday's sleep"
"Added today about how you feel right now"
"Added today about an event from 3 days ago"
```

### Validation
- ❌ Non permettere date future
- ❌ Non permettere date più vecchie di X giorni (es. 30 giorni)
- ✅ Mostrare avviso se >7 giorni fa

---

**Status**: ✅ Feature Complete  
**Next**: Implementare UI con date selector
