package com.grevlingappen.services

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.grevlingappen.utils.EmailSender
import com.grevlingappen.utils.Logger

import jakarta.mail.AuthenticationFailedException

/**
 * EmailWorker - WorkManager Worker som håndterer e-postutsending i bakgrunnen.
 * 
 * Funksjonalitet:
 * - Mottar e-postemne og body som input fra WorkManager
 * - Sender e-post via Gmail SMTP
 * - Håndterer feil med automatisk retry (opptil 3 forsøk)
 * - Garanterer levering selv om appen blir drept av systemet
 * 
 * Merk: WorkManager håndterer selv retry-logikk og overlever app-død.
 * Workeren kjører selvstendig uavhengig av appens livssyklus.
 */
class EmailWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "EmailWorker"
        
        // Nøkler for WorkManager input data
        const val KEY_SUBJECT = "subject"   // E-postemne
        const val KEY_BODY = "body"         // E-post body
        
        // Maksimalt antall retry-forsøk ved midlertidige feil
        private const val MAX_RETRIES = 3
    }

    /**
     * Hovedjobben som kjøres av WorkManager.
     * @return Result.success() ved vellykket sending, Result.failure() ved permanent feil,
     *         eller Result.retry() ved midlertidig feil (hvis forsøk gjenstår)
     */
    override suspend fun doWork(): Result {
        // Hent e-postdata fra WorkManager input
        val subject = inputData.getString(KEY_SUBJECT) ?: return Result.failure()
        val body = inputData.getString(KEY_BODY) ?: return Result.failure()

        return try {
            Logger.d(TAG, "Starter bakgrunnsutsending av e-post: $subject")
            
            // Send e-post umiddelbart
            EmailSender.sendEmailNow(applicationContext, subject, body)
            Result.success()

        } catch (e: AuthenticationFailedException) {
            // Autentiseringsfeil - feil passord/brukernavn, gir ikke mening å prøve igjen
            Logger.e(TAG, "Autentisering feilet - sjekk Gmail-passord", e)
            Result.failure()
            
        } catch (e: IllegalArgumentException) {
            // Konfigurasjonsfeil - mangler påkrevde felt, gir ikke mening å prøve igjen
            Logger.e(TAG, "Konfigurasjonsfeil: ${e.message}", e)
            Result.failure()
            
        } catch (e: Exception) {
            // Andre feil - prøv på nytt hvis vi har forsøk igjen
            Logger.w(TAG, "Midlertidig feil ved sending: ${e.message}")
            
            if (runAttemptCount < MAX_RETRIES) {
                Logger.d(TAG, "Setter opp nytt forsøk (forsøk ${runAttemptCount + 1} av $MAX_RETRIES)")
                Result.retry()
            } else {
                Logger.e(TAG, "Maks antall forsøk nådd ($MAX_RETRIES), gir opp")
                Result.failure()
            }
        }
    }
}