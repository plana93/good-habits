package com.programminghut.pose_detection.utils

/**
 * Collezione di frasi motivazionali per giorni passati senza allenamenti
 * 
 * Questo file contiene citazioni da film, libri e poesie famose
 * per motivare l'utente quando visualizza giorni passati senza esercizi.
 * 
 * Puoi aggiungere nuove frasi semplicemente inserendole nell'array motivationalQuotes.
 * Mantieni le frasi brevi e impattanti (max 2 righe).
 */
object MotivationalQuotes {
    
    /**
     * Array di frasi motivazionali
     * 
     * Per aggiungere nuove frasi:
     * 1. Aggiungi la stringa nell'array
     * 2. Mantieni il formato breve (max 2 righe)
     * 3. Usa caratteri di escape (\n) per andare a capo se necessario
     * 
     * Fonti: Film, letteratura, poesie, citazioni famose
     */
    private val motivationalQuotes = arrayOf(
        // Film classici
        "\"The future belongs to those\nwho believe in the beauty of their dreams\"",
        "\"Yesterday is history, tomorrow is a mystery,\nbut today is a gift\"",
        "\"It's not about how hard you hit,\nit's about how hard you can get hit and keep moving\"",
        "\"The only impossible journey\nis the one you never begin\"",
        "\"Success is not final, failure is not fatal:\nit is the courage to continue that counts\"",
        
        // Citazioni motivazionali
        "\"A journey of a thousand miles\nbegins with a single step\"",
        "\"The best time to plant a tree was 20 years ago.\nThe second best time is now\"",
        "\"Fall seven times, stand up eight\"",
        "\"What doesn't kill you makes you stronger\"",
        "\"Every expert was once a beginner\"",
        
        // Frasi italiane
        "\"Chi non risica, non rosica\"",
        "\"Volere è potere\"",
        "\"Dopo la tempesta viene sempre il sereno\"",
        "\"Non è mai troppo tardi per ricominciare\"",
        "\"Il domani appartiene a chi ci crede oggi\"",
        
        // Sport e fitness
        "\"No pain, no gain\"",
        "\"Your body can stand it.\nIt's your mind you have to convince\"",
        "\"The only bad workout\nis the one that didn't happen\"",
        "\"Strength doesn't come from what you can do.\nIt comes from overcoming what you thought you couldn't\"",
        "\"Champions train, losers complain\"",
        
        // Poesia e letteratura
        "\"Two roads diverged in a wood,\nand I took the one less traveled by\"",
        "\"Do not go gentle into that good night\"",
        "\"Invictus: I am the master of my fate,\nI am the captain of my soul\"",
        "\"Per aspera ad astra\"",
        "\"Carpe diem, quam minimum credula postero\"",
        
        // Filosofiche
        "\"The only way to do great work\nis to love what you do\"",
        "\"Be yourself; everyone else is already taken\"",
        "\"In the middle of difficulty lies opportunity\"",
        "\"Life is 10% what happens to you\nand 90% how you react to it\"",
        "\"The greatest glory is not in never falling,\nbut in rising every time we fall\""
    )
    
    /**
     * Restituisce una frase motivazionale casuale
     * 
     * @return Stringa con frase motivazionale random
     */
    fun getRandomQuote(): String {
        return motivationalQuotes.random()
    }
    
    /**
     * Restituisce una frase motivazionale basata su un seed
     * Utile per avere la stessa frase per la stessa data
     * 
     * @param seed Seed per determinare la frase (es. timestamp del giorno)
     * @return Stringa con frase motivazionale deterministica
     */
    fun getQuoteForSeed(seed: Long): String {
        val index = (seed % motivationalQuotes.size).toInt()
        return motivationalQuotes[index]
    }
    
    /**
     * Restituisce il numero totale di frasi disponibili
     * 
     * @return Numero di frasi nella collezione
     */
    fun getQuoteCount(): Int {
        return motivationalQuotes.size
    }
}