package com.grevlingappen.data

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import com.grevlingappen.domain.models.AppInfo
import com.grevlingappen.utils.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** AppRepository - Håndterer henting og filtrering av installerte applikasjoner på enheten. */
class AppRepository(context: Context) {

    private val appContext = context.applicationContext
    private val packageManager: PackageManager = appContext.packageManager

    /**
     * Henter alle installerte apper som har et ikon i launcheren.
     * Bruker asynkron lasting på Dispatchers.IO for å unngå blokkering av UI.
     */
    @SuppressLint("QueryPermissionsNeeded")
    suspend fun getInstalledApps(selectedPackages: Set<String>): List<AppInfo> = withContext(Dispatchers.IO) {
        try {
            val startTime = System.currentTimeMillis()

            val apps = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // Bruk getInstalledPackages for API 33+
                packageManager.getInstalledPackages(PackageManager.PackageInfoFlags.of(0))
                    .mapNotNull { info ->
                        try {
                            val appInfo = info.applicationInfo ?: return@mapNotNull null
                            if (packageManager.getLaunchIntentForPackage(appInfo.packageName) != null) {
                                AppInfo(
                                    packageName = appInfo.packageName,
                                    appName = getAppName(appInfo),
                                    isSelected = selectedPackages.contains(appInfo.packageName)
                                )
                            } else null
                        } catch (e: Exception) {
                            null
                        }
                    }
            } else {
                // Fallback for eldre API-versjoner
                @Suppress("DEPRECATION")
                packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
                    .filter { packageManager.getLaunchIntentForPackage(it.packageName) != null }
                    .map { appInfo ->
                        AppInfo(
                            packageName = appInfo.packageName,
                            appName = getAppName(appInfo),
                            isSelected = selectedPackages.contains(appInfo.packageName)
                        )
                    }
            }

            val sortedApps = apps.sortedWith(
                compareByDescending<AppInfo> { it.isSelected }
                    .thenBy { it.appName.lowercase() }
            )

            Logger.d(TAG, "Lastet ${sortedApps.size} apper på ${System.currentTimeMillis() - startTime}ms")
            sortedApps
        } catch (e: Exception) {
            Logger.e(TAG, "Kritisk feil ved henting av app-liste", e)
            emptyList()
        }
    }

    private fun getAppName(appInfo: ApplicationInfo): String = try {
        packageManager.getApplicationLabel(appInfo).toString()
    } catch (e: Exception) {
        Logger.w(TAG, "Kunne ikke hente navn for ${appInfo.packageName}, bruker fallback", e)
        appInfo.packageName
    }

    companion object {
        private const val TAG = "AppRepository"

        /** Utfører søk i en eksisterende app-liste basert på navn eller pakkenavn. */
        fun searchApps(apps: List<AppInfo>, query: String): List<AppInfo> {
            if (query.isBlank()) return apps
            val lowerQuery = query.lowercase()
            return apps.filter { 
                it.appName.lowercase().contains(lowerQuery) || it.packageName.lowercase().contains(lowerQuery) 
            }
        }

        /** Filtrerer listen til å kun vise valgte apper. */
        fun filterSelected(apps: List<AppInfo>, showOnlySelected: Boolean): List<AppInfo> {
            return if (showOnlySelected) apps.filter { it.isSelected } else apps
        }
    }
}
