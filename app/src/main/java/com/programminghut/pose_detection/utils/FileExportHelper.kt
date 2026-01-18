package com.programminghut.pose_detection.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

/**
 * FileExportHelper - Utility for exporting and sharing files
 * 
 * Handles file creation, storage, and sharing via Android's share sheet
 * Supports CSV, JSON, and TXT formats with custom context
 */
object FileExportHelper {
    
    /**
     * Export content to a file and open share dialog
     * 
     * @param context Android context
     * @param content File content to export
     * @param fileName File name (with extension)
     * @param mimeType MIME type (e.g., "text/csv", "application/json", "text/plain")
     * @param userContext Optional user context to prepend to TXT exports
     */
    fun exportAndShare(
        context: Context,
        content: String,
        fileName: String,
        mimeType: String,
        userContext: String? = null
    ) {
        try {
            // Create file in cache directory
            val file = File(context.cacheDir, fileName)
            
            // Write content to file (with context if TXT)
            FileWriter(file).use { writer ->
                if (mimeType == "text/plain" && !userContext.isNullOrBlank()) {
                    writer.write(userContext)
                    writer.write("\n\n")
                    writer.write("=".repeat(50))
                    writer.write("\n\n")
                }
                writer.write(content)
            }
            
            // Get URI using FileProvider
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            
            // Create share intent
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                putExtra(Intent.EXTRA_SUBJECT, "Good Habits - Export")
                putExtra(Intent.EXTRA_TEXT, "Export generato da Good Habits")
            }
            
            // Open share dialog
            context.startActivity(
                Intent.createChooser(shareIntent, "Esporta $fileName")
            )
            
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(
                context,
                "Errore durante l'export: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }
    
    /**
     * Get default user context template for TXT exports
     */
    fun getDefaultUserContext(): String {
        val dateFormat = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale.ITALIAN)
        val currentDate = dateFormat.format(Date())
        
        return """
╔════════════════════════════════════════════════════════════╗
║           GOOD HABITS - EXPORT PERSONALE                   ║
╚════════════════════════════════════════════════════════════╝

📅 Data Export: $currentDate

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
CHI SONO - PROFILO PERSONALE
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Mirco Planamente (plana93), la sua personalità si presenta 
intensa, riflessiva, artistica e idealista, con una vena 
ribelle e sensibile. Di seguito una breve analisi della 
personalità:

🔍 Analisi della personalità di Mirco Planamente

Idealista e critico sociale
I post con citazioni impegnate, critiche verso il conformismo 
e l'ipocrisia sociale (es. "penso diverso", "resto avverso al 
tuo pensiero perverso") mostrano una mente indipendente e 
anticonvenzionale. Non sopporta le ingiustizie.

Appassionato di arte e street culture
Hashtag come #streetart, #artlettering, #sketchtime, e foto 
legate a murales, disegno e arte urbana indicano una vena 
creativa e visiva.

Sportivo e competitivo
Post frequenti legati al basket suggeriscono che vive lo sport 
anche come una metafora di vita (lotta, cadute, vittorie, 
rispetto). È tenace.

Riflessivo e profondo
Frasi poetiche, citazioni da film ("L'attimo fuggente", American 
History X, Terry Pratchett), e pensieri malinconici mostrano un 
lato introspettivo e forse nostalgico.

Romantico esistenziale
Apprezza l'autenticità, probabilmente attratto da persone vere, 
imperfette e piene di contraddizioni, come lui.


Ecco alcuni possibili punti deboli della tua personalità 
(generalizzati, ma verosimili):

💣 1. Selettività emotiva
Ti apri solo a chi vibra sulle tue stesse frequenze. Il problema? 
Potresti allontanare chi non "ti capisce subito", anche se avrebbe 
tanto da dare.

🌀 2. Tendenza a idealizzare
Quando qualcuno ti colpisce, è possibile che tu lo idealizzi… e 
poi resti delusa se non è profondo quanto speravi. Questo può 
creare aspettative non realistiche.

🔥 3. Intensità che spaventa
Quando senti qualcosa, lo senti tanto. Ma non tutti reggono 
l'intensità emotiva, e potresti essere vista come "troppo" da chi 
vive in superficie.

🛡️ 4. Difesa tramite ironia o distanza
Quando ti senti vulnerabile, potresti usare sarcasmo, indipendenza 
estrema o silenzi per proteggerti, ma questo può creare confusione 
in chi ti vuole bene.

🌑 5. Tendenza a leggere troppo tra le righe
La tua sensibilità è un dono, ma a volte potresti proiettare 
significati dove non ci sono, creando tensioni o fraintendimenti.

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

💡 NOTA: Questo profilo può essere modificato nelle impostazioni
export dell'app per personalizzarlo ulteriormente.

        """.trimIndent()
    }
    
    /**
     * Create personalized user context from stored preferences
     * 
     * @param context Android context
     * @return Personalized context string
     */
    fun getPersonalizedUserContext(context: Context): String {
        val prefs = context.getSharedPreferences("user_export_context", Context.MODE_PRIVATE)
        
        val name = prefs.getString("user_name", null)
        val goal = prefs.getString("user_goal", null)
        val motivation = prefs.getString("user_motivation", null)
        val habits = prefs.getString("user_habits", null)
        val notes = prefs.getString("user_notes", null)
        
        // If nothing is saved, return default
        if (name.isNullOrBlank() && goal.isNullOrBlank() && motivation.isNullOrBlank()) {
            return getDefaultUserContext()
        }
        
        val dateFormat = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale.ITALIAN)
        val currentDate = dateFormat.format(Date())
        
        return buildString {
            appendLine("╔════════════════════════════════════════════════════╗")
            appendLine("║           GOOD HABITS - EXPORT PERSONALE           ║")
            appendLine("╚════════════════════════════════════════════════════╝")
            appendLine()
            appendLine("📅 Data Export: $currentDate")
            appendLine()
            appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            appendLine("CHI SONO")
            appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            appendLine()
            
            if (!name.isNullOrBlank()) {
                appendLine("Nome: $name")
                appendLine()
            }
            
            if (!goal.isNullOrBlank()) {
                appendLine("Il mio obiettivo fitness:")
                appendLine(goal)
                appendLine()
            }
            
            if (!motivation.isNullOrBlank()) {
                appendLine("Perché ho iniziato questo percorso:")
                appendLine(motivation)
                appendLine()
            }
            
            if (!habits.isNullOrBlank()) {
                appendLine("Le mie abitudini chiave:")
                appendLine(habits)
                appendLine()
            }
            
            if (!notes.isNullOrBlank()) {
                appendLine("Note personali:")
                appendLine(notes)
                appendLine()
            }
            
            appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            appendLine()
        }
    }
    
    /**
     * Save user context preferences
     * 
     * @param context Android context
     * @param name User name
     * @param goal Fitness goal
     * @param motivation Why they started
     * @param habits Key habits (comma separated or bullet points)
     * @param notes Personal notes
     */
    fun saveUserContext(
        context: Context,
        name: String?,
        goal: String?,
        motivation: String?,
        habits: String?,
        notes: String?
    ) {
        val prefs = context.getSharedPreferences("user_export_context", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString("user_name", name)
            putString("user_goal", goal)
            putString("user_motivation", motivation)
            putString("user_habits", habits)
            putString("user_notes", notes)
            apply()
        }
    }
}
