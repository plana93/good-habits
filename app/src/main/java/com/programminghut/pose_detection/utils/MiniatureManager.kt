package com.programminghut.pose_detection.utils

import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import com.programminghut.pose_detection.R
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * Gestore per le miniature degli esercizi e allenamenti
 * Supporta generazione automatica, cache e gestione delle immagini
 */
class MiniatureManager(private val context: Context) {
    
    companion object {
        private const val MINIATURE_WIDTH = 300
        private const val MINIATURE_HEIGHT = 200
        private const val MINIATURE_QUALITY = 85
        private const val MINIATURES_DIR = "miniatures"
        
        // Colori per miniature generate automaticamente
        private val DEFAULT_COLORS = listOf(
            Color.parseColor("#FF6B6B"), // Rosso
            Color.parseColor("#4ECDC4"), // Turchese
            Color.parseColor("#45B7D1"), // Blu
            Color.parseColor("#96CEB4"), // Verde
            Color.parseColor("#FFEAA7"), // Giallo
            Color.parseColor("#DDA0DD"), // Viola
            Color.parseColor("#98D8C8"), // Verde acqua
            Color.parseColor("#F7DC6F"), // Giallo chiaro
            Color.parseColor("#BB8FCE"), // Lavanda
            Color.parseColor("#85C1E9")  // Azzurro
        )
        
        // Icone per tipi di esercizio
        private val EXERCISE_TYPE_ICONS = mapOf(
            "squat" to "🏋️",
            "pushup" to "💪",
            "plank" to "🧘",
            "jumping" to "🤸",
            "running" to "🏃",
            "cardio" to "❤️",
            "strength" to "💪",
            "flexibility" to "🧘",
            "balance" to "⚖️",
            "default" to "🏃"
        )
    }
    
    private val miniaturesDir: File by lazy {
        File(context.filesDir, MINIATURES_DIR).apply {
            if (!exists()) mkdirs()
        }
    }
    
    // ============================================================================
    // GESTIONE MINIATURE ESERCIZI
    // ============================================================================
    
    /**
     * Ottieni miniatura per esercizio
     * Se non esiste, la genera automaticamente
     */
    fun getExerciseMiniature(exerciseId: Long, exerciseName: String, exerciseType: String? = null): String? {
        val fileName = "exercise_${exerciseId}.jpg"
        val file = File(miniaturesDir, fileName)
        
        return if (file.exists()) {
            file.absolutePath
        } else {
            generateExerciseMiniature(exerciseId, exerciseName, exerciseType, file)
        }
    }
    
    /**
     * Genera miniatura automatica per esercizio
     */
    private fun generateExerciseMiniature(
        exerciseId: Long, 
        exerciseName: String, 
        exerciseType: String?,
        file: File
    ): String? {
        try {
            val bitmap = createExerciseBitmap(exerciseName, exerciseType, exerciseId)
            return saveBitmapToFile(bitmap, file)
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
    
    /**
     * Crea bitmap per miniatura esercizio
     */
    private fun createExerciseBitmap(exerciseName: String, exerciseType: String?, exerciseId: Long): Bitmap {
        val bitmap = Bitmap.createBitmap(MINIATURE_WIDTH, MINIATURE_HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        // Colore di sfondo basato su ID esercizio
        val backgroundColor = DEFAULT_COLORS[(exerciseId % DEFAULT_COLORS.size).toInt()]
        canvas.drawColor(backgroundColor)
        
        // Gradiente overlay
        val gradient = LinearGradient(
            0f, 0f, MINIATURE_WIDTH.toFloat(), MINIATURE_HEIGHT.toFloat(),
            backgroundColor, Color.parseColor("#80000000"),
            Shader.TileMode.CLAMP
        )
        val gradientPaint = Paint().apply { shader = gradient }
        canvas.drawRect(0f, 0f, MINIATURE_WIDTH.toFloat(), MINIATURE_HEIGHT.toFloat(), gradientPaint)
        
        // Icona tipo esercizio
        val icon = EXERCISE_TYPE_ICONS[exerciseType?.lowercase()] ?: EXERCISE_TYPE_ICONS["default"]!!
        val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 60f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT
        }
        
        canvas.drawText(
            icon,
            MINIATURE_WIDTH / 2f,
            MINIATURE_HEIGHT / 2f - 20f,
            iconPaint
        )
        
        // Nome esercizio
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 24f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setShadowLayer(2f, 1f, 1f, Color.parseColor("#80000000"))
        }
        
        // Tronca il nome se troppo lungo
        val displayName = if (exerciseName.length > 15) {
            "${exerciseName.take(12)}..."
        } else {
            exerciseName
        }
        
        canvas.drawText(
            displayName,
            MINIATURE_WIDTH / 2f,
            MINIATURE_HEIGHT - 30f,
            textPaint
        )
        
        return bitmap
    }
    
    // ============================================================================
    // GESTIONE MINIATURE ALLENAMENTI
    // ============================================================================
    
    /**
     * Ottieni miniatura per allenamento
     */
    fun getWorkoutMiniature(workoutId: Long, workoutName: String, exerciseCount: Int = 0): String? {
        val fileName = "workout_${workoutId}.jpg"
        val file = File(miniaturesDir, fileName)
        
        return if (file.exists()) {
            file.absolutePath
        } else {
            generateWorkoutMiniature(workoutId, workoutName, exerciseCount, file)
        }
    }
    
    /**
     * Genera miniatura automatica per allenamento
     */
    private fun generateWorkoutMiniature(
        workoutId: Long,
        workoutName: String,
        exerciseCount: Int,
        file: File
    ): String? {
        try {
            val bitmap = createWorkoutBitmap(workoutName, exerciseCount, workoutId)
            return saveBitmapToFile(bitmap, file)
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
    
    /**
     * Crea bitmap per miniatura allenamento
     */
    private fun createWorkoutBitmap(workoutName: String, exerciseCount: Int, workoutId: Long): Bitmap {
        val bitmap = Bitmap.createBitmap(MINIATURE_WIDTH, MINIATURE_HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        // Sfondo gradiente per allenamenti
        val color1 = DEFAULT_COLORS[(workoutId % DEFAULT_COLORS.size).toInt()]
        val color2 = DEFAULT_COLORS[((workoutId + 1) % DEFAULT_COLORS.size).toInt()]
        
        val gradient = LinearGradient(
            0f, 0f, MINIATURE_WIDTH.toFloat(), MINIATURE_HEIGHT.toFloat(),
            color1, color2,
            Shader.TileMode.CLAMP
        )
        val gradientPaint = Paint().apply { shader = gradient }
        canvas.drawRect(0f, 0f, MINIATURE_WIDTH.toFloat(), MINIATURE_HEIGHT.toFloat(), gradientPaint)
        
        // Overlay scuro
        val overlayPaint = Paint().apply {
            color = Color.parseColor("#40000000")
        }
        canvas.drawRect(0f, 0f, MINIATURE_WIDTH.toFloat(), MINIATURE_HEIGHT.toFloat(), overlayPaint)
        
        // Icona allenamento
        val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 50f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT
        }
        
        canvas.drawText(
            "🏋️‍♂️",
            MINIATURE_WIDTH / 2f,
            MINIATURE_HEIGHT / 2f - 30f,
            iconPaint
        )
        
        // Nome allenamento
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 22f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setShadowLayer(2f, 1f, 1f, Color.parseColor("#80000000"))
        }
        
        val displayName = if (workoutName.length > 18) {
            "${workoutName.take(15)}..."
        } else {
            workoutName
        }
        
        canvas.drawText(
            displayName,
            MINIATURE_WIDTH / 2f,
            MINIATURE_HEIGHT / 2f + 20f,
            textPaint
        )
        
        // Conteggio esercizi
        val countPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E0E0E0")
            textSize = 16f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT
        }
        
        canvas.drawText(
            "$exerciseCount esercizi",
            MINIATURE_WIDTH / 2f,
            MINIATURE_HEIGHT - 20f,
            countPaint
        )
        
        return bitmap
    }
    
    // ============================================================================
    // GESTIONE FILE E UTILITY
    // ============================================================================
    
    /**
     * Salva bitmap su file
     */
    private fun saveBitmapToFile(bitmap: Bitmap, file: File): String? {
        return try {
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, MINIATURE_QUALITY, out)
                out.flush()
            }
            file.absolutePath
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Elimina miniatura
     */
    fun deleteMiniature(imagePath: String?): Boolean {
        if (imagePath.isNullOrBlank()) return false
        
        return try {
            val file = File(imagePath)
            if (file.exists() && file.parentFile == miniaturesDir) {
                file.delete()
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Salva immagine personalizzata come miniatura
     */
    fun saveCustomMiniature(bitmap: Bitmap, type: String, id: Long): String? {
        val fileName = "${type}_${id}.jpg"
        val file = File(miniaturesDir, fileName)
        
        return try {
            // Ridimensiona se necessario
            val resizedBitmap = if (bitmap.width != MINIATURE_WIDTH || bitmap.height != MINIATURE_HEIGHT) {
                Bitmap.createScaledBitmap(bitmap, MINIATURE_WIDTH, MINIATURE_HEIGHT, true)
            } else {
                bitmap
            }
            
            saveBitmapToFile(resizedBitmap, file)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Ottieni miniatura di default per tipo
     */
    fun getDefaultMiniature(type: String): Drawable? {
        val resourceId = when (type.lowercase()) {
            "exercise" -> R.drawable.ic_launcher_foreground // Usiamo un'icona esistente
            "workout" -> R.drawable.ic_launcher_foreground   // Usiamo un'icona esistente
            else -> R.drawable.ic_launcher_foreground
        }
        
        return try {
            ContextCompat.getDrawable(context, resourceId)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Pulisci miniature orfane (senza entità corrispondenti)
     */
    fun cleanupOrphanedMiniatures(existingExerciseIds: List<Long>, existingWorkoutIds: List<Long>) {
        try {
            miniaturesDir.listFiles()?.forEach { file ->
                val fileName = file.name
                val shouldDelete = when {
                    fileName.startsWith("exercise_") -> {
                        val id = fileName.removePrefix("exercise_").removeSuffix(".jpg").toLongOrNull()
                        id != null && !existingExerciseIds.contains(id)
                    }
                    fileName.startsWith("workout_") -> {
                        val id = fileName.removePrefix("workout_").removeSuffix(".jpg").toLongOrNull()
                        id != null && !existingWorkoutIds.contains(id)
                    }
                    else -> false
                }
                
                if (shouldDelete) {
                    file.delete()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}