package com.smsforwarder

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.checkbox.MaterialCheckBox

class AppListAdapter(
    private var apps: List<AppInfo>,
    private val onAppToggled: (AppInfo, Boolean) -> Unit
) : RecyclerView.Adapter<AppListAdapter.AppViewHolder>() {

    private var filteredApps: List<AppInfo> = apps

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
            app.isSelected = !app.isSelected
            holder.checkbox.isChecked = app.isSelected
            onAppToggled(app, app.isSelected)
        }

        // Klikk på checkbox
        holder.checkbox.setOnClickListener {
            app.isSelected = holder.checkbox.isChecked
            onAppToggled(app, app.isSelected)
        }
    }

    override fun getItemCount() = filteredApps.size

    fun filter(query: String) {
        filteredApps = if (query.isEmpty()) {
            apps
        } else {
            apps.filter {
                it.appName.contains(query, ignoreCase = true) ||
                it.packageName.contains(query, ignoreCase = true)
            }
        }
        notifyDataSetChanged()
    }

    fun updateApps(newApps: List<AppInfo>) {
        apps = newApps
        filteredApps = newApps
        notifyDataSetChanged()
    }

    fun showOnlySelected(onlySelected: Boolean) {
        filteredApps = if (onlySelected) {
            apps.filter { it.isSelected }
        } else {
            apps
        }
        notifyDataSetChanged()
    }
}