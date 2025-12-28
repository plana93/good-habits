package com.programminghut.pose_detection.util

import android.content.Context
import android.util.Log
import com.google.gson.*
import com.google.gson.reflect.TypeToken
import com.programminghut.pose_detection.data.model.ExerciseTemplate
import com.programminghut.pose_detection.data.model.TemplateExerciseType
import com.programminghut.pose_detection.data.model.TemplateExerciseMode
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import java.io.File
import java.io.IOException
import java.lang.reflect.Type

/**
 * Manager per caricare exercise templates da file JSON nella cartella assets
 */
object ExerciseTemplateFileManager {

    private const val TAG = "ExerciseTemplateFileManager"
    private const val EXERCISE_TEMPLATES_DIR = "exercise_templates"

    private val gson = GsonBuilder()
        .registerTypeAdapter(TemplateExerciseType::class.java, ExerciseTypeDeserializer())
        .registerTypeAdapter(TemplateExerciseMode::class.java, ExerciseModeDeserializer())
        .create()

    /**
     * Carica tutti i template esercizi dalla cartella assets
     */
    fun loadExerciseTemplates(context: Context): List<ExerciseTemplate> {
        val templates = mutableListOf<ExerciseTemplate>()

        try {
            val assetManager = context.assets
            val templateFiles = assetManager.list(EXERCISE_TEMPLATES_DIR)

            templateFiles?.forEach { fileName ->
                if (fileName.endsWith(".json")) {
                    try {
                        val template = loadExerciseTemplateFromAsset(context, fileName)
                        template?.let { templates.add(it) }
                    } catch (e: Exception) {
                        Log.e(TAG, "Errore caricamento template $fileName", e)
                    }
                }
            }

            Log.d(TAG, "Caricati ${templates.size} template esercizi")
        } catch (e: Exception) {
            Log.e(TAG, "Errore caricamento template esercizi", e)
        }

        return templates
    }

    /**
     * Carica un singolo template esercizio da file JSON
     */
    private fun loadExerciseTemplateFromAsset(context: Context, fileName: String): ExerciseTemplate? {
        return try {
            val assetManager = context.assets
            val inputStream = assetManager.open("$EXERCISE_TEMPLATES_DIR/$fileName")
            val jsonString = inputStream.bufferedReader().use { it.readText() }

            val template = gson.fromJson(jsonString, ExerciseTemplate::class.java)
            Log.d(TAG, "Caricato template: ${template.name} (ID: ${template.id})")
            template
        } catch (e: Exception) {
            Log.e(TAG, "Errore caricamento template $fileName", e)
            null
        }
    }

    /**
     * Carica un template esercizio specifico per ID
     */
    fun loadExerciseTemplateById(context: Context, id: Long): ExerciseTemplate? {
        return loadExerciseTemplates(context).find { it.id == id }
    }

    /**
     * Carica un template esercizio specifico per nome (esatto)
     */
    fun loadExerciseTemplateByName(context: Context, name: String): ExerciseTemplate? {
        return try {
            loadExerciseTemplates(context).find { it.name == name }
        } catch (e: Exception) {
            Log.e(TAG, "Errore ricerca template per nome: $name", e)
            null
        }
    }

    /**
     * Salva un template esercizio come file JSON
     */
    fun saveExerciseTemplate(context: Context, template: ExerciseTemplate): Boolean {
        return try {
            val jsonString = gson.toJson(template)
            val fileName = "${template.name.lowercase().replace(" ", "_")}.json"

            // Nota: Per salvare in assets servirebbe un approccio diverso
            // Questo è solo un placeholder per future implementazioni
            Log.d(TAG, "Template salvato (simulato): $fileName")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Errore salvataggio template", e)
            false
        }
    }

    /**
     * Deserializzatore per TemplateExerciseType
     */
    private class ExerciseTypeDeserializer : JsonDeserializer<TemplateExerciseType> {
        override fun deserialize(
            json: JsonElement,
            typeOfT: Type,
            context: JsonDeserializationContext
        ): TemplateExerciseType {
            return try {
                TemplateExerciseType.valueOf(json.asString)
            } catch (e: Exception) {
                Log.w(TAG, "Tipo esercizio sconosciuto: ${json.asString}, uso default STRENGTH")
                TemplateExerciseType.STRENGTH
            }
        }
    }

    /**
     * Deserializzatore per TemplateExerciseMode
     */
    private class ExerciseModeDeserializer : JsonDeserializer<TemplateExerciseMode> {
        override fun deserialize(
            json: JsonElement,
            typeOfT: Type,
            context: JsonDeserializationContext
        ): TemplateExerciseMode {
            return try {
                TemplateExerciseMode.valueOf(json.asString)
            } catch (e: Exception) {
                Log.w(TAG, "Modalità esercizio sconosciuta: ${json.asString}, uso default REPS")
                TemplateExerciseMode.REPS
            }
        }
    }
}

/**
 * Estensioni per ExerciseTemplate per ottenere l'icona Material Design
 */
fun ExerciseTemplate.getIcon(): ImageVector {
    return when (iconName) {
        "Accessibility" -> Icons.Default.Accessibility
        "DirectionsRun" -> Icons.Default.DirectionsRun
        "Timer" -> Icons.Default.Timer
        "FitnessCenter" -> Icons.Default.FitnessCenter
        "Science" -> Icons.Default.Science
        else -> Icons.Default.FitnessCenter // Default fallback
    }
}