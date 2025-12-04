package com.programminghut.pose_detection.effects

import android.graphics.*
import com.programminghut.pose_detection.effects.RandomProvider
import com.programminghut.pose_detection.R

// Assicurati che il package sia aggiornato dopo lo spostamento in effects/

/**
 * Filtro che disegna box/rettangoli urbani intorno al corpo
 */
class UrbanBoxesFilter : AdaptiveFilter(
    id = "urban_boxes",
    name = "Urban Boxes",
    iconResId = R.drawable.ic_urban_boxes,
    description = "Disegna box e forme geometriche urbane intorno ai keypoint del corpo. Stile futuristico/cyberpunk con linee nette e angoli precisi."
) {
    override val requiresPose: Boolean = true
    
    private val paint = Paint().apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.SQUARE
        strokeJoin = Paint.Join.MITER
        isAntiAlias = true
    }
    
    init {
        parameters["boxSize"] = FilterParameter.Slider(
            paramKey = "boxSize",
            paramDisplayName = "Dimensione Box",
            paramDescription = "Regola la dimensione dei box intorno ai keypoint",
            value = 180f,
            min = 20f,
            max = 500f,
            step = 5f
        )

        // Parametri per il numero minimo e massimo di box
        parameters["boxCountMin"] = FilterParameter.Slider(
            paramKey = "boxCountMin",
            paramDisplayName = "Numero Minimo Box",
            paramDescription = "Numero minimo di box generati",
            value = 3f,
            min = 1f,
            max = 20f,
            step = 1f
        )
        parameters["boxCountMax"] = FilterParameter.Slider(
            paramKey = "boxCountMax",
            paramDisplayName = "Numero Massimo Box",
            paramDescription = "Numero massimo di box generati",
            value = 8f,
            min = 1f,
            max = 20f,
            step = 1f
        )
        
        parameters["customSizeMode"] = FilterParameter.Choice(
            paramKey = "customSizeMode",
            paramDisplayName = "Modalità Dimensione",
            paramDescription = "Scegli come vengono calcolate le dimensioni dei box",
            selectedIndex = 2,
            options = listOf("Fissa", "Animata", "Random")
        )
        
        parameters["sizeVariation"] = FilterParameter.Slider(
            paramKey = "sizeVariation",
            paramDisplayName = "Variazione Dimensione",
            paramDescription = "Quanto variano le dimensioni dei box (0 = nessuna variazione)",
            value = 50f,
            min = 0f,
            max = 100f,
            step = 5f
        )
        
        parameters["lineWidth"] = FilterParameter.Slider(
            paramKey = "lineWidth",
            paramDisplayName = "Spessore Linea",
            paramDescription = "Spessore dei bordi dei box",
            value = 1f,
            min = 1f,
            max = 10f,
            step = 1f
        )
        
        parameters["color"] = FilterParameter.ColorRGB(
            paramKey = "color",
            paramDisplayName = "Colore",
            paramDescription = "Colore dei box urbani",
            red = 57,
            green = 255,
            blue = 20
        )
        
        parameters["boxStyle"] = FilterParameter.Choice(
            paramKey = "boxStyle",
            paramDisplayName = "Stile Box",
            paramDescription = "Scegli lo stile delle forme geometriche",
            selectedIndex = 0,
            options = listOf("Quadrati", "Cerchi", "Esagoni", "Rombi", "Crocette")
        )
        
        parameters["fillOpacity"] = FilterParameter.Slider(
            paramKey = "fillOpacity",
            paramDisplayName = "Opacità Riempimento",
            paramDescription = "0 = solo bordi, 255 = completamente riempito",
            value = 0f,
            min = 0f,
            max = 255f,
            step = 5f
        )
        
        parameters["innerEffect"] = FilterParameter.Choice(
            paramKey = "innerEffect",
            paramDisplayName = "Effetto Interno",
            paramDescription = "Applica un effetto all'interno dei box",
            selectedIndex = 5,
            options = listOf("Nessuno", "Pixelato", "Sobel Edge", "Bianco/Nero", "Blur", "Random")
        )
        
        parameters["pixelSize"] = FilterParameter.Slider(
            paramKey = "pixelSize",
            paramDisplayName = "Dimensione Pixel",
            paramDescription = "Dimensione dei pixel per l'effetto pixelato",
            value = 20f,
            min = 2f,
            max = 50f,
            step = 2f
        )
        
        parameters["edgeThreshold"] = FilterParameter.Slider(
            paramKey = "edgeThreshold",
            paramDisplayName = "Soglia Edge",
            paramDescription = "Sensibilità dell'effetto Sobel per rilevare i bordi",
            value = 100f,
            min = 10f,
            max = 200f,
            step = 10f
        )
        
        parameters["cornerLines"] = FilterParameter.Toggle(
            paramKey = "cornerLines",
            paramDisplayName = "Linee Angolo",
            paramDescription = "Disegna solo gli angoli invece dei box completi (stile HUD)",
            enabled = false
        )
        
        parameters["rotateBoxes"] = FilterParameter.Toggle(
            paramKey = "rotateBoxes",
            paramDisplayName = "Rotazione Box",
            paramDescription = "I box ruotano leggermente per seguire il movimento",
            enabled = false
        )
    }
    
    override fun apply(canvas: Canvas, bitmap: Bitmap, poseKeypoints: FloatArray?) {
        if (poseKeypoints == null || poseKeypoints.size < 51) return
        
        // Recupera parametri
        val baseBoxSize = (parameters["boxSize"] as FilterParameter.Slider).value
        val customSizeMode = (parameters["customSizeMode"] as FilterParameter.Choice).selectedIndex
        val sizeVariation = (parameters["sizeVariation"] as FilterParameter.Slider).value
        val lineWidth = (parameters["lineWidth"] as FilterParameter.Slider).value
        val colorParam = parameters["color"] as FilterParameter.ColorRGB
        val boxStyle = (parameters["boxStyle"] as FilterParameter.Choice).selectedIndex
        val fillOpacity = (parameters["fillOpacity"] as FilterParameter.Slider).value.toInt()
        val innerEffect = (parameters["innerEffect"] as FilterParameter.Choice).selectedIndex
        val pixelSize = (parameters["pixelSize"] as FilterParameter.Slider).value.toInt()
        val edgeThreshold = (parameters["edgeThreshold"] as FilterParameter.Slider).value
        val cornerLines = (parameters["cornerLines"] as FilterParameter.Toggle).enabled
        val rotateBoxes = (parameters["rotateBoxes"] as FilterParameter.Toggle).enabled
        
        // Debug logging (rimuovere dopo il test)
        if (innerEffect > 0) {
            android.util.Log.d("UrbanBoxesFilter", "InnerEffect active: $innerEffect (pixelSize=$pixelSize, edgeThreshold=$edgeThreshold)")
        }
        
        // Configura paint
        paint.strokeWidth = lineWidth
        paint.color = colorParam.getColor()
        
        // Disegna box su keypoint principali
        val keypointsToShow = listOf(0, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16)
        
        keypointsToShow.forEachIndexed { index, idx ->
            val x = poseKeypoints[idx * 3]
            val y = poseKeypoints[idx * 3 + 1]
            val confidence = poseKeypoints[idx * 3 + 2]
            
            if (confidence > 0.3) {
                // Calcola dimensione in base alla modalità
                val boxSize = when (customSizeMode) {
                    1 -> {
                        // Modalità Animata: varia in base alla posizione e al confidence
                        val variation = (Math.sin(index * 0.5 + com.programminghut.pose_detection.effects.FrameClock.timeMs / 1000.0) * sizeVariation).toFloat()
                        baseBoxSize + variation * confidence
                    }
                    2 -> {
                        // Modalità Random: dimensione casuale per ogni box
                        val randomVariation = ((RandomProvider.nextDouble() * sizeVariation * 2) - sizeVariation).toFloat()
                        (baseBoxSize + randomVariation).coerceAtLeast(10f)
                    }
                    else -> {
                        // Modalità Fissa
                        baseBoxSize
                    }
                }
                
                // Calcola rotazione se abilitata
                val rotation = if (rotateBoxes && idx + 1 < 17) {
                    val nextX = poseKeypoints[(idx + 1) * 3]
                    val nextY = poseKeypoints[(idx + 1) * 3 + 1]
                    Math.toDegrees(Math.atan2((nextY - y).toDouble(), (nextX - x).toDouble())).toFloat()
                } else {
                    0f
                }
                
                // Disegna il box (bordo e riempimento base se innerEffect = 0)
                drawBox(canvas, bitmap, x, y, boxSize, boxStyle, fillOpacity, cornerLines, rotation, innerEffect, pixelSize, edgeThreshold)
            }
        }
    }
    
    private fun drawBox(
        canvas: Canvas,
        bitmap: Bitmap,
        x: Float,
        y: Float,
        size: Float,
        style: Int,
        fillOpacity: Int,
        cornerOnly: Boolean,
        rotation: Float,
        innerEffect: Int,
        pixelSize: Int,
        edgeThreshold: Float
    ) {
        canvas.save()
        canvas.rotate(rotation, x, y)
        
        // Se c'è un effetto interno (innerEffect > 0), applicalo con clipping
        if (innerEffect > 0) {
            // Crea un path per il clipping basato sulla forma
            val clipPath = createShapePath(x, y, size, style)
            
            // Applica il clipping
            canvas.save()
            canvas.clipPath(clipPath)
            
            // Determina l'effetto da applicare (random se innerEffect == 5)
                val effectToApply = if (innerEffect == 5) {
                    // Random: scegli tra 1-4 using RandomProvider
                    1 + RandomProvider.nextInt(4)
                } else {
                    innerEffect
                }
            
            applyInnerEffect(canvas, bitmap, x, y, size, effectToApply, pixelSize, edgeThreshold)
            canvas.restore()
        } else if (fillOpacity > 0) {
            // Altrimenti disegna il riempimento normale
            val fillPaint = Paint(paint).apply {
                this.style = Paint.Style.FILL
                alpha = fillOpacity
            }
            
            when (style) {
                0 -> drawSquare(canvas, x, y, size, fillPaint)
                1 -> canvas.drawCircle(x, y, size / 2, fillPaint)
                2 -> drawHexagon(canvas, x, y, size, fillPaint)
                3 -> drawDiamond(canvas, x, y, size, fillPaint)
                4 -> drawCross(canvas, x, y, size, fillPaint)
            }
        }
        
        // Disegna sempre il bordo
        if (cornerOnly) {
            drawCorners(canvas, x, y, size, style)
        } else {
            when (style) {
                0 -> drawSquare(canvas, x, y, size, paint)
                1 -> canvas.drawCircle(x, y, size / 2, paint)
                2 -> drawHexagon(canvas, x, y, size, paint)
                3 -> drawDiamond(canvas, x, y, size, paint)
                4 -> drawCross(canvas, x, y, size, paint)
            }
        }
        
        canvas.restore()
    }
    
    private fun createShapePath(x: Float, y: Float, size: Float, style: Int): Path {
        val path = Path()
        val halfSize = size / 2
        
        when (style) {
            0 -> { // Quadrato
                path.addRect(x - halfSize, y - halfSize, x + halfSize, y + halfSize, Path.Direction.CW)
            }
            1 -> { // Cerchio
                path.addCircle(x, y, halfSize, Path.Direction.CW)
            }
            2 -> { // Esagono
                val radius = halfSize
                for (i in 0..5) {
                    val angle = Math.toRadians((60 * i - 30).toDouble())
                    val px = x + radius * Math.cos(angle).toFloat()
                    val py = y + radius * Math.sin(angle).toFloat()
                    
                    if (i == 0) path.moveTo(px, py)
                    else path.lineTo(px, py)
                }
                path.close()
            }
            3 -> { // Rombo
                path.moveTo(x, y - halfSize)
                path.lineTo(x + halfSize, y)
                path.lineTo(x, y + halfSize)
                path.lineTo(x - halfSize, y)
                path.close()
            }
            4 -> { // Crocetta - usa un cerchio come fallback per il clipping
                path.addCircle(x, y, halfSize, Path.Direction.CW)
            }
        }
        
        return path
    }
    
    private fun applyInnerEffect(
        canvas: Canvas,
        bitmap: Bitmap,
        x: Float,
        y: Float,
        size: Float,
        effectType: Int,
        pixelSize: Int,
        edgeThreshold: Float
    ) {
        val halfSize = (size / 2).toInt()
        val left = (x - halfSize).toInt().coerceIn(0, bitmap.width - 1)
        val top = (y - halfSize).toInt().coerceIn(0, bitmap.height - 1)
        val right = (x + halfSize).toInt().coerceIn(0, bitmap.width)
        val bottom = (y + halfSize).toInt().coerceIn(0, bitmap.height)
        
        if (left >= right || top >= bottom) return
        
        try {
            // Estrai la regione del bitmap
            val width = right - left
            val height = bottom - top
            
            if (width <= 0 || height <= 0) return
            
            val regionBitmap = Bitmap.createBitmap(bitmap, left, top, width, height)
            
            // Applica l'effetto scelto
            val processedBitmap = when (effectType) {
                1 -> applyPixelEffect(regionBitmap, pixelSize)
                2 -> applySobelEffect(regionBitmap, edgeThreshold)
                3 -> applyBWEffect(regionBitmap)
                4 -> applyBlurEffect(regionBitmap)
                else -> regionBitmap
            }
            
            // Disegna il bitmap processato (già nella posizione corretta per la rotazione)
            val destRect = RectF(
                (x - width / 2f),
                (y - height / 2f),
                (x + width / 2f),
                (y + height / 2f)
            )
            canvas.drawBitmap(processedBitmap, null, destRect, null)
            
            if (processedBitmap != regionBitmap) {
                processedBitmap.recycle()
            }
            regionBitmap.recycle()
        } catch (e: Exception) {
            android.util.Log.e("UrbanBoxesFilter", "Error applying inner effect: ${e.message}")
        }
    }
    
    private fun applyPixelEffect(bitmap: Bitmap, pixelSize: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        
        for (y in 0 until height step pixelSize) {
            for (x in 0 until width step pixelSize) {
                // Campiona il colore dal centro del blocco pixel
                val sampleX = (x + pixelSize / 2).coerceIn(0, width - 1)
                val sampleY = (y + pixelSize / 2).coerceIn(0, height - 1)
                val color = bitmap.getPixel(sampleX, sampleY)
                
                // Disegna il blocco pixelato
                for (dy in 0 until pixelSize) {
                    for (dx in 0 until pixelSize) {
                        val px = x + dx
                        val py = y + dy
                        if (px < width && py < height) {
                            result.setPixel(px, py, color)
                        }
                    }
                }
            }
        }
        
        return result
    }
    
    private fun applySobelEffect(bitmap: Bitmap, threshold: Float): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        
        // Kernel Sobel per rilevare i bordi
        val sobelX = arrayOf(
            intArrayOf(-1, 0, 1),
            intArrayOf(-2, 0, 2),
            intArrayOf(-1, 0, 1)
        )
        val sobelY = arrayOf(
            intArrayOf(-1, -2, -1),
            intArrayOf(0, 0, 0),
            intArrayOf(1, 2, 1)
        )
        
        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                var gx = 0
                var gy = 0
                
                // Applica i kernel Sobel
                for (ky in -1..1) {
                    for (kx in -1..1) {
                        val pixel = bitmap.getPixel(x + kx, y + ky)
                        val gray = (Color.red(pixel) + Color.green(pixel) + Color.blue(pixel)) / 3
                        
                        gx += gray * sobelX[ky + 1][kx + 1]
                        gy += gray * sobelY[ky + 1][kx + 1]
                    }
                }
                
                // Calcola la magnitudine del gradiente
                val magnitude = Math.sqrt((gx * gx + gy * gy).toDouble()).toInt()
                val edgeValue = if (magnitude > threshold) 255 else 0
                
                result.setPixel(x, y, Color.rgb(edgeValue, edgeValue, edgeValue))
            }
        }
        
        return result
    }
    
    private fun applyBWEffect(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        
        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = bitmap.getPixel(x, y)
                val gray = (Color.red(pixel) * 0.299 + Color.green(pixel) * 0.587 + Color.blue(pixel) * 0.114).toInt()
                result.setPixel(x, y, Color.rgb(gray, gray, gray))
            }
        }
        
        return result
    }
    
    private fun applyBlurEffect(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val radius = 3
        
        for (y in 0 until height) {
            for (x in 0 until width) {
                var r = 0
                var g = 0
                var b = 0
                var count = 0
                
                for (dy in -radius..radius) {
                    for (dx in -radius..radius) {
                        val px = (x + dx).coerceIn(0, width - 1)
                        val py = (y + dy).coerceIn(0, height - 1)
                        val pixel = bitmap.getPixel(px, py)
                        
                        r += Color.red(pixel)
                        g += Color.green(pixel)
                        b += Color.blue(pixel)
                        count++
                    }
                }
                
                result.setPixel(x, y, Color.rgb(r / count, g / count, b / count))
            }
        }
        
        return result
    }
    
    private fun drawSquare(canvas: Canvas, x: Float, y: Float, size: Float, p: Paint) {
        val rect = RectF(x - size / 2, y - size / 2, x + size / 2, y + size / 2)
        canvas.drawRect(rect, p)
    }
    
    private fun drawHexagon(canvas: Canvas, x: Float, y: Float, size: Float, p: Paint) {
        val path = Path()
        val radius = size / 2
        
        for (i in 0..5) {
            val angle = Math.toRadians((60 * i - 30).toDouble())
            val px = x + radius * Math.cos(angle).toFloat()
            val py = y + radius * Math.sin(angle).toFloat()
            
            if (i == 0) path.moveTo(px, py)
            else path.lineTo(px, py)
        }
        path.close()
        canvas.drawPath(path, p)
    }
    
    private fun drawDiamond(canvas: Canvas, x: Float, y: Float, size: Float, p: Paint) {
        val path = Path()
        val halfSize = size / 2
        
        path.moveTo(x, y - halfSize)
        path.lineTo(x + halfSize, y)
        path.lineTo(x, y + halfSize)
        path.lineTo(x - halfSize, y)
        path.close()
        
        canvas.drawPath(path, p)
    }
    
    private fun drawCross(canvas: Canvas, x: Float, y: Float, size: Float, p: Paint) {
        val halfSize = size / 2
        val thickness = size / 6
        
        // Braccio verticale
        canvas.drawRect(
            x - thickness, y - halfSize,
            x + thickness, y + halfSize,
            p
        )
        
        // Braccio orizzontale
        canvas.drawRect(
            x - halfSize, y - thickness,
            x + halfSize, y + thickness,
            p
        )
    }
    
    private fun drawCorners(canvas: Canvas, x: Float, y: Float, size: Float, style: Int) {
        val halfSize = size / 2
        val cornerLength = size / 4
        
        // Angolo top-left
        canvas.drawLine(x - halfSize, y - halfSize, x - halfSize + cornerLength, y - halfSize, paint)
        canvas.drawLine(x - halfSize, y - halfSize, x - halfSize, y - halfSize + cornerLength, paint)
        
        // Angolo top-right
        canvas.drawLine(x + halfSize, y - halfSize, x + halfSize - cornerLength, y - halfSize, paint)
        canvas.drawLine(x + halfSize, y - halfSize, x + halfSize, y - halfSize + cornerLength, paint)
        
        // Angolo bottom-left
        canvas.drawLine(x - halfSize, y + halfSize, x - halfSize + cornerLength, y + halfSize, paint)
        canvas.drawLine(x - halfSize, y + halfSize, x - halfSize, y + halfSize - cornerLength, paint)
        
        // Angolo bottom-right
        canvas.drawLine(x + halfSize, y + halfSize, x + halfSize - cornerLength, y + halfSize, paint)
        canvas.drawLine(x + halfSize, y + halfSize, x + halfSize, y + halfSize - cornerLength, paint)
    }
    
    override fun clone(): AdaptiveFilter {
        return UrbanBoxesFilter().also { newFilter ->
            this.parameters.forEach { (key, param) ->
                newFilter.parameters[key] = param
            }
        }
    }
}
