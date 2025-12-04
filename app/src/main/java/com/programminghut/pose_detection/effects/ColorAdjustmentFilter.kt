package com.programminghut.pose_detection.effects

import android.graphics.*
import com.programminghut.pose_detection.R

/**
 * Filtro che applica modifiche ai colori dell'immagine (contrasto, saturazione, luminosità)
 */
class ColorAdjustmentFilter : AdaptiveFilter(
    id = "color_adjust",
    name = "Color Adjustment",
    iconResId = R.drawable.ic_filter,
    description = "Regola contrasto, saturazione, luminosità e altri parametri colore dell'immagine catturata dalla camera."
) {
    
    private val colorMatrix = ColorMatrix()
    private val paint = Paint()
    
    init {
        parameters["contrast"] = FilterParameter.Slider(
            paramKey = "contrast",
            paramDisplayName = "Contrasto",
            paramDescription = "Aumenta o diminuisce il contrasto dell'immagine",
            value = 1.0f,
            min = 0.5f,
            max = 2.0f,
            step = 0.1f
        )
        
        parameters["saturation"] = FilterParameter.Slider(
            paramKey = "saturation",
            paramDisplayName = "Saturazione",
            paramDescription = "Controlla l'intensità dei colori (0 = bianco e nero, 2 = colori molto vivaci)",
            value = 1.0f,
            min = 0.0f,
            max = 2.0f,
            step = 0.1f
        )
        
        parameters["brightness"] = FilterParameter.Slider(
            paramKey = "brightness",
            paramDisplayName = "Luminosità",
            paramDescription = "Schiarisce o scurisce l'immagine",
            value = 0f,
            min = -50f,
            max = 50f,
            step = 5f
        )
        
        parameters["hueShift"] = FilterParameter.Slider(
            paramKey = "hueShift",
            paramDisplayName = "Rotazione Tonalità",
            paramDescription = "Ruota la ruota dei colori (effetto psichedelico)",
            value = 0f,
            min = 0f,
            max = 360f,
            step = 10f
        )
        
        parameters["colorTint"] = FilterParameter.ColorRGB(
            paramKey = "colorTint",
            paramDisplayName = "Tinta Colore",
            paramDescription = "Aggiunge una tinta colorata all'immagine",
            red = 255,
            green = 255,
            blue = 255
        )
        
        parameters["tintStrength"] = FilterParameter.Slider(
            paramKey = "tintStrength",
            paramDisplayName = "Forza Tinta",
            paramDescription = "Intensità della tinta colorata (0 = nessuna tinta)",
            value = 0f,
            min = 0f,
            max = 1f,
            step = 0.1f
        )
    }
    
    override fun apply(canvas: Canvas, bitmap: Bitmap, poseKeypoints: FloatArray?) {
        // Recupera parametri
        val contrast = (parameters["contrast"] as FilterParameter.Slider).value
        val saturation = (parameters["saturation"] as FilterParameter.Slider).value
        val brightness = (parameters["brightness"] as FilterParameter.Slider).value
        val hueShift = (parameters["hueShift"] as FilterParameter.Slider).value
        val tintColor = parameters["colorTint"] as FilterParameter.ColorRGB
        val tintStrength = (parameters["tintStrength"] as FilterParameter.Slider).value
        
        // Reset matrix
        colorMatrix.reset()
        
        // Applica saturazione
        val satMatrix = ColorMatrix()
        satMatrix.setSaturation(saturation)
        colorMatrix.postConcat(satMatrix)
        
        // Applica contrasto e luminosità
        val scale = contrast
        val translate = brightness
        val contrastMatrix = ColorMatrix(floatArrayOf(
            scale, 0f, 0f, 0f, translate,
            0f, scale, 0f, 0f, translate,
            0f, 0f, scale, 0f, translate,
            0f, 0f, 0f, 1f, 0f
        ))
        colorMatrix.postConcat(contrastMatrix)
        
        // Applica rotazione tonalità
        if (hueShift != 0f) {
            val hueMatrix = ColorMatrix()
            val degrees = hueShift
            val cos = Math.cos(Math.toRadians(degrees.toDouble())).toFloat()
            val sin = Math.sin(Math.toRadians(degrees.toDouble())).toFloat()
            
            hueMatrix.set(floatArrayOf(
                0.213f + cos * 0.787f - sin * 0.213f, 0.715f - cos * 0.715f - sin * 0.715f, 0.072f - cos * 0.072f + sin * 0.928f, 0f, 0f,
                0.213f - cos * 0.213f + sin * 0.143f, 0.715f + cos * 0.285f + sin * 0.140f, 0.072f - cos * 0.072f - sin * 0.283f, 0f, 0f,
                0.213f - cos * 0.213f - sin * 0.787f, 0.715f - cos * 0.715f + sin * 0.715f, 0.072f + cos * 0.928f + sin * 0.072f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            ))
            
            colorMatrix.postConcat(hueMatrix)
        }
        
        // Applica tinta se forza > 0
        if (tintStrength > 0) {
            val tintMatrix = ColorMatrix()
            val invStrength = 1f - tintStrength
            
            tintMatrix.set(floatArrayOf(
                invStrength, 0f, 0f, 0f, tintColor.red * tintStrength,
                0f, invStrength, 0f, 0f, tintColor.green * tintStrength,
                0f, 0f, invStrength, 0f, tintColor.blue * tintStrength,
                0f, 0f, 0f, 1f, 0f
            ))
            
            colorMatrix.postConcat(tintMatrix)
        }
        
        // Applica il filtro
        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
    }
    
    override fun clone(): AdaptiveFilter {
        return ColorAdjustmentFilter().also { newFilter ->
            this.parameters.forEach { (key, param) ->
                newFilter.parameters[key] = param
            }
        }
    }
}
