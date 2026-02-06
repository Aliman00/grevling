package com.smsforwarder

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.checkbox.MaterialCheckBox

class AppListAdapter(
    private var apps: List<AppInfo>,
    private val onAppToggled: (AppInfo, Boolean) -> Unit
) : RecyclerView.Adapter<AppListAdapter.AppViewHolder>() {

    private var filteredApps: List<AppInfo> = apps
    private var currentQuery: String = ""
    private var showOnlySelected: Boolean = false

    class AppViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.app_icon)
        val name: TextView = view.findViewById(R.id.app_name)
        val packageName: TextView = view.findViewById(R.id.app_package)
        val checkbox: MaterialCheckBox = view.findViewById(R.id.app_checkbox)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_app, parent, false)
        return AppViewHolder(view)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        val app = filteredApps[position]
        
        holder.icon.setImageDrawable(app.icon)
        holder.name.text = app.appName
        holder.packageName.text = app.packageName
        holder.checkbox.isChecked = app.isSelected

        // Klikk på hele raden
        holder.itemView.setOnClickListener {
            val toggled = !app.isSelected
            holder.checkbox.isChecked = toggled
            onAppToggled(app, toggled)
        }

        // Klikk på checkbox
        holder.checkbox.setOnClickListener {
            onAppToggled(app, holder.checkbox.isChecked)
        }
    }

    override fun getItemCount() = filteredApps.size

    fun filter(query: String) {
        currentQuery = query
        applyFilter()
    }

    fun updateApps(newApps: List<AppInfo>) {
        apps = newApps
        applyFilter()
    }

    fun showOnlySelected(onlySelected: Boolean) {
        showOnlySelected = onlySelected
        applyFilter()
    }

    /**
     * Beregner filtrert liste og bruker DiffUtil for effektiv oppdatering.
     */
    private fun applyFilter() {
        var result = apps

        // Filtrer på søk
        if (currentQuery.isNotEmpty()) {
            result = result.filter {
                it.appName.contains(currentQuery, ignoreCase = true) ||
                it.packageName.contains(currentQuery, ignoreCase = true)
            }
        }

        // Filtrer på kun valgte
        if (showOnlySelected) {
            result = result.filter { it.isSelected }
        }

        val oldList = filteredApps
        val newList = result

        val diffResult = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize() = oldList.size
            override fun getNewListSize() = newList.size

            override fun areItemsTheSame(oldPos: Int, newPos: Int): Boolean {
                return oldList[oldPos].packageName == newList[newPos].packageName
            }

            override fun areContentsTheSame(oldPos: Int, newPos: Int): Boolean {
                val old = oldList[oldPos]
                val new = newList[newPos]
                return old.packageName == new.packageName &&
                       old.appName == new.appName &&
                       old.isSelected == new.isSelected
            }
        })

        filteredApps = newList
        diffResult.dispatchUpdatesTo(this)
    }
}