package com.programminghut.pose_detection

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.util.Log
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileWriter
import java.io.InputStreamReader
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Classe per il logging delle coordinate dello scheletro su file.
 * Salva frame per frame tutte le coordinate e gli score dei punti rilevati.
 */
class PoseLogger(context: Context) {
    
    companion object {
        private const val TAG = "PoseLogger"
    }
    
    private val context: Context = context.applicationContext
    private val file: File
    private var writer: FileWriter? = null
    private var frameCounter: Long = 0L
    
    init {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        file = File(context.filesDir, "skeleton_log_$timestamp.txt")
        writer = FileWriter(file, true)
        
        // Intestazione che spiega il formato dei dati
        writer?.write("# SKELETON POSE LOGGING\n")
        writer?.write("# FORMAT: FRAME <frame_number>\n")
        writer?.write("# Poi per ogni punto: <joint_index> <x> <y> <score>\n")
        writer?.write("# \n")
        writer?.write("# COORDINATE:\n")
        writer?.write("# x, y: coordinate normalizzate (0.0 - 1.0)\n")
        writer?.write("# z: profondità/depth (dipende dal modello)\n")
        writer?.write("# score: confidenza della rilevazione (0.0 - 1.0)\n")
        writer?.write("# \n")
        writer?.write("# INDICI GIUNTI (MoveNet 17 keypoints):\n")
        writer?.write("# 0: nose, 1: left_eye, 2: right_eye, 3: left_ear, 4: right_ear\n")
        writer?.write("# 5: left_shoulder, 6: right_shoulder, 7: left_elbow, 8: right_elbow\n")
        writer?.write("# 9: left_wrist, 10: right_wrist, 11: left_hip, 12: right_hip\n")
        writer?.write("# 13: left_knee, 14: right_knee, 15: left_ankle, 16: right_ankle\n")
        writer?.write("# ========================================\n\n")
        writer?.flush()
    }
    
    /**
     * Salva un frame con tutte le coordinate dei giunti.
     * @param outputFeature0 Array di 51 float (17 keypoints × 3: y, x, score)
     */
    @Synchronized
    fun logFrame(outputFeature0: FloatArray) {
        try {
            writer?.write("FRAME ${frameCounter++}\n")
            
            // MoveNet restituisce 17 keypoints, ogni keypoint ha 3 valori: y, x, score
            for (i in 0 until 17) {
                val y = outputFeature0[i * 3 + 0]  // coordinata Y
                val x = outputFeature0[i * 3 + 1]  // coordinata X
                val score = outputFeature0[i * 3 + 2]  // score di confidenza
                val z = 0.0f  // MoveNet non fornisce depth, usa 0
                
                writer?.write(String.format(Locale.US, "%d %.4f %.4f %.4f\n", i, x, y, score))
            }
            writer?.write("\n")  // riga vuota tra frame
            writer?.flush()
        } catch (e: IOException) {
            Log.e(TAG, "Errore scrittura frame", e)
        }
    }
    
    /**
     * Chiude il writer e finalizza il file.
     */
    @Synchronized
    fun close() {
        try {
            writer?.flush()
            writer?.close()
            writer = null
            Log.d(TAG, "File chiuso: ${file.absolutePath}")
        } catch (e: IOException) {
            Log.e(TAG, "Errore chiusura file", e)
        }
    }
    
    /**
     * Copia tutto il contenuto del file negli appunti e chiude l'app.
     */
    fun copyFileToClipboardAndExit(activity: Activity) {
        close()
        try {
            val fis = FileInputStream(file)
            val br = BufferedReader(InputStreamReader(fis, "UTF-8"))
            val sb = StringBuilder()
            var line: String?
            
            while (br.readLine().also { line = it } != null) {
                sb.append(line).append("\n")
            }
            br.close()
            
            val clipboardManager = activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("skeleton_log", sb.toString())
            clipboardManager.setPrimaryClip(clip)
            
            Log.d(TAG, "File copiato negli appunti: ${sb.length} caratteri")
        } catch (e: Exception) {
            Log.e(TAG, "Errore copia appunti", e)
        }
        
        // Chiude l'app completamente
        activity.finishAffinity()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            activity.finishAndRemoveTask()
        }
    }
    
    fun getFilePath(): String = file.absolutePath
}
