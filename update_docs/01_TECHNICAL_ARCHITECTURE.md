# 🔧 Good Habits App - Technical Architecture

**Last Updated**: December 2024  
**Architecture**: Clean Architecture + MVVM  
**Language**: Kotlin 100%  
**UI Framework**: Jetpack Compose

---

## 🏗️ Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────┐
│                        GOOD HABITS APP                              │
│                     (Clean Architecture)                            │
└─────────────────────────────┬───────────────────────────────────────┘
                              │
        ┌─────────────────────┼─────────────────────┐
        │                     │                     │
    ┌───▼────┐        ┌──────▼──────┐        ┌────▼─────┐
    │   UI   │        │  DOMAIN     │        │   DATA   │
    │ Layer  │        │   Layer     │        │  Layer   │
    └───┬────┘        └──────┬──────┘        └────┬─────┘
        │                    │                    │
    Compose             ViewModels          Repository
    Screens             UseCases            + Database
```

---

## 📱 UI Layer (Jetpack Compose)

### Activity Architecture
```kotlin
NewMainActivity {
    ├── Navigation Controller (Conditional Rendering)
    ├── Scaffold (Bottom Bar + FAB)
    ├── DashboardScreen (Statistics + Quick Actions)
    ├── TodayScreen (Daily Session + Horizontal Pager)
    ├── ExercisesScreen (Template Library)
    ├── WorkoutsScreen (Workout Templates) 
    └── HistoryScreen (Session History)
}
```

### Navigation System
- **Type**: Navigation Compose
- **Route-based**: Conditional UI rendering per route
- **State Management**: NavController + currentBackStackEntry
- **Conditional Elements**:
  ```kotlin
  screensWithBottomBar = setOf("dashboard", "today", "history")
  screensWithAddFAB = setOf("dashboard", "today", "exercises", "workouts")
  ```

### Material3 Integration
- **Design System**: Full Material3 components
- **Dynamic Theming**: Automatic color adaptation
- **Components Used**:
  - Scaffold, Card, FloatingActionButton
  - Dialog, Surface, LazyVerticalGrid
  - NavigationBar, TopAppBar

---

## 💾 Data Layer

### Database Schema (Room)

#### Core Tables
```sql
-- Sessioni giornaliere
daily_sessions {
    sessionId: Long PK
    name: String
    date: Long (timestamp)
    startTime: Long?
    endTime: Long?
    isCompleted: Boolean
    createdAt: Long
}

-- Elementi sessione (esercizi/workouts)
daily_session_items {
    itemId: Long PK
    sessionId: Long FK
    order: Int
    itemType: SessionItemType (EXERCISE/WORKOUT)
    exerciseId: Long? FK
    workoutId: Long? FK
    customReps: Int?
    customTime: Int?
    actualReps: Int?
    actualTime: Int?
    isCompleted: Boolean
    completedAt: Long?
    parentWorkoutItemId: Long? FK (self)
    notes: String?
    aiData: String? (JSON per AI squat)
}

-- Template esercizi
exercises {
    exerciseId: Long PK
    name: String
    type: ExerciseType
    description: String
    mode: ExerciseMode (REPS/TIME)
    imagePath: String?
    createdAt: Long
    modifiedAt: Long
    isCustom: Boolean
}

-- Template workout
workouts {
    workoutId: Long PK
    name: String
    description: String
    createdAt: Long
    modifiedAt: Long
    isCustom: Boolean
    imagePath: String?
}

-- Junction table workout-esercizi
workout_exercises {
    workoutId: Long FK
    exerciseId: Long FK  
    orderIndex: Int
    targetReps: Int?
    targetSets: Int
    targetTime: Int?
    restTime: Int?
    notes: String
}
```

### Repository Pattern
```kotlin
// Core repositories
├── DailySessionRepository    // Gestione sessioni giornaliere
├── SessionRepository         // Storico completo sessioni
├── ExerciseRepository        // CRUD esercizi  
└── WorkoutRepository         // CRUD workout

// Data flow
ViewModels ←→ Repositories ←→ DAOs ←→ Room Database
```

### Database Relations
- **1:N** DailySession → DailySessionItems
- **N:N** Workouts ←→ Exercises (via workout_exercises)
- **Self-Reference** DailySessionItems (parentWorkoutItemId)

---

## 🧠 Domain Layer

### ViewModels

#### TodayViewModel
```kotlin
class TodayViewModel {
    // State management
    private val _selectedDate = MutableStateFlow<Long>()
    private val _todaySession = MutableStateFlow<DailySessionWithItems?>()
    
    // Core functions
    fun setSelectedDate(dateMillis: Long)
    fun addExerciseToSession(exerciseId: Long)
    fun addWorkoutToSession(workoutId: Long)
    fun addAISquatToSession(targetReps: Int)
    
    // Temporal restrictions
    fun canAddExercisesToSelectedDate(): Boolean
    fun canNavigateToNextDay(): Boolean
    fun isSelectedDateInPast(): Boolean
}
```

#### CalendarViewModel  
```kotlin
class CalendarViewModel {
    // Calendar state
    private val _uiState = MutableStateFlow<CalendarUiState>()
    
    // Recovery system
    private val recoveryConfig = RecoveryConfig(
        minRepsRequired = 50,
        maxDaysBack = 7,
        isEnabled = true
    )
    
    // Functions
    fun loadCalendarData(monthOffset: Int)
    fun selectDate(timestamp: Long)
    fun performRecovery(timestamp: Long)
}
```

### Business Logic

#### Temporal System
- **Today Only**: Add operations restricted to current day
- **Past Navigation**: Read-only access to historical data
- **Future Blocking**: No navigation to future dates

#### AI Integration
- **MoveNet Model**: TensorFlow Lite pose detection
- **Calibration**: Auto-adjustment to user posture
- **Counting Algorithm**: Shoulder-knee distance + foot positioning
- **Data Storage**: AI results saved as JSON in aiData field

---

## 🤖 AI/ML Components

### Pose Detection Pipeline
```
Camera Feed → TextureView → Bitmap → MoveNet → Keypoints → Analysis → UI Update
```

### MoveNet Integration
```kotlin
// Core detection class
SquatCounter {
    private val model: MoveNet
    private val interpreter: Interpreter
    
    fun analyzeFrame(bitmap: Bitmap): SquatAnalysis {
        // 1. Preprocess bitmap
        // 2. Run inference
        // 3. Extract keypoints
        // 4. Calculate metrics
        // 5. Determine squat state
    }
}
```

### Detection Metrics
- **Keypoints**: 17 body landmarks with confidence scores
- **Distance Metrics**: Shoulder-knee distance (left/right)
- **Foot Analysis**: Parallel positioning validation  
- **Temporal Smoothing**: 3-frame moving average
- **State Machine**: Standing → Squatting → Standing

---

## 🔄 State Management

### Flow Architecture
```kotlin
// Reactive streams throughout app
Database → Flow<T> → Repository → StateFlow<T> → ViewModel → collectAsState() → UI
```

### Compose State Integration
```kotlin
// Typical screen pattern
@Composable
fun TodayScreen(viewModel: TodayViewModel) {
    val todaySession by viewModel.todaySession.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    
    // UI reacts automatically to state changes
}
```

---

## 📊 Performance Optimizations

### Database Performance
- **Indexed Queries**: Primary keys + foreign keys indexed
- **Efficient Relations**: @Embedded and @Relation for complex queries
- **Lazy Loading**: Data loaded on-demand with Flow
- **Query Optimization**: Specific projections, avoid N+1 problems

### UI Performance  
- **Compose Optimization**: remember, LaunchedEffect, derivedStateOf
- **List Performance**: LazyColumn/LazyVerticalGrid with keys
- **Recomposition**: Minimal recomposition with stable states

### AI Performance
- **Model Size**: MoveNet Lite (~2MB) optimized for mobile
- **Inference Speed**: ~30ms per frame on modern devices
- **Memory Management**: Bitmap recycling, texture cleanup
- **Threading**: Background inference with UI updates on main thread

---

## 🔐 Data Flow Security

### Input Validation
- **Database Constraints**: NOT NULL, FOREIGN KEY constraints
- **Type Safety**: Kotlin null safety throughout
- **Bounds Checking**: Array/list bounds validation

### Error Handling
```kotlin
// Consistent error handling pattern
sealed class UiState<out T> {
    object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val exception: Throwable) : UiState<Nothing>()
}
```

---

## 🧪 Testing Strategy

### Unit Testing
- **ViewModels**: Business logic validation
- **Repositories**: Database operations testing  
- **Utilities**: Helper functions and calculations

### Integration Testing
- **Database**: Room migration and query testing
- **AI Pipeline**: MoveNet inference validation

### Architecture Benefits
- **Testability**: Clean separation of concerns
- **Modularity**: Independent component testing
- **Mockability**: Interface-based dependencies

---

## 🚀 Build & Deployment

### Gradle Configuration
```kotlin
// Key dependencies
implementation "androidx.compose.bom:2023.10.01"
implementation "androidx.room:2.5.0" 
implementation "org.tensorflow:tensorflow-lite:2.13.0"
implementation "androidx.navigation:navigation-compose:2.7.5"
```

### ProGuard Rules
- **AI Model**: Keep TensorFlow classes
- **Room**: Keep entity classes  
- **Compose**: Standard compose rules

---

*This technical architecture enables a scalable, maintainable, and high-performance fitness tracking application with advanced AI capabilities.*