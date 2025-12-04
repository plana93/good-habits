package com.programminghut.pose_detection.effects

import android.graphics.Bitmap
import android.graphics.Canvas
import com.programminghut.pose_detection.effects.SkeletonFilter
import com.programminghut.pose_detection.effects.GlowDotsFilter
import com.programminghut.pose_detection.effects.UrbanBoxesFilter
import com.programminghut.pose_detection.effects.ColorAdjustmentFilter
import com.programminghut.pose_detection.effects.BlurFilter

/**
 * Gestisce tutti i filtri adattivi disponibili e quelli attualmente attivi
 * Singleton pattern per accesso globale
 */
object FilterManager {
    
    // Lista di tutti i filtri disponibili
    private val availableFilters = mutableListOf<AdaptiveFilter>()
    
    // Lista dei filtri attualmente attivi (nell'ordine di applicazione)
    private val activeFilters = mutableListOf<AdaptiveFilter>()
    
    // Listener per cambiamenti nella lista filtri
    private val listeners = mutableListOf<FilterChangeListener>()
    
    /**
     * Inizializza il manager con tutti i filtri disponibili
     */
    fun initialize(filters: List<AdaptiveFilter>) {
        availableFilters.clear()
        availableFilters.addAll(filters)
    }
    
    /**
     * Registra tutti i filtri built-in
     */
    fun registerDefaultFilters() {
        val defaultFilters = listOf(
            SkeletonFilter(),
            GlowDotsFilter(),
            ConnectedLineCenterSobel(),
            UrbanBoxesFilter(),
            ColorAdjustmentFilter(),
            BlurFilter()
        )
        initialize(defaultFilters)
    }
    
    /**
     * Restituisce tutti i filtri disponibili
     */
    fun getAvailableFilters(): List<AdaptiveFilter> {
        return availableFilters.toList()
    }
    
    /**
     * Restituisce i filtri attualmente attivi
     */
    fun getActiveFilters(): List<AdaptiveFilter> {
        return activeFilters.toList()
    }

    /**
     * Returns true if any active filter requires pose keypoints. Used to skip
     * expensive pose detection when no active filter needs it.
     */
    fun activeFiltersRequirePose(): Boolean {
        return activeFilters.any { it.isActive && it.requiresPose }
    }
    
    /**
     * Attiva un filtro (lo aggiunge agli attivi)
     */
    fun activateFilter(filter: AdaptiveFilter) {
        if (!activeFilters.contains(filter)) {
            filter.isActive = true
            activeFilters.add(filter)
            notifyFilterActivated(filter)
        }
    }
    
    /**
     * Disattiva un filtro (lo rimuove dagli attivi)
     */
    fun deactivateFilter(filter: AdaptiveFilter) {
        if (activeFilters.remove(filter)) {
            filter.isActive = false
            notifyFilterDeactivated(filter)
        }
    }
    
    /**
     * Rimuove un filtro dagli attivi tramite ID
     */
    fun deactivateFilterById(filterId: String) {
        val filter = activeFilters.find { it.id == filterId }
        filter?.let { deactivateFilter(it) }
    }
    
    /**
     * Cambia l'ordine di un filtro attivo
     */
    fun moveActiveFilter(fromPosition: Int, toPosition: Int) {
        if (fromPosition in activeFilters.indices && toPosition in activeFilters.indices) {
            val filter = activeFilters.removeAt(fromPosition)
            activeFilters.add(toPosition, filter)
            notifyFiltersReordered()
        }
    }
    
    /**
     * Applica tutti i filtri attivi in sequenza
     */
    fun applyFilters(canvas: Canvas, bitmap: Bitmap, poseKeypoints: FloatArray?) {
        activeFilters.forEach { filter ->
            if (filter.isActive) {
                filter.apply(canvas, bitmap, poseKeypoints)
            }
        }
    }
    
    /**
     * Trova un filtro per ID
     */
    fun findFilterById(id: String): AdaptiveFilter? {
        return availableFilters.find { it.id == id }
    }
    
    /**
     * Resetta tutti i filtri ai parametri di default
     */
    fun resetAllFilters() {
        availableFilters.forEach { it.resetParameters() }
        activeFilters.forEach { it.resetParameters() }
    }
    
    /**
     * Disattiva tutti i filtri
     */
    fun deactivateAll() {
        activeFilters.clear()
        availableFilters.forEach { it.isActive = false }
        notifyAllFiltersDeactivated()
    }
    
    // ===== Listener Management =====
    
    fun addListener(listener: FilterChangeListener) {
        listeners.add(listener)
    }
    
    fun removeListener(listener: FilterChangeListener) {
        listeners.remove(listener)
    }
    
    private fun notifyFilterActivated(filter: AdaptiveFilter) {
        listeners.forEach { it.onFilterActivated(filter) }
    }
    
    private fun notifyFilterDeactivated(filter: AdaptiveFilter) {
        listeners.forEach { it.onFilterDeactivated(filter) }
    }
    
    private fun notifyFiltersReordered() {
        listeners.forEach { it.onFiltersReordered(activeFilters) }
    }
    
    private fun notifyAllFiltersDeactivated() {
        listeners.forEach { it.onAllFiltersDeactivated() }
    }
    
    /**
     * Interface per ascoltare i cambiamenti nei filtri
     */
    interface FilterChangeListener {
        fun onFilterActivated(filter: AdaptiveFilter) {}
        fun onFilterDeactivated(filter: AdaptiveFilter) {}
        fun onFiltersReordered(filters: List<AdaptiveFilter>) {}
        fun onAllFiltersDeactivated() {}
    }
}
