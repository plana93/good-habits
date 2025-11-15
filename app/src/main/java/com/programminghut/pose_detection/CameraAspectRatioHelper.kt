package com.programminghut.pose_detection

import android.graphics.Matrix
import android.view.TextureView
import android.view.ViewGroup

/**
 * Helper per gestire l'aspect ratio corretto della camera
 * Previene l'allungamento verticale/orizzontale dell'immagine
 */
object CameraAspectRatioHelper {
    
    /**
     * Configura il TextureView per mostrare la camera con proporzioni corrette
     * 
     * @param textureView Il TextureView da configurare
     * @param previewWidth Larghezza preview camera (es. 1920)
     * @param previewHeight Altezza preview camera (es. 1080)
     * @param isFrontCamera Se true, specchia l'immagine orizzontalmente
     */
    fun configureTextureView(
        textureView: TextureView,
        previewWidth: Int = 1920,
        previewHeight: Int = 1080,
        isFrontCamera: Boolean = false
    ) {
        val viewWidth = textureView.width
        val viewHeight = textureView.height
        
        if (viewWidth == 0 || viewHeight == 0) return
        
        // Calcola aspect ratio della camera e del view
        val cameraAspectRatio = previewHeight.toFloat() / previewWidth.toFloat()
        val viewAspectRatio = viewHeight.toFloat() / viewWidth.toFloat()
        
        // Calcola lo scale per mantenere aspect ratio
        var scaleX: Float
        val scaleY: Float
        
        if (viewAspectRatio > cameraAspectRatio) {
            // View più alto → scala in base all'altezza
            scaleX = viewAspectRatio / cameraAspectRatio
            scaleY = 1f
        } else {
            // View più largo → scala in base alla larghezza
            scaleX = 1f
            scaleY = cameraAspectRatio / viewAspectRatio
        }
        
        // Specchia l'immagine per la fotocamera frontale
        if (isFrontCamera) {
            scaleX = -scaleX
        }
        
        // Applica la trasformazione
        val matrix = Matrix()
        matrix.setScale(scaleX, scaleY, viewWidth / 2f, viewHeight / 2f)
        textureView.setTransform(matrix)
    }
    
    /**
     * Configura il TextureView con aspect ratio 16:9 (standard camera)
     * @param isFrontCamera Se true, specchia l'immagine orizzontalmente
     */
    fun configureTextureView16x9(textureView: TextureView, isFrontCamera: Boolean = false) {
        configureTextureView(textureView, 1920, 1080, isFrontCamera)
    }
    
    /**
     * Configura il TextureView con aspect ratio 4:3 (camera frontale)
     * @param isFrontCamera Se true, specchia l'immagine orizzontalmente
     */
    fun configureTextureView4x3(textureView: TextureView, isFrontCamera: Boolean = false) {
        configureTextureView(textureView, 640, 480, isFrontCamera)
    }
    
    /**
     * Ridimensiona dinamicamente il TextureView per matchare l'aspect ratio
     * Questa è un'alternativa che modifica le dimensioni del view stesso
     */
    fun resizeTextureView(
        textureView: TextureView,
        containerWidth: Int,
        containerHeight: Int,
        previewWidth: Int = 1920,
        previewHeight: Int = 1080
    ) {
        val cameraAspectRatio = previewWidth.toFloat() / previewHeight.toFloat()
        
        val newWidth: Int
        val newHeight: Int
        
        if (containerWidth > containerHeight * cameraAspectRatio) {
            // Container più largo
            newHeight = containerHeight
            newWidth = (containerHeight * cameraAspectRatio).toInt()
        } else {
            // Container più alto
            newWidth = containerWidth
            newHeight = (containerWidth / cameraAspectRatio).toInt()
        }
        
        val layoutParams = textureView.layoutParams
        layoutParams.width = newWidth
        layoutParams.height = newHeight
        textureView.layoutParams = layoutParams
    }
}
