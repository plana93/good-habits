package com.programminghut.pose_detection.urban

import android.graphics.Color

/**
 * ╔════════════════════════════════════════════════════════════════╗
 * ║          CONFIGURAZIONE EFFETTI URBAN CAMERA                   ║
 * ║                                                                ║
 * ║  Modifica questi valori per personalizzare gli effetti        ║
 * ║  grafici senza toccare il codice principale                   ║
 * ╚════════════════════════════════════════════════════════════════╝
 */
object UrbanConfig {
    
    // ═══════════════════════════════════════════════════════════
    // 🎨 FILTRI DISPONIBILI
    // ═══════════════════════════════════════════════════════════
    
    /**
     * Tipi di filtro disponibili per l'immagine
     */
    enum class FilterType {
        NONE,           // Nessun filtro
        BLACK_WHITE,    // Bianco e nero
        SOBEL,          // Edge detection
        PIXELATED       // Effetto pixelato
    }
    
    /**
     * Filtro attualmente attivo
     */
    var CURRENT_FILTER = FilterType.NONE
    
    // ═══════════════════════════════════════════════════════════
    // 📦 BOX INTORNO AI GIUNTI
    // ═══════════════════════════════════════════════════════════
    
    /**
     * Probabilità che appaia una box (0.0 = mai, 1.0 = sempre)
     * Valori consigliati: 0.2 - 0.5
     */
    var BOX_APPEAR_PROBABILITY = 0.45f  // Aumentato da 0.3f per apparizione più veloce
    
    /**
     * Durata minima/massima di una box in frame
     * 1 frame ≈ 30-60 FPS → 30 frame ≈ 0.5-1 secondo
     */
    var BOX_MIN_DURATION = 15   // Ridotto da 10
    var BOX_MAX_DURATION = 55  // Ridotto da 40
    
    /**
     * Dimensioni delle box in pixel
     */
    var BOX_SIZE_MIN = 100f
    var BOX_SIZE_MAX = 600f
    
    /**
     * Dimensione box standard (usata dai controlli UI)
     */
    var BOX_SIZE = 350
    
    /**
     * Opacità delle box (0 = trasparente, 255 = opaco)
     */
    var BOX_OPACITY = 200
    
    /**
     * Spessore del bordo delle box in pixel
     */
    var BORDER_WIDTH = 1f  // Molto sottile (ridotto da 8f)
    
    // ═══════════════════════════════════════════════════════════
    // 🎨 COLORI E STILE
    // ═══════════════════════════════════════════════════════════
    
    /**
     * Colori monocromatici per i bordi delle box
     * Bianco, nero e scale di grigi
     */
    val BORDER_COLORS = listOf(
        Color.WHITE,              // Bianco
        Color.BLACK,              // Nero
        Color.rgb(200, 200, 200), // Grigio chiaro
        Color.rgb(150, 150, 150), // Grigio medio
        Color.rgb(100, 100, 100), // Grigio scuro
        Color.rgb(50, 50, 50),    // Grigio molto scuro
        Color.WHITE,              // Bianco (ripetuto per più probabilità)
        Color.BLACK               // Nero (ripetuto per più probabilità)
    )
    
    // ═══════════════════════════════════════════════════════════
    // 🦴 GIUNTI ATTIVI
    // ═══════════════════════════════════════════════════════════
    
    /**
     * Seleziona quali giunti mostrare con le box.
     * Commenta quelli che non vuoi vedere.
     * 
     * Indici giunti MoveNet:
     * 0: nose
     * 1-2: eyes (left, right)
     * 3-4: ears (left, right)
     * 5-6: shoulders (left, right)
     * 7-8: elbows (left, right)
     * 9-10: wrists (left, right)
     * 11-12: hips (left, right)
     * 13-14: knees (left, right)
     * 15-16: ankles (left, right)
     */
    val ACTIVE_JOINTS = listOf(
        0,      // nose
        5, 6,   // shoulders
        7, 8,   // elbows
        9, 10,  // wrists
        11, 12, // hips
        13, 14, // knees
        15, 16  // ankles
    )
    
    // ═══════════════════════════════════════════════════════════
    // 🎭 FILTRI IMMAGINE
    // ═══════════════════════════════════════════════════════════
    
    /**
     * Dimensione del pixel per l'effetto pixelato
     * Valori più alti = pixel più grandi
     */
    var PIXEL_SIZE = 32
    
    /**
     * Intensità dell'effetto Sobel (edge detection)
     * Valori più alti = bordi più marcati
     */
    var SOBEL_INTENSITY = 1.0f
    
    // ═══════════════════════════════════════════════════════════
    // ⏰ EFFETTI TEMPORALI (Future features)
    // ═══════════════════════════════════════════════════════════
    
    /**
     * Abilita effetti diversi in base all'orario
     * (DA IMPLEMENTARE)
     */
    var TIME_BASED_EFFECTS = false
    
    /**
     * Ore del giorno per effetti speciali
     * Es: [6, 12, 18, 22] = alba, mezzogiorno, tramonto, notte
     */
    val SPECIAL_HOURS = listOf(6, 12, 18, 22)
    
    // ═══════════════════════════════════════════════════════════
    // 🎯 THRESHOLD E PERFORMANCE
    // ═══════════════════════════════════════════════════════════
    
    /**
     * Score minimo per considerare un giunto valido (0.0 - 1.0)
     */
    var MIN_JOINT_SCORE = 0.2f
    
    /**
     * Limita FPS per risparmiare batteria
     * (0 = nessun limite, valore consigliato: 30)
     */
    var MAX_FPS = 30
}

/**
 * ═══════════════════════════════════════════════════════════
 * 📝 PRESET PREDEFINITI
 * ═══════════════════════════════════════════════════════════
 */
object UrbanPresets {
    
    /**
     * Preset "Glitch Art" - effetto glitch/distorsione
     */
    fun applyGlitchPreset() {
        UrbanConfig.BOX_APPEAR_PROBABILITY = 0.6f
        UrbanConfig.BOX_MIN_DURATION = 8
        UrbanConfig.BOX_MAX_DURATION = 15
        UrbanConfig.BOX_SIZE_MIN = 50f
        UrbanConfig.BOX_SIZE_MAX = 350f
        UrbanConfig.BORDER_WIDTH = 2f
    }
    
    /**
     * Preset "Neon City" - stile neon city lights
     */
    fun applyNeonPreset() {
        UrbanConfig.BOX_APPEAR_PROBABILITY = 0.4f
        UrbanConfig.BOX_MIN_DURATION = 20
        UrbanConfig.BOX_MAX_DURATION = 60
        UrbanConfig.BOX_SIZE_MIN = 100f
        UrbanConfig.BOX_SIZE_MAX = 250f
        UrbanConfig.BORDER_WIDTH = 12f
        UrbanConfig.BOX_OPACITY = 180
    }
    
    /**
     * Preset "Minimal" - effetto minimalista e pulito
     */
    fun applyMinimalPreset() {
        UrbanConfig.BOX_APPEAR_PROBABILITY = 0.2f
        UrbanConfig.BOX_MIN_DURATION = 30
        UrbanConfig.BOX_MAX_DURATION = 90
        UrbanConfig.BOX_SIZE_MIN = 120f
        UrbanConfig.BOX_SIZE_MAX = 180f
        UrbanConfig.BORDER_WIDTH = 2f
        UrbanConfig.BOX_OPACITY = 150
    }
    
    /**
     * Preset "Chaos" - effetto caotico e dinamico
     */
    fun applyChaosPreset() {
        UrbanConfig.BOX_APPEAR_PROBABILITY = 0.8f
        UrbanConfig.BOX_MIN_DURATION = 2
        UrbanConfig.BOX_MAX_DURATION = 15
        UrbanConfig.BOX_SIZE_MIN = 40f
        UrbanConfig.BOX_SIZE_MAX = 300f
        UrbanConfig.BORDER_WIDTH = 6f
    }
}
