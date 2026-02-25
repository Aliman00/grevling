package com.grevlingappen.utils

import android.content.Context
import com.grevlingappen.R
import com.grevlingappen.data.PreferenceKeys
import com.grevlingappen.services.EmailWorker
import androidx.annotation.VisibleForTesting
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.workDataOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.LinkedList
import java.util.Properties
import java.util.concurrent.TimeUnit
import jakarta.mail.Authenticator
import jakarta.mail.Message
import jakarta.mail.PasswordAuthentication
import jakarta.mail.Session
import jakarta.mail.Transport
import jakarta.mail.internet.AddressException
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeMessage

/**
 * EmailSender - Håndterer sending av e-post via Gmail SMTP.
 * Tilbyr både direkte sending (suspend) og bakgrunnssending via WorkManager.
 */
object EmailSender {
    private const val TAG = "EmailSender"
    private const val SMTP_HOST = "smtp.gmail.com"
    private const val SMTP_PORT = "587"
    private const val SMTP_TIMEOUT = "15000"

    private const val MAX_EMAILS_PER_MINUTE = 10
    private val sentTimestamps = LinkedList<Long>()

    @Synchronized
    private fun canSendAndRegister(): Boolean {
        val now = System.currentTimeMillis()
        sentTimestamps.removeIf { it < now - 60_000 }
        return if (sentTimestamps.size < MAX_EMAILS_PER_MINUTE) {
            sentTimestamps.add(now)
            true
        } else {
            false
        }
    }

    @Synchronized
    private fun releaseLastSlot() {
        if (sentTimestamps.isNotEmpty()) {
            sentTimestamps.removeLast()
        }
    }

    @VisibleForTesting
    fun resetRateLimitForTesting() {
        synchronized(this) { sentTimestamps.clear() }
    }

    fun enqueueEmail(context: Context, subject: String, body: String) {
        val appContext = context.applicationContext
        
        val prefs = EncryptedPrefsFactory.get(appContext)
        if (!prefs.getBoolean(PreferenceKeys.ENABLED, false)) {
            Logger.d(TAG, "Videresending deaktivert, dropper e-post: $subject")
            return
        }

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<EmailWorker>()
            .setInputData(workDataOf(
                EmailWorker.KEY_SUBJECT to subject,
                EmailWorker.KEY_BODY to body
            ))
            .setConstraints(constraints)
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .build()

        WorkManager.getInstance(appContext).enqueue(workRequest)
        Logger.d(TAG, "E-post lagt i WorkManager-kø: $subject")
    }

    suspend fun sendEmailNow(context: Context, subject: String, body: String) = withContext(Dispatchers.IO) {
        val appContext = context.applicationContext
        val prefs = EncryptedPrefsFactory.get(appContext)

        val gmail = prefs.getString(PreferenceKeys.GMAIL_ADDRESS, "") ?: ""
        val pass = prefs.getString(PreferenceKeys.GMAIL_PASSWORD, "")?.trim() ?: ""
        val dest = prefs.getString(PreferenceKeys.RECIPIENT_EMAIL, "") ?: ""

        if (gmail.isEmpty() || pass.isEmpty() || dest.isEmpty()) {
            throw IllegalArgumentException("Mangler e-postkonfigurasjon")
        }

        if (!canSendAndRegister()) {
            throw IllegalStateException("Rate-limit nådd, prøver igjen senere")
        }

        try {
            val session = createSession(gmail, pass)
            val safeSub = StringUtils.sanitizeSubject(subject)
            val footer = appContext.getString(R.string.email_footer_text)
            val htmlBody = buildHtmlBody(safeSub, body, footer)
            
            val message = createMimeMessage(session, gmail, dest, appContext.getString(R.string.app_display_name), safeSub, htmlBody)
            Transport.send(message)
            Logger.d(TAG, "E-post sendt: $safeSub")
        } catch (e: Exception) {
            Logger.e(TAG, "Feil ved sending av e-post: ${e.message}")
            releaseLastSlot()
            throw e
        }
    }

    suspend fun testEmailConfig(context: Context): String = testEmailConfigWithParams(
        context = context,
        gmailAddress = null,
        gmailPassword = null,
        recipientEmail = null
    )

    suspend fun testEmailConfigWithParams(
        context: Context,
        gmailAddress: String?,
        gmailPassword: String?,
        recipientEmail: String?
    ): String = withContext(Dispatchers.IO) {
        val appContext = context.applicationContext
        val prefs = EncryptedPrefsFactory.get(appContext)

        val gmail = gmailAddress ?: prefs.getString(PreferenceKeys.GMAIL_ADDRESS, "") ?: ""
        val pass = gmailPassword?.takeIf { it.isNotEmpty() } ?: prefs.getString(PreferenceKeys.GMAIL_PASSWORD, "")?.trim() ?: ""
        val dest = recipientEmail ?: prefs.getString(PreferenceKeys.RECIPIENT_EMAIL, "") ?: ""

        if (gmail.isEmpty() || pass.isEmpty() || dest.isEmpty()) {
            return@withContext appContext.getString(R.string.email_test_result_config_error)
        }

        if (!canSendAndRegister()) {
            return@withContext "Rate-limit: Vent litt før ny test"
        }

        try {
            InternetAddress(gmail).validate()
            InternetAddress(dest).validate()

            val session = createSession(gmail, pass)
            val sub = appContext.getString(R.string.email_test_subject, appContext.getString(R.string.app_display_name))
            val html = "<h3>${appContext.getString(R.string.email_test_body_title)}</h3>" +
                       "<p>${appContext.getString(R.string.email_test_body_content)}</p>"
            
            Transport.send(createMimeMessage(session, gmail, dest, appContext.getString(R.string.app_display_name), sub, html))
            appContext.getString(R.string.email_test_result_success)
        } catch (e: jakarta.mail.AuthenticationFailedException) {
            releaseLastSlot()
            Logger.w(TAG, "Autentisering feilet", e)
            appContext.getString(R.string.email_test_result_auth_error)
        } catch (e: AddressException) {
            releaseLastSlot()
            Logger.w(TAG, "Ugyldig adresse", e)
            appContext.getString(R.string.test_email_error_invalid_recipient)
        } catch (e: Exception) {
            releaseLastSlot()
            Logger.e(TAG, "Uventet feil", e)
            appContext.getString(R.string.email_test_result_error, e.localizedMessage ?: "Ukjent feil")
        }
    }

    private fun createSession(user: String, pass: String): Session {
        val props = Properties().apply {
            put("mail.smtp.host", SMTP_HOST)
            put("mail.smtp.port", SMTP_PORT)
            put("mail.smtp.auth", "true")
            put("mail.smtp.starttls.enable", "true")
            put("mail.smtp.starttls.required", "true")
            put("mail.smtp.ssl.protocols", "TLSv1.2 TLSv1.3")
            put("mail.smtp.connectiontimeout", SMTP_TIMEOUT)
            put("mail.smtp.timeout", SMTP_TIMEOUT)
            put("mail.smtp.writetimeout", SMTP_TIMEOUT)
        }
        return Session.getInstance(props, object : Authenticator() {
            override fun getPasswordAuthentication() = PasswordAuthentication(user, pass)
        })
    }

    private fun createMimeMessage(session: Session, from: String, to: String, displayName: String, sub: String, html: String): MimeMessage {
        return MimeMessage(session).apply {
            setFrom(InternetAddress(from, displayName))
            setRecipient(Message.RecipientType.TO, InternetAddress(to))
            setSubject(sub, "UTF-8")
            setContent(html, "text/html; charset=UTF-8")
        }
    }

    private fun buildHtmlBody(safeSubject: String, body: String, footer: String): String {
        return """
            <div style="font-family:sans-serif;line-height:1.5;color:#333;">
                <h3 style="color:#2c3e50;">${StringUtils.escapeHtml(safeSubject)}</h3>
                <p style="white-space:pre-wrap;">${StringUtils.escapeHtml(body)}</p>
                <br>
                <hr style="border:none;border-top:1px solid #eee;">
                <p style="color:#95a5a6;font-size:0.8em;">$footer</p>
            </div>
        """.trimIndent()
    }
}
