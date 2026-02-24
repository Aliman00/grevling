package com.grevlingappen.services

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.grevlingappen.utils.EmailSender
import com.grevlingappen.utils.Logger

import jakarta.mail.AuthenticationFailedException

/**
 * EmailWorker - Håndterer robust utsending av e-post i bakgrunnen via WorkManager.
 * Garanterer levering selv om appen drepes av systemet.
 */
class EmailWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "EmailWorker"
        const val KEY_SUBJECT = "subject"
        const val KEY_BODY = "body"
        private const val MAX_RETRIES = 3
    }

    override suspend fun doWork(): Result {
        val subject = inputData.getString(KEY_SUBJECT) ?: return Result.failure()
        val body = inputData.getString(KEY_BODY) ?: return Result.failure()

        return try {
            Logger.d(TAG, "Starter bakgrunnsutsending av e-post")
            
            EmailSender.sendEmailNow(applicationContext, subject, body)
            Result.success()

        } catch (e: AuthenticationFailedException) {
            Logger.e(TAG, "Autentisering feilet - sjekk passord. Dropper e-post.", e)
            Result.failure()
        } catch (e: IllegalArgumentException) {
            Logger.e(TAG, "Konfigurasjonsfeil: ${e.message}. Dropper e-post.", e)
            Result.failure()
        } catch (e: Exception) {
            Logger.w(TAG, "Feil under sending: ${e.message}")
            
            if (runAttemptCount < MAX_RETRIES) {
                Logger.d(TAG, "Prøver på nytt (forsøk ${runAttemptCount + 1})")
                Result.retry()
            } else {
                Logger.e(TAG, "Maks antall forsøk nådd, gir opp")
                Result.failure()
            }
        }
    }
}
