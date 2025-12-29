# 👨‍💻 Good Habits App - Developer Quick Reference

**TL;DR**: Production-ready Android fitness app with AI pose detection, complete with modern architecture and comprehensive features.

---

## 🚀 Quick Setup (5 minutes)

```bash
# 1. Clone & setup
git clone <repository>
cd realtime_pose_detection_android-main

# 2. Build & install
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk

# 3. Test core feature
# Open app → Dashboard → + (FAB) → AI Squat → Camera → Start squatting
```

---

## 🏗️ Architecture at a Glance

```
UI (Compose) ←→ ViewModels ←→ Repositories ←→ Room Database
     ↓
   AI Engine (TensorFlow Lite MoveNet)
```

**Key Files**:
- `NewMainActivity.kt` - Main navigation & UI
- `TodayViewModel.kt` - Session management logic
- `DailySessionRepository.kt` - Core business logic
- `SquatCounter.kt` - AI pose detection

---

## 💾 Database Schema (Simplified)

```sql
daily_sessions {id, name, date, isCompleted}
    ↓ 1:N  
daily_session_items {id, sessionId, exerciseId?, workoutId?, customReps?, aiData?}
    ↓ N:1
exercises {id, name, mode, description}
workouts {id, name, description} ←→ exercises (N:N)
```

---

## 🔧 Key Components

### AI Detection
```kotlin
// Main AI class
SquatCounter(context) {
    fun analyzeFrame(bitmap): SquatAnalysis
    // MoveNet → keypoints → squat logic → count
}

// Usage in MainActivity
squatCounter.analyzeFrame(getBitmapFromCamera())
```

### Session Management  
```kotlin
// Add exercise to today's session
dailySessionRepository.addExerciseToTodaySession(exerciseId)

// Add AI squat with target reps
dailySessionRepository.addAISquatToTodaySession(targetReps = 20)

// Navigate between days
todayViewModel.setSelectedDate(timestamp)
```

### UI Patterns
```kotlin
// Typical screen
@Composable
fun ExampleScreen(viewModel: ExampleViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    
    when (val state = uiState) {
        is Loading -> CircularProgressIndicator()
        is Success -> Content(state.data)
        is Error -> ErrorMessage(state.message)
    }
}
```

---

## 🎨 Material3 UI Structure

```
Scaffold {
    topBar = TopAppBar(...)
    bottomBar = NavigationBar(...) // Conditional
    floatingActionButton = FAB(...) // Context-sensitive
    content = NavHost {...}
}
```

**Conditional Rendering**:
- Bottom bar: Only on dashboard/today/history
- FAB: Add icon for main screens, Settings for others

---

## 📊 Data Flow Examples

### Adding an Exercise
```
User clicks FAB → 
ExerciseLibraryActivity launched →
User selects exercise → 
Result returned to NewMainActivity →
TodayViewModel.addExercise() →
DailySessionRepository.addExerciseToTodaySession() →
Database insert →
UI auto-updates via StateFlow
```

### AI Squat Flow
```
User clicks AI Squat →
CameraSelectionActivity → 
MainActivity with AI mode →
SquatCounter analyzes frames →
Count increments →
Session saved to database
```

---

## 🐛 Common Issues & Solutions

### Build Issues
```bash
# Clean build
./gradlew clean && ./gradlew assembleDebug

# Gradle sync issues
rm -rf .gradle && ./gradlew build
```

### Database Issues
```kotlin
// Check session creation
adb logcat | grep "TODAY_DEBUG"

Logging flags
-------------
Per evitare log pesanti in produzione, i log principali possono essere attivati/disattivati a runtime tramite l'oggetto `com.programminghut.pose_detection.util.Logging`.
Di default i flag sono abilitati solo in build DEBUG (`BuildConfig.DEBUG`). Esempio:

    // Abilita i log Today anche in release (solo per debug locale)
    com.programminghut.pose_detection.util.Logging.TODAY_DEBUG = true

Usa `todayDebug(...)`, `calendarDebug(...)` e `recoveryDebug(...)` per i messaggi controllati dai flag.

// Clear app data for fresh start
adb shell pm clear com.programminghut.pose_detection
```

### AI Detection Issues
```kotlin
// Camera permissions
<uses-permission android:name="android.permission.CAMERA" />

// Check model loading
Log.d("AI", "MoveNet model loaded: ${model != null}")
```

---

## 🔍 Testing Checklist

### Core Features (5 min test)
- [ ] Dashboard loads with statistics
- [ ] Add AI Squat works (camera opens, counts)
- [ ] Add Exercise from library works  
- [ ] Calendar opens and shows days
- [ ] Export CSV generates file
- [ ] Day navigation (swipe back in time)

### Edge Cases
- [ ] No permissions granted
- [ ] Empty days (motivational quotes appear)
- [ ] Past day restrictions (no add button)
- [ ] App restart (data persists)

---

## 📈 Performance Notes

### Optimization Points
- **Compose**: Uses `remember`, `LaunchedEffect`, `derivedStateOf`
- **Database**: Indexed queries with Flow reactivity  
- **AI**: Background inference, main thread UI updates
- **Memory**: Bitmap recycling, texture cleanup

### Monitoring
```kotlin
// Useful logs
adb logcat | grep -E "(TODAY_DEBUG|UI_PERFORMANCE|DATABASE)"

// Database queries
adb shell 
cd /data/data/com.programminghut.pose_detection/databases/
sqlite3 app_database "SELECT COUNT(*) FROM daily_sessions;"
```

---

## 🔧 Development Tips

### Adding New Screens
1. Create Composable function
2. Add to Navigation in `NewMainActivity`
3. Update conditional rendering if needed
4. Create ViewModel if complex state

### Adding New Exercise Templates
```kotlin
// In DailySessionRepository.getSampleExerciseTemplateById()
ExerciseTemplate(
    id = newId, 
    name = "New Exercise",
    type = STRENGTH, 
    mode = REPS,
    defaultReps = 15
)
```

### Database Migrations
```kotlin
// In AppDatabase.kt
@Database(
    entities = [...], 
    version = newVersion,
    exportSchema = false
)
```

---

## 📋 Code Style

### Naming Conventions
- **Files**: `PascalCase.kt`
- **Functions**: `camelCase()`
- **Composables**: `PascalCase()`
- **Constants**: `UPPER_SNAKE_CASE`

### Architecture Rules
- UI should only call ViewModel functions
- ViewModels should only call Repository functions
- Repository handles all database operations
- Business logic in Repository, not ViewModel

---

## 🎯 Extension Points

### Easy Additions
```kotlin
// New exercise template
// New motivational quote
// New statistics in dashboard
// New export format
```

### Medium Complexity
```kotlin
// New AI exercise type
// Advanced chart in dashboard
// Social features (sharing)
// Workout programs
```

### High Complexity
```kotlin
// Multi-exercise AI detection
// Wearable integration
// Real-time multiplayer
// Advanced analytics
```

---

## 🔗 Key Dependencies

```gradle
// Core
implementation "androidx.compose.bom:2023.10.01"
implementation "androidx.room:room-runtime:2.5.0"
implementation "androidx.navigation:navigation-compose:2.7.5"

// AI/ML
implementation "org.tensorflow:tensorflow-lite:2.13.0"
implementation "org.tensorflow:tensorflow-lite-gpu:2.13.0"

// Architecture  
implementation "androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0"
implementation "androidx.lifecycle:lifecycle-runtime-ktx:2.7.0"
```

---

**Need help?** Check the detailed technical architecture document or the specific implementation files mentioned above. The codebase is well-documented and follows consistent patterns throughout.