package com.smsforwarder

import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.progressindicator.CircularProgressIndicator
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppSelectionFragment : BaseFragment() {

    private lateinit var prefs: SharedPreferences
    private lateinit var recyclerView: RecyclerView
    private lateinit var searchView: SearchView
    private lateinit var chipAll: Chip
    private lateinit var chipSelected: Chip
    private lateinit var progressIndicator: CircularProgressIndicator
    private lateinit var selectedCountText: TextView
    private lateinit var adapter: AppListAdapter
    
    private var allApps: List<AppInfo> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_app_selection, container, false)
        
        prefs = getEncryptedPreferences()

        // Bind views
        recyclerView = view.findViewById(R.id.apps_recycler_view)
        searchView = view.findViewById(R.id.search_view)
        chipAll = view.findViewById(R.id.chip_all)
        chipSelected = view.findViewById(R.id.chip_selected)
        progressIndicator = view.findViewById(R.id.progress_indicator)
        selectedCountText = view.findViewById(R.id.selected_count_text)

        // Setup RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = AppListAdapter(emptyList()) { app, isSelected ->
            onAppToggled(app, isSelected)
        }
        recyclerView.adapter = adapter

        // Setup search
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?) = false
            override fun onQueryTextChange(newText: String?): Boolean {
                adapter.filter(newText ?: "")
                return true
            }
        })

        // Setup chips
        chipAll.setOnClickListener {
            chipAll.isChecked = true
            chipSelected.isChecked = false
            adapter.showOnlySelected(false)
        }

        chipSelected.setOnClickListener {
            chipSelected.isChecked = true
            chipAll.isChecked = false
            adapter.showOnlySelected(true)
        }

        // Load apps
        loadInstalledApps()

        return view
    }

    override fun onResume() {
        super.onResume()
        updateSelectedCount()
    }

    private fun loadInstalledApps() {
        progressIndicator.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val pm = requireContext().packageManager
            val selectedApps = prefs.getStringSet("monitored_apps", emptySet()) ?: emptySet()

            val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
                .filter { appInfo ->
                    // Filtrer ut system-apper uten launcher icon
                    pm.getLaunchIntentForPackage(appInfo.packageName) != null
                }
                .map { appInfo ->
                    AppInfo(
                        packageName = appInfo.packageName,
                        appName = pm.getApplicationLabel(appInfo).toString(),
                        icon = pm.getApplicationIcon(appInfo),
                        isSelected = selectedApps.contains(appInfo.packageName)
                    )
                }
                .sortedWith(compareByDescending<AppInfo> { it.isSelected }.thenBy { it.appName })

            withContext(Dispatchers.Main) {
                if (isAdded && view != null) {
                    allApps = apps
                    adapter.updateApps(apps)
                    progressIndicator.visibility = View.GONE
                    recyclerView.visibility = View.VISIBLE
                    updateSelectedCount()
                }
            }
        }
    }

    private fun onAppToggled(app: AppInfo, isSelected: Boolean) {
        val selectedApps = prefs.getStringSet("monitored_apps", emptySet())?.toMutableSet() ?: mutableSetOf()
        
        if (isSelected) {
            selectedApps.add(app.packageName)
        } else {
            selectedApps.remove(app.packageName)
        }
        
        prefs.edit().putStringSet("monitored_apps", selectedApps).apply()
        updateSelectedCount()
        
        Logger.d("AppSelection", "App ${app.appName} ${if (isSelected) "lagt til" else "fjernet"}")
    }

    private fun updateSelectedCount() {
        val count = prefs.getStringSet("monitored_apps", emptySet())?.size ?: 0
        selectedCountText.text = getString(R.string.selected_apps_count, count)
    }
}