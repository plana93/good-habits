package com.programminghut.pose_detection.filters

import android.graphics.Bitmap
import android.graphics.Canvas

/**
 * Classe base astratta per tutti i filtri adattivi
 * Ogni filtro può essere applicato in real-time sulla camera
 */
abstract class AdaptiveFilter(
    val id: String,
    val name: String,
    val iconResId: Int,
    val description: String
) {
    // Parametri del filtro (possono essere modificati dall'utente)
    val parameters = mutableMapOf<String, FilterParameter>()
    
    // Stato attivo/inattivo
    var isActive: Boolean = false
    
    /**
     * Applica il filtro al frame corrente
     * @param canvas Canvas su cui disegnare
     * @param bitmap Bitmap del frame corrente
     * @param poseKeypoints Array dei keypoint della posa (se disponibili)
     */
    abstract fun apply(canvas: Canvas, bitmap: Bitmap, poseKeypoints: FloatArray?)
    
    /**
     * Restituisce la lista dei parametri configurabili
     */
    fun getParameters(): List<FilterParameter> {
        return parameters.values.toList()
    }
    
    /**
     * Aggiorna un parametro
     */
    fun updateParameter(key: String, value: Any) {
        parameters[key]?.currentValue = value
    }
    
    /**
     * Resetta tutti i parametri ai valori di default
     */
    fun resetParameters() {
        parameters.values.forEach { it.reset() }
    }
    
    /**
     * Crea una copia del filtro (per poter avere istanze multiple con parametri diversi)
     */
    abstract fun clone(): AdaptiveFilter
}

/**
 * Rappresenta un parametro configurabile di un filtro
 */
sealed class FilterParameter(
    val key: String,
    val displayName: String,
    val description: String,
    open var currentValue: Any,
    val defaultValue: Any
) {
    abstract fun serialize(): String
    abstract fun deserialize(serialized: String)
    fun reset() {
        currentValue = defaultValue
    }
    
    /**
     * Parametro numerico con slider (SeekBar)
     */
    data class Slider(
        val paramKey: String,
        val paramDisplayName: String,
        val paramDescription: String,
        var value: Float,
        val min: Float,
        val max: Float,
        val step: Float = 1f,
        val paramDefaultValue: Float = value
    ) : FilterParameter(paramKey, paramDisplayName, paramDescription, value, paramDefaultValue) {
        override fun serialize(): String = value.toString()
        override fun deserialize(serialized: String) {
            value = serialized.toFloatOrNull() ?: value
            currentValue = value
        }
        fun getProgress(): Int = ((value - min) / step).toInt()
        fun setProgress(progress: Int) {
            value = min + (progress * step)
            currentValue = value
        }
        fun getMaxProgress(): Int = ((max - min) / step).toInt()
    }
    
    /**
     * Parametro booleano (Switch)
     */
    data class Toggle(
        val paramKey: String,
        val paramDisplayName: String,
        val paramDescription: String,
        var enabled: Boolean,
        val paramDefaultValue: Boolean = enabled
    ) : FilterParameter(paramKey, paramDisplayName, paramDescription, enabled, paramDefaultValue) {
        override fun serialize(): String = enabled.toString()
        override fun deserialize(serialized: String) {
            enabled = serialized.toBooleanStrictOrNull() ?: enabled
            currentValue = enabled
        }
    }
    
    /**
     * Parametro colore RGB
     */
    data class ColorRGB(
        val paramKey: String,
        val paramDisplayName: String,
        val paramDescription: String,
        var red: Int,
        var green: Int,
        var blue: Int,
        val paramDefaultValue: Triple<Int, Int, Int> = Triple(red, green, blue)
    ) : FilterParameter(paramKey, paramDisplayName, paramDescription, Triple(red, green, blue), paramDefaultValue) {
        override fun serialize(): String = "$red,$green,$blue"
        override fun deserialize(serialized: String) {
            val parts = serialized.split(",")
            if (parts.size == 3) {
                red = parts[0].toIntOrNull() ?: red
                green = parts[1].toIntOrNull() ?: green
                blue = parts[2].toIntOrNull() ?: blue
                currentValue = Triple(red, green, blue)
            }
        }
        fun getColor(): Int = android.graphics.Color.rgb(red, green, blue)
    }
    
    /**
     * Parametro selezione da lista (Spinner/Dropdown)
     */
    data class Choice(
        val paramKey: String,
        val paramDisplayName: String,
        val paramDescription: String,
        var selectedIndex: Int,
        val options: List<String>,
        val paramDefaultValue: Int = selectedIndex
    ) : FilterParameter(paramKey, paramDisplayName, paramDescription, selectedIndex, paramDefaultValue) {
        override fun serialize(): String = selectedIndex.toString()
        override fun deserialize(serialized: String) {
            selectedIndex = serialized.toIntOrNull()?.coerceIn(0, options.size - 1) ?: selectedIndex
            currentValue = selectedIndex
        }
        fun getSelectedOption(): String = options[selectedIndex]
    }
}
