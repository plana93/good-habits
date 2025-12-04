package com.programminghut.pose_detection.urban

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Rect
import kotlin.math.abs
import com.programminghut.pose_detection.effects.RandomProvider

/**
 * Enum per i diversi tipi di filtri applicabili alle box
 */
enum class FilterType {
    BLACK_WHITE,
    SOBEL_EFFECT,
    PIXELATED
}

/**
 * Classe che rappresenta una box intorno a un giunto con effetti grafici
 */
data class JointBox(
    val jointIndex: Int,
    val x: Float,
    val y: Float,
    val score: Float,
    val size: Float,
    val filter: FilterType,
    val visible: Boolean,
    val frameCreated: Long
)

/**
 * Manager per gli effetti grafici urban street art.
 * Gestisce box dinamiche intorno ai giunti con vari effetti visivi.
 */
class UrbanEffectsManager {
    
    private val activeBoxes = mutableMapOf<Int, JointBox>()
    private var frameCounter = 0L
    private val paint = Paint()
    
    /**
     * Aggiorna le box in base alle nuove pose rilevate
     */
    fun updateBoxes(outputFeature0: FloatArray, imageWidth: Int, imageHeight: Int) {
        frameCounter++
        
        // Rimuovi box scadute
        val toRemove = mutableListOf<Int>()
        activeBoxes.forEach { (jointIndex, box) ->
            val duration = frameCounter - box.frameCreated
            val maxDuration = (RandomProvider.nextInt(UrbanConfig.BOX_MAX_DURATION - UrbanConfig.BOX_MIN_DURATION) + UrbanConfig.BOX_MIN_DURATION)
            if (duration > maxDuration) {
                toRemove.add(jointIndex)
            }
        }
        toRemove.forEach { activeBoxes.remove(it) }
        
        // Crea nuove box casuali
        UrbanConfig.ACTIVE_JOINTS.forEach { jointIndex ->
            if (!activeBoxes.containsKey(jointIndex) && RandomProvider.nextFloat() < UrbanConfig.BOX_APPEAR_PROBABILITY) {
                val y = outputFeature0[jointIndex * 3 + 0]
                val x = outputFeature0[jointIndex * 3 + 1]
                val score = outputFeature0[jointIndex * 3 + 2]
                
                if (score > UrbanConfig.MIN_JOINT_SCORE) {
                    val box = JointBox(
                        jointIndex = jointIndex,
                        x = x * imageWidth,
                        y = y * imageHeight,
                        score = score,
                        size = RandomProvider.nextFloat() * (UrbanConfig.BOX_SIZE_MAX - UrbanConfig.BOX_SIZE_MIN) + UrbanConfig.BOX_SIZE_MIN,
                        filter = FilterType.values()[RandomProvider.nextInt(FilterType.values().size)],
                        visible = true,
                        frameCreated = frameCounter
                    )
                    activeBoxes[jointIndex] = box
                }
            }
        }
    }
    
    /**
     * Disegna tutte le box attive con i loro effetti
     */
    fun drawBoxes(canvas: Canvas, sourceBitmap: Bitmap) {
        activeBoxes.values.forEach { box ->
            drawSingleBox(canvas, sourceBitmap, box)
        }
    }
    
    private fun drawSingleBox(canvas: Canvas, sourceBitmap: Bitmap, box: JointBox) {
        val halfSize = box.size / 2
        val left = (box.x - halfSize).toInt().coerceIn(0, sourceBitmap.width - 1)
        val top = (box.y - halfSize).toInt().coerceIn(0, sourceBitmap.height - 1)
        val right = (box.x + halfSize).toInt().coerceIn(0, sourceBitmap.width)
        val bottom = (box.y + halfSize).toInt().coerceIn(0, sourceBitmap.height)
        
        if (left >= right || top >= bottom) return
        
        // Estrai la porzione di immagine
        val croppedBitmap = Bitmap.createBitmap(
            sourceBitmap,
            left, top,
            right - left, bottom - top
        )
        
        // Applica il filtro
        val filteredBitmap = applyFilter(croppedBitmap, box.filter)
        
        // Disegna la bitmap filtrata
        paint.alpha = UrbanConfig.BOX_OPACITY
        canvas.drawBitmap(filteredBitmap, left.toFloat(), top.toFloat(), paint)
        
        // Disegna il bordo (bianco/nero/grigio molto sottile)
        // Disegna il bordo (bianco/nero/grigio molto sottile)
        paint.alpha = 255
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = UrbanConfig.BORDER_WIDTH
        // Usa colori monocromatici (bianco, nero, grigi)
        paint.color = UrbanConfig.BORDER_COLORS[box.jointIndex % UrbanConfig.BORDER_COLORS.size]
        canvas.drawRect(
            left.toFloat(),
            top.toFloat(),
            right.toFloat(),
            bottom.toFloat(),
            paint
        )
        
        // Reset paint
        paint.style = Paint.Style.FILL
        
        // Cleanup
        filteredBitmap.recycle()
        croppedBitmap.recycle()
    }
    
    /**
     * Applica il filtro scelto alla bitmap
     */
    private fun applyFilter(bitmap: Bitmap, filter: FilterType): Bitmap {
        return when (filter) {
            FilterType.BLACK_WHITE -> applyBlackWhiteFilter(bitmap)
            FilterType.SOBEL_EFFECT -> applySobelEffect(bitmap)
            FilterType.PIXELATED -> applyPixelation(bitmap)
        }
    }
    
    /**
     * Filtro bianco e nero
     */
    private fun applyBlackWhiteFilter(bitmap: Bitmap): Bitmap {
        val result = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config)
        val canvas = Canvas(result)
        
        val paint = Paint()
        val colorMatrix = ColorMatrix()
        colorMatrix.setSaturation(0f)
        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return result
    }
    
    /**
     * Effetto Sobel (edge detection)
     */
    private fun applySobelEffect(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val result = Bitmap.createBitmap(width, height, bitmap.config)
        
        // Sobel kernels
        val gx = arrayOf(
            intArrayOf(-1, 0, 1),
            intArrayOf(-2, 0, 2),
            intArrayOf(-1, 0, 1)
        )
        
        val gy = arrayOf(
            intArrayOf(-1, -2, -1),
            intArrayOf(0, 0, 0),
            intArrayOf(1, 2, 1)
        )
        
        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                var pixelX = 0
                var pixelY = 0
                
                for (i in -1..1) {
                    for (j in -1..1) {
                        val pixel = bitmap.getPixel(x + j, y + i)
                        val gray = (Color.red(pixel) + Color.green(pixel) + Color.blue(pixel)) / 3
                        
                        pixelX += gray * gx[i + 1][j + 1]
                        pixelY += gray * gy[i + 1][j + 1]
                    }
                }
                
                val magnitude = Math.sqrt((pixelX * pixelX + pixelY * pixelY).toDouble()).toInt()
                    .coerceIn(0, 255)
                
                result.setPixel(x, y, Color.rgb(magnitude, magnitude, magnitude))
            }
        }
        
        return result
    }
    
    /**
     * Effetto pixelato
     */
    private fun applyPixelation(bitmap: Bitmap): Bitmap {
        val pixelSize = UrbanConfig.PIXEL_SIZE
        val width = bitmap.width
        val height = bitmap.height
        val result = Bitmap.createBitmap(width, height, bitmap.config)
        
        for (y in 0 until height step pixelSize) {
            for (x in 0 until width step pixelSize) {
                // Prendi il colore del pixel centrale del blocco
                val centerX = (x + pixelSize / 2).coerceIn(0, width - 1)
                val centerY = (y + pixelSize / 2).coerceIn(0, height - 1)
                val color = bitmap.getPixel(centerX, centerY)
                
                // Riempi tutto il blocco con quel colore
                for (dy in 0 until pixelSize) {
                    for (dx in 0 until pixelSize) {
                        val px = x + dx
                        val py = y + dy
                        if (px < width && py < height) {
                            result.setPixel(px, py, color)
                        }
                    }
                }
            }
        }
        
        return result
    }
    
    /**
     * Resetta il manager
     */
    fun reset() {
        activeBoxes.clear()
        frameCounter = 0
    }
}
