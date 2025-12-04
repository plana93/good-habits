package com.programminghut.pose_detection.effects

import android.graphics.*
import com.programminghut.pose_detection.R
import kotlin.math.*
import com.programminghut.pose_detection.effects.RandomProvider

/**
 * ConnectedLineCenterSobel
 * - Applica un filtro Sobel nella regione quadrata centrale (size parametrica)
 * - Seleziona punti casuali all'interno della regione e li collega con linee sottili
 * - Le connessioni principali vengono ordinate attorno al centro per evitare intersezioni
 * - Opzione per disegnare sotto-linee più sottili che si intrecciano casualmente
 */
class ConnectedLineCenterSobel : AdaptiveFilter(
    id = "connected_center_sobel",
    name = "Connected Center Sobel",
    iconResId = R.drawable.ic_urban_boxes,
    description = "Applica Sobel nella regione centrale e connette punti casuali con linee non intersecanti"
) {

    private val paint = Paint().apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        isAntiAlias = true
    }

    init {
        parameters["centerSize"] = FilterParameter.Slider(
            paramKey = "centerSize",
            paramDisplayName = "Dimensione Quadrato Centrale",
            paramDescription = "Lato del quadrato centrale dove viene applicato il Sobel (px)",
            value = 400f,
            min = 50f,
            max = 1200f,
            step = 10f
        )

        parameters["pointCount"] = FilterParameter.Slider(
            paramKey = "pointCount",
            paramDisplayName = "Numero Punti",
            paramDescription = "Quanti punti casuali generare nella regione centrale",
            value = 8f,
            min = 3f,
            max = 40f,
            step = 1f
        )

        // Riusa impostazioni simili a SkeletonFilter
        parameters["lineWidth"] = FilterParameter.Slider(
            paramKey = "lineWidth",
            paramDisplayName = "Spessore Linea",
            paramDescription = "Spessore delle linee principali",
            value = 8f,
            min = 1f,
            max = 30f,
            step = 1f
        )

        parameters["color"] = FilterParameter.ColorRGB(
            paramKey = "color",
            paramDisplayName = "Colore Linee",
            paramDescription = "Colore delle linee principali",
            red = 57,
            green = 255,
            blue = 20
        )

        parameters["glowEffect"] = FilterParameter.Toggle(
            paramKey = "glowEffect",
            paramDisplayName = "Effetto Glow",
            paramDescription = "Aggiunge alone alle linee principali",
            enabled = false
        )

        // Sotto-linee (più sottili, intrecciate)
        parameters["subLinesEnabled"] = FilterParameter.Toggle(
            paramKey = "subLinesEnabled",
            paramDisplayName = "Sotto-linee",
            paramDescription = "Disegna sotto-linee sottili intrecciate",
            enabled = true
        )

        parameters["subLineCount"] = FilterParameter.Slider(
            paramKey = "subLineCount",
            paramDisplayName = "Numero Sotto-linee",
            paramDescription = "Quante sotto-linee disegnare",
            value = 12f,
            min = 0f,
            max = 200f,
            step = 1f
        )

        parameters["subLineWidth"] = FilterParameter.Slider(
            paramKey = "subLineWidth",
            paramDisplayName = "Spessore Sotto-linee",
            paramDescription = "Spessore delle sotto-linee",
            value = 2f,
            min = 0.5f,
            max = 8f,
            step = 0.5f
        )

        parameters["subLineAlpha"] = FilterParameter.Slider(
            paramKey = "subLineAlpha",
            paramDisplayName = "Alpha Sotto-linee",
            paramDescription = "Opacità delle sotto-linee (0-255)",
            value = 120f,
            min = 0f,
            max = 255f,
            step = 5f
        )
        
        // Parametri Sobel modificabili
        parameters["sobelThreshold"] = FilterParameter.Slider(
            paramKey = "sobelThreshold",
            paramDisplayName = "Soglia Sobel",
            paramDescription = "Soglia per la rilevazione dei bordi (maggiore = meno bordi)",
            value = 100f,
            min = 0f,
            max = 1024f,
            step = 1f
        )

        parameters["sobelScale"] = FilterParameter.Slider(
            paramKey = "sobelScale",
            paramDisplayName = "Scala Sobel",
            paramDescription = "Moltiplicatore per l'intensità del gradiente (1 = normale)",
            value = 1f,
            min = 0.1f,
            max = 5f,
            step = 0.1f
        )

        parameters["sobelInvert"] = FilterParameter.Toggle(
            paramKey = "sobelInvert",
            paramDisplayName = "Inverti Sobel",
            paramDescription = "Inverti i colori del risultato Sobel (bianco -> nero)",
            enabled = false
        )
    }

    override fun apply(canvas: Canvas, bitmap: Bitmap, poseKeypoints: FloatArray?) {
        val centerX = bitmap.width / 2f
        val centerY = bitmap.height / 2f

        val centerSize = (parameters["centerSize"] as FilterParameter.Slider).value
        val half = centerSize / 2f

        val left = (centerX - half).coerceAtLeast(0f).toInt()
        val top = (centerY - half).coerceAtLeast(0f).toInt()
        val right = (centerX + half).coerceAtMost(bitmap.width.toFloat()).toInt()
        val bottom = (centerY + half).coerceAtMost(bitmap.height.toFloat()).toInt()

        if (left >= right || top >= bottom) return

        // Estrai regione e applica Sobel
        val regionWidth = right - left
        val regionHeight = bottom - top
        val regionBitmap = try {
            Bitmap.createBitmap(bitmap, left, top, regionWidth, regionHeight)
        } catch (e: Exception) {
            return
        }

    val sobelThreshold = (parameters["sobelThreshold"] as FilterParameter.Slider).value
    val sobelScale = (parameters["sobelScale"] as FilterParameter.Slider).value
    val sobelInvert = (parameters["sobelInvert"] as FilterParameter.Toggle).enabled
    val sobelBitmap = applySobelEffect(regionBitmap, sobelThreshold, sobelScale, sobelInvert)

        // Disegna il risultato Sobel nella regione centrale
        val destRect = RectF(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat())
        canvas.drawBitmap(sobelBitmap, null, destRect, null)
        if (sobelBitmap != regionBitmap) sobelBitmap.recycle()
        regionBitmap.recycle()

        // Genera punti casuali all'interno del quadrato centrale
        val pointCount = (parameters["pointCount"] as FilterParameter.Slider).value.toInt().coerceAtLeast(3)
        val pts = mutableListOf<PointF>()
        // Use RandomProvider so we can seed randomness for deterministic frames
        val randWrapper = object {
            fun nextInt(bound: Int) = RandomProvider.nextInt(bound)
            fun nextFloat() = RandomProvider.nextFloat()
        }
        val margin = 8
        for (i in 0 until pointCount) {
            val px = left + margin + randWrapper.nextInt(max(1, regionWidth - margin * 2))
            val py = top + margin + randWrapper.nextInt(max(1, regionHeight - margin * 2))
            pts.add(PointF(px.toFloat(), py.toFloat()))
        }

        if (pts.size < 3) return

        // Ordina i punti attorno al centro per evitare intersezioni (angolo polar)
        pts.sortBy { atan2(it.y - centerY, it.x - centerX) }

        // Disegna linee principali non intersecanti
        val lineWidth = (parameters["lineWidth"] as FilterParameter.Slider).value
        val colorParam = parameters["color"] as FilterParameter.ColorRGB
        val glow = (parameters["glowEffect"] as FilterParameter.Toggle).enabled

        paint.strokeWidth = lineWidth
        paint.color = colorParam.getColor()
        if (glow) paint.setShadowLayer(12f, 0f, 0f, colorParam.getColor()) else paint.clearShadowLayer()

        // Connette i punti in ordine e chiude il poligono
        for (i in pts.indices) {
            val p1 = pts[i]
            val p2 = pts[(i + 1) % pts.size]
            canvas.drawLine(p1.x, p1.y, p2.x, p2.y, paint)
        }

        // Opzionali sotto-linee più sottili e intrecciate
        val subEnabled = (parameters["subLinesEnabled"] as FilterParameter.Toggle).enabled
        if (subEnabled) {
            val subCount = (parameters["subLineCount"] as FilterParameter.Slider).value.toInt()
            val subWidth = (parameters["subLineWidth"] as FilterParameter.Slider).value
            val subAlpha = (parameters["subLineAlpha"] as FilterParameter.Slider).value.toInt().coerceIn(0, 255)

            val subPaint = Paint(paint).apply {
                strokeWidth = subWidth
                color = Color.argb(subAlpha, Color.red(colorParam.getColor()), Color.green(colorParam.getColor()), Color.blue(colorParam.getColor()))
                strokeCap = Paint.Cap.ROUND
                isAntiAlias = true
                style = Paint.Style.STROKE
                clearShadowLayer()
            }

            for (i in 0 until subCount) {
                val a = pts[randWrapper.nextInt(pts.size)]
                val b = pts[randWrapper.nextInt(pts.size)]
                // Aggiungi piccoli offset casuali per intrecciare
                val ox = (randWrapper.nextFloat() - 0.5f) * 20f
                val oy = (randWrapper.nextFloat() - 0.5f) * 20f
                canvas.drawLine(a.x + ox, a.y + oy, b.x - ox, b.y - oy, subPaint)
            }

        }

    }

    // Semplice implementazione Sobel (scala di grigi e soglia)
    private fun applySobelEffect(bitmap: Bitmap, threshold: Float, scale: Float, invert: Boolean): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

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
                for (ky in -1..1) {
                    for (kx in -1..1) {
                        val pixel = bitmap.getPixel(x + kx, y + ky)
                        val gray = (Color.red(pixel) + Color.green(pixel) + Color.blue(pixel)) / 3
                        gx += gray * sobelX[ky + 1][kx + 1]
                        gy += gray * sobelY[ky + 1][kx + 1]
                    }
                }
                var magnitude = sqrt((gx * gx + gy * gy).toDouble()).toInt()
                magnitude = (magnitude * scale).toInt()
                val edgeValue = if (magnitude > threshold) 255 else 0
                val final = if (invert) 255 - edgeValue else edgeValue
                result.setPixel(x, y, Color.rgb(final, final, final))
            }
        }

        return result
    }

    override fun clone(): AdaptiveFilter {
        return ConnectedLineCenterSobel().also { newFilter ->
            this.parameters.forEach { (k, v) -> newFilter.parameters[k] = v }
        }
    }
}
