package com.programminghut.pose_detection.filters

import android.graphics.*
import com.programminghut.pose_detection.R

/**
 * Filtro che disegna lo scheletro animato con linee tra i keypoint
 */
class SkeletonFilter : AdaptiveFilter(
    id = "skeleton",
    name = "Animated Skeleton",
    iconResId = R.drawable.ic_animated_lines,
    description = "Disegna lo scheletro della posa con linee animate che seguono i movimenti del corpo. Le linee utilizzano curve di Bezier per un effetto fluido e naturale."
) {
    
    private val paint = Paint().apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        isAntiAlias = true
    }
    
    init {
        // Parametri configurabili
        parameters["lineWidth"] = FilterParameter.Slider(
            paramKey = "lineWidth",
            paramDisplayName = "Spessore Linea",
            paramDescription = "Regola lo spessore delle linee dello scheletro",
            value = 8f,
            min = 2f,
            max = 20f,
            step = 1f
        )
        
        parameters["color"] = FilterParameter.ColorRGB(
            paramKey = "color",
            paramDisplayName = "Colore",
            paramDescription = "Scegli il colore delle linee dello scheletro",
            red = 57,
            green = 255,
            blue = 20
        )
        
        parameters["glowEffect"] = FilterParameter.Toggle(
            paramKey = "glowEffect",
            paramDisplayName = "Effetto Glow",
            paramDescription = "Aggiunge un alone luminoso intorno alle linee",
            enabled = true
        )
        
        parameters["glowIntensity"] = FilterParameter.Slider(
            paramKey = "glowIntensity",
            paramDisplayName = "Intensità Glow",
            paramDescription = "Regola l'intensità dell'effetto luminoso",
            value = 15f,
            min = 5f,
            max = 30f,
            step = 1f
        )
        
        parameters["skeletonMode"] = FilterParameter.Choice(
            paramKey = "skeletonMode",
            paramDisplayName = "Modalità Scheletro",
            paramDescription = "Scegli come visualizzare lo scheletro",
            selectedIndex = 0,
            options = listOf("Normale", "Dalla Testa")
        )
        
        parameters["multipleHeadLines"] = FilterParameter.Toggle(
            paramKey = "multipleHeadLines",
            paramDisplayName = "Linee Multiple Testa",
            paramDescription = "Aggiunge linee multiple animate dalla testa verso tutte le parti del corpo",
            enabled = false
        )
        
        parameters["headLinesCount"] = FilterParameter.Slider(
            paramKey = "headLinesCount",
            paramDisplayName = "Numero Linee Testa",
            paramDescription = "Quante linee partono dalla testa",
            value = 5f,
            min = 3f,
            max = 15f,
            step = 1f
        )
    }
    
    override fun apply(canvas: Canvas, bitmap: Bitmap, poseKeypoints: FloatArray?) {
        if (poseKeypoints == null || poseKeypoints.size < 51) return
        
        // Recupera parametri
        val lineWidth = (parameters["lineWidth"] as FilterParameter.Slider).value
        val colorParam = parameters["color"] as FilterParameter.ColorRGB
        val glowEnabled = (parameters["glowEffect"] as FilterParameter.Toggle).enabled
        val glowIntensity = (parameters["glowIntensity"] as FilterParameter.Slider).value
        val skeletonMode = (parameters["skeletonMode"] as FilterParameter.Choice).selectedIndex
        val multipleHeadLines = (parameters["multipleHeadLines"] as FilterParameter.Toggle).enabled
        val headLinesCount = (parameters["headLinesCount"] as FilterParameter.Slider).value.toInt()
        
        // Configura paint
        paint.strokeWidth = lineWidth
        paint.color = colorParam.getColor()
        
        // Aggiungi effetto glow se abilitato
        if (glowEnabled) {
            paint.setShadowLayer(glowIntensity, 0f, 0f, colorParam.getColor())
        } else {
            paint.clearShadowLayer()
        }
        
        // Modalità Dalla Testa (stile urban)
        if (skeletonMode == 1) {
            drawFromHeadMode(canvas, poseKeypoints, multipleHeadLines, headLinesCount)
            return
        }
        
        // Modalità Normale - continua con il codice esistente
        
        // Connessioni dello scheletro (basato su COCO pose format)
        // Definisci le connessioni e se sono lato sinistro o destro
        data class Connection(val idx1: Int, val idx2: Int, val isLeft: Boolean?)
        val connections = listOf(
            // Torso
            Connection(5, 6, null),   // spalle
            Connection(5, 11, true),  // spalla sx - anca sx
            Connection(6, 12, false), // spalla dx - anca dx
            Connection(11, 12, null), // anche

            // Braccio sinistro
            Connection(5, 7, true),   // spalla - gomito sx
            Connection(7, 9, true),   // gomito sx - polso sx

            // Braccio destro
            Connection(6, 8, false),  // spalla - gomito dx
            Connection(8, 10, false), // gomito dx - polso dx

            // Gamba sinistra
            Connection(11, 13, true), // anca sx - ginocchio sx
            Connection(13, 15, true), // ginocchio sx - caviglia sx

            // Gamba destra
            Connection(12, 14, false), // anca dx - ginocchio dx
            Connection(14, 16, false), // ginocchio dx - caviglia dx

            // Testa
            Connection(0, 1, true),   // naso - occhio sx
            Connection(0, 2, false),  // naso - occhio dx
            Connection(1, 3, true),   // occhio sx - orecchio sx
            Connection(2, 4, false),  // occhio dx - orecchio dx
            Connection(3, 5, true),   // orecchio sx - spalla sx
            Connection(4, 6, false)   // orecchio dx - spalla dx
        )

        // Disegna le connessioni
        connections.forEachIndexed { connectionIndex, conn ->
            val x1 = poseKeypoints[conn.idx1 * 3]
            val y1 = poseKeypoints[conn.idx1 * 3 + 1]
            val confidence1 = poseKeypoints[conn.idx1 * 3 + 2]

            val x2 = poseKeypoints[conn.idx2 * 3]
            val y2 = poseKeypoints[conn.idx2 * 3 + 1]
            val confidence2 = poseKeypoints[conn.idx2 * 3 + 2]

            if (confidence1 > 0.3 && confidence2 > 0.3) {
                val curvatureVariation = (connectionIndex % 3) - 1 // -1, 0, 1
                drawUrbanCurveLine(
                    canvas, x1, y1, x2, y2, curvatureVariation.toFloat(), connectionIndex, poseKeypoints, conn.isLeft
                )
            }
        }
    }
    
    private fun drawFromHeadMode(canvas: Canvas, poseKeypoints: FloatArray, multipleLines: Boolean, linesCount: Int) {
        // Centro della testa (naso - keypoint 0)
        val headX = poseKeypoints[0 * 3]
        val headY = poseKeypoints[0 * 3 + 1]
        val headConfidence = poseKeypoints[0 * 3 + 2]

        if (headConfidence < 0.3) return

        // Keypoint target per le linee (esclude testa stessa)
        val targetKeypoints = listOf(5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16)

    // Mappa per capire se il target è a sinistra o destra
    val leftIndices = setOf(5, 7, 9, 11, 13, 15)
    val rightIndices = setOf(6, 8, 10, 12, 14, 16)

        if (multipleLines) {
            val selectedTargets = targetKeypoints.take(linesCount.coerceAtMost(targetKeypoints.size))
            selectedTargets.forEachIndexed { index, targetIdx ->
                val targetX = poseKeypoints[targetIdx * 3]
                val targetY = poseKeypoints[targetIdx * 3 + 1]
                val targetConfidence = poseKeypoints[targetIdx * 3 + 2]

                if (targetConfidence > 0.3) {
                    val curvatureVariation = (index % 3) - 1 // -1, 0, 1
                    // Inverti la logica: sinistra curva a destra, destra curva a sinistra
                    val isLeft = when {
                        leftIndices.contains(targetIdx) -> false
                        rightIndices.contains(targetIdx) -> true
                        else -> null
                    }
                    drawUrbanCurveLine(canvas, headX, headY, targetX, targetY, curvatureVariation.toFloat(), index, poseKeypoints, isLeft)
                }
            }
        } else {
            targetKeypoints.forEachIndexed { index, targetIdx ->
                val targetX = poseKeypoints[targetIdx * 3]
                val targetY = poseKeypoints[targetIdx * 3 + 1]
                val targetConfidence = poseKeypoints[targetIdx * 3 + 2]

                if (targetConfidence > 0.3) {
                    // Inverti la logica: sinistra curva a destra, destra curva a sinistra
                    val isLeft = when {
                        leftIndices.contains(targetIdx) -> false
                        rightIndices.contains(targetIdx) -> true
                        else -> null
                    }
                    drawUrbanCurveLine(canvas, headX, headY, targetX, targetY, 0f, index, poseKeypoints, isLeft)
                }
            }
        }
    }
    
    private fun drawMultipleHeadLines(canvas: Canvas, poseKeypoints: FloatArray, count: Int) {
        // Centro della testa (naso - keypoint 0)
        val headX = poseKeypoints[0 * 3]
        val headY = poseKeypoints[0 * 3 + 1]
        val headConfidence = poseKeypoints[0 * 3 + 2]
        
        if (headConfidence < 0.3) return
        
        // Keypoint target per le linee (esclude testa stessa)
        val targetKeypoints = listOf(5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16)
        
        // Prendi solo i primi N target basato su count
        val selectedTargets = targetKeypoints.take(count.coerceAtMost(targetKeypoints.size))
        
        // Disegna linee curve multiple dalla testa
        selectedTargets.forEachIndexed { index, targetIdx ->
            val targetX = poseKeypoints[targetIdx * 3]
            val targetY = poseKeypoints[targetIdx * 3 + 1]
            val targetConfidence = poseKeypoints[targetIdx * 3 + 2]
            
            if (targetConfidence > 0.3) {
                // Varia la curvatura per ogni linea e passa il numero linea
                val curvatureVariation = (index % 3) - 1 // -1, 0, 1
                drawUrbanCurveLine(canvas, headX, headY, targetX, targetY, curvatureVariation.toFloat(), index)
            }
        }
    }
    
    private fun drawUrbanCurveLine(
        canvas: Canvas,
        startX: Float, startY: Float,
        endX: Float, endY: Float,
        curvatureVariation: Float,
        lineNum: Int,
        poseKeypoints: FloatArray? = null, // serve per calcolare centro corpo
        isLeft: Boolean? = null // nuovo parametro: true = sinistra, false = destra, null = centrale
    ) {
        val path = Path()
        path.moveTo(startX, startY)

        // Calcola centro del corpo (media tra anche e spalle)
        var centerX = 0f
        var centerY = 0f
        if (poseKeypoints != null && poseKeypoints.size >= 51) {
            val xSpallaSx = poseKeypoints[5 * 3]
            val ySpallaSx = poseKeypoints[5 * 3 + 1]
            val xSpallaDx = poseKeypoints[6 * 3]
            val ySpallaDx = poseKeypoints[6 * 3 + 1]
            val xAncaSx = poseKeypoints[11 * 3]
            val yAncaSx = poseKeypoints[11 * 3 + 1]
            val xAncaDx = poseKeypoints[12 * 3]
            val yAncaDx = poseKeypoints[12 * 3 + 1]
            centerX = (xSpallaSx + xSpallaDx + xAncaSx + xAncaDx) / 4f
            centerY = (ySpallaSx + ySpallaDx + yAncaSx + yAncaDx) / 4f
        } else {
            centerX = (startX + endX) / 2f
            centerY = (startY + endY) / 2f
        }

        // Calcola parametri base della linea
        val midX = (startX + endX) / 2
        val midY = (startY + endY) / 2

        val dx = endX - startX
        val dy = endY - startY
        val distance = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()

        // Vettore perpendicolare
        var perpX = -dy / distance
        var perpY = dx / distance

        // Curvatura speculare: sinistra curva a sinistra, destra a destra
        if (isLeft == true) {
            // Lato sinistro: curva sempre verso sinistra (mantieni perpendicolare)
            // nessuna inversione
        } else if (isLeft == false) {
            // Lato destro: curva sempre verso destra (inverte perpendicolare)
            perpX = -perpX
            perpY = -perpY
        } else {
            // Centrale: mantieni logica precedente (inverte se a sinistra del centro)
            if (midX < centerX) {
                perpX = -perpX
                perpY = -perpY
            }
        }

        // *** CURVA VARIABILE BASATA SU CURVATURE E TEMPO ***
        val timeInSeconds = System.currentTimeMillis() / 1000.0
        val lineOffset = lineNum * 0.8
        val curveFactor = distance * (0.35f + curvatureVariation * 0.25f) *
            (1.0f + Math.sin(timeInSeconds * 2.0 + lineOffset).toFloat() * 0.3f)

        val outwardPerpX = perpX * curveFactor
        val outwardPerpY = perpY * curveFactor

        // *** PUNTO DI CONTROLLO PRINCIPALE CON OFFSET CASUALE ***
        val randomOffsetX = (Math.random().toFloat() - 0.5f) * distance * 0.15f
        val randomOffsetY = (Math.random().toFloat() - 0.5f) * distance * 0.15f

        val controlX = midX + outwardPerpX + randomOffsetX
        val controlY = midY + outwardPerpY + randomOffsetY

        // *** PUNTI DI CONTROLLO PER CURVA CUBICA CON VARIAZIONI ESTREME ***
        val offset1Factor = 0.25f + Math.sin(timeInSeconds * 3.0 + lineOffset).toFloat() * 0.2f +
            (Math.random().toFloat() - 0.5f) * 0.15f
        val offset2Factor = 0.75f + Math.sin(timeInSeconds * 3.0 + Math.PI + lineOffset).toFloat() * 0.2f +
            (Math.random().toFloat() - 0.5f) * 0.15f

        val control1X = startX * (1 - offset1Factor) + controlX * offset1Factor
        val control1Y = startY * (1 - offset1Factor) + controlY * offset1Factor

        val control2X = endX * (1 - offset2Factor) + controlX * offset2Factor
        val control2Y = endY * (1 - offset2Factor) + controlY * offset2Factor

        // *** VARIAZIONE OPACITÀ PER LINEE MULTIPLE (effetto profondità) ***
        val alphaPaint = Paint(paint)
        val alphaVariation = 0.3f + (lineNum * 0.15f) + Math.random().toFloat() * 0.3f
        alphaPaint.alpha = (255 * alphaVariation.coerceIn(0.2f, 1.0f)).toInt()

        // *** VARIAZIONE SPESSORE PER LINEE MULTIPLE ***
        val strokeVariation = 0.6f + Math.random().toFloat() * 0.8f
        alphaPaint.strokeWidth = paint.strokeWidth * strokeVariation

        // *** CREA CURVA CUBICA DI BÉZIER CAOTICA ***
        path.cubicTo(control1X, control1Y, control2X, control2Y, endX, endY)

        // Disegna la curva fluida e caotica
        canvas.drawPath(path, alphaPaint)
    }
    
    override fun clone(): AdaptiveFilter {
        return SkeletonFilter().also { newFilter ->
            this.parameters.forEach { (key, param) ->
                newFilter.parameters[key] = param
            }
        }
    }
}
