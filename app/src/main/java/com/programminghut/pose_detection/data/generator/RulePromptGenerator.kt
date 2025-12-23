package com.programminghut.pose_detection.data.generator

/**
 * Genera prompt per LLM per creare regole di esercizi
 */
object RulePromptGenerator {
    
    /**
     * Genera un prompt completo da copiare in un LLM (ChatGPT, Claude, ecc.)
     */
    fun generateRulePrompt(
        exerciseName: String,
        exerciseDescription: String,
        exerciseMode: String, // "DYNAMIC" o "STATIC"
        category: String
    ): String {
        val modeDescription = when (exerciseMode) {
            "DYNAMIC" -> "DINAMICO (conta ripetizioni, con movimento da posizione iniziale a finale)"
            "STATIC" -> "STATICO (mantieni posizione, conta secondi di hold)"
            else -> "CUSTOM"
        }
        
        return buildString {
            appendLine("# 🏋️ Prompt per Generazione Regole Esercizio")
            appendLine()
            appendLine("Crea regole di validazione per questo esercizio:")
            appendLine()
            appendLine("**Nome Esercizio:** $exerciseName")
            appendLine("**Descrizione:** $exerciseDescription")
            appendLine("**Categoria:** $category")
            appendLine("**Tipo:** $modeDescription")
            appendLine()
            appendLine("---")
            appendLine()
            appendLine(getRuleGuideTemplate())
            appendLine()
            appendLine("---")
            appendLine()
            appendLine(getKeypointReferenceGuide())
            appendLine()
            appendLine("---")
            appendLine()
            appendLine("## 📋 Output Richiesto")
            appendLine()
            appendLine("Genera una lista di regole in formato Kotlin che posso copiare direttamente nel codice.")
            appendLine("Usa questo formato:")
            appendLine()
            appendLine("```kotlin")
            appendLine("val rules = listOf(")
            appendLine("    Rule(")
            appendLine("        type = RuleType.ANGLE,")
            appendLine("        keypointIndices = listOf(11, 13, 15),  // Spalla-Gomito-Polso sinistro")
            appendLine("        targetValue = 90f,")
            appendLine("        tolerance = 15f,")
            appendLine("        description = \"Gomito piegato a 90°\"")
            appendLine("    ),")
            appendLine("    // ... altre regole")
            appendLine(")")
            appendLine("```")
            appendLine()
            appendLine("**Genera 3-5 regole principali per questo esercizio.**")
        }
    }
    
    /**
     * Template guida per le regole
     */
    private fun getRuleGuideTemplate(): String {
        return """
## 📚 Guida alle Regole

### Tipi di Regole Disponibili:

1. **ANGLE** - Verifica angolo tra 3 keypoints
   - `keypointIndices`: Lista di 3 indici [punto1, punto2, punto3]
   - `targetValue`: Angolo target in gradi (0-180)
   - `tolerance`: Tolleranza in gradi (es. ±15°)
   - Esempio: Gomito piegato a 90° = [11, 13, 15] target 90°

2. **DISTANCE** - Verifica distanza tra 2 keypoints
   - `keypointIndices`: Lista di 2 indici [punto1, punto2]
   - `targetValue`: Distanza target normalizzata (0.0-1.0)
   - `tolerance`: Tolleranza (es. ±0.1)
   - Esempio: Mani vicine = [9, 10] target 0.1

3. **Y_POSITION** - Verifica posizione verticale di un keypoint
   - `keypointIndices`: Lista di 1 indice [punto]
   - `targetValue`: Y target normalizzato (0.0-1.0, 0=alto, 1=basso)
   - `tolerance`: Tolleranza verticale
   - Esempio: Mano sopra la testa = [9] target 0.2

4. **X_POSITION** - Verifica posizione orizzontale di un keypoint
   - `keypointIndices`: Lista di 1 indice [punto]
   - `targetValue`: X target normalizzato (0.0-1.0, 0=sinistra, 1=destra)
   - `tolerance`: Tolleranza orizzontale
   - Esempio: Mano al centro = [9] target 0.5

### Come Scegliere le Regole:

**Per esercizi DINAMICI:**
- Definisci regole per POSIZIONE INIZIALE (quando parte)
- Definisci regole per POSIZIONE FINALE (quando completa)
- Aggiungi regole di "transizione" se necessario

**Per esercizi STATICI:**
- Definisci solo regole per la posizione da mantenere
- Sii più preciso con le tolleranze (±10° invece di ±20°)

**Regole comuni utili:**
- Schiena dritta: Angolo spalla-anca-ginocchio ≈ 170-180°
- Ginocchio piegato 90°: Angolo anca-ginocchio-caviglia = 90°
- Braccia distese: Angolo spalla-gomito-polso ≈ 170-180°
- Squat profondo: Anca sotto ginocchia (Y_POSITION)
        """.trimIndent()
    }
    
    /**
     * Guida ai keypoints MoveNet
     */
    private fun getKeypointReferenceGuide(): String {
        return """
## 🎯 Riferimento Keypoints (MoveNet)

### Indici Keypoints:
```
0  - Naso
1  - Occhio Sinistro
2  - Occhio Destro
3  - Orecchio Sinistro
4  - Orecchio Destro
5  - Spalla Sinistra
6  - Spalla Destra
7  - Gomito Sinistro
8  - Gomito Destro
9  - Polso Sinistro
10 - Polso Destro
11 - Anca Sinistra
12 - Anca Destra
13 - Ginocchio Sinistro
14 - Ginocchio Destro
15 - Caviglia Sinistra
16 - Caviglia Destra
```

### Combinazioni Comuni:

**Braccio Sinistro:**
- Spalla-Gomito-Polso: [5, 7, 9]

**Braccio Destro:**
- Spalla-Gomito-Polso: [6, 8, 10]

**Gamba Sinistra:**
- Anca-Ginocchio-Caviglia: [11, 13, 15]

**Gamba Destra:**
- Anca-Ginocchio-Caviglia: [12, 14, 16]

**Busto:**
- Spalla-Anca-Ginocchio (sinistra): [5, 11, 13]
- Spalla-Anca-Ginocchio (destra): [6, 12, 14]

**Simmetria:**
- Distanza Polsi: [9, 10]
- Distanza Caviglie: [15, 16]
        """.trimIndent()
    }
    
    /**
     * Genera esempio di output per l'utente
     */
    fun generateExampleOutput(): String {
        return """
## 📝 Esempio Output LLM

Dopo aver incollato il prompt in ChatGPT/Claude, riceverai qualcosa tipo:

```kotlin
val rules = listOf(
    Rule(
        type = RuleType.ANGLE,
        keypointIndices = listOf(11, 13, 15),
        targetValue = 90f,
        tolerance = 15f,
        description = "Ginocchio sinistro piegato a 90°"
    ),
    Rule(
        type = RuleType.ANGLE,
        keypointIndices = listOf(12, 14, 16),
        targetValue = 90f,
        tolerance = 15f,
        description = "Ginocchio destro piegato a 90°"
    ),
    Rule(
        type = RuleType.Y_POSITION,
        keypointIndices = listOf(11),
        targetValue = 0.6f,
        tolerance = 0.1f,
        description = "Anca abbassata (squat profondo)"
    )
)
```

Copia questo codice e incollalo nella sezione "Regole" dell'app!
        """.trimIndent()
    }
}
