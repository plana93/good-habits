package com.programminghut.pose_detection.effects

import android.graphics.*
import com.programminghut.pose_detection.R
import com.programminghut.pose_detection.effects.FrameClock

/**
 * Filtro che disegna punti luminosi sui keypoint del corpo
 */
class GlowDotsFilter : AdaptiveFilter(
    id = "glow_dots",
    name = "Glow Dots",
    iconResId = R.drawable.ic_glow_dots,
    description = "Disegna punti luminosi sui keypoint principali del corpo. I punti possono pulsare e brillare creando un effetto visivo accattivante."
) {
    override val requiresPose: Boolean = true
    
    private val paint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    
    // pulsePhase computed from FrameClock.timeMs per-frame to allow deterministic animation
    
    init {
        parameters["dotSize"] = FilterParameter.Slider(
            paramKey = "dotSize",
            paramDisplayName = "Dimensione Punti",
            paramDescription = "Regola la dimensione dei punti luminosi",
            value = 15f,
            min = 5f,
            max = 40f,
            step = 1f
        )
        
        parameters["color"] = FilterParameter.ColorRGB(
            paramKey = "color",
            paramDisplayName = "Colore",
            paramDescription = "Scegli il colore dei punti luminosi",
            red = 57,
            green = 255,
            blue = 20
        )
        
        parameters["glowRadius"] = FilterParameter.Slider(
            paramKey = "glowRadius",
            paramDisplayName = "Raggio Glow",
            paramDescription = "Dimensione dell'alone luminoso intorno ai punti",
            value = 20f,
            min = 0f,
            max = 50f,
            step = 1f
        )
        
        parameters["pulseEffect"] = FilterParameter.Toggle(
            paramKey = "pulseEffect",
            paramDisplayName = "Effetto Pulsazione",
            paramDescription = "I punti pulsano ritmicamente",
            enabled = true
        )
        
        parameters["pulseSpeed"] = FilterParameter.Slider(
            paramKey = "pulseSpeed",
            paramDisplayName = "Velocità Pulsazione",
            paramDescription = "Quanto velocemente pulsano i punti",
            value = 0.1f,
            min = 0.01f,
            max = 0.3f,
            step = 0.01f
        )
        
        parameters["showAllKeypoints"] = FilterParameter.Toggle(
            paramKey = "showAllKeypoints",
            paramDisplayName = "Mostra Tutti i Keypoint",
            paramDescription = "Mostra tutti i 17 keypoint o solo quelli principali (spalle, anche, ginocchia)",
            enabled = true
        )
        
        parameters["outerRing"] = FilterParameter.Toggle(
            paramKey = "outerRing",
            paramDisplayName = "Anello Esterno",
            paramDescription = "Aggiunge un anello luminoso intorno a ogni punto",
            enabled = false
        )
    }
    
    override fun apply(canvas: Canvas, bitmap: Bitmap, poseKeypoints: FloatArray?) {
        if (poseKeypoints == null || poseKeypoints.size < 51) return
        
        // Recupera parametri
        val dotSize = (parameters["dotSize"] as FilterParameter.Slider).value
        val colorParam = parameters["color"] as FilterParameter.ColorRGB
        val glowRadius = (parameters["glowRadius"] as FilterParameter.Slider).value
        val pulseEnabled = (parameters["pulseEffect"] as FilterParameter.Toggle).enabled
        val pulseSpeed = (parameters["pulseSpeed"] as FilterParameter.Slider).value
        val showAll = (parameters["showAllKeypoints"] as FilterParameter.Toggle).enabled
        val outerRing = (parameters["outerRing"] as FilterParameter.Toggle).enabled
        
        // Calculate pulse factor based on global FrameClock so animation advances per-frame
        val pulseFactor = if (pulseEnabled) {
            val t = FrameClock.timeMs / 1000.0 // seconds
            // Map pulseSpeed (0.01..0.3) to angular velocity: multiply by 2PI
            val phase = (t * pulseSpeed * 2.0 * Math.PI)
            0.8f + 0.2f * Math.sin(phase).toFloat()
        } else {
            1f
        }
        
        val color = colorParam.getColor()
        
        // Determina quali keypoint disegnare
        val keypointsToShow = if (showAll) {
            (0..16).toList() // Tutti i 17 keypoint
        } else {
            listOf(5, 6, 11, 12, 13, 14) // Solo keypoint principali (spalle, anche, ginocchia)
        }
        
        // Disegna i punti
        keypointsToShow.forEach { idx ->
            val x = poseKeypoints[idx * 3]
            val y = poseKeypoints[idx * 3 + 1]
            val confidence = poseKeypoints[idx * 3 + 2]
            
            if (confidence > 0.3) {
                drawGlowDot(canvas, x, y, dotSize * pulseFactor, glowRadius, color, outerRing)
            }
        }
    }
    
    private fun drawGlowDot(
        canvas: Canvas,
        x: Float,
        y: Float,
        size: Float,
        glowRadius: Float,
        color: Int,
        drawRing: Boolean
    ) {
        // Effetto glow
        if (glowRadius > 0) {
            paint.color = color
            paint.alpha = 50
            paint.setShadowLayer(glowRadius, 0f, 0f, color)
            canvas.drawCircle(x, y, size + glowRadius / 2, paint)
        }
        
        // Punto centrale
        paint.alpha = 255
        paint.clearShadowLayer()
        canvas.drawCircle(x, y, size, paint)
        
        // Anello esterno opzionale
        if (drawRing) {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 3f
            paint.alpha = 150
            canvas.drawCircle(x, y, size * 1.5f, paint)
            paint.style = Paint.Style.FILL
        }
    }
    
    override fun clone(): AdaptiveFilter {
        return GlowDotsFilter().also { newFilter ->
            this.parameters.forEach { (key, param) ->
                newFilter.parameters[key] = param
            }
        }
    }
}
