package com.programminghut.pose_detection.filters

import android.graphics.*
import com.programminghut.pose_detection.R
import android.renderscript.*

/**
 * Filtro che applica effetti di blur all'immagine
 */
class BlurFilter : AdaptiveFilter(
    id = "blur_effect",
    name = "Blur Effect",
    iconResId = R.drawable.ic_filter,
    description = "Applica effetto sfocatura all'immagine. Utile per creare effetto bokeh o sfocare lo sfondo mantenendo la posa in primo piano."
) {
    
    private val paint = Paint()
    
    init {
        parameters["blurRadius"] = FilterParameter.Slider(
            paramKey = "blurRadius",
            paramDisplayName = "Raggio Blur",
            paramDescription = "Intensità della sfocatura (0 = nessun blur)",
            value = 0f,
            min = 0f,
            max = 25f,
            step = 1f
        )
        
        parameters["blurType"] = FilterParameter.Choice(
            paramKey = "blurType",
            paramDisplayName = "Tipo Blur",
            paramDescription = "Seleziona il tipo di sfocatura da applicare",
            selectedIndex = 0,
            options = listOf("Gaussian", "Motion Horizontal", "Motion Vertical", "Radial")
        )
        
        parameters["preservePose"] = FilterParameter.Toggle(
            paramKey = "preservePose",
            paramDisplayName = "Mantieni Posa Nitida",
            paramDescription = "Sfoca solo lo sfondo, mantenendo la posa della persona nitida",
            enabled = false
        )
    }
    
    override fun apply(canvas: Canvas, bitmap: Bitmap, poseKeypoints: FloatArray?) {
        val blurRadius = (parameters["blurRadius"] as FilterParameter.Slider).value
        
        if (blurRadius <= 0) return
        
        val blurType = (parameters["blurType"] as FilterParameter.Choice).selectedIndex
        val preservePose = (parameters["preservePose"] as FilterParameter.Toggle).enabled
        
        // Applica blur in base al tipo
        val blurredBitmap = when (blurType) {
            0 -> applyGaussianBlur(bitmap, blurRadius)
            1 -> applyMotionBlur(bitmap, blurRadius, horizontal = true)
            2 -> applyMotionBlur(bitmap, blurRadius, horizontal = false)
            3 -> applyRadialBlur(bitmap, blurRadius)
            else -> bitmap
        }
        
        // Se preserve pose è abilitato, crea una maschera
        if (preservePose && poseKeypoints != null) {
            // TODO: implementare maschera per preservare la persona nitida
            // Per ora disegna semplicemente il blur
            canvas.drawBitmap(blurredBitmap, 0f, 0f, paint)
        } else {
            canvas.drawBitmap(blurredBitmap, 0f, 0f, paint)
        }
    }
    
    private fun applyGaussianBlur(bitmap: Bitmap, radius: Float): Bitmap {
        val blurred = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(blurred)
        
        val blurPaint = Paint().apply {
            maskFilter = BlurMaskFilter(radius, BlurMaskFilter.Blur.NORMAL)
        }
        
        canvas.drawBitmap(bitmap, 0f, 0f, blurPaint)
        return blurred
    }
    
    private fun applyMotionBlur(bitmap: Bitmap, radius: Float, horizontal: Boolean): Bitmap {
        // Simula motion blur con multiple copie traslate
        val blurred = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(blurred)
        
        val steps = radius.toInt().coerceAtLeast(1)
        val alpha = (255 / steps).coerceAtLeast(10)
        
        val motionPaint = Paint().apply {
            this.alpha = alpha
        }
        
        for (i in 0 until steps) {
            val offset = (i * radius / steps)
            val dx = if (horizontal) offset else 0f
            val dy = if (!horizontal) offset else 0f
            
            canvas.drawBitmap(bitmap, dx, dy, motionPaint)
        }
        
        return blurred
    }
    
    private fun applyRadialBlur(bitmap: Bitmap, radius: Float): Bitmap {
        // Simula radial blur dal centro
        val blurred = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(blurred)
        
        val centerX = bitmap.width / 2f
        val centerY = bitmap.height / 2f
        
        val steps = (radius / 2).toInt().coerceAtLeast(1)
        val alpha = (255 / steps).coerceAtLeast(10)
        
        val radialPaint = Paint().apply {
            this.alpha = alpha
        }
        
        for (i in 0 until steps) {
            val scale = 1f + (i * 0.01f)
            canvas.save()
            canvas.scale(scale, scale, centerX, centerY)
            canvas.drawBitmap(bitmap, 0f, 0f, radialPaint)
            canvas.restore()
        }
        
        return blurred
    }
    
    override fun clone(): AdaptiveFilter {
        return BlurFilter().also { newFilter ->
            this.parameters.forEach { (key, param) ->
                newFilter.parameters[key] = param
            }
        }
    }
}
