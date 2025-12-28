package com.programminghut.pose_detection.data.repository

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.Locale
import com.programminghut.pose_detection.data.model.WorkoutTemplate
import com.programminghut.pose_detection.data.model.WorkoutExerciseTemplate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStreamReader

/**
 * WorkoutTemplateFileManager - Gestisce i template di workout salvati come file JSON
 */
object WorkoutTemplateFileManager {

    private const val TAG = "WorkoutTemplateFileManager"
    private const val WORKOUT_TEMPLATES_DIR = "workout_templates"

    // Use a custom Gson that tolerates createdAt being a string date (e.g., "2024-12-25") or a long
    private val gson = GsonBuilder()
        .registerTypeAdapter(WorkoutTemplate::class.java, com.google.gson.JsonDeserializer<WorkoutTemplate> { json: JsonElement, typeOfT, deserializationContext ->
            try {
                val obj = json.asJsonObject

                val id = if (obj.has("id") && obj.get("id").isJsonPrimitive) obj.get("id").asLong else 0L
                val name = if (obj.has("name")) obj.get("name").asString else ""
                val description = if (obj.has("description")) obj.get("description").asString else ""
                val imagePath = if (obj.has("imagePath") && !obj.get("imagePath").isJsonNull) obj.get("imagePath").asString else null
                val thumbnailPath = if (obj.has("thumbnailPath") && !obj.get("thumbnailPath").isJsonNull) obj.get("thumbnailPath").asString else null

                // exercises array
                val exercises = if (obj.has("exercises") && obj.get("exercises").isJsonArray) {
                    deserializationContext.deserialize<List<WorkoutExerciseTemplate>>(obj.get("exercises"), object : TypeToken<List<WorkoutExerciseTemplate>>() {}.type)
                } else emptyList()

                // createdAt may be a number (epoch millis) or a string like yyyy-MM-dd
                var createdAtValue: Long = System.currentTimeMillis()
                if (obj.has("createdAt")) {
                    try {
                        val el = obj.get("createdAt")
                        if (el.isJsonPrimitive && el.asJsonPrimitive.isNumber) {
                            createdAtValue = el.asLong
                        } else if (el.isJsonPrimitive && el.asJsonPrimitive.isString) {
                            val s = el.asString
                            // try parse yyyy-MM-dd
                            try {
                                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT)
                                val d = sdf.parse(s)
                                if (d != null) createdAtValue = d.time
                            } catch (pe: Exception) {
                                // ignore, keep currentTimeMillis
                            }
                        }
                    } catch (_: Exception) {
                        // ignore, fallback to currentTimeMillis
                    }
                }

                val estimatedDuration = if (obj.has("estimatedDuration") && obj.get("estimatedDuration").isJsonPrimitive) obj.get("estimatedDuration").asInt else 0
                val isCustom = if (obj.has("isCustom") && obj.get("isCustom").isJsonPrimitive) obj.get("isCustom").asBoolean else true

                WorkoutTemplate(
                    id = id,
                    name = name,
                    description = description,
                    imagePath = imagePath,
                    thumbnailPath = thumbnailPath,
                    exercises = exercises,
                    createdAt = createdAtValue,
                    estimatedDuration = estimatedDuration,
                    isCustom = isCustom
                )
            } catch (e: Exception) {
                throw JsonSyntaxException("Error deserializing WorkoutTemplate", e)
            }
        })
        .create()

    /**
     * Carica tutti i template workout dalla cartella assets
     */
    suspend fun loadWorkoutTemplates(context: Context): List<WorkoutTemplate> = withContext(Dispatchers.IO) {
        val templates = mutableListOf<WorkoutTemplate>()

        try {
            val assetManager = context.assets
            val templateFiles = assetManager.list(WORKOUT_TEMPLATES_DIR)
            Log.d(TAG, "Template files in assets: ${templateFiles?.joinToString() ?: "(none)"}")

            templateFiles?.forEach { fileName ->
                if (fileName.endsWith(".json")) {
                    Log.d(TAG, "Parsing workout template file: $fileName")
                    try {
                        val template = loadWorkoutTemplateFromAsset(context, fileName)
                        if (template != null) {
                            templates.add(template)
                            Log.d(TAG, "Added workout template from $fileName -> ${template.name} (id=${template.id})")
                        } else {
                            Log.w(TAG, "Parsing produced null for file $fileName")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Errore caricamento template workout $fileName: ${e.message}", e)
                    }
                }
            }

            Log.d(TAG, "Caricati ${templates.size} template workout")
            // Fallback permissivo: se abbiamo file ma nessun template valido, tenta un parsing leggero per mostrare almeno nome/id
            if (templates.isEmpty() && templateFiles != null && templateFiles.isNotEmpty()) {
                Log.w(TAG, "Nessun template valido dopo parsing, eseguo fallback permissivo")
                templateFiles.filter { it.endsWith(".json") }.forEach { fileName ->
                    try {
                        val input = assetManager.open("$WORKOUT_TEMPLATES_DIR/$fileName")
                        val reader = InputStreamReader(input)
                        val el = JsonParser.parseReader(reader)
                        reader.close()

                        if (el != null && el.isJsonObject) {
                            val obj = el.asJsonObject
                            val name = if (obj.has("name")) obj.get("name").asString else fileName
                            val id = if (obj.has("id") && obj.get("id").isJsonPrimitive) obj.get("id").asLong else 0L
                            val exercisesCount = if (obj.has("exercises") && obj.get("exercises").isJsonArray) obj.get("exercises").asJsonArray.size() else 0

                            val minimal = WorkoutTemplate(
                                id = id,
                                name = name,
                                description = "(Parsed fallback)",
                                exercises = List(exercisesCount) { idx -> WorkoutExerciseTemplate(exerciseId = 0L, orderIndex = idx) },
                                estimatedDuration = 0,
                                isCustom = false
                            )
                            templates.add(minimal)
                            Log.w(TAG, "Fallback: aggiunto template minimale da $fileName -> $name (exercises=$exercisesCount)")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Errore fallback parsing file $fileName", e)
                    }
                }
                Log.d(TAG, "Dopo fallback templates count: ${templates.size}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Errore caricamento template workout", e)
        }

        return@withContext templates
    }

    /**
     * Carica un singolo template workout da file JSON
     */
    private suspend fun loadWorkoutTemplateFromAsset(context: Context, fileName: String): WorkoutTemplate? = withContext(Dispatchers.IO) {
        return@withContext try {
            val assetManager = context.assets
            val inputStream = assetManager.open("$WORKOUT_TEMPLATES_DIR/$fileName")
            val reader = InputStreamReader(inputStream)

            val template = gson.fromJson(reader, WorkoutTemplate::class.java)
            reader.close()

            Log.d(TAG, "Caricato template workout: ${template.name} (ID: ${template.id})")
            template
        } catch (e: Exception) {
            Log.e(TAG, "Errore caricamento template workout $fileName", e)
            null
        }
    }

    /**
     * Load a single workout template file by filename (for debugging / inspection).
     */
    suspend fun loadWorkoutTemplateFromFile(context: Context, fileName: String): WorkoutTemplate? = withContext(Dispatchers.IO) {
        return@withContext try {
            val assetManager = context.assets
            val inputStream = assetManager.open("$WORKOUT_TEMPLATES_DIR/$fileName")
            val reader = InputStreamReader(inputStream)
            val template = gson.fromJson(reader, WorkoutTemplate::class.java)
            reader.close()
            Log.d(TAG, "[DEBUG] Parsed file $fileName -> ${template.name} (ID: ${template.id})")
            template
        } catch (e: Exception) {
            Log.e(TAG, "[DEBUG] Error parsing file $fileName", e)
            null
        }
    }

    /**
     * Carica un template workout specifico per ID
     */
    suspend fun loadWorkoutTemplateById(context: Context, id: Long): WorkoutTemplate? = withContext(Dispatchers.IO) {
        return@withContext loadWorkoutTemplates(context).find { it.id == id }
    }

    /**
     * Salva un template workout come file JSON
     */
    suspend fun saveWorkoutTemplate(context: Context, template: WorkoutTemplate): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val jsonString = gson.toJson(template)
            val fileName = "${template.name.lowercase().replace(" ", "_").replace("[^a-z0-9_]".toRegex(), "")}.json"

            // Nota: Per salvare in assets servirebbe un approccio diverso
            // Questo è solo un placeholder per future implementazioni
            Log.d(TAG, "Template workout salvato (simulato): $fileName")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Errore salvataggio template workout", e)
            false
        }
    }

    /**
     * Ottiene la lista dei nomi dei file template disponibili
     */
    fun getAvailableTemplateFiles(context: Context): List<String> {
        return try {
            val assetManager = context.assets
            assetManager.list(WORKOUT_TEMPLATES_DIR)?.filter { it.endsWith(".json") } ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Errore lettura file template disponibili", e)
            emptyList()
        }
    }
}