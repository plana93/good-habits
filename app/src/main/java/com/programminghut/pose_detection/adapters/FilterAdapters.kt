package com.programminghut.pose_detection.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.programminghut.pose_detection.R
import com.programminghut.pose_detection.effects.AdaptiveFilter

/**
 * Adapter per mostrare i filtri disponibili (menu sinistro)
 */
class AvailableFiltersAdapter(
    private val filters: List<AdaptiveFilter>,
    private val onFilterClick: (AdaptiveFilter) -> Unit,
    private val onFilterLongClick: (AdaptiveFilter) -> Unit
) : RecyclerView.Adapter<AvailableFiltersAdapter.FilterViewHolder>() {
    
    class FilterViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val iconView: ImageView = view.findViewById(R.id.filterIcon)
        val activeIndicator: View = view.findViewById(R.id.activeIndicator)
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FilterViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_filter_icon, parent, false)
        return FilterViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: FilterViewHolder, position: Int) {
        val filter = filters[position]
        
        // Imposta icona
        holder.iconView.setImageResource(filter.iconResId)
        
        // Mostra indicatore se filtro è attivo
        holder.activeIndicator.visibility = if (filter.isActive) View.VISIBLE else View.GONE
        
        // Click per attivare/disattivare
        holder.itemView.setOnClickListener {
            onFilterClick(filter)
        }
        
        // Long click per aprire parametri
        holder.itemView.setOnLongClickListener {
            onFilterLongClick(filter)
            true
        }
        
        // Animazione feedback visivo
        holder.itemView.alpha = if (filter.isActive) 0.5f else 1f
    }
    
    override fun getItemCount() = filters.size
    
    fun updateFilter(filter: AdaptiveFilter) {
        val position = filters.indexOf(filter)
        if (position >= 0) {
            notifyItemChanged(position)
        }
    }
}

/**
 * Adapter per mostrare i filtri attivi (menu destro)
 */
class ActiveFiltersAdapter(
    private val onFilterRemove: (AdaptiveFilter) -> Unit,
    private val onFilterLongClick: (AdaptiveFilter) -> Unit
) : RecyclerView.Adapter<ActiveFiltersAdapter.FilterViewHolder>() {
    
    private var filters: MutableList<AdaptiveFilter> = mutableListOf()
    
    class FilterViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val iconView: ImageView = view.findViewById(R.id.filterIcon)
        val orderNumber: View = view.findViewById(R.id.orderNumber)
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FilterViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_active_filter, parent, false)
        return FilterViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: FilterViewHolder, position: Int) {
        if (position < 0 || position >= filters.size) return
        
        val filter = filters[position]
        
        // Imposta icona
        holder.iconView.setImageResource(filter.iconResId)
        
        // Mostra numero ordine (opzionale)
        holder.orderNumber.visibility = View.GONE
        
        // Long click per aprire parametri
        holder.itemView.setOnLongClickListener {
            onFilterLongClick(filter)
            true
        }
        
        // Swipe handling verrà gestito dall'ItemTouchHelper nell'Activity
    }
    
    override fun getItemCount() = filters.size
    
    fun updateFilters(newFilters: List<AdaptiveFilter>) {
        filters.clear()
        filters.addAll(newFilters)
        notifyDataSetChanged()
    }
    
    fun addFilter(filter: AdaptiveFilter) {
        if (!filters.contains(filter)) {
            filters.add(filter)
            notifyItemInserted(filters.size - 1)
        }
    }
    
    fun removeFilter(position: Int) {
        if (position >= 0 && position < filters.size) {
            val filter = filters[position]
            filters.removeAt(position)
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, filters.size)
            onFilterRemove(filter)
        }
    }
    
    fun moveFilter(fromPosition: Int, toPosition: Int) {
        if (fromPosition < 0 || fromPosition >= filters.size || 
            toPosition < 0 || toPosition >= filters.size) return
            
        val filter = filters.removeAt(fromPosition)
        filters.add(toPosition, filter)
        notifyItemMoved(fromPosition, toPosition)
    }
}
