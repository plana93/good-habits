# 🔧 Good Habits - Technical Guide

**Last Updated**: Gennaio 2026  
**Architecture**: Clean Architecture + MVVM  
**Language**: Kotlin 100%  
**UI Framework**: Jetpack Compose  
**Dependency Injection**: Manual DI with Factory Pattern

---

## 🚀 Quick Start (5 minutes)

```bash
# 1. Clone & setup
cd realtime_pose_detection_android-main

# 2. Build & install
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk

# 3. Test core feature
# Open app → Dashboard → + (FAB) → AI Squat → Camera → Start squatting
```

---

## 🏗️ Architecture Overview

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

### Key Architectural Principles

- **Single Source of Truth**: Room Database + Flows
- **Reactive Programming**: StateFlow/Flow per aggiornamenti UI
- **Manual Dependency Injection**: Factory pattern per ViewModels
- **Separation of Concerns**: Clear boundaries tra layers

---

## 📦 Data Layer

### Repository Pattern

**4 Repository principali**:

1. **SessionRepository** - Workout sessions (AI squat sessions)
2. **DailySessionRepository** - Daily modular sessions (exercises/workouts)
3. **ExerciseRepository** - Exercise templates
4. **WorkoutRepository** - Workout templates

```kotlin
// Repository pattern structure
class SessionRepository(
    private val sessionDao: SessionDao,
    private val repDao: RepDao,
    private val dailySessionDao: DailySessionDao? = null  // For calendar integration
) {
    fun getAllSessions(): Flow<List<WorkoutSession>> 
    suspend fun insertSession(session: WorkoutSession): Long
    // ... business logic
}
```

### Database Schema (Room - Version 10)

#### Core Tables

```sql
-- Daily sessions (modular system)
daily_sessions {
    sessionId: Long PK
    name: String
    date: Long (timestamp)
    startTime: Long?
    endTime: Long?
    isCompleted: Boolean
    createdAt: Long
}

-- Session items (exercises/workouts/wellness trackers)
daily_session_items {
    itemId: Long PK
    sessionId: Long FK
    order: Int
    itemType: SessionItemType (EXERCISE/WORKOUT/WELLNESS_TRACKER)
    exerciseId: Long? FK
    workoutId: Long? FK
    trackerTemplateId: Int?         -- ✅ For wellness tracking
    trackerResponseJson: String?    -- ✅ JSON response data
    customReps: Int?
    customTime: Int?
    actualReps: Int?
    actualTime: Int?
    isCompleted: Boolean
    completedAt: Long?
    parentWorkoutItemId: Long? FK (self-reference)
    notes: String?
    aiData: String? (JSON for AI squat data)
    countsAsActivity: Boolean DEFAULT 1  -- ✅ false for wellness trackers
}

-- Workout sessions (legacy AI squat system)
workout_sessions {
    sessionId: Long PK
    startTime: Long
    endTime: Long
    totalReps: Int
    avgFormScore: Float
    sessionType: String (MANUAL/RECOVERY/AI)
    affectsStreak: Boolean
}

-- Exercise templates
exercises {
    exerciseId: Long PK
    name: String
    description: String
    mode: ExerciseMode (REPS/TIME/TIME_HOLD)
    defaultReps: Int?
    defaultTime: Int?
    defaultSets: Int?
    defaultRest: Int?
}

-- Workout templates
workouts {
    workoutId: Long PK
    name: String
    description: String
}

-- Workout-Exercise junction
workout_exercises {
    id: Long PK
    workoutId: Long FK
    exerciseId: Long FK
    orderInWorkout: Int
}
```

### Key Database Features

#### 1. Dual System Architecture
- **Legacy System**: `workout_sessions` + `rep_data` (AI squat tracking)
- **Modern System**: `daily_sessions` + `daily_session_items` (modular tracking)
- **Integration**: Both systems work together for calendar/streak calculation

#### 2. Wellness Tracking Separation
```sql
-- Query for calendar: ONLY physical activities
SELECT DISTINCT date FROM daily_sessions s
JOIN daily_session_items i ON s.sessionId = i.sessionId
WHERE i.countsAsActivity = 1  -- ✅ Excludes wellness trackers
```

#### 3. Cascade Behaviors
- Delete `daily_session` → Cascade delete all `daily_session_items`
- Delete `workout` → Update references in `workout_exercises`
- Delete `exercise` → Soft delete (keep historical data)

---

## 💻 Domain Layer

### ViewModels (8+ Active)

```kotlin
// ViewModel structure with Factory
class TodayViewModel(
    private val dailySessionRepository: DailySessionRepository,
    private val sessionRepository: SessionRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<TodayUiState>(TodayUiState.Loading)
    val uiState: StateFlow<TodayUiState> = _uiState.asStateFlow()
    
    // Business logic...
}

// Factory pattern for manual DI
class TodayViewModelFactory(
    private val dailyRepo: DailySessionRepository,
    private val sessionRepo: SessionRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TodayViewModel::class.java)) {
            return TodayViewModel(dailyRepo, sessionRepo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
```

**ViewModels Implementati**:
1. `TodayViewModel` - Daily session management + temporal navigation
2. `DashboardViewModel` - Statistics + KPIs
3. `CalendarViewModel` - Calendar + streak + recovery system
4. `ExportViewModel` - CSV/JSON/TXT export
5. `SessionsViewModel` - Session history
6. `SessionDetailViewModel` - Single session details
7. `WorkoutTrackingViewModel` - Workout execution tracking
8. `WorkoutCreationViewModel` - Workout builder
9. `ManualSessionViewModel` - Manual session entry

### UI State Pattern

```kotlin
sealed class TodayUiState {
    object Loading : TodayUiState()
    data class Success(
        val session: DailySessionWithItems,
        val items: List<DailySessionItemWithDetails>,
        val dayStatus: DayStatus
    ) : TodayUiState()
    data class Error(val message: String) : TodayUiState()
}
```

---

## 🎨 UI Layer (Jetpack Compose)

### Main Activity Structure

```kotlin
NewMainActivity {
    ├── Scaffold (Bottom Bar + Central FAB)
    ├── NavHost (Conditional Rendering)
    │   ├── DashboardScreen (Statistics + Quick Actions)
    │   ├── TodayScreen (Daily Session + HorizontalPager)
    │   ├── ExercisesScreen (Template Library)
    │   ├── WorkoutsScreen (Workout Templates)
    │   └── HistoryScreen (Session History)
    └── Dialogs
        ├── CalendarDialog (Full-screen calendar)
        ├── ExportDialog (Export options)
        └── RecoveryDialog (Recovery confirmation)
}
```

### Navigation System

- **Type**: Navigation Compose
- **Routes**: `"dashboard"`, `"today"`, `"exercises"`, `"workouts"`, `"history"`
- **Conditional UI**: Bottom bar e FAB cambiano in base alla route
- **State Management**: `NavController` + `currentBackStackEntry`

```kotlin
// Conditional bottom bar
val screensWithBottomBar = setOf("dashboard", "today", "history")
val showBottomBar = currentRoute in screensWithBottomBar

// Contextual FAB
val fabIcon = when (currentRoute) {
    "dashboard" -> Icons.Default.FitnessCenter  // AI Squat
    "today" -> Icons.Default.Add                // Add item
    "exercises" -> Icons.Default.Add            // Create exercise
    "workouts" -> Icons.Default.Add             // Create workout
    else -> null
}
```

### Material3 Components

- **Design System**: Full Material3 adoption
- **Dynamic Theming**: Automatic color adaptation
- **Typography**: Consistent hierarchy
- **Components**: Card, FAB, Dialog, Surface, LazyVerticalGrid, HorizontalPager

---

## 🤖 AI Detection System

### MoveNet Integration

```kotlin
class SquatCounter(context: Context) {
    private val interpreter: Interpreter  // TensorFlow Lite
    private val model = MoveNet.create(
        context,
        device = MoveNet.Device.GPU,
        modelType = MoveNet.ModelType.Lightning  // Optimized for mobile
    )
    
    fun analyzeFrame(bitmap: Bitmap): SquatAnalysis {
        // 1. Detect pose keypoints
        val person = model.estimateSinglePose(bitmap)
        
        // 2. Extract relevant keypoints
        val leftHip = person.keyPoints[11]
        val rightHip = person.keyPoints[12]
        val leftKnee = person.keyPoints[13]
        val rightKnee = person.keyPoints[14]
        
        // 3. Calculate angles and position
        val squatDepth = calculateSquatDepth(hips, knees)
        
        // 4. State machine for rep counting
        return updateSquatState(squatDepth)
    }
}
```

**Keypoints Used**:
- **Hips** (11, 12): Position reference
- **Knees** (13, 14): Bend angle calculation
- **Ankles** (15, 16): Stability check

**Calibration**: Auto-adapts to user's starting position

---

## 📊 Key Business Logic

### Streak Calculation

```kotlin
// Hybrid streak: considers BOTH systems
suspend fun calculateStreakWithDailySessions(
    dailySummaries: Map<Long, DailySessionDaySummary>
): Int {
    var streak = 0
    var currentDay = getStartOfDay(System.currentTimeMillis())
    
    while (true) {
        val dayEnd = currentDay + oneDayMillis
        
        // Check old system (workout_sessions)
        val hasWorkoutSessions = sessionDao.hasSessionsForDay(currentDay, dayEnd)
        
        // Check new system (daily_session_items with countsAsActivity=true)
        val hasDailySummary = dailySummaries[currentDay]?.let { it.itemCount > 0 } ?: false
        
        if (hasWorkoutSessions || hasDailySummary) {
            streak++
            currentDay -= oneDayMillis
        } else {
            break
        }
    }
    
    return streak
}
```

### Recovery System

- **Trigger**: Tap on missed day in calendar
- **Action**: Complete 20 AI squats
- **Effect**: Day marked as RECOVERED (distinct from normal completion)
- **Tracking**: `aiData = "squat_recovery"` marker in database
- **Streak**: Recovered days contribute to streak

### Missed Days Logic (✅ FIXED)

```kotlin
// ✅ Now checks BOTH systems for correct calendar display
suspend fun getMissedDays(startDate: Long, endDate: Long): List<Long> {
    val missedDays = mutableListOf<Long>()
    
    var currentDay = startDate
    while (currentDay <= endDate) {
        val dayEnd = currentDay + oneDayMillis
        
        // Check BOTH systems:
        val hasWorkoutSessions = sessionDao.hasSessionsForDay(currentDay, dayEnd)
        val hasDailyActivities = dailySessionDao?.hasPhysicalActivityForDay(currentDay, dayEnd) ?: false
        
        val hasSessions = hasWorkoutSessions || hasDailyActivities
        
        if (!hasSessions && currentDay < System.currentTimeMillis()) {
            missedDays.add(currentDay)
        }
        
        currentDay += oneDayMillis
    }
    
    return missedDays
}
```

---

## 🧪 Testing

### Unit Tests

```kotlin
// Example: CalendarViewModel test
@Test
fun testStreakCalculationWithDailySessions() {
    val dailySummary = DailySessionDaySummary(
        date = targetDay,
        itemCount = 1,
        completedCount = 1,
        totalReps = 10
    )
    
    val dayDataMap = buildDayDataMapWithDailySummariesImpl(
        sessions = emptyList(),
        missedDays = listOf(),
        dailySummaries = mapOf(targetDay to dailySummary)
    )
    
    assertEquals(DayStatus.COMPLETED_DAILY, dayDataMap[targetDay]?.status)
}
```

### Test Coverage

- ✅ Database migration tests
- ✅ ViewModel unit tests (CalendarViewModel, others)
- ✅ Repository integration tests
- ⏳ UI tests (Compose) - Partially implemented

---

## 🔧 Common Development Tasks

### 1. Add New Exercise Template

```kotlin
// In JSON: /assets/exercise_templates.json
{
  "id": 99,
  "name": "Plank",
  "description": "Core stability exercise",
  "mode": "TIME_HOLD",
  "defaultTime": 60,
  "category": "core"
}
```

### 2. Add New Database Column

```kotlin
// 1. Update entity
@Entity
data class DailySessionItem(
    // ...
    val newField: String? = null  // ✅ Always nullable for migrations
)

// 2. Create migration
val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE daily_session_items ADD COLUMN newField TEXT")
    }
}

// 3. Register in AppDatabase
@Database(version = 11, entities = [...])
abstract class AppDatabase : RoomDatabase() {
    companion object {
        private fun buildDatabase(context: Context) = Room.databaseBuilder(...)
            .addMigrations(MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11)  // ✅
            .build()
    }
}
```

### 3. Create New ViewModel

```kotlin
// 1. Create ViewModel
class MyViewModel(
    private val repository: MyRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow<MyUiState>(MyUiState.Loading)
    val uiState: StateFlow<MyUiState> = _uiState.asStateFlow()
    
    fun loadData() {
        viewModelScope.launch {
            repository.getData().collect { data ->
                _uiState.value = MyUiState.Success(data)
            }
        }
    }
}

// 2. Create Factory
class MyViewModelFactory(
    private val repository: MyRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MyViewModel::class.java)) {
            return MyViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

// 3. Use in Activity/Composable
val database = AppDatabase.getDatabase(context)
val repository = MyRepository(database.myDao())
val factory = MyViewModelFactory(repository)
val viewModel: MyViewModel = viewModel(factory = factory)
```

---

## 📚 Key File Locations

### Data Layer
- **DAOs**: `/app/src/main/java/com/programminghut/pose_detection/data/dao/`
- **Entities**: `/app/src/main/java/com/programminghut/pose_detection/data/model/`
- **Repositories**: `/app/src/main/java/com/programminghut/pose_detection/data/repository/`
- **Database**: `/app/src/main/java/com/programminghut/pose_detection/data/database/AppDatabase.kt`

### Domain Layer
- **ViewModels**: `/app/src/main/java/com/programminghut/pose_detection/ui/*/`
- **Business Logic**: Primarily in Repositories and ViewModels

### UI Layer
- **Main Activity**: `/app/src/main/java/com/programminghut/pose_detection/ui/activity/NewMainActivity.kt`
- **Screens**: Inline in `NewMainActivity.kt` as Composables
- **Components**: `/app/src/main/java/com/programminghut/pose_detection/ui/components/`

### AI Layer
- **SquatCounter**: `/app/src/main/java/com/programminghut/pose_detection/SquatCounter.kt`
- **MoveNet**: `/app/src/main/java/com/programminghut/pose_detection/ml/`

### Assets
- **Exercise Templates**: `/app/src/main/assets/exercise_templates.json`
- **Workout Templates**: `/app/src/main/assets/workout_templates.json`
- **Wellness Templates**: `/app/src/main/assets/wellness_tracker_templates.json`
- **Motivational Quotes**: `/app/src/main/assets/motivational_quotes.json`

---

## ⚡ Performance Considerations

### Database Optimization
- **Indices**: Auto-created on FKs and PKs
- **Flows**: Reactive updates minimize unnecessary queries
- **Transactions**: Use `@Transaction` for multi-step operations

### Compose Recomposition
```kotlin
// Good: Stable data classes
@Immutable
data class ExerciseTemplate(...)

// Good: remember expensive operations
val processedData = remember(key) { expensiveOperation() }

// Good: derivedStateOf for calculations
val completedCount by remember {
    derivedStateOf { items.count { it.isCompleted } }
}
```

### AI Performance
- **Model**: MoveNet Lightning (optimized for mobile)
- **GPU Acceleration**: Enabled when available
- **Frame Processing**: ~30 FPS target
- **Memory**: Bitmap recycling to avoid leaks

---

## 🐛 Known Issues & Solutions

### Issue: Streak Resets Unexpectedly
**Cause**: Timezone inconsistencies in date calculations  
**Solution**: Always use `getStartOfDay()` with normalized timestamps

### Issue: Wellness Trackers Show in Calendar ✅ FIXED
**Cause**: `getMissedDays()` only checked `workout_sessions`  
**Solution**: Now checks both systems with `hasPhysicalActivityForDay()`

### Issue: Calendar Navigation Loop
**Cause**: Circular updates between Pager and ViewModel  
**Solution**: Flag-based loop prevention with auto-reset timer

---

## 📖 Further Reading

- **App Overview**: `00_APP_OVERVIEW.md`
- **Roadmap**: `02_DEVELOPMENT_ROADMAP.md`
- **Build Guide**: `05_BUILD_DEPLOY_GUIDE.md`
- **Features**: `FEATURES_REFERENCE.md`
- **Wellness Tracking**: `WELLNESS_TRACKING.md`

---

*Documentazione tecnica completa per Good Habits app - Un sistema fitness moderno con AI, tracking modulare e architettura scalabile.*
