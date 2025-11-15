package com.programminghut.pose_detection

import android.content.Context
import java.io.File
import java.io.IOException

/**
 * Gestisce il salvataggio e caricamento del conteggio totale degli squat
 */
class SquatCounter(private val context: Context) {
    
    companion object {
        private const val SQUAT_COUNT_FILE = "squat_total_count.txt"
        private const val TAG = "SquatCounter"
    }
    
    private var totalSquats: Int = 0
    private var currentSessionSquats: Int = 0
    
    init {
        // Carica il totale salvato all'inizializzazione
        loadTotalSquats()
    }
    
    /**
     * Carica il totale degli squat dal file locale
     * @return Il numero totale di squat salvati
     */
    fun loadTotalSquats(): Int {
        try {
            val file = File(context.filesDir, SQUAT_COUNT_FILE)
            if (file.exists()) {
                val content = file.readText().trim()
                totalSquats = content.toIntOrNull() ?: 0
                android.util.Log.d(TAG, "Caricati $totalSquats squat totali dal file")
            } else {
                totalSquats = 0
                android.util.Log.d(TAG, "File non esistente, totale impostato a 0")
            }
        } catch (e: IOException) {
            android.util.Log.e(TAG, "Errore nel caricamento degli squat: ${e.message}")
            totalSquats = 0
        } catch (e: NumberFormatException) {
            android.util.Log.e(TAG, "Formato numero non valido nel file: ${e.message}")
            totalSquats = 0
        }
        return totalSquats
    }
    
    /**
     * Salva il totale degli squat nel file locale
     * @return true se il salvataggio ha avuto successo
     */
    fun saveTotalSquats(): Boolean {
        return try {
            val file = File(context.filesDir, SQUAT_COUNT_FILE)
            file.writeText(totalSquats.toString())
            android.util.Log.d(TAG, "Salvati $totalSquats squat totali nel file: ${file.absolutePath}")
            true
        } catch (e: IOException) {
            android.util.Log.e(TAG, "Errore nel salvataggio degli squat: ${e.message}")
            false
        }
    }
    
    /**
     * Incrementa il conteggio degli squat
     */
    fun incrementSquat() {
        totalSquats++
        currentSessionSquats++
        // Salva automaticamente ad ogni incremento per sicurezza
        saveTotalSquats()
        android.util.Log.d(TAG, "Squat incrementato: sessione=$currentSessionSquats, totale=$totalSquats")
    }
    
    /**
     * Ottiene il totale degli squat
     */
    fun getTotalSquats(): Int = totalSquats
    
    /**
     * Ottiene gli squat della sessione corrente
     */
    fun getCurrentSessionSquats(): Int = currentSessionSquats
    
    /**
     * Resetta il conteggio della sessione corrente (ma mantiene il totale)
     */
    fun resetSessionSquats() {
        currentSessionSquats = 0
        android.util.Log.d(TAG, "Conteggio sessione resettato")
    }
    
    /**
     * Resetta completamente il conteggio (totale e sessione)
     * Usare con cautela!
     */
    fun resetAllSquats() {
        totalSquats = 0
        currentSessionSquats = 0
        saveTotalSquats()
        android.util.Log.d(TAG, "Conteggio totale resettato a 0")
    }
    
    /**
     * Da chiamare quando l'app viene chiusa per salvare i dati
     */
    fun onAppClosing() {
        saveTotalSquats()
        android.util.Log.d(TAG, "Salvataggio finale prima della chiusura dell'app")
    }
}
