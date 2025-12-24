package com.programminghut.pose_detection.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import androidx.core.content.ContextCompat
import com.programminghut.pose_detection.R
import com.programminghut.pose_detection.data.model.TemplateExerciseType
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * Sistema completo per la generazione e gestione delle miniature degli esercizi
 * 
 * Funzionalità:
 * - Generazione automatica thumbnail da immagini esistenti
 * - Creazione thumbnail placeholder per esercizi senza immagine
 * - Cache intelligente per evitare rigenerazioni
 * - Ottimizzazione delle dimensioni per performance UI
 */
object ThumbnailGenerator {
    
    private const val THUMBNAIL_SIZE = 120 // dp convertiti in px
    private const val THUMBNAIL_QUALITY = 85 // compressione JPEG
    
    /**
     * Genera o recupera la thumbnail per un esercizio
     * @param context Context dell'app
     * @param exerciseId ID univoco dell'esercizio
     * @param imagePath Path dell'immagine originale (opzionale)
     * @param exerciseType Tipo di esercizio per placeholder
     * @param exerciseName Nome esercizio per placeholder
     * @return Path della thumbnail generata
     */
    fun generateThumbnail(
        context: Context,
        exerciseId: Long,
        imagePath: String? = null,
        exerciseType: TemplateExerciseType,
        exerciseName: String
    ): String {
        
        val thumbnailsDir = getThumbnailsDirectory(context)
        val thumbnailFile = File(thumbnailsDir, "thumb_${exerciseId}.jpg")
        
        android.util.Log.d("ThumbnailGenerator", "🎯 generateThumbnail - exerciseId: $exerciseId, imagePath: $imagePath")
        android.util.Log.d("ThumbnailGenerator", "🎯 thumbnailFile exists: ${thumbnailFile.exists()}, path: ${thumbnailFile.absolutePath}")
        
        // Se abbiamo un'immagine custom, rigenera sempre la thumbnail per assicurarsi sia aggiornata
        if (!imagePath.isNullOrEmpty() && imagePath.startsWith("content://") && thumbnailFile.exists()) {
            android.util.Log.d("ThumbnailGenerator", "🎯 Deleting existing thumbnail for custom image")
            thumbnailFile.delete()
        }
        
        // Se esiste già, la restituiamo (cache)
        if (thumbnailFile.exists()) {
            android.util.Log.d("ThumbnailGenerator", "🎯 Using cached thumbnail")
            return thumbnailFile.absolutePath
        }
        
        val bitmap = if (!imagePath.isNullOrEmpty()) {
            try {
                if (imagePath.startsWith("content://")) {
                    android.util.Log.d("ThumbnailGenerator", "🎯 Generating from URI: $imagePath")
                    // Gestisce URI (da image picker)
                    generateFromUri(context, Uri.parse(imagePath))
                } else {
                    android.util.Log.d("ThumbnailGenerator", "🎯 Generating from file path: $imagePath")
                    // Gestisce file path tradizionale
                    if (File(imagePath).exists()) {
                        generateFromImage(imagePath)
                    } else {
                        android.util.Log.d("ThumbnailGenerator", "🎯 File doesn't exist, using placeholder")
                        generatePlaceholder(context, exerciseType, exerciseName)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("ThumbnailGenerator", "🎯 Error loading image: ${e.message}")
                // Se c'è errore nel caricamento, usa placeholder
                generatePlaceholder(context, exerciseType, exerciseName)
            }
        } else {
            android.util.Log.d("ThumbnailGenerator", "🎯 No imagePath, using placeholder")
            // Genera placeholder
            generatePlaceholder(context, exerciseType, exerciseName)
        }
        
        // Salva thumbnail
        android.util.Log.d("ThumbnailGenerator", "🎯 Saving thumbnail to: ${thumbnailFile.absolutePath}")
        saveThumbnail(bitmap, thumbnailFile)
        
        return thumbnailFile.absolutePath
    }
    
    /**
     * Genera thumbnail da immagine esistente
     */
    private fun generateFromImage(imagePath: String): Bitmap {
        val originalBitmap = BitmapFactory.decodeFile(imagePath)
            ?: throw IOException("Impossibile caricare immagine: $imagePath")
        
        return resizeAndCropBitmap(originalBitmap)
    }
    
    /**
     * Genera thumbnail da URI (per immagini selezionate da image picker)
     */
    private fun generateFromUri(context: Context, uri: Uri): Bitmap {
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw IOException("Impossibile aprire URI: $uri")
        
        val originalBitmap = BitmapFactory.decodeStream(inputStream)
            ?: throw IOException("Impossibile decodificare immagine da URI: $uri")
        
        inputStream.close()
        return resizeAndCropBitmap(originalBitmap)
    }
    
    /**
     * Genera placeholder colorato per esercizio senza immagine
     */
    private fun generatePlaceholder(
        context: Context,
        exerciseType: TemplateExerciseType,
        exerciseName: String
    ): Bitmap {
        val size = (THUMBNAIL_SIZE * context.resources.displayMetrics.density).toInt()
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        // Background gradiente basato sul tipo di esercizio
        val gradientColors = getGradientColorsForType(context, exerciseType)
        val gradientDrawable = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            gradientColors
        )
        gradientDrawable.setBounds(0, 0, size, size)
        gradientDrawable.cornerRadius = (size * 0.1f)
        gradientDrawable.draw(canvas)
        
        // Icona centrale
        val iconRes = getIconForType(exerciseType)
        val icon = ContextCompat.getDrawable(context, iconRes)
        if (icon != null) {
            val iconSize = (size * 0.4f).toInt()
            val iconLeft = (size - iconSize) / 2
            val iconTop = (size - iconSize) / 2
            icon.setBounds(iconLeft, iconTop, iconLeft + iconSize, iconTop + iconSize)
            icon.setTint(ContextCompat.getColor(context, android.R.color.white))
            icon.draw(canvas)
        }
        
        // Iniziali del nome (se non c'è icona o come fallback)
        drawTextInitials(canvas, exerciseName, size)
        
        return bitmap
    }
    
    /**
     * Ridimensiona e ritaglia bitmap mantenendo aspect ratio
     */
    private fun resizeAndCropBitmap(originalBitmap: Bitmap): Bitmap {
        val targetSize = THUMBNAIL_SIZE
        val originalWidth = originalBitmap.width
        val originalHeight = originalBitmap.height
        
        // Calcola il fattore di scala per riempire completamente il quadrato
        val scale = maxOf(
            targetSize.toFloat() / originalWidth,
            targetSize.toFloat() / originalHeight
        )
        
        val scaledWidth = (originalWidth * scale).toInt()
        val scaledHeight = (originalHeight * scale).toInt()
        
        // Ridimensiona
        val scaledBitmap = Bitmap.createScaledBitmap(
            originalBitmap, scaledWidth, scaledHeight, true
        )
        
        // Ritaglia al centro per ottenere un quadrato perfetto
        val startX = (scaledWidth - targetSize) / 2
        val startY = (scaledHeight - targetSize) / 2
        
        return Bitmap.createBitmap(scaledBitmap, startX, startY, targetSize, targetSize)
    }
    
    /**
     * Ottiene i colori del gradiente in base al tipo di esercizio
     */
    private fun getGradientColorsForType(
        context: Context, 
        type: TemplateExerciseType
    ): IntArray {
        return when (type) {
            TemplateExerciseType.STRENGTH -> intArrayOf(
                ContextCompat.getColor(context, android.R.color.holo_red_dark),
                ContextCompat.getColor(context, android.R.color.holo_orange_dark)
            )
            TemplateExerciseType.CARDIO -> intArrayOf(
                ContextCompat.getColor(context, android.R.color.holo_blue_dark),
                ContextCompat.getColor(context, android.R.color.holo_blue_light)
            )
            TemplateExerciseType.FLEXIBILITY -> intArrayOf(
                ContextCompat.getColor(context, android.R.color.holo_green_dark),
                ContextCompat.getColor(context, android.R.color.holo_green_light)
            )
            TemplateExerciseType.SQUAT_AI -> intArrayOf(
                ContextCompat.getColor(context, android.R.color.holo_purple),
                ContextCompat.getColor(context, android.R.color.holo_red_light)
            )
            TemplateExerciseType.BALANCE -> intArrayOf(
                ContextCompat.getColor(context, android.R.color.darker_gray),
                ContextCompat.getColor(context, android.R.color.black)
            )
            TemplateExerciseType.CUSTOM -> intArrayOf(
                ContextCompat.getColor(context, android.R.color.holo_orange_light),
                ContextCompat.getColor(context, android.R.color.holo_orange_dark)
            )
        }
    }
    
    /**
     * Ottiene l'icona appropriata per il tipo di esercizio
     */
    private fun getIconForType(type: TemplateExerciseType): Int {
        return when (type) {
            TemplateExerciseType.STRENGTH -> R.drawable.ic_fitness_center_24
            TemplateExerciseType.CARDIO -> R.drawable.ic_directions_run_24
            TemplateExerciseType.FLEXIBILITY -> R.drawable.ic_self_improvement_24
            TemplateExerciseType.SQUAT_AI -> R.drawable.ic_visibility_24
            TemplateExerciseType.BALANCE -> R.drawable.ic_balance_24
            TemplateExerciseType.CUSTOM -> R.drawable.ic_fitness_center_24
        }
    }
    
    /**
     * Disegna le iniziali del nome come fallback
     */
    private fun drawTextInitials(canvas: Canvas, name: String, size: Int) {
        val initials = name.split(" ")
            .mapNotNull { it.firstOrNull()?.toString() }
            .take(2)
            .joinToString("")
            .uppercase()
        
        if (initials.isNotEmpty()) {
            val paint = Paint().apply {
                color = android.graphics.Color.WHITE
                textSize = size * 0.25f
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
            }
            
            val textBounds = Rect()
            paint.getTextBounds(initials, 0, initials.length, textBounds)
            
            val centerX = size / 2f
            val centerY = size / 2f + textBounds.height() / 2f
            
                canvas.drawText(initials, centerX, centerY, paint)
        }
    }
    
    /**
     * Salva la bitmap come thumbnail JPEG
     */
    private fun saveThumbnail(bitmap: Bitmap, file: File): Boolean {
        return try {
            file.parentFile?.mkdirs()
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, THUMBNAIL_QUALITY, out)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    /**
     * Ottiene la directory per le thumbnail (cache interna)
     */
    private fun getThumbnailsDirectory(context: Context): File {
        val thumbnailsDir = File(context.cacheDir, "exercise_thumbnails")
        if (!thumbnailsDir.exists()) {
            thumbnailsDir.mkdirs()
        }
        return thumbnailsDir
    }
    
    /**
     * Cancella tutte le thumbnail (per pulizia cache)
     */
    fun clearThumbnailCache(context: Context): Boolean {
        return try {
            getThumbnailsDirectory(context).deleteRecursively()
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Cancella thumbnail specifica
     */
    fun deleteThumbnail(context: Context, exerciseId: Long): Boolean {
        return try {
            val thumbnailFile = File(getThumbnailsDirectory(context), "thumb_${exerciseId}.jpg")
            thumbnailFile.delete()
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Ottiene dimensione cache thumbnails in bytes
     */
    fun getCacheSize(context: Context): Long {
        return try {
            getThumbnailsDirectory(context).walkTopDown()
                .filter { it.isFile }
                .map { it.length() }
                .sum()
        } catch (e: Exception) {
            0L
        }
    }
}