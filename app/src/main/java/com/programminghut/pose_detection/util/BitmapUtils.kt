package com.programminghut.pose_detection.util

import android.graphics.Bitmap
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object BitmapUtils {
    /**
     * Salva una bitmap su file PNG.
     * @param bitmap La bitmap da salvare
     * @param file Il file di destinazione
     * @return true se il salvataggio è andato a buon fine
     */
    fun saveBitmapToFile(bitmap: Bitmap, file: File): Boolean {
        return try {
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            true
        } catch (e: IOException) {
            e.printStackTrace()
            false
        }
    }
}
