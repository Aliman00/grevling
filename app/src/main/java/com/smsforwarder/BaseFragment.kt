package com.smsforwarder

import android.content.SharedPreferences
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

/**
 * Base fragment med delt funksjonalitet for alle fragments i appen.
 * Reduserer kodeduplisering ved å samle felles hjelpefunksjoner.
 */
abstract class BaseFragment : Fragment() {

    /**
     * Henter krypterte SharedPreferences for sikker lagring av sensitiv data.
     */
    protected fun getEncryptedPreferences(): SharedPreferences {
        return PreferencesManager.getEncryptedPreferences(requireContext())
    }

    /**
     * Henter farge fra resources på en lifecycle-safe måte.
     */
    protected fun getColor(colorResId: Int): Int {
        return ContextCompat.getColor(requireContext(), colorResId)
    }

    /**
     * Oppretter en TextWatcher med auto-save debounce funksjonalitet.
     * 
     * @param savedIndicator Optional TextView som viser "Lagret" indikator
     * @param debounceMs Forsinkelse før lagring (default 1000ms)
     * @param shouldSave Lambda som returnerer true hvis lagring skal skje (default: alltid true)
     * @param onSave Callback som kalles med den nye teksten
     * @param onAfterSave Optional callback etter lagring (f.eks. for å oppdatere status)
     */
    protected fun createAutoSaveWatcher(
        savedIndicator: TextView? = null,
        debounceMs: Long = 1000L,
        shouldSave: () -> Boolean = { true },
        onSave: (String) -> Unit,
        onAfterSave: (() -> Unit)? = null
    ): TextWatcher {
        var saveJob: Job? = null
        val indicatorRef = WeakReference(savedIndicator)

        return object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (shouldSave()) {
                    saveJob?.cancel()
                    indicatorRef.get()?.visibility = View.GONE

                    saveJob = viewLifecycleOwner.lifecycleScope.launch {
                        delay(debounceMs)
                        onSave(s.toString())
                        onAfterSave?.invoke()

                        // Vis "Lagret" indikator hvis tilgjengelig
                        indicatorRef.get()?.let { indicator ->
                            indicator.visibility = View.VISIBLE
                            delay(2000)
                            indicator.visibility = View.GONE
                        }
                    }
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }
    }
}
