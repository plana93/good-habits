package com.programminghut.pose_detection.ui.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.background
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.BorderStroke
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.programminghut.pose_detection.ui.calendar.CalendarViewModel
import com.programminghut.pose_detection.ui.calendar.CalendarUiState
import com.programminghut.pose_detection.ui.calendar.StreakCalendarScreen
import com.programminghut.pose_detection.ui.export.ExportViewModel
import com.programminghut.pose_detection.ui.export.ExportScreen
import com.programminghut.pose_detection.utils.FileExportHelper
import com.programminghut.pose_detection.data.repository.SessionRepository
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.collectAsState
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.room.Room
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import java.util.*
import com.programminghut.pose_detection.CameraSelectionActivity
import com.programminghut.pose_detection.ui.theme.Pose_detectionTheme
import com.programminghut.pose_detection.data.model.*
import com.programminghut.pose_detection.ui.calendar.DayStatus
import com.programminghut.pose_detection.service.TemplateToSessionService
import com.programminghut.pose_detection.ui.viewmodel.TodayViewModel
import com.programminghut.pose_detection.ui.viewmodel.TodayViewModelFactory
import com.programminghut.pose_detection.data.repository.DailySessionRepository
import com.programminghut.pose_detection.data.database.AppDatabase
import com.programminghut.pose_detection.utils.MotivationalQuotes
import com.programminghut.pose_detection.util.ExerciseTemplateFileManager
import com.programminghut.pose_detection.data.model.ExerciseTemplate

/**
 * ✅ Status del giorno per logica UI
 */
enum class DayStatus {
    CURRENT,    // Giorno corrente (oggi)
    DONE,       // Giorno passato con esercizi fatti
    LOST,       // Giorno passato vuoto non recuperato
    RECOVER     // Giorno passato recuperato con AI squat
}

/**
 * ✅ Classe per raggruppamento gerarchico degli elementi sessione
 */
sealed class GroupedSessionItem {
    abstract val order: Int
    
    data class WorkoutGroup(
        val workout: DailySessionItem,
        val exercises: List<DailySessionItem>
    ) : GroupedSessionItem() {
        override val order: Int = workout.order
    }
    
    data class StandaloneExercise(
        val exercise: DailySessionItem
    ) : GroupedSessionItem() {
        override val order: Int = exercise.order
    }
}

class NewMainActivity : ComponentActivity() {
    
    // ✅ SHARED REPOSITORY & VIEWMODEL - Initialized once per Activity
    private lateinit var dailySessionRepository: DailySessionRepository
    private lateinit var sessionRepository: SessionRepository
    private lateinit var todayViewModel: TodayViewModel
    
    // ✅ Esercizi caricati dinamicamente dai JSON
    internal var loadedExercises: List<ExerciseTemplate> = emptyList()
    
    // ✅ Activity Result Launchers for selection
    private val exerciseSelectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
    com.programminghut.pose_detection.util.todayDebug("🔄 exerciseSelectionLauncher ricevuto result: resultCode=${result.resultCode}")
        println("🔄 exerciseSelectionLauncher ricevuto result: resultCode=${result.resultCode}")
        
        if (result.resultCode == RESULT_OK) {
            result.data?.let { data ->
                val exerciseId = data.getLongExtra("SELECTED_EXERCISE_ID", -1L)
                val customReps = if (data.hasExtra("SELECTED_EXERCISE_REPS")) {
                    data.getIntExtra("SELECTED_EXERCISE_REPS", -1)
                } else null
                val customTime = if (data.hasExtra("SELECTED_EXERCISE_TIME")) {
                    data.getIntExtra("SELECTED_EXERCISE_TIME", -1) 
                } else null
                
                if (exerciseId != -1L) {
                    com.programminghut.pose_detection.util.todayDebug("✅ Esercizio selezionato: $exerciseId, reps=$customReps, time=$customTime")
                    todayViewModel.addExerciseToToday(this, exerciseId, customReps, customTime)
                    // ✅ Naviga automaticamente alla schermata "Oggi" dopo aver aggiunto l'esercizio
                    navigateToToday()
                } else {
                    com.programminghut.pose_detection.util.todayDebug("❌ ExerciseId non valido: $exerciseId")
                }
            } ?: run {
                com.programminghut.pose_detection.util.todayDebug("❌ Nessun data nell'intent result")
                println("❌ Nessun data nell'intent result")
            }
        } else {
            com.programminghut.pose_detection.util.todayDebug("❌ Result non OK: ${result.resultCode}")
            println("❌ Result non OK: ${result.resultCode}")
        }
    }
    
    private val workoutSelectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
    com.programminghut.pose_detection.util.todayDebug("🔄 workoutSelectionLauncher ricevuto result: resultCode=${result.resultCode}")
        println("🔄 workoutSelectionLauncher ricevuto result: resultCode=${result.resultCode}")
        
        if (result.resultCode == RESULT_OK) {
            result.data?.let { data ->
                val workoutId = data.getLongExtra("SELECTED_WORKOUT_ID", -1L)
                if (workoutId != -1L) {
                    todayViewModel.addWorkoutToToday(this, workoutId)
                    // ✅ Naviga automaticamente alla schermata "Oggi" dopo aver aggiunto il workout
                    navigateToToday()
                } else {
                    com.programminghut.pose_detection.util.todayDebug("❌ WorkoutId non valido: $workoutId")
                }
            } ?: println("❌ Nessun workoutId trovato nel result")
        } else {
            println("❌ Result non OK: ${result.resultCode}")
        }
    }
    
    // ✅ AI Camera Squat Launcher
    private val aiSquatCameraLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
    com.programminghut.pose_detection.util.todayDebug("🤖 aiSquatCameraLauncher ricevuto result: resultCode=${result.resultCode}")
        
        if (result.resultCode == RESULT_OK) {
            result.data?.let { data ->
                val repsCompleted = data.getIntExtra("REPS_COMPLETED", 0)
                val sessionDuration = data.getLongExtra("SESSION_DURATION", 0L)
                val isRecoveryMode = data.getBooleanExtra("RECOVERY_MODE", false)
                val recoveryDate = data.getLongExtra("RECOVERY_DATE", 0L)
                
                if (repsCompleted > 0) {
                    com.programminghut.pose_detection.util.todayDebug("🎯 AI Squat completato! Reps: $repsCompleted, Duration: $sessionDuration")
                    
                    if (isRecoveryMode && recoveryDate > 0L) {
                        // ✅ Gestione del recovery
                        com.programminghut.pose_detection.util.todayDebug("🔄 Recovery completato per data: $recoveryDate con $repsCompleted squat")
                        
                        // Verifica se ha raggiunto i 20 squat richiesti per il recovery
                        if (repsCompleted >= 20) {
                            lifecycleScope.launch {
                                try {
                                    val recoverySuccess = todayViewModel.completeRecoveryForDate(recoveryDate, repsCompleted)
                                    
                                    if (recoverySuccess) {
                                        com.programminghut.pose_detection.util.todayDebug("🎉 Recovery completato con successo per data: $recoveryDate")
                                        
                        // ✅ Forza refresh di tutti i dati dopo recovery
                        todayViewModel.refreshTodayData()
                        
                        // ✅ Refresh del calendario nella dashboard tramite callback
                        refreshCalendarCallback?.invoke() ?: run {
                            Log.w("TODAY_DEBUG", "⚠️ refreshCalendarCallback è null - calendario non aggiornato")
                        }
                        
                        com.programminghut.pose_detection.util.todayDebug("🔄 Refresh completato per Today e Calendar")                                        // TODO: Mostrare messaggio di successo
                                    } else {
                                        com.programminghut.pose_detection.util.todayDebug("⚠️ Recovery non riuscito - data già recuperata: $recoveryDate")
                                    }
                                } catch (e: Exception) {
                                    if (com.programminghut.pose_detection.util.Logging.TODAY_DEBUG) {
                                        Log.e("TODAY_DEBUG", "❌ Errore durante recovery: ${e.message}", e)
                                    }
                                }
                            }
                        } else {
                            com.programminghut.pose_detection.util.todayDebug("⚠️ Squat insufficienti per recovery: $repsCompleted/20")
                            // TODO: Mostrare messaggio che servono 20 squat
                        }
                    } else {
                        // ✅ Gestione normale degli AI squat - aggiungi come esercizio giornaliero
                        com.programminghut.pose_detection.util.todayDebug("💪 AI Squat normale completato - aggiungendo come esercizio giornaliero")
                        
                        lifecycleScope.launch {
                            try {
                                // Aggiunge l'AI squat come esercizio nella sessione di oggi
                                // usa l'istanza Activity come Context ("context" non è definito qui)
                                todayViewModel.addAISquatToToday(this@NewMainActivity, targetReps = repsCompleted)
                                com.programminghut.pose_detection.util.todayDebug("✅ AI Squat aggiunto alla sessione di oggi con $repsCompleted reps")
                                
                                // Refresh per mostrare il nuovo esercizio
                                todayViewModel.refreshTodayData()
                            } catch (e: Exception) {
                                if (com.programminghut.pose_detection.util.Logging.TODAY_DEBUG) {
                                    Log.e("TODAY_DEBUG", "❌ Errore durante aggiunta AI squat normale: ${e.message}", e)
                                }
                            }
                        }
                    }
                } else {
                    Log.d("TODAY_DEBUG", "⚠️ Nessuna ripetizione completata")
                }
                    } ?: com.programminghut.pose_detection.util.todayDebug("❌ Nessun data nell'intent result")
        } else {
            Log.d("TODAY_DEBUG", "❌ Result non OK: ${result.resultCode}")
        }
    }
    
    /**
     * ✅ Avvia il recovery per una data specifica tramite AI squat (unificato con Today screen)
     */
    private fun startRecoveryForDate(recoveryDate: Long) {
        Log.d("TODAY_DEBUG", "🎯 Avvio recovery tramite AI squat per data: $recoveryDate (CALENDARIO)")
        
        // ✅ UNIFICATO: Usa stesso sistema del Today screen
        val aiSquatIntent = Intent(this, CameraSelectionActivity::class.java).apply {
            putExtra("MODE", "RECOVERY")
            putExtra("RECOVERY_DATE", recoveryDate)
            putExtra("RECOVERY_TARGET_SQUAT", 20)
        }
        startActivity(aiSquatIntent)
    }

    // ✅ SHARED CALLBACKS - Set when needed
    internal var onExerciseSelected: (Long) -> Unit = { }
    internal var onWorkoutSelected: (Long) -> Unit = { }
    
    // ✅ Callback per navigazione - sarà impostata da MainContent
    private var navigateToToday: () -> Unit = { }
    
    // ✅ Callback per refresh del calendario nella dashboard
    internal var refreshCalendarCallback: (() -> Unit)? = null

    /**
     * ⚠️ DEPRECATO - Usare il launcher invece di startActivity diretto
     * Avvia la procedura di recupero per un giorno passato con 20 squat AI
     */
    private fun startRecoveryProcedure(recoveredDate: Long) {
        Log.d("🔄 RECOVERY", "Avviando recupero per data: $recoveredDate")
        
        val intent = Intent(this, CameraSelectionActivity::class.java).apply {
            putExtra("MODE", "RECOVERY")
            putExtra("RECOVERY_DATE", recoveredDate)
            putExtra("RECOVERY_TARGET_SQUAT", 20)
        }
        startActivity(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // ✅ Initialize database and repository ONCE
        initializeComponents()
        
        // ✅ Handle navigation to specific date from calendar
        val navigateToDate = intent.getLongExtra("NAVIGATE_TO_DATE", -1L)
        if (navigateToDate != -1L) {
            Log.d("TODAY_DEBUG", "🗓️ Navigating to specific date: $navigateToDate")
            // Set the date in the ViewModel
            todayViewModel.setSelectedDate(navigateToDate)
        }
        
        // ✅ Handle navigation to specific section
        val navigateToSection = intent.getStringExtra("NAVIGATE_TO")
        Log.d("TODAY_DEBUG", "🧭 Navigate to section: $navigateToSection")
        
        setContent {
            Pose_detectionTheme {
                MainContent(
                    exerciseSelectionLauncher = exerciseSelectionLauncher,
                    workoutSelectionLauncher = workoutSelectionLauncher,
                    aiSquatCameraLauncher = aiSquatCameraLauncher,
                    todayViewModel = todayViewModel,
                    exercises = loadedExercises,
                    initialRoute = navigateToSection ?: "dashboard",
                    onSetNavigateToToday = { callback -> navigateToToday = callback },
                    onStartRecovery = { recoveryDate -> startRecoveryForDate(recoveryDate) }
                )
            }
        }
    }
    
    private fun initializeComponents() {
        // ✅ CRITICAL FIX: Usa la stessa istanza singleton del database come MainActivity
        val database = AppDatabase.getDatabase(applicationContext)
        
        dailySessionRepository = DailySessionRepository(
            database.dailySessionDao(),
            database.dailySessionRelationDao(),
            database.exerciseDao(),
            database.workoutDao()
        )
        
        sessionRepository = SessionRepository(
            database.sessionDao(),
            database.repDao()
        )
        
        val factory = TodayViewModelFactory(dailySessionRepository, sessionRepository)
        todayViewModel = factory.create(TodayViewModel::class.java)
        
        // ✅ Carica esercizi dai JSON
        loadExercisesFromJson()
    }
    
    private fun loadExercisesFromJson() {
        try {
            loadedExercises = ExerciseTemplateFileManager.loadExerciseTemplates(applicationContext)
            Log.d("NewMainActivity", "✅ Caricati ${loadedExercises.size} esercizi dai JSON")
        } catch (e: Exception) {
            Log.e("NewMainActivity", "❌ Errore caricamento esercizi dai JSON", e)
            loadedExercises = emptyList()
        }
    }
}

@Composable
fun MainContent(
    exerciseSelectionLauncher: androidx.activity.result.ActivityResultLauncher<Intent>,
    workoutSelectionLauncher: androidx.activity.result.ActivityResultLauncher<Intent>,
    aiSquatCameraLauncher: androidx.activity.result.ActivityResultLauncher<Intent>,
    todayViewModel: TodayViewModel,
    exercises: List<ExerciseTemplate>, // ✅ Aggiunto parametro esercizi
    initialRoute: String = "dashboard",
    onSetNavigateToToday: (() -> Unit) -> Unit,
    onStartRecovery: (Long) -> Unit = {}
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    
    // ✅ Ottieni selectedDate dal TodayViewModel
    val selectedDate by todayViewModel.selectedDate.collectAsState()
    
    // ✅ Check if we need to navigate to today screen for a specific date
    val activity = context as? NewMainActivity
    val navigateToDate = remember { activity?.intent?.getLongExtra("NAVIGATE_TO_DATE", -1L) ?: -1L }
    
    LaunchedEffect(navigateToDate) {
        if (navigateToDate != -1L) {
            Log.d("TODAY_DEBUG", "🗓️ Auto-navigating to 'today' screen for date: $navigateToDate")
            navController.navigate("today") {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = false
                }
                launchSingleTop = true
                restoreState = false
            }
        }
    }
    
    // ✅ Imposta la callback di navigazione
    LaunchedEffect(navController) {
        onSetNavigateToToday {
            Log.d("TODAY_DEBUG", "🏠 Navigazione automatica verso 'today'")
            navController.navigate("today") {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    }
    
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    // ✅ Reset selectedDate to today SOLO quando si esce completamente dal gruppo di schermate correlate
    LaunchedEffect(currentRoute) {
        // Reset SOLO se passiamo a schermate non correlate (non Today, non Dashboard che può portare a Today)
        if (currentRoute != null && currentRoute !in setOf("today", "dashboard")) {
            val today = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            
            Log.d("TODAY_DEBUG", "📱 Route changed to $currentRoute (unrelated screen) - resetting selectedDate to today: $today")
            todayViewModel.setSelectedDate(today)
        }
    }
    
    // ✅ Barra di navigazione sempre visibile
    val screensWithBottomBar = setOf("dashboard", "today", "history", "exercises", "workouts")
    val screensWithAddFAB = setOf("today", "exercises", "workouts")
    
    var showBottomSheet by remember { mutableStateOf(false) }
    var showWellnessPickerDialog by remember { mutableStateOf(false) }
    var showWellnessEntryDialog by remember { mutableStateOf(false) }
    var selectedWellnessTracker by remember { mutableStateOf<com.programminghut.pose_detection.data.model.WellnessTrackerTemplate?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var wellnessItemToDelete by remember { mutableStateOf<DailySessionItemWithDetails?>(null) }
    
    Scaffold(
        bottomBar = {
            // ✅ Barra di navigazione sempre visibile
            if (currentRoute == null || currentRoute in screensWithBottomBar) {
                NavigationBar {
                    val currentDestination = navBackStackEntry?.destination
                    
                    listOf(
                        BottomNavItem("dashboard", "Dashboard", Icons.Default.Dashboard),
                        BottomNavItem("today", "Oggi", Icons.Default.Today),
                        BottomNavItem("exercises", "Esercizi", Icons.Default.FitnessCenter),
                        BottomNavItem("workouts", "Allenamenti", Icons.Default.List),
                        BottomNavItem("history", "Storico", Icons.Default.History)
                    ).forEach { item ->
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.title) },
                            selected = currentDestination?.hierarchy?.any { it.route == item.route } == true,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            // ✅ FAB centrale che cambia funzione in base alla data e schermata
            // Mostra solo nelle schermate che lo usano
            if (currentRoute in screensWithAddFAB) {
                FloatingActionButton(
                    onClick = {
                        when (currentRoute) {
                            "today" -> {
                                // Controlla se la data selezionata è nel passato
                                val today = Calendar.getInstance()
                                val selected = Calendar.getInstance().apply { timeInMillis = selectedDate }
                                val isToday = today.get(Calendar.YEAR) == selected.get(Calendar.YEAR) &&
                                        today.get(Calendar.DAY_OF_YEAR) == selected.get(Calendar.DAY_OF_YEAR)
                                val isPast = selected.before(today) && !isToday
                                
                                if (isPast) {
                                    // ⚠️ DEPRECATO: Per date passate: avvia procedura di recupero (meglio usare il pulsante nella TodayScreen)
                                    val intent = Intent(context, CameraSelectionActivity::class.java).apply {
                                        putExtra("MODE", "RECOVERY")
                                        putExtra("RECOVERY_DATE", selectedDate)
                                        putExtra("RECOVERY_TARGET_SQUAT", 20)
                                    }
                                    context.startActivity(intent)
                                } else {
                                    // Per data odierna o future: mostra menu aggiunta
                                    showBottomSheet = true
                                }
                            }
                            "exercises" -> {
                                val intent = Intent(context, ExerciseLibraryActivity::class.java).apply {
                                    putExtra("SELECTION_MODE", true)
                                }
                                exerciseSelectionLauncher.launch(intent)
                            }
                            "workouts" -> {
                                val intent = Intent(context, WorkoutLibraryActivity::class.java).apply {
                                    putExtra("SELECTION_MODE", true)
                                }
                                workoutSelectionLauncher.launch(intent)
                            }
                        }
                    }
                ) {
                    // Cambia icona in base alla data e schermata
                    val isSelectedDatePast = remember(selectedDate, currentRoute) {
                        if (currentRoute == "today") {
                            val today = Calendar.getInstance()
                            val selected = Calendar.getInstance().apply { timeInMillis = selectedDate }
                            val isToday = today.get(Calendar.YEAR) == selected.get(Calendar.YEAR) &&
                                    today.get(Calendar.DAY_OF_YEAR) == selected.get(Calendar.DAY_OF_YEAR)
                            selected.before(today) && !isToday
                        } else false
                    }
                    
                    Icon(
                        imageVector = when {
                            currentRoute == "today" && isSelectedDatePast -> Icons.Default.Refresh // Icona recupera
                            else -> Icons.Default.Add
                        },
                        contentDescription = when {
                            currentRoute == "today" && isSelectedDatePast -> "Recupera Giorno"
                            else -> "Aggiungi"
                        }
                    )
                }
            }
        },
        floatingActionButtonPosition = FabPosition.Center
    ) { paddingValues ->
        NavHost(
                navController = navController,
                startDestination = initialRoute,
                modifier = Modifier.padding(paddingValues)
            ) {
                composable("dashboard") { 
                    DashboardScreen(navController, todayViewModel, onStartRecovery)
                }
                composable("today") { 
                    TodayScreen(
                        showBottomSheet = showBottomSheet,
                        exercises = exercises,
                        exerciseSelectionLauncher = exerciseSelectionLauncher,
                        workoutSelectionLauncher = workoutSelectionLauncher,
                        aiSquatCameraLauncher = aiSquatCameraLauncher,
                        todayViewModel = todayViewModel,
                        onBottomSheetDismiss = { showBottomSheet = false },
                        showWellnessPickerDialog = showWellnessPickerDialog,
                        showWellnessEntryDialog = showWellnessEntryDialog,
                        selectedWellnessTracker = selectedWellnessTracker,
                        onShowWellnessPickerDialog = { showWellnessPickerDialog = it },
                        onShowWellnessEntryDialog = { showWellnessEntryDialog = it },
                        onSelectedWellnessTracker = { selectedWellnessTracker = it },
                        showDeleteConfirmDialog = showDeleteConfirmDialog,
                        wellnessItemToDelete = wellnessItemToDelete,
                        onShowDeleteConfirmDialog = { showDeleteConfirmDialog = it },
                        onWellnessItemToDelete = { wellnessItemToDelete = it }
                    )
                }
                composable("exercises") { 
                    ExerciseLibraryScreen()
                }
                composable("workouts") { 
                    WorkoutLibraryScreen(workoutSelectionLauncher)
                }
                composable("history") { 
                    HistoryScreen()
                }
            }
    }
}

data class BottomNavItem(
    val route: String,
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TodayScreen(
    showBottomSheet: Boolean,
    exercises: List<ExerciseTemplate>, // ✅ Aggiunto parametro esercizi
    exerciseSelectionLauncher: androidx.activity.result.ActivityResultLauncher<Intent>,
    workoutSelectionLauncher: androidx.activity.result.ActivityResultLauncher<Intent>,
    aiSquatCameraLauncher: androidx.activity.result.ActivityResultLauncher<Intent>,
    todayViewModel: TodayViewModel,
    onBottomSheetDismiss: () -> Unit,
    showWellnessPickerDialog: Boolean,
    showWellnessEntryDialog: Boolean,
    selectedWellnessTracker: com.programminghut.pose_detection.data.model.WellnessTrackerTemplate?,
    onShowWellnessPickerDialog: (Boolean) -> Unit,
    onShowWellnessEntryDialog: (Boolean) -> Unit,
    onSelectedWellnessTracker: (com.programminghut.pose_detection.data.model.WellnessTrackerTemplate?) -> Unit,
    showDeleteConfirmDialog: Boolean,
    wellnessItemToDelete: DailySessionItemWithDetails?,
    onShowDeleteConfirmDialog: (Boolean) -> Unit,
    onWellnessItemToDelete: (DailySessionItemWithDetails?) -> Unit
) {
    val context = LocalContext.current
    
    // ✅ Observe la data selezionata dal ViewModel
    val selectedDate by todayViewModel.selectedDate.collectAsState()
    val todaySession by todayViewModel.todaySession.collectAsState()
    
    // ✅ Scope per coroutines
    val scope = rememberCoroutineScope()
    
    // ✅ Setup HorizontalPager - solo passato e presente (no futuro)
    val maxPastDays = 365 // Un anno di storico
    val initialPage = maxPastDays // Oggi è alla posizione maxPastDays
    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { maxPastDays + 1 } // Da -365 giorni a oggi (0)
    )
    
    // ✅ FIXED: Base date fissa per calcoli consistenti
    val baseDate = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    
    // ✅ Flag per controllare la navigazione dal calendario - con stato duraturo
    var isNavigatingFromCalendar by remember { mutableStateOf(false) }
    
    // ✅ Timer per reset automatico del flag (sicurezza)
    LaunchedEffect(isNavigatingFromCalendar) {
        if (isNavigatingFromCalendar) {
            Log.d("TODAY_DEBUG", "🧭 Calendar navigation flag set - auto-reset in 3 seconds")
            kotlinx.coroutines.delay(3000) // 3 secondi di sicurezza
            isNavigatingFromCalendar = false
            Log.d("TODAY_DEBUG", "🧭 Calendar navigation flag auto-reset after timeout")
        }
    }
    
    // ✅ Aggiorna il ViewModel quando cambia la pagina del pager (solo se NON navighiamo dal calendario)
    LaunchedEffect(pagerState.currentPage) {
        if (!isNavigatingFromCalendar) {
            val currentPageOffset = pagerState.currentPage - initialPage // negativo per passato, 0 per oggi
            val calendar = Calendar.getInstance().apply { 
                timeInMillis = baseDate
                add(Calendar.DAY_OF_YEAR, currentPageOffset) // ✅ FIXED: Use DAY_OF_YEAR instead of DAY_OF_MONTH
            }
            todayViewModel.setSelectedDate(calendar.timeInMillis)
        }
    }
    
    // ✅ Naviga al pager quando viene impostata una data specifica dal calendario
    LaunchedEffect(selectedDate) {
        // ✅ Agisci SOLO se NON stiamo già navigando dal calendario
        if (!isNavigatingFromCalendar) {
            // Calcola quale pagina corrisponde alla selectedDate usando la stessa baseDate
            val currentCal = Calendar.getInstance().apply { 
                timeInMillis = baseDate
            }
            val selectedCal = Calendar.getInstance().apply { 
                timeInMillis = selectedDate
                // Normalizza la selectedDate
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            
            // Calcola la differenza in giorni
            val diffInMillis = selectedCal.timeInMillis - currentCal.timeInMillis
            val diffInDays = (diffInMillis / (24 * 60 * 60 * 1000)).toInt()
            
            // Calcola l'indice del pager (today = initialPage = maxPastDays)
            val targetPage = initialPage + diffInDays
            
            // ✅ Naviga al pager solo se è una pagina valida e diversa da quella attuale
            if (targetPage in 0 until (maxPastDays + 1) && targetPage != pagerState.currentPage) {
                // ✅ Imposta flag per BLOCCARE la sincronizzazione pager->ViewModel
                isNavigatingFromCalendar = true
                
                // ✅ Usa animazione semplice senza try-catch per evitare errori di scope
                scope.launch {
                    try {
                        pagerState.animateScrollToPage(targetPage)
                    } catch (e: Exception) {
                        Log.e("TODAY_DEBUG", "🧭 Error during pager animation: ${e.message}")
                    }
                    // Il flag verrà resettato automaticamente dal timer dopo 3 secondi
                }
            }
        }
    }
    
    // ✅ Verifica se si può aggiungere esercizi
    val canAddExercises by remember { 
        derivedStateOf { todayViewModel.canAddExercisesToSelectedDate() }
    }
    
    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // ✅ Header con controlli di navigazione
            DateNavigationHeader(
                    selectedDate = remember(pagerState.currentPage, baseDate) {
                        // ✅ Usa sempre la data calcolata dalla pagina corrente del pager per sincronizzazione immediata
                        val pageOffset = pagerState.currentPage - initialPage
                        val calendar = Calendar.getInstance().apply {
                            timeInMillis = baseDate
                            add(Calendar.DAY_OF_YEAR, pageOffset)
                        }
                        calendar.timeInMillis
                    },
                    onPreviousDay = { 
                        scope.launch { 
                            pagerState.animateScrollToPage(pagerState.currentPage - 1)
                        }
                    },
                    onNextDay = { 
                        scope.launch { 
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    },
                    onGoToToday = {
                        scope.launch {
                            pagerState.animateScrollToPage(initialPage)
                        }
                    },
                    todayViewModel = todayViewModel
                )
                
                // ✅ HorizontalPager per i giorni
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) { page ->
                    // Calcola la data per questa pagina - solo passato e presente
                    val pageOffset = page - initialPage // negativo per passato, 0 per oggi
                    val pageDate = Calendar.getInstance().apply {
                        timeInMillis = baseDate
                        add(Calendar.DAY_OF_YEAR, pageOffset) // ✅ FIXED: Use DAY_OF_YEAR instead of DAY_OF_MONTH
                    }.timeInMillis
                    
                    // ✅ Crea contenuto con dati specifici per questa data
                    DayPageContent(
                        pageDate = pageDate,
                        exercises = exercises,
                        onAddClick = { /* Non usato - FAB ora è nel MainContent */ },
                        todayViewModel = todayViewModel,
                        aiSquatCameraLauncher = aiSquatCameraLauncher,
                        onShowWellnessPicker = { onShowWellnessPickerDialog(true) },
                        onShowWellnessEntry = { tracker ->
                            onSelectedWellnessTracker(tracker)
                            onShowWellnessEntryDialog(true)
                        },
                        onDeleteWellnessItem = { itemDetails ->
                            onWellnessItemToDelete(itemDetails)
                            onShowDeleteConfirmDialog(true)
                        }
                    )
                }
            }
        
        // ✅ Bottom sheet per aggiungere elementi
        if (showBottomSheet) {
            SimpleAddItemBottomSheet(
                onDismiss = onBottomSheetDismiss,
                onAddExercise = { 
                    onBottomSheetDismiss()
                    val intent = Intent(context, ExerciseLibraryActivity::class.java).apply {
                        putExtra("SELECTION_MODE", true)
                    }
                    exerciseSelectionLauncher.launch(intent)
                },
                onAddWorkout = { 
                    onBottomSheetDismiss()
                    val intent = Intent(context, WorkoutLibraryActivity::class.java).apply {
                        putExtra("SELECTION_MODE", true)
                    }
                    workoutSelectionLauncher.launch(intent)
                },
                onAddAISquat = {
                    onBottomSheetDismiss()
                    // ✅ Dalla schermata "Oggi" → Avvia direttamente la camera per conteggio AI
                    val intent = Intent(context, CameraSelectionActivity::class.java).apply {
                        putExtra("MODE", "AI_SQUAT")
                        putExtra("RECOVERY_TARGET_SQUAT", 20)
                    }
                    aiSquatCameraLauncher.launch(intent)
                },
                onAddWellness = {
                    onBottomSheetDismiss()
                    onShowWellnessPickerDialog(true)
                }
            )
        }
        
        // ✅ Wellness Tracker Picker Dialog
        if (showWellnessPickerDialog) {
            val fileManager = remember { com.programminghut.pose_detection.data.manager.WellnessTrackerFileManager(context) }
            val wellnessTrackers = remember { fileManager.getAllTrackers() }
            val wellnessCategories = remember { fileManager.getAllCategories() }
            
            com.programminghut.pose_detection.ui.components.WellnessTrackerPickerDialog(
                trackers = wellnessTrackers,
                categories = wellnessCategories,
                onDismiss = { onShowWellnessPickerDialog(false) },
                onTrackerSelected = { tracker ->
                    onSelectedWellnessTracker(tracker)
                    onShowWellnessPickerDialog(false)
                    onShowWellnessEntryDialog(true)
                }
            )
        }
        
        // ✅ Wellness Tracker Entry Dialog
        if (showWellnessEntryDialog && selectedWellnessTracker != null) {
            com.programminghut.pose_detection.ui.components.WellnessTrackerEntryDialog(
                tracker = selectedWellnessTracker,
                onDismiss = {
                    onShowWellnessEntryDialog(false)
                    onSelectedWellnessTracker(null)
                },
                onSave = { trackerResponse ->
                    // Aggiungi il wellness tracker alla sessione
                    todayViewModel.addWellnessTrackerToToday(
                        context = context,
                        trackerTemplateId = selectedWellnessTracker.id,
                        trackerResponse = trackerResponse
                    )
                    onShowWellnessEntryDialog(false)
                    onSelectedWellnessTracker(null)
                }
            )
        }
        
        // ✅ Delete Confirmation Dialog
        if (showDeleteConfirmDialog && wellnessItemToDelete != null) {
            AlertDialog(
                onDismissRequest = {
                    onShowDeleteConfirmDialog(false)
                    onWellnessItemToDelete(null)
                },
                icon = { Icon(Icons.Default.Delete, contentDescription = null) },
                title = { Text("Delete wellness tracker?") },
                text = { Text("This action cannot be undone.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            wellnessItemToDelete?.let { item ->
                                todayViewModel.removeExerciseFromToday(item.itemId)
                            }
                            onShowDeleteConfirmDialog(false)
                            onWellnessItemToDelete(null)
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            onShowDeleteConfirmDialog(false)
                            onWellnessItemToDelete(null)
                        }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun DateNavigationHeader(
    selectedDate: Long,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onGoToToday: () -> Unit,
    todayViewModel: TodayViewModel
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Title row with navigation
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onPreviousDay,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        Icons.Default.ChevronLeft, 
                        contentDescription = "Giorno precedente",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // ✅ Usa il parametro selectedDate passato dal pager invece del ViewModel
                    val dateFormat = remember { java.text.SimpleDateFormat("d MMMM", java.util.Locale.ITALIAN) }
                    val longDateFormat = remember { java.text.SimpleDateFormat("EEEE, d MMMM yyyy", java.util.Locale.ITALIAN) }
                    
                    Text(
                        text = dateFormat.format(java.util.Date(selectedDate)),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    
                    Text(
                        text = longDateFormat.format(java.util.Date(selectedDate)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
                
                // ✅ Verifica se si può navigare al giorno successivo usando selectedDate
                val canNavigateToNextDay = remember(selectedDate) {
                    val selected = Calendar.getInstance().apply { timeInMillis = selectedDate }
                    val today = Calendar.getInstance()
                    selected.get(Calendar.YEAR) < today.get(Calendar.YEAR) || 
                    (selected.get(Calendar.YEAR) == today.get(Calendar.YEAR) && 
                     selected.get(Calendar.DAY_OF_YEAR) < today.get(Calendar.DAY_OF_YEAR))
                }
                
                IconButton(
                    onClick = {
                        // ✅ Controlla se si può navigare al giorno successivo
                        if (canNavigateToNextDay) {
                            onNextDay()
                        }
                    },
                    modifier = Modifier.size(48.dp),
                    enabled = canNavigateToNextDay // ✅ Disabilita se non può navigare
                ) {
                    Icon(
                        Icons.Default.ChevronRight, 
                        contentDescription = "Giorno successivo",
                        tint = if (canNavigateToNextDay) 
                            MaterialTheme.colorScheme.onPrimaryContainer 
                        else 
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.3f)
                    )
                }
            }
            
            // "Vai a oggi" button se non siamo oggi
            val isToday = remember(selectedDate) {
                val today = Calendar.getInstance()
                val selected = Calendar.getInstance().apply { timeInMillis = selectedDate }
                today.get(Calendar.YEAR) == selected.get(Calendar.YEAR) &&
                today.get(Calendar.DAY_OF_YEAR) == selected.get(Calendar.DAY_OF_YEAR)
            }
            
            // Verifica se la data è nel passato
            val isPastDate = remember(selectedDate) {
                val today = Calendar.getInstance()
                val selected = Calendar.getInstance().apply { timeInMillis = selectedDate }
                selected.before(today) && !isToday
            }
            
            if (!isToday) {
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = onGoToToday,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Icon(
                        Icons.Default.Today, 
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Vai a oggi")
                }
            }
        }
    }
}

@Composable
fun DayPageContent(
    pageDate: Long,
    exercises: List<ExerciseTemplate>, // ✅ Aggiunto parametro esercizi
    onAddClick: () -> Unit,
    todayViewModel: TodayViewModel,
    aiSquatCameraLauncher: androidx.activity.result.ActivityResultLauncher<Intent>,
    onShowWellnessPicker: () -> Unit = {}, // ✅ Callback per mostrare wellness picker
    onShowWellnessEntry: (com.programminghut.pose_detection.data.model.WellnessTrackerTemplate) -> Unit = {}, // ✅ Callback per mostrare wellness entry
    onDeleteWellnessItem: (DailySessionItemWithDetails) -> Unit = {} // ✅ Callback per eliminare wellness item
) {
    // ✅ Ottieni i dati per questa specifica data
    val sessionData by todayViewModel.getSessionForDate(pageDate).collectAsState(initial = null)
    
    // ✅ Determina se si possono aggiungere esercizi per questa data
    val canAddExercises = remember(pageDate) {
        val targetDate = Calendar.getInstance().apply {
            timeInMillis = pageDate
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        targetDate.timeInMillis == today.timeInMillis
    }
    
    // ✅ Determina se è un giorno passato
    val isInPast = remember(pageDate) {
        val targetDate = Calendar.getInstance().apply {
            timeInMillis = pageDate
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        targetDate.before(today)
    }
    
    DaySessionContent(
        sessionData = sessionData,
        exercises = exercises,
        onAddClick = onAddClick,
        canAddExercises = canAddExercises,
        isInPast = isInPast,
        pageDate = pageDate,
        todayViewModel = todayViewModel,
        aiSquatCameraLauncher = aiSquatCameraLauncher,
        onShowWellnessPicker = onShowWellnessPicker,
        onShowWellnessEntry = onShowWellnessEntry,
        onDeleteWellnessItem = onDeleteWellnessItem
    )
}

@Composable
fun DaySessionContent(
    sessionData: DailySessionWithItems?,
    exercises: List<ExerciseTemplate>, // ✅ Aggiunto parametro esercizi
    onAddClick: () -> Unit,
    canAddExercises: Boolean,
    isInPast: Boolean,
    pageDate: Long,
    todayViewModel: TodayViewModel,
    aiSquatCameraLauncher: androidx.activity.result.ActivityResultLauncher<Intent>,
    onShowWellnessPicker: () -> Unit = {}, // ✅ Callback per mostrare wellness picker
    onShowWellnessEntry: (com.programminghut.pose_detection.data.model.WellnessTrackerTemplate) -> Unit = {}, // ✅ Callback per mostrare wellness entry
    onDeleteWellnessItem: (DailySessionItemWithDetails) -> Unit = {} // ✅ Callback per eliminare wellness item
) {
    // ✅ Determina se questo è un giorno passato vuoto per il background rosso
    val isEmpty = sessionData?.items?.isEmpty() ?: true
    val shouldShowRedBackground = isInPast && isEmpty
    // Contesto Composable
    val context = androidx.compose.ui.platform.LocalContext.current
    
    // ✅ Verifica se la data è stata recuperata (per giorni passati) - reagisce ai cambiamenti del DB
    val isRecovered by produceState(initialValue = false, pageDate, isInPast) {
        if (isInPast) {
            // ✅ Log debug per tracking
            com.programminghut.pose_detection.util.todayDebug("🔍 Observing recovery status for past date: $pageDate")
            
            // ✅ Osserva continuamente i cambiamenti tramite il Flow del TodayViewModel
            todayViewModel.getSessionForDate(pageDate).collect { sessionData ->
                // ✅ STRATEGIA MULTIPLA per rilevare recovery
                val hasAnySession = sessionData != null && sessionData.items.isNotEmpty()
                
                // 1. Controlla specifically recovery-marked items nelle sessioni (aiData='squat_recovery')
                var hasRecoveryItem = false
                var hasNonRecoveryItem = false
                sessionData?.items?.let { items ->
                    for (item in items) {
                        val isRecoveryMarker = item.aiData?.contains("squat_recovery") == true || item.aiData?.contains("recovery") == true
                        if (isRecoveryMarker) {
                            hasRecoveryItem = true
                        } else {
                            // Any item that is not explicitly a recovery marker counts as non-recovery
                            hasNonRecoveryItem = true
                        }
                        if (hasRecoveryItem && hasNonRecoveryItem) break
                    }
                }
                
                // 2. Controlla recovery diretto nel ViewModel
                val directRecovery = todayViewModel.isDateRecovered(pageDate)
                
                // 3. Controlla se c'è una sessione per questa data (qualsiasi tipo)
                val hasAnyActivity = hasAnySession
                
                // ✅ CONSIDERA RECUPERATO SOLO SE È STATO MARKATO COME RECOVERY
                // Regola: se la sessione contiene anche esercizi non-recovery, trattiamo il giorno come COMPLETATO,
                // quindi recovery vale solo se ci sono SOLO recovery items, o se directRecovery esiste e non ci sono items.
                val newValue = (hasRecoveryItem && !hasNonRecoveryItem) || (directRecovery && !hasAnySession)
                
                com.programminghut.pose_detection.util.todayDebug("🔄 MULTI-CHECK Recovery status - Date: $pageDate")
                com.programminghut.pose_detection.util.todayDebug("   ✅ IsRecovered: $newValue")
                com.programminghut.pose_detection.util.todayDebug("   📊 hasAnySession: $hasAnySession (${sessionData?.items?.size ?: 0} items)")
                com.programminghut.pose_detection.util.todayDebug("   🤖 hasRecoveryItem: $hasRecoveryItem, hasNonRecoveryItem: $hasNonRecoveryItem")
                com.programminghut.pose_detection.util.todayDebug("   🎯 directRecovery: $directRecovery")
                com.programminghut.pose_detection.util.todayDebug("   📅 hasAnyActivity (daily session items present): $hasAnyActivity")
                com.programminghut.pose_detection.util.todayDebug("   ✅ final isRecovered evaluation: $newValue")
                
                value = newValue
            }
        } else {
            value = false
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (shouldShowRedBackground) {
                    Modifier.background(
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(0.dp)
                    )
                } else {
                    Modifier
                }
            )
    ) {
        sessionData?.let { sessionWithItems ->
            // ✅ Check if there are items that count as physical activity (exercises/workouts, not just wellness trackers)
            val hasActivityItems = sessionWithItems.items.any { item ->
                item.countsAsActivity
            }
            
            // ✅ Filtra i wellness trackers separatamente per usarli nella UI
            val wellnessTrackers = remember(sessionWithItems.items) {
                sessionWithItems.items.filter { it.itemType == SessionItemType.WELLNESS_TRACKER }
            }
            
            // ✅ Raggruppa elementi per gerarchia: Workout -> Esercizi
            val groupedItems = remember(sessionWithItems.items) {
                // ✅ Debug log per vedere tutti gli item
                com.programminghut.pose_detection.util.todayDebug("📋 Processing ${sessionWithItems.items.size} items for Today screen:")
                sessionWithItems.items.forEachIndexed { index, item ->
                    com.programminghut.pose_detection.util.todayDebug("  $index: itemId=${item.itemId}, type=${item.itemType}, exerciseId=${item.exerciseId}, aiData=${item.aiData}, customReps=${item.customReps}")
                }
                
                // Raggruppa per gerarchia
                val workouts = sessionWithItems.items.filter { it.itemType == SessionItemType.WORKOUT }
                val standaloneExercises = sessionWithItems.items.filter { 
                    it.itemType == SessionItemType.EXERCISE && it.parentWorkoutItemId == null 
                    // ✅ Includi tutti gli esercizi (normali e AI squats)
                }
                val wellnessTrackers = sessionWithItems.items.filter { it.itemType == SessionItemType.WELLNESS_TRACKER }
                
                // ✅ Debug log per vedere il filtro
                Log.d("TODAY_DEBUG", "📊 Filtered results:")
                Log.d("TODAY_DEBUG", "  - Workouts: ${workouts.size}")
                Log.d("TODAY_DEBUG", "  - Standalone exercises: ${standaloneExercises.size}")
                Log.d("TODAY_DEBUG", "  - Wellness trackers: ${wellnessTrackers.size}")
                Log.d("TODAY_DEBUG", "  - Standalone exercises: ${standaloneExercises.size}")
                standaloneExercises.forEachIndexed { index, item ->
                    Log.d("TODAY_DEBUG", "    $index: ${item.itemId} (exerciseId=${item.exerciseId}, aiData=${item.aiData})")
                }
                val workoutItems = mutableListOf<GroupedSessionItem>()
                
                // Aggiungi workout con i loro esercizi
                workouts.forEach { workout ->
                    val childExercises = sessionWithItems.items.filter { 
                        it.parentWorkoutItemId == workout.itemId 
                    }
                    workoutItems.add(
                        GroupedSessionItem.WorkoutGroup(
                            workout = workout,
                            exercises = childExercises
                        )
                    )
                }
                
                // Aggiungi esercizi standalone
                standaloneExercises.forEach { exercise ->
                    workoutItems.add(GroupedSessionItem.StandaloneExercise(exercise))
                }
                
                // ✅ Debug log finale
                Log.d("TODAY_DEBUG", "🎯 Final grouped items: ${workoutItems.size}")
                workoutItems.forEachIndexed { index, item ->
                    when (item) {
                        is GroupedSessionItem.StandaloneExercise -> {
                            Log.d("TODAY_DEBUG", "  $index: StandaloneExercise(${item.exercise.itemId}, exerciseId=${item.exercise.exerciseId}, aiData=${item.exercise.aiData})")
                        }
                        is GroupedSessionItem.WorkoutGroup -> {
                            Log.d("TODAY_DEBUG", "  $index: WorkoutGroup(${item.workout.itemId}, ${item.exercises.size} exercises)")
                        }
                    }
                }

                workoutItems.sortedBy { it.order }
            }
            
            if (sessionWithItems.items.isEmpty()) {
                // ✅ Mostra messaggio diverso per passato vs presente
                if (canAddExercises) {
                    EmptySessionCard(onAddClick = onAddClick)
                } else {
                    EmptyHistoryCard(
                        isInPast = isInPast,
                        pageDate = pageDate,
                        isRecovered = isRecovered,
                        aiSquatCameraLauncher = aiSquatCameraLauncher
                    )
                }
            } else {
                // ✅ Se la sessione ha items ma è un recovery, mostra il messaggio di recuperato
                val hasRecoveryItems = sessionWithItems.items.any { item ->
                    // Recovery only if explicitly marked (aiData contains 'squat_recovery' or 'recovery')
                    item.aiData?.contains("squat_recovery") == true || item.aiData?.contains("recovery") == true
                }

                // Local check: are there any non-recovery items in this session? If yes, prefer showing the completed session UI.
                val hasNonRecoveryLocal = sessionWithItems.items.any { item ->
                    !(item.aiData?.contains("squat_recovery") == true || item.aiData?.contains("recovery") == true)
                }

                com.programminghut.pose_detection.util.todayDebug("📝 Checking hasRecoveryItems for date $pageDate: $hasRecoveryItems")
                com.programminghut.pose_detection.util.todayDebug("📋 Items in session: ${sessionWithItems.items.size}")
                com.programminghut.pose_detection.util.todayDebug("💪 Has activity items: $hasActivityItems")
                sessionWithItems.items.forEach { item ->
                    com.programminghut.pose_detection.util.todayDebug("   📌 Item: exerciseId=${item.exerciseId}, aiData=${item.aiData}, countsAsActivity=${item.countsAsActivity}")
                }

                // Avoid transient UI when isRecovered state from produceState lags behind session emission.
                // Compute a local recovered state together with a 'determined' flag so we only show RECOVERED
                // after we've recomputed using fresh data (including a directRecovery check).
                var isRecoveredLocal by remember(pageDate, sessionWithItems) { mutableStateOf(false) }
                var recoveryDetermined by remember(pageDate, sessionWithItems) { mutableStateOf(false) }

                LaunchedEffect(pageDate, sessionWithItems) {
                    recoveryDetermined = false
                    val hasAnySession = sessionWithItems.items.isNotEmpty()

                    // Re-evaluate local markers
                    var hasRecoveryItemLocal = false
                    var hasNonRecoveryItemLocal = false
                    for (item in sessionWithItems.items) {
                        val isRecoveryMarker = item.aiData?.contains("squat_recovery") == true || item.aiData?.contains("recovery") == true
                        if (isRecoveryMarker) hasRecoveryItemLocal = true else hasNonRecoveryItemLocal = true
                        if (hasRecoveryItemLocal && hasNonRecoveryItemLocal) break
                    }

                    // Query repository for directRecovery synchronously here to avoid ordering issues
                    val directRecovery = todayViewModel.isDateRecovered(pageDate)

                    val newValue = (hasRecoveryItemLocal && !hasNonRecoveryItemLocal) || (directRecovery && !hasAnySession)
                    isRecoveredLocal = newValue
                    recoveryDetermined = true
                    com.programminghut.pose_detection.util.todayDebug("🔁 Local recovery re-eval for date $pageDate -> isRecoveredLocal=$isRecoveredLocal (directRecovery=$directRecovery, hasRecoveryItemLocal=$hasRecoveryItemLocal, hasNonRecoveryItemLocal=$hasNonRecoveryItemLocal)")
                }

                if (isInPast && recoveryDetermined && isRecoveredLocal && !hasNonRecoveryLocal) {
                    // ✅ Mostra messaggio di "Giorno recuperato" solo se effettivamente recuperato
                    com.programminghut.pose_detection.util.todayDebug("🎉 Showing RECOVERED day with exercises for date $pageDate (isRecoveredLocal=$isRecoveredLocal, hasNonRecoveryLocal=$hasNonRecoveryLocal)")
                    
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(vertical = 16.dp),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        // ✅ Prima mostra il messaggio di recupero
                        item {
                            EmptyHistoryCard(
                                isInPast = isInPast,
                                pageDate = pageDate,
                                isRecovered = true,
                                aiSquatCameraLauncher = aiSquatCameraLauncher
                            )
                        }
                        
                        // ✅ Poi mostra l'header della sessione
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Esercizi completati",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "${groupedItems.size}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        
                        // ✅ Infine mostra gli esercizi in modalità read-only
                        items(groupedItems) { item ->
                            when (item) {
                                is GroupedSessionItem.WorkoutGroup -> {
                                    WorkoutGroupCard(
                                        workout = item.workout,
                                        exercises = item.exercises,
                                        exerciseTemplates = exercises,
                                        todayViewModel = todayViewModel,
                                        isReadOnly = true // Read-only per giorni passati
                                    )
                                }
                                is GroupedSessionItem.StandaloneExercise -> {
                                    StandaloneExerciseCard(
                                        exercise = item.exercise,
                                        exercises = exercises,
                                        todayViewModel = todayViewModel,
                                        isReadOnly = true // Read-only per giorni passati
                                    )
                                }
                            }
                        }
                        
                        // ✅ Wellness Tracker Section (read-only for recovered days)
                        item {
                            Spacer(modifier = Modifier.height(24.dp))
                            val wellnessItemsWithDetails = remember(sessionWithItems) {
                                wellnessTrackers.map { wellnessItem ->
                                    DailySessionItemWithDetails(
                                        itemId = wellnessItem.itemId,
                                        sessionId = wellnessItem.sessionId,
                                        exerciseId = null,
                                        workoutId = null,
                                        type = SessionItemType.WELLNESS_TRACKER.name,
                                        order = wellnessItem.order,
                                        targetReps = null,
                                        targetTime = null,
                                        actualReps = null,
                                        actualTime = null,
                                        isCompleted = wellnessItem.isCompleted,
                                        completedAt = wellnessItem.completedAt,
                                        notes = wellnessItem.notes ?: "",
                                        aiData = null,
                                        countsAsActivity = wellnessItem.countsAsActivity,
                                        trackerTemplateId = wellnessItem.trackerTemplateId,
                                        trackerResponseJson = wellnessItem.trackerResponseJson,
                                        name = "",
                                        description = null,
                                        parentWorkoutItemId = null,
                                        exerciseName = null,
                                        exerciseDescription = null,
                                        exerciseImagePath = null,
                                        exerciseType = null,
                                        exerciseMode = null,
                                        workoutName = null,
                                        workoutDescription = null,
                                        workoutImagePath = null
                                    )
                                }
                            }
                            com.programminghut.pose_detection.ui.components.WellnessSection(
                                wellnessItems = wellnessItemsWithDetails,
                                onAddWellnessClick = { },
                                onItemClick = { },
                                onItemDelete = { }
                            )
                        }
                    }
                } else {
                    // ✅ Mostra la lista normale degli esercizi
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(vertical = 16.dp),
                        contentPadding = PaddingValues(bottom = 80.dp) // ✅ Spazio per il FAB
                    ) {
                        item {
                            if (isInPast && !isRecovered) {
                                // Show a positive EmptyHistoryCard for completed past days
                                // ✅ Only mark as completed if there are actual activity items (not just wellness trackers)
                                EmptyHistoryCard(
                                    isInPast = true,
                                    pageDate = pageDate,
                                    isRecovered = false,
                                    isCompleted = hasActivityItems,  // ✅ TRUE only if has exercises/workouts
                                    aiSquatCameraLauncher = aiSquatCameraLauncher
                                )
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (canAddExercises) "Sessione di allenamento" else "Sessione completata",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = if (canAddExercises) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    // ✅ Indicatore visivo per sessioni passate/future
                                    if (!canAddExercises) {
                                        Icon(
                                            Icons.Default.History,
                                            contentDescription = "Sessione non modificabile",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                        
                        items(groupedItems) { groupedItem ->
                            when (groupedItem) {
                                is GroupedSessionItem.WorkoutGroup -> {
                                    WorkoutGroupCard(
                                        workout = groupedItem.workout,
                                        exercises = groupedItem.exercises,
                                        exerciseTemplates = exercises,
                                        todayViewModel = todayViewModel,
                                        isReadOnly = !canAddExercises
                                    )
                                }
                                is GroupedSessionItem.StandaloneExercise -> {
                                    StandaloneExerciseCard(
                                        exercise = groupedItem.exercise,
                                        exercises = exercises,
                                        todayViewModel = todayViewModel,
                                        isReadOnly = !canAddExercises
                                    )
                                }
                            }
                        }
                        
                        // ✅ Wellness Tracker Section
                        item {
                            Spacer(modifier = Modifier.height(24.dp))
                            val wellnessItemsWithDetails = remember(sessionWithItems) {
                                wellnessTrackers.map { wellnessItem ->
                                    DailySessionItemWithDetails(
                                        itemId = wellnessItem.itemId,
                                        sessionId = wellnessItem.sessionId,
                                        exerciseId = null,
                                        workoutId = null,
                                        type = SessionItemType.WELLNESS_TRACKER.name,
                                        order = wellnessItem.order,
                                        targetReps = null,
                                        targetTime = null,
                                        actualReps = null,
                                        actualTime = null,
                                        isCompleted = wellnessItem.isCompleted,
                                        completedAt = wellnessItem.completedAt,
                                        notes = wellnessItem.notes ?: "",
                                        aiData = null,
                                        countsAsActivity = wellnessItem.countsAsActivity,
                                        trackerTemplateId = wellnessItem.trackerTemplateId,
                                        trackerResponseJson = wellnessItem.trackerResponseJson,
                                        name = "",
                                        description = null,
                                        parentWorkoutItemId = null,
                                        exerciseName = null,
                                        exerciseDescription = null,
                                        exerciseImagePath = null,
                                        exerciseType = null,
                                        exerciseMode = null,
                                        workoutName = null,
                                        workoutDescription = null,
                                        workoutImagePath = null
                                    )
                                }
                            }
                            com.programminghut.pose_detection.ui.components.WellnessSection(
                                wellnessItems = wellnessItemsWithDetails,
                                onAddWellnessClick = {
                                    if (canAddExercises) {
                                        onShowWellnessPicker()
                                    }
                                },
                                onItemClick = { itemDetails ->
                                    if (canAddExercises) {
                                        // Carica il template e mostra il dialog di modifica
                                        val fileManager = com.programminghut.pose_detection.data.manager.WellnessTrackerFileManager(context)
                                        itemDetails.trackerTemplateId?.let { templateId ->
                                            val tracker = fileManager.getTrackerById(templateId)
                                            if (tracker != null) {
                                                onShowWellnessEntry(tracker)
                                            }
                                        }
                                    }
                                },
                                onItemDelete = { itemDetails ->
                                    if (canAddExercises) {
                                        onDeleteWellnessItem(itemDetails)
                                    }
                                }
                            )
                        }
                        
                    }
                }
            }
        } ?: if (canAddExercises) {
            EmptySessionCard(onAddClick = onAddClick)
        } else {
            EmptyHistoryCard(
                isInPast = isInPast,
                pageDate = pageDate,
                isRecovered = isRecovered,
                aiSquatCameraLauncher = aiSquatCameraLauncher
            )
        }
    }
}

@Composable
fun SessionItemCard(
    sessionItem: DailySessionItem,
    exercises: List<ExerciseTemplate>, // ✅ Aggiunto parametro esercizi
    todayViewModel: TodayViewModel, // ✅ Aggiunto per espansione automatica
    onComplete: (Long) -> Unit,
    onRepsUpdate: (Long, Int) -> Unit,
    onTimeUpdate: (Long, Int) -> Unit = { _, _ -> }, // Aggiungiamo callback per tempo
    onNavigateToExercise: (Long) -> Unit = { }, // Callback per navigare all'esercizio
    onRemove: (Long) -> Unit = { }, // ✅ Nuovo callback per rimuovere
    isReadOnly: Boolean = false, // ✅ Nuova modalità read-only per il passato
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // ✅ State per espansione/collasso della card
    var isExpanded by remember { mutableStateOf(false) }
    
    // ✅ Osserva l'ultimo elemento aggiunto per espansione automatica
    val lastAddedItemId by todayViewModel.lastAddedItemId.collectAsState()
    
    // ✅ Espandi automaticamente se questo è l'ultimo elemento aggiunto
    LaunchedEffect(lastAddedItemId) {
        if (lastAddedItemId == sessionItem.itemId && lastAddedItemId != null) {
            isExpanded = true
            // Reset dopo aver gestito l'espansione
            todayViewModel.clearLastAddedItem()
        }
    }
    
    // Get exercise name from JSON templates (use exerciseId as template.id)
    var exerciseName by remember(sessionItem.exerciseId, sessionItem.aiData) { 
        mutableStateOf(when {
            // ✅ AI Squat detection by aiData marker
            sessionItem.aiData?.contains("squat_ai") == true -> "🤖 AI Squat"
            // Normal exercise: exerciseId = template.id, find in exercises list
            sessionItem.exerciseId != null -> {
                val exercise = exercises.find { it.id == sessionItem.exerciseId }
                exercise?.name ?: "Esercizio #${sessionItem.exerciseId}"
            }
            // Fallback
            else -> "Attività personalizzata"
        })
    }
    
    // Nessun LaunchedEffect - il nome è già risolto dal JSON
    // La lista 'exercises' contiene tutti gli esercizi dal JSON con id = template.id
    
    val workoutName = remember(sessionItem.workoutId) {
        sessionItem.workoutId?.let { 
            getWorkoutNameById(it) 
        } ?: "Allenamento personalizzato"
    }

    // Calculate progress for background
    val progress = remember(sessionItem) {
        when {
            sessionItem.customReps != null -> {
                val actualReps = sessionItem.actualReps ?: 0
                actualReps.toFloat() / sessionItem.customReps.toFloat()
            }
            sessionItem.customTime != null -> {
                val actualTime = sessionItem.actualTime ?: 0
                actualTime.toFloat() / sessionItem.customTime.toFloat()
            }
            else -> if (sessionItem.isCompleted) 1f else 0f
        }
    }.coerceIn(0f, 1f)

    // ✅ Card compatta e collassabile
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded },
        colors = CardDefaults.cardColors(
            containerColor = if (sessionItem.isCompleted) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isExpanded) 4.dp else 2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // ✅ Header compatto - sempre visibile
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // ✅ Icona + Nome compatto
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Icona dell'esercizio
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (sessionItem.aiData?.contains("squat_ai") == true) 
                                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)
                                else MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when {
                                sessionItem.aiData?.contains("squat_ai") == true -> Icons.Default.VideoCall
                                sessionItem.exerciseId != null -> {
                                    val exercise = exercises.find { it.id == sessionItem.exerciseId }
                                    exercise?.let { ex ->
                                        when (ex.iconName) {
                                            "Accessibility" -> Icons.Default.Accessibility
                                            "DirectionsRun" -> Icons.Default.DirectionsRun
                                            "Timer" -> Icons.Default.Timer
                                            "FitnessCenter" -> Icons.Default.FitnessCenter
                                            "Science" -> Icons.Default.Science
                                            else -> Icons.Default.FitnessCenter
                                        }
                                    } ?: Icons.Default.FitnessCenter
                                }
                                else -> Icons.Default.FitnessCenter
                            },
                            contentDescription = null,
                            tint = if (sessionItem.aiData?.contains("squat_ai") == true) 
                                MaterialTheme.colorScheme.tertiary
                            else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = when {
                                sessionItem.exerciseId != null -> exerciseName
                                sessionItem.workoutId != null -> workoutName
                                else -> "Attività personalizzata"
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = if (sessionItem.isCompleted) {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        
                        // ✅ Progress compatto
                        Text(
                            text = when {
                                sessionItem.customReps != null -> {
                                    val actual = sessionItem.actualReps ?: 0
                                    "$actual/${sessionItem.customReps} reps"
                                }
                                sessionItem.customTime != null -> {
                                    val actual = sessionItem.actualTime ?: 0
                                    "${actual}/${sessionItem.customTime}s"
                                }
                                sessionItem.itemType == SessionItemType.WORKOUT -> "Workout"
                                else -> "Libero"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
                
                // ✅ Controlli compatti sulla destra
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // ✅ Progresso visuale circolare
                    if (progress > 0f) {
                        CircularProgressIndicator(
                            progress = progress,
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 3.dp,
                        )
                    }
                    
                    // ✅ Checkbox/Status
                    if (isReadOnly) {
                        Icon(
                            imageVector = if (sessionItem.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            contentDescription = if (sessionItem.isCompleted) "Completato" else "Non completato",
                            tint = if (sessionItem.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Checkbox(
                            checked = sessionItem.isCompleted,
                            onCheckedChange = { onComplete(sessionItem.itemId) },
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    
                    // ✅ Expand/Collapse indicator
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "Riduci" else "Espandi",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // ✅ Sezione espandibile con dettagli e controlli
            if (isExpanded) {
                Divider(modifier = Modifier.padding(horizontal = 16.dp))
                
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // ✅ Controlli interattivi (solo se non read-only)
                    if (!isReadOnly) {
                        when {
                            sessionItem.customReps != null -> {
                                RepsControlSection(
                                    currentReps = sessionItem.actualReps ?: 0,
                                    targetReps = sessionItem.customReps!!,
                                    onRepsChange = { newReps -> 
                                        onRepsUpdate(sessionItem.itemId, newReps)
                                    }
                                )
                            }
                            sessionItem.customTime != null -> {
                                TimeControlSection(
                                    currentTime = sessionItem.actualTime ?: 0,
                                    targetTime = sessionItem.customTime!!,
                                    onTimeChange = { newTime ->
                                        onTimeUpdate(sessionItem.itemId, newTime)
                                    }
                                )
                            }
                        }
                    }
                    
                    // ✅ Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // ✅ Start/Navigate button
                        OutlinedButton(
                            onClick = {
                                when {
                                    sessionItem.exerciseId != null -> onNavigateToExercise(sessionItem.exerciseId!!)
                                    sessionItem.aiData?.contains("squat_ai") == true -> {
                                        Log.d("TODAY_DEBUG", "🤖 Avviando AI Squat camera")
                                        val intent = Intent(context, CameraSelectionActivity::class.java).apply {
                                            putExtra("EXERCISE_ID", sessionItem.itemId)
                                            putExtra("EXERCISE_NAME", "AI Squat")
                                            putExtra("EXERCISE_TYPE", "SQUAT")
                                            putExtra("MODE", "AI_SQUAT")
                                        }
                                        context.startActivity(intent)
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            enabled = !isReadOnly
                        ) {
                            Icon(
                                imageVector = if (sessionItem.aiData?.contains("squat_ai") == true) 
                                    Icons.Default.VideoCall 
                                else 
                                    Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (sessionItem.aiData?.contains("squat_ai") == true) 
                                    "Avvia AI" 
                                else 
                                    "Inizia",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        
                        // ✅ Remove button (solo se non read-only)
                        if (!isReadOnly) {
                            OutlinedButton(
                                onClick = { onRemove(sessionItem.itemId) },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Rimuovi",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Helper functions for new design
@Composable 
fun RepsControlSection(
    currentReps: Int,
    targetReps: Int,
    onRepsChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "Ripetizioni",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "$currentReps",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
    }
}

@Composable
fun TimeControlSection(
    currentTime: Int,
    targetTime: Int,
    onTimeChange: (Int) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Tempo (secondi)",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Text(
                text = "${currentTime}s",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Slider(
            value = currentTime.toFloat(),
            onValueChange = { newTime -> 
                onTimeChange(newTime.toInt())
            },
            valueRange = 0f..targetTime.toFloat(),
            steps = maxOf(0, targetTime - 1),
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleAddItemBottomSheet(
    onDismiss: () -> Unit,
    onAddExercise: () -> Unit,
    onAddWorkout: () -> Unit,
    onAddAISquat: () -> Unit = { }, // ✅ Nuovo callback per AI Squat
    onAddWellness: () -> Unit = { } // ✅ Nuovo callback per Wellness Tracker
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Aggiungi alla sessione",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            // ✅ AI Squat - Opzione speciale in evidenza
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                ),
                onClick = onAddAISquat
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.VideoCall,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Column {
                        Text(
                            text = "🤖 AI Squat",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Text(
                            text = "Conteggio automatico con camera",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }
            
            Button(
                onClick = onAddExercise,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.DirectionsRun,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Aggiungi Esercizio")
            }
            
            Button(
                onClick = onAddWorkout,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.FitnessCenter,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Aggiungi Allenamento")
            }
            
            Button(
                onClick = onAddWellness,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Aggiungi Wellness Tracker")
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun ExerciseLibraryScreen() {
    val context = LocalContext.current
    
    LaunchedEffect(Unit) {
        val intent = Intent(context, ExerciseLibraryActivity::class.java)
        context.startActivity(intent)
    }
    
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator()
            Text(
                text = "Apertura Libreria Esercizi...",
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
fun WorkoutLibraryScreen(workoutSelectionLauncher: androidx.activity.result.ActivityResultLauncher<Intent>) {
    val context = LocalContext.current
    
    LaunchedEffect(Unit) {
        val intent = Intent(context, WorkoutLibraryActivity::class.java).apply {
            putExtra("SELECTION_MODE", true)
        }
        Log.d("TODAY_DEBUG", "🏃 Navigazione WorkoutLibraryScreen con workoutSelectionLauncher")
        workoutSelectionLauncher.launch(intent)
    }
    
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator()
            Text(
                text = "Apertura Libreria Allenamenti...",
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
fun HistoryScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "📊 STORICO\n\nCronologia delle sessioni completate.\nSolo dati reali.",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun DashboardScreen(
    navController: NavController, 
    todayViewModel: TodayViewModel,
    onStartRecovery: (Long) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope() // ✅ Aggiungi scope per navigazione
    var showExportDialog by remember { mutableStateOf(false) }
    var showExportSettings by remember { mutableStateOf(false) }
    var showCalendarDialog by remember { mutableStateOf(false) }
    
    // ✅ Ottieni riferimento all'Activity per impostare la callback
    val activity = context as? NewMainActivity
    
    // Database per statistiche 
    val database = remember { com.programminghut.pose_detection.data.database.AppDatabase.getDatabase(context) }
    val sessionRepository = remember { 
        SessionRepository(
            database.sessionDao(),
            database.repDao()
        )
    }
    val dailySessionRepository = remember {
        DailySessionRepository(
            database.dailySessionDao(),
            database.dailySessionRelationDao(),
            database.exerciseDao(),
            database.workoutDao()
        )
    }
    
    // Stati per le statistiche
    val totalSessions by sessionRepository.getTotalSessionsCount().collectAsState(initial = 0)
    
    // ✅ Conteggio squat: usa il template JSON per trovare l'ID
    // Squat nel JSON ha id=2, quindi cercherò esercizi con exerciseId=2
    val exercises = remember {
        try {
            com.programminghut.pose_detection.util.ExerciseTemplateFileManager.loadExerciseTemplates(context)
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    val squatExerciseId = remember(exercises) {
        val squatTemplate = exercises.find { it.name == "Squat" }
        squatTemplate?.id
    }

    val totalSquatsFlow = remember(squatExerciseId) {
        if (squatExerciseId != null) {
            dailySessionRepository.getTotalSquatAggregateCount(squatExerciseId!!)
        } else {
            // Fallback: se non trovi il template, usa il conteggio per nome
            dailySessionRepository.getTotalCountByTemplateName(context, "Squat")
        }
    }
    val totalSquats by totalSquatsFlow.collectAsState(initial = 0)

    val todaySession by todayViewModel.todaySession.collectAsState()
    
    // ViewModels per Calendar e Export
    val calendarViewModel: CalendarViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return CalendarViewModel(sessionRepository, dailySessionRepository) as T
            }
        }
    )
    
    // ✅ Ottieni lo streak dal CalendarViewModel
    val calendarUiState by calendarViewModel.uiState.collectAsState()
    val currentStreak = when (val state = calendarUiState) {
        is CalendarUiState.Success -> state.currentStreak
        else -> 0
    }
    
    // ✅ Imposta la callback per il refresh del calendario al mount del componente
    LaunchedEffect(calendarViewModel) {
        activity?.refreshCalendarCallback = {
            Log.d("TODAY_DEBUG", "🔄 Refreshing calendar from callback...")
            calendarViewModel.loadCalendarData()
        }
        Log.d("TODAY_DEBUG", "✅ Callback del calendario impostata con successo")
    }
    
    // ✅ Cleanup della callback quando il componente si smonta
    DisposableEffect(Unit) {
        onDispose {
            activity?.refreshCalendarCallback = null
        }
    }
    
    val exportViewModel: ExportViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ExportViewModel(sessionRepository, dailySessionRepository) as T
            }
        }
    )
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ✅ Header della Dashboard
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "📈 Dashboard",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Panoramica del tuo progresso",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        // ✅ Statistiche rapide con Squat totali in evidenza
        // Prima riga: Card grande per squat totali
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { navController.navigate("today") },
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.FitnessCenter,
                    contentDescription = "Squat totali",
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "$totalSquats",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "🦵 SQUAT TOTALI",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "AI + Manuali + Recupero",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )
            }
        }
        
        // ✅ Statistiche rapide - una riga con 3 card
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatCard(
                title = "Oggi",
                value = "${todaySession?.items?.size ?: 0}",
                subtitle = "esercizi",
                icon = Icons.Default.Today,
                onClick = { navController.navigate("today") },
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "Streak",
                value = "$currentStreak",
                subtitle = "giorni",
                icon = Icons.Default.LocalFireDepartment,
                onClick = { showCalendarDialog = true },
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "Calendario",
                value = "📅",
                subtitle = "costanza",
                icon = Icons.Default.CalendarMonth,
                onClick = { showCalendarDialog = true },
                modifier = Modifier.weight(1f)
            )
        }
        
        // ✅ Azioni rapide
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Azioni Rapide",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { showExportDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Export CSV")
                    }
                }
            }
        }
    }
    
    // ✅ Dialog per il calendario completo
    if (showCalendarDialog) {
        // ✅ Refresh automatico del calendario ogni volta che viene aperto
        LaunchedEffect(showCalendarDialog) {
            if (showCalendarDialog) {
                Log.d("TODAY_DEBUG", "🔄 Refresh automatico calendario all'apertura")
                calendarViewModel.loadCalendarData()
            }
        }
        
        Dialog(
            onDismissRequest = { showCalendarDialog = false },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = true,
                dismissOnClickOutside = true
            )
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                StreakCalendarScreen(
                    viewModel = calendarViewModel,
                    onBackClick = { showCalendarDialog = false },
                    onDayClick = { timestamp, dayStatus ->
                        Log.d("TODAY_DEBUG", "🗓️ Calendar day clicked - timestamp: $timestamp, dayStatus: $dayStatus")
                        // ✅ Comportamento differenziato per giorni mancati vs normali  
                        if (dayStatus == DayStatus.MISSED) {
                            // Chiudi calendario e avvia recovery
                            Log.d("TODAY_DEBUG", "🔧 MISSED day - starting recovery for $timestamp")
                            showCalendarDialog = false
                            onStartRecovery(timestamp)
                        } else {
                            // ✅ Comportamento normale: imposta data e naviga 
                            Log.d("TODAY_DEBUG", "📅 Normal navigation - setting selectedDate to $timestamp and navigating to today")
                            todayViewModel.setSelectedDate(timestamp)
                            showCalendarDialog = false
                            navController.navigate("today")
                        }
                    },
                    onRecoveryClick = { timestamp ->
                        // ✅ Recovery click specifico - avvia direttamente AI squat
                        showCalendarDialog = false
                        onStartRecovery(timestamp)
                    }
                )
            }
        }
    }
    
    // ✅ Dialog per export
    if (showExportDialog) {
        Dialog(
            onDismissRequest = { showExportDialog = false },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = true,
                dismissOnClickOutside = true
            )
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                ExportScreen(
                    viewModel = exportViewModel,
                    onBackClick = { showExportDialog = false },
                    onSettingsClick = { 
                        showExportDialog = false
                        showExportSettings = true
                    },
                    onExportClick = { content, fileName, mimeType ->
                        // Get user context from preferences
                        val userContext = FileExportHelper.getPersonalizedUserContext(context)
                        
                        // Export and share the file
                        FileExportHelper.exportAndShare(
                            context = context,
                            content = content,
                            fileName = fileName,
                            mimeType = mimeType,
                            userContext = userContext
                        )
                        
                        showExportDialog = false
                    }
                )
            }
        }
    }
    
    // Export Settings Dialog
    if (showExportSettings) {
        Dialog(
            onDismissRequest = { showExportSettings = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                com.programminghut.pose_detection.ui.export.ExportSettingsScreen(
                    onBackClick = { showExportSettings = false }
                )
            }
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun EmptyHistoryCard(
    isInPast: Boolean, 
    pageDate: Long, 
    isRecovered: Boolean = false,
    isCompleted: Boolean = false,
    aiSquatCameraLauncher: androidx.activity.result.ActivityResultLauncher<Intent>
) {
    val context = LocalContext.current
    
    // ✅ Ottieni frase motivazionale casuale
    val motivationalQuote = remember { 
        MotivationalQuotes.getRandomQuote() 
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isInPast && isCompleted -> MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                isInPast && isRecovered -> MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                isInPast && !isRecovered -> MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = when {
                    isInPast && isCompleted -> Icons.Default.SentimentVerySatisfied
                    isInPast && isRecovered -> Icons.Default.SentimentVerySatisfied
                    isInPast && !isRecovered -> Icons.Default.SentimentVeryDissatisfied
                    else -> Icons.Default.History
                },
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = when {
                    isInPast && isCompleted -> MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                    isInPast && isRecovered -> MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                    isInPast && !isRecovered -> MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = when {
                    isInPast && isCompleted -> "Giorno completo"
                    isInPast && isRecovered -> "Giorno recuperato"
                    isInPast && !isRecovered -> "Giorno perso"
                    else -> "Nessun allenamento"
                },
                style = MaterialTheme.typography.titleMedium,
                color = when {
                    isInPast && isCompleted -> MaterialTheme.colorScheme.primary
                    isInPast && isRecovered -> MaterialTheme.colorScheme.primary
                    isInPast && !isRecovered -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            when {
                isInPast && !isRecovered && !isCompleted -> {
                    // Giorno passato NON recuperato - mostra frase motivazionale e pulsante recupero
                    Text(
                        text = motivationalQuote,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Pulsante per recuperare il giorno
                    Button(
                        onClick = {
                            val intent = Intent(context, CameraSelectionActivity::class.java).apply {
                                putExtra("MODE", "RECOVERY")
                                putExtra("RECOVERY_DATE", pageDate)
                                putExtra("RECOVERY_TARGET_SQUAT", 20)
                            }
                            aiSquatCameraLauncher.launch(intent)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Recupera Giorno (20 Squat AI)")
                    }
                }

                isInPast && isCompleted -> {
                    // Completed past day - show motivational quote, no recovery button
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = motivationalQuote,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        lineHeight = 18.sp
                    )
                }

                isInPast && isRecovered -> {
                    // Giorno passato RECUPERATO - mostra messaggio positivo con emoticon
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "🎉",
                            style = MaterialTheme.typography.displayMedium,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )

                        Text(
                            text = "Hai recuperato questo giorno completando 20 squat con l'IA!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = motivationalQuote,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                            lineHeight = 16.sp
                        )
                    }
                }

                else -> {
                    // Giorno attuale - nessun allenamento
                    Text(
                        text = "Gli esercizi possono essere aggiunti solo oggi",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun EmptySessionCard(
    onAddClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Inizia la tua sessione",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = "Aggiungi esercizi o allenamenti per iniziare",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = onAddClick
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Aggiungi primo elemento")
            }
        }
    }
}

/**
 * Utility function to get workout name by ID from templates
 */
fun getWorkoutNameById(workoutId: Long): String {
    val templates = mapOf(
        1L to "💪 Upper Body Power",
        2L to "🏃 Cardio Blast",
        3L to "🧘 Core & Balance"
    )
    return templates[workoutId] ?: "Allenamento #$workoutId"
}

/**
 * ✅ Componente per gruppi di workout (workout + esercizi figli)
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WorkoutGroupCard(
    workout: DailySessionItem,
    exercises: List<DailySessionItem>,
    exerciseTemplates: List<ExerciseTemplate>, // ✅ Aggiunto parametro template esercizi
    todayViewModel: TodayViewModel,
    isReadOnly: Boolean,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    var showDeleteButton by remember { mutableStateOf(false) }
    
    // Auto-espansione se è l'ultimo elemento aggiunto
    val lastAddedItemId by todayViewModel.lastAddedItemId.collectAsState()
    LaunchedEffect(lastAddedItemId) {
        if (lastAddedItemId == workout.itemId && lastAddedItemId != null) {
            isExpanded = true
            todayViewModel.clearLastAddedItem()
        }
    }
    
    val workoutName = remember(workout.workoutId) {
        workout.workoutId?.let { getWorkoutNameById(it) } ?: "Allenamento personalizzato"
    }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isExpanded) 6.dp else 2.dp)
    ) {
        Column {
            // Header del workout (sempre visibile) - con long press per mostrare elimina
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = { isExpanded = !isExpanded },
                        onLongClick = { 
                            if (!isReadOnly) {
                                showDeleteButton = !showDeleteButton 
                            }
                        }
                    )
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Icona del workout
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FitnessCenter,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Column {
                        Text(
                            text = workoutName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${exercises.size} esercizi",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // ✅ Bottone elimina - visibile solo dopo long press
                    AnimatedVisibility(
                        visible = showDeleteButton && !isReadOnly,
                        enter = fadeIn() + scaleIn(),
                        exit = fadeOut() + scaleOut()
                    ) {
                        IconButton(
                            onClick = { 
                                todayViewModel.removeSessionItem(workout.itemId)
                                showDeleteButton = false // Nascondi dopo l'eliminazione
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Elimina workout",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    
                    // Icona di espansione
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "Comprimi" else "Espandi",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Lista degli esercizi (visibile quando espanso)
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                ) {
                    Divider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                    
                    exercises.forEach { exercise ->
                        SimpleExerciseItem(
                            exercise = exercise,
                            exercises = exerciseTemplates, // ✅ Passa i template esercizi
                            todayViewModel = todayViewModel,
                            isReadOnly = isReadOnly,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * ✅ Componente per esercizi standalone (senza workout parent)
 */
@Composable
fun StandaloneExerciseCard(
    exercise: DailySessionItem,
    exercises: List<ExerciseTemplate>, // ✅ Aggiunto parametro esercizi
    todayViewModel: TodayViewModel,
    isReadOnly: Boolean,
    modifier: Modifier = Modifier
) {
    SimpleExerciseItem(
        exercise = exercise,
        exercises = exercises, // ✅ Passa esercizi
        todayViewModel = todayViewModel,
        isReadOnly = isReadOnly,
        modifier = modifier.padding(vertical = 4.dp)
    )
}

/**
 * ✅ Componente semplificato per singolo esercizio (sempre compatto, bottone elimina con long press)
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SimpleExerciseItem(
    exercise: DailySessionItem,
    exercises: List<ExerciseTemplate>, // ✅ Aggiunto parametro esercizi
    todayViewModel: TodayViewModel,
    isReadOnly: Boolean,
    modifier: Modifier = Modifier
) {
    // ✅ State per mostrare/nascondere il bottone elimina
    var showDeleteButton by remember { mutableStateOf(false) }
    
    // Get exercise name
    val exerciseName = remember(exercise.exerciseId, exercise.aiData) {
        when {
            exercise.aiData?.contains("squat_ai") == true -> "🤖 AI Squat"
            exercise.exerciseId != null -> {
                val exerciseTemplate = exercises.find { it.id == exercise.exerciseId }
                exerciseTemplate?.name ?: "Esercizio #${exercise.exerciseId}"
            }
            else -> "Attività personalizzata"
        }
    }
    
    Card(
        modifier = modifier
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        // Header dell'esercizio - sempre compatto, con long press per mostrare elimina
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = { /* Nessuna azione sul click normale */ },
                    onLongClick = { 
                        if (!isReadOnly && exercise.parentWorkoutItemId == null) {
                            showDeleteButton = !showDeleteButton 
                        }
                    }
                )
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icona dell'esercizio
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (exercise.aiData?.contains("squat_ai") == true) 
                                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)
                            else MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when {
                            exercise.aiData?.contains("squat_ai") == true -> Icons.Default.VideoCall
                            exercise.exerciseId != null -> {
                                val exerciseTemplate = exercises.find { it.id == exercise.exerciseId }
                                exerciseTemplate?.let { ex ->
                                    when (ex.iconName) {
                                        "Accessibility" -> Icons.Default.Accessibility
                                        "DirectionsRun" -> Icons.Default.DirectionsRun
                                        "Timer" -> Icons.Default.Timer
                                        "FitnessCenter" -> Icons.Default.FitnessCenter
                                        "Science" -> Icons.Default.Science
                                        else -> Icons.Default.FitnessCenter
                                    }
                                } ?: Icons.Default.FitnessCenter
                            }
                            else -> Icons.Default.FitnessCenter
                        },
                        contentDescription = null,
                        tint = if (exercise.aiData?.contains("squat_ai") == true) 
                            MaterialTheme.colorScheme.tertiary
                        else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Column {
                    Text(
                        text = exerciseName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    
                    // Mostra quantità impostata
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        when {
                            exercise.customReps != null -> {
                                Text(
                                    text = "${exercise.customReps} ripetizioni",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            exercise.customTime != null -> {
                                Text(
                                    text = "${exercise.customTime}s",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            else -> {
                                Text(
                                    text = "Quantità libera",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
            
            // ✅ Bottone elimina - visibile solo dopo long press
            AnimatedVisibility(
                visible = showDeleteButton && !isReadOnly && exercise.parentWorkoutItemId == null,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                IconButton(
                    onClick = { 
                        todayViewModel.removeSessionItem(exercise.itemId)
                        showDeleteButton = false // Nascondi dopo l'eliminazione
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Elimina esercizio",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}