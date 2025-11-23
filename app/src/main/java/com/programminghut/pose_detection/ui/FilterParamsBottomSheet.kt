package com.programminghut.pose_detection.ui

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.programminghut.pose_detection.R
import com.programminghut.pose_detection.filters.AdaptiveFilter
import com.programminghut.pose_detection.filters.FilterParameter

/**
 * BottomSheet per modificare i parametri di un filtro
 */
class FilterParamsBottomSheet(
    context: Context,
    private val filter: AdaptiveFilter,
    private val onParametersChanged: () -> Unit
) {
    
    private val bottomSheet: BottomSheetDialog = BottomSheetDialog(context)
    private val view: View = LayoutInflater.from(context)
        .inflate(R.layout.bottom_sheet_filter_params, null)
    
    private val filterIconLarge: ImageView = view.findViewById(R.id.filterIconLarge)
    private val filterName: TextView = view.findViewById(R.id.filterName)
    private val filterDescription: TextView = view.findViewById(R.id.filterDescription)
    private val parametersContainer: LinearLayout = view.findViewById(R.id.parametersContainer)
    private val resetButton: MaterialButton = view.findViewById(R.id.resetButton)
    private val closeButton: MaterialButton = view.findViewById(R.id.closeButton)
    
    init {
        setupBottomSheet()
        populateFilterInfo()
        buildParametersUI()
        setupButtons()
    }
    
    private fun setupBottomSheet() {
        bottomSheet.setContentView(view)
        bottomSheet.behavior.peekHeight = 600
    }
    
    private fun populateFilterInfo() {
        filterIconLarge.setImageResource(filter.iconResId)
        filterName.text = filter.name
        filterDescription.text = filter.description
    }
    
    private fun buildParametersUI() {
        parametersContainer.removeAllViews()
        
        filter.getParameters().forEach { param ->
            val paramView = when (param) {
                is FilterParameter.Slider -> createSliderView(param)
                is FilterParameter.Toggle -> createToggleView(param)
                is FilterParameter.ColorRGB -> createColorView(param)
                is FilterParameter.Choice -> createChoiceView(param)
                else -> null
            }
            
            paramView?.let { parametersContainer.addView(it) }
        }
    }
    
    private fun createSliderView(param: FilterParameter.Slider): View {
        val context = parametersContainer.context
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 16, 0, 16)
        }
        
        // Label e valore corrente
        val headerLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        
        val label = TextView(context).apply {
            text = param.displayName
            textSize = 16f
            setTextColor(Color.BLACK)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        
        val valueText = TextView(context).apply {
            text = String.format("%.1f", param.value)
            textSize = 16f
            setTextColor(Color.parseColor("#39FF14"))
            gravity = android.view.Gravity.END
        }
        
        headerLayout.addView(label)
        headerLayout.addView(valueText)
        
        // Descrizione
        val description = TextView(context).apply {
            text = param.description
            textSize = 12f
            setTextColor(Color.GRAY)
            setPadding(0, 4, 0, 8)
        }
        
        // SeekBar
        val seekBar = SeekBar(context).apply {
            val maxProgress = param.getMaxProgress()
            max = if (maxProgress > 0) maxProgress else 100
            progress = param.getProgress().coerceIn(0, max)
            setPadding(0, 8, 0, 0)
            
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                private var updateRunnable: Runnable? = null
                
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser) {
                        try {
                            param.setProgress(progress)
                            valueText.text = String.format("%.1f", param.value)
                            
                            // Cancella il callback precedente
                            updateRunnable?.let { valueText.handler?.removeCallbacks(it) }
                            
                            // Crea nuovo callback con debouncing più aggressivo
                            updateRunnable = Runnable {
                                try {
                                    onParametersChanged()
                                } catch (e: Exception) {
                                    android.util.Log.e("FilterParams", "Error in callback: ${e.message}", e)
                                }
                            }
                            
                            // Posta con delay di 150ms invece di 50ms per ridurre le chiamate
                            valueText.handler?.postDelayed(updateRunnable!!, 150)
                        } catch (e: Exception) {
                            android.util.Log.e("FilterParams", "Error updating slider: ${e.message}", e)
                        }
                    }
                }
                
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    // Forza un aggiornamento finale quando l'utente rilascia
                    try {
                        onParametersChanged()
                    } catch (e: Exception) {
                        android.util.Log.e("FilterParams", "Error in final callback: ${e.message}", e)
                    }
                }
            })
        }
        
        layout.addView(headerLayout)
        layout.addView(description)
        layout.addView(seekBar)
        
        return layout
    }
    
    private fun createToggleView(param: FilterParameter.Toggle): View {
        val context = parametersContainer.context
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 16, 0, 16)
        }
        
        // Switch e label sulla stessa riga
        val switchLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
        }
        
        val label = TextView(context).apply {
            text = param.displayName
            textSize = 16f
            setTextColor(Color.BLACK)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        
        val switch = androidx.appcompat.widget.SwitchCompat(context).apply {
            isChecked = param.enabled
            setOnCheckedChangeListener { _, isChecked ->
                try {
                    param.enabled = isChecked
                    param.currentValue = isChecked
                    // Posta sul thread UI con un piccolo delay per evitare conflitti
                    handler?.postDelayed({
                        try {
                            onParametersChanged()
                        } catch (e: Exception) {
                            android.util.Log.e("FilterParams", "Error in toggle callback: ${e.message}", e)
                        }
                    }, 100)
                } catch (e: Exception) {
                    android.util.Log.e("FilterParams", "Error updating toggle: ${e.message}", e)
                }
            }
        }
        
        switchLayout.addView(label)
        switchLayout.addView(switch)
        
        // Descrizione
        val description = TextView(context).apply {
            text = param.description
            textSize = 12f
            setTextColor(Color.GRAY)
            setPadding(0, 4, 0, 0)
        }
        
        layout.addView(switchLayout)
        layout.addView(description)
        
        return layout
    }
    
    private fun createColorView(param: FilterParameter.ColorRGB): View {
        val context = parametersContainer.context
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 16, 0, 16)
        }
        
        // Label
        val label = TextView(context).apply {
            text = param.displayName
            textSize = 16f
            setTextColor(Color.BLACK)
            setPadding(0, 0, 0, 8)
        }
        
        // Descrizione
        val description = TextView(context).apply {
            text = param.description
            textSize = 12f
            setTextColor(Color.GRAY)
            setPadding(0, 0, 0, 12)
        }
        
        // Preview colore
        val colorPreview = View(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                60
            )
            setBackgroundColor(param.getColor())
        }
        
        // RGB Sliders
        val rgbLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 12, 0, 0)
        }
        
        // R slider
        val rSlider = createColorSlider(context, "R", param.red, 255) { value ->
            try {
                param.red = value
                param.currentValue = Triple(param.red, param.green, param.blue)
                colorPreview.setBackgroundColor(param.getColor())
                colorPreview.handler?.postDelayed({
                    try {
                        onParametersChanged()
                    } catch (e: Exception) {
                        android.util.Log.e("FilterParams", "Error in color callback: ${e.message}", e)
                    }
                }, 50)
            } catch (e: Exception) {
                android.util.Log.e("FilterParams", "Error updating red: ${e.message}", e)
            }
        }
        
        // G slider
        val gSlider = createColorSlider(context, "G", param.green, 255) { value ->
            try {
                param.green = value
                param.currentValue = Triple(param.red, param.green, param.blue)
                colorPreview.setBackgroundColor(param.getColor())
                colorPreview.handler?.postDelayed({
                    try {
                        onParametersChanged()
                    } catch (e: Exception) {
                        android.util.Log.e("FilterParams", "Error in color callback: ${e.message}", e)
                    }
                }, 50)
            } catch (e: Exception) {
                android.util.Log.e("FilterParams", "Error updating green: ${e.message}", e)
            }
        }
        
        // B slider
        val bSlider = createColorSlider(context, "B", param.blue, 255) { value ->
            try {
                param.blue = value
                param.currentValue = Triple(param.red, param.green, param.blue)
                colorPreview.setBackgroundColor(param.getColor())
                colorPreview.handler?.postDelayed({
                    try {
                        onParametersChanged()
                    } catch (e: Exception) {
                        android.util.Log.e("FilterParams", "Error in color callback: ${e.message}", e)
                    }
                }, 50)
            } catch (e: Exception) {
                android.util.Log.e("FilterParams", "Error updating blue: ${e.message}", e)
            }
        }
        
        rgbLayout.addView(rSlider)
        rgbLayout.addView(gSlider)
        rgbLayout.addView(bSlider)
        
        layout.addView(label)
        layout.addView(description)
        layout.addView(colorPreview)
        layout.addView(rgbLayout)
        
        return layout
    }
    
    private fun createColorSlider(
        context: Context,
        name: String,
        initialValue: Int,
        max: Int,
        onValueChange: (Int) -> Unit
    ): LinearLayout {
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 4, 0, 4)
        }
        
        val label = TextView(context).apply {
            text = "$name:"
            textSize = 14f
            setTextColor(Color.BLACK)
            layoutParams = LinearLayout.LayoutParams(40, LinearLayout.LayoutParams.WRAP_CONTENT)
        }
        
        val valueText = TextView(context).apply {
            text = initialValue.toString()
            textSize = 14f
            setTextColor(Color.BLACK)
            gravity = android.view.Gravity.END
            layoutParams = LinearLayout.LayoutParams(50, LinearLayout.LayoutParams.WRAP_CONTENT)
        }
        
        val seekBar = SeekBar(context).apply {
            this.max = max
            progress = initialValue.coerceIn(0, max)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                private var updateRunnable: Runnable? = null
                
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser) {
                        try {
                            // Aggiorna il valore visualizzato immediatamente
                            valueText.text = progress.toString()
                            
                            // Cancella il callback precedente
                            updateRunnable?.let { valueText.handler?.removeCallbacks(it) }
                            
                            // Crea nuovo callback con debouncing
                            updateRunnable = Runnable {
                                try {
                                    onValueChange(progress)
                                } catch (e: Exception) {
                                    android.util.Log.e("FilterParams", "Error in color callback: ${e.message}", e)
                                }
                            }
                            
                            // Delay di 150ms per ridurre le chiamate durante lo slide
                            valueText.handler?.postDelayed(updateRunnable!!, 150)
                        } catch (e: Exception) {
                            android.util.Log.e("FilterParams", "Error in color slider: ${e.message}", e)
                        }
                    }
                }
                
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    // Aggiornamento finale quando rilascia
                    try {
                        onValueChange(seekBar?.progress ?: initialValue)
                    } catch (e: Exception) {
                        android.util.Log.e("FilterParams", "Error in final color update: ${e.message}", e)
                    }
                }
            })
        }
        
        layout.addView(label)
        layout.addView(seekBar)
        layout.addView(valueText)
        
        return layout
    }
    
    private fun createChoiceView(param: FilterParameter.Choice): View {
        val context = parametersContainer.context
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 16, 0, 16)
        }
        
        // Label
        val label = TextView(context).apply {
            text = param.displayName
            textSize = 16f
            setTextColor(Color.BLACK)
            setPadding(0, 0, 0, 8)
        }
        
        // Descrizione
        val description = TextView(context).apply {
            text = param.description
            textSize = 12f
            setTextColor(Color.GRAY)
            setPadding(0, 0, 0, 8)
        }
        
        // Container per lo spinner con bordo visibile
        val spinnerContainer = FrameLayout(context).apply {
            setPadding(8, 8, 8, 8)
            setBackgroundColor(Color.parseColor("#F5F5F5"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 8
            }
        }
        
        // Spinner
        val spinner = Spinner(context).apply {
            val arrayAdapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, param.options).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }
            adapter = arrayAdapter
            
            // Imposta la selezione iniziale
            val initialIndex = param.selectedIndex.coerceIn(0, param.options.size - 1)
            setSelection(initialIndex)
            
            // Aggiungi padding per renderlo più cliccabile
            setPadding(16, 16, 16, 16)
            minimumHeight = 100
            
            android.util.Log.d("FilterParams", "Creating Choice: ${param.displayName}, options: ${param.options}, selected: $initialIndex")
            
            var isInitializing = true
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    android.util.Log.d("FilterParams", "Spinner selected: position=$position, isInit=$isInitializing, param=${param.displayName}")
                    
                    if (!isInitializing && position >= 0 && position < param.options.size) {
                        try {
                            param.selectedIndex = position
                            param.currentValue = position
                            
                            android.util.Log.d("FilterParams", "Updated ${param.displayName} to index $position (${param.options[position]})")
                            
                            // Delay per evitare conflitti
                            handler?.postDelayed({
                                try {
                                    onParametersChanged()
                                } catch (e: Exception) {
                                    android.util.Log.e("FilterParams", "Error in choice callback: ${e.message}", e)
                                }
                            }, 100)
                        } catch (e: Exception) {
                            android.util.Log.e("FilterParams", "Error updating choice: ${e.message}", e)
                        }
                    }
                    isInitializing = false
                }
                
                override fun onNothingSelected(parent: AdapterView<*>?) {
                    android.util.Log.d("FilterParams", "Spinner nothing selected")
                }
            }
        }
        
        spinnerContainer.addView(spinner)
        
        layout.addView(label)
        layout.addView(description)
        layout.addView(spinnerContainer)
        
        return layout
    }
    
    private fun setupButtons() {
        resetButton.setOnClickListener {
            filter.resetParameters()
            buildParametersUI()
            onParametersChanged()
        }
        
        closeButton.setOnClickListener {
            bottomSheet.dismiss()
        }
    }
    
    fun show() {
        bottomSheet.show()
    }
    
    fun dismiss() {
        bottomSheet.dismiss()
    }
}
