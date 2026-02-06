package com.smsforwarder

import android.content.Context
import android.util.Patterns
import java.util.Properties
import java.util.concurrent.Executors
import java.util.concurrent.ThreadPoolExecutor
import java.util.LinkedList
import javax.mail.*
import javax.mail.internet.*

object EmailSender {

    private const val TAG = "EmailSender"
    private const val SMTP_HOST = "smtp.gmail.com"
    private const val SMTP_PORT = 587
    private const val SMTP_TIMEOUT = "10000"
    private const val EMAIL_DISPLAY_NAME = "Grevling Appen"
    private const val MAX_EMAILS_PER_MINUTE = 10
    private const val MAX_RETRY_ATTEMPTS = 3
    private const val EXECUTOR_SHUTDOWN_TIMEOUT_SECONDS = 5L
    private const val MAX_RATE_LIMIT_WAIT_ATTEMPTS = 24 // 2 minutter maks venting

    // Begrens til maks 3 samtidige e-poster (reinitialiserbar)
    @Volatile
    private var emailExecutor = createExecutor()
    
    // Rate-limiting: tidsstempler for sendte e-poster (sliding window)
    private val sentTimestamps = LinkedList<Long>()

    private fun createExecutor(): ThreadPoolExecutor {
        return Executors.newFixedThreadPool(3) as ThreadPoolExecutor
    }

    /**
     * Henter executor, reinitaliserer hvis nødvendig.
     */
    @Synchronized
    private fun getExecutor(): ThreadPoolExecutor {
        if (emailExecutor.isShutdown || emailExecutor.isTerminated) {
            Logger.d(TAG, "Reinitaliserer email executor")
            emailExecutor = createExecutor()
        }
        return emailExecutor
    }

    /**
     * Stenger executor gracefully. Bør kalles ved app-terminering.
     * Venter maks 5 sekunder på at pågående oppgaver fullføres.
     */
    fun shutdown() {
        try {
            Logger.d(TAG, "Stenger email executor...")
            emailExecutor.shutdown()
            if (!emailExecutor.awaitTermination(EXECUTOR_SHUTDOWN_TIMEOUT_SECONDS, java.util.concurrent.TimeUnit.SECONDS)) {
                Logger.w(TAG, "Executor tok for lang tid, tvinger shutdown")
                emailExecutor.shutdownNow()
            }
            sentTimestamps.clear()
            Logger.d(TAG, "Email executor stengt")
        } catch (e: InterruptedException) {
            Logger.e(TAG, "Shutdown avbrutt", e)
            emailExecutor.shutdownNow()
            Thread.currentThread().interrupt()
        }
    }

    private fun getEncryptedPreferences(context: Context) =
        PreferencesManager.getEncryptedPreferences(context)

    /**
     * Rate-limiter: sjekker om vi kan sende e-post nå (maks 10 per minutt)
     */
    @Synchronized
    private fun canSendEmail(): Boolean {
        val now = System.currentTimeMillis()
        val oneMinuteAgo = now - 60_000
        
        // Fjern gamle tidsstempler (eldre enn 1 minutt)
        sentTimestamps.removeIf { it < oneMinuteAgo }
        
        return sentTimestamps.size < MAX_EMAILS_PER_MINUTE
    }

    /**
     * Registrer at en e-post ble sendt (for rate-limiting)
     */
    @Synchronized
    private fun registerEmailSent() {
        sentTimestamps.add(System.currentTimeMillis())
    }

    /**
     * Sender e-post med retry-logikk (maks 3 forsøk med exponential backoff)
     */
    private fun sendWithRetry(
        session: Session,
        message: MimeMessage,
        attempt: Int = 1
    ) {
        try {
            Transport.send(message)
            registerEmailSent()
            Logger.d(TAG, "Email sendt vellykket${if (attempt > 1) " (forsøk $attempt)" else ""}")
        } catch (e: Exception) {
            if (attempt < MAX_RETRY_ATTEMPTS && isRetriableError(e)) {
                val backoffMs = 1000L * (1 shl (attempt - 1)) // 1s, 2s, 4s
                Logger.w(TAG, "Email feilet (forsøk $attempt/$MAX_RETRY_ATTEMPTS), prøver igjen om ${backoffMs}ms")
                Thread.sleep(backoffMs)
                sendWithRetry(session, message, attempt + 1)
            } else {
                Logger.e(TAG, "Email feilet etter $attempt forsøk", e)
                throw e
            }
        }
    }

    /**
     * Sjekker om feilen er retriable (nettverksfeil, timeouts, etc)
     */
    private fun isRetriableError(e: Exception): Boolean {
        return when (e) {
            is MessagingException -> {
                // Ikke retry auth-feil, men retry nettverksfeil
                e !is AuthenticationFailedException
            }
            else -> true
        }
    }

    /**
     * Fjerner CR/LF fra subject for å hindre SMTP header-injeksjon
     */
    internal fun sanitizeSubject(input: String): String =
        input.replace("\r", " ").replace("\n", " ").trim().take(120)

    /**
     * Escapes HTML special characters to prevent HTML injection
     */
    internal fun escapeHtml(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")
    }

    /**
     * Oppretter SMTP properties for Gmail
     */
    private fun createSmtpProperties(): Properties {
        return Properties().apply {
            put("mail.smtp.host", SMTP_HOST)
            put("mail.smtp.port", SMTP_PORT.toString())
            put("mail.smtp.auth", "true")
            put("mail.smtp.starttls.enable", "true")
            put("mail.smtp.starttls.required", "true")
            put("mail.smtp.ssl.protocols", "TLSv1.2 TLSv1.3")
            put("mail.smtp.ssl.checkserveridentity", "true")
            put("mail.smtp.connectiontimeout", SMTP_TIMEOUT)
            put("mail.smtp.timeout", SMTP_TIMEOUT)
            put("mail.smtp.writetimeout", SMTP_TIMEOUT)
        }
    }

    /**
     * Oppretter email session med autentisering
     */
    private fun createSession(gmailAddress: String, gmailPassword: String): Session {
        val props = createSmtpProperties()
        return Session.getInstance(props, object : Authenticator() {
            override fun getPasswordAuthentication(): PasswordAuthentication {
                return PasswordAuthentication(gmailAddress, gmailPassword)
            }
        })
    }

    /**
     * Oppretter en MIME message
     */
    private fun createMessage(
        session: Session,
        fromAddress: String,
        toAddress: String,
        subject: String,
        htmlBody: String
    ): MimeMessage {
        return MimeMessage(session).apply {
            setFrom(InternetAddress(fromAddress, EMAIL_DISPLAY_NAME))
            setRecipient(Message.RecipientType.TO, InternetAddress(toAddress))
            setSubject(subject, "UTF-8")
            setContent(htmlBody, "text/html; charset=UTF-8")
        }
    }

    fun sendEmail(context: Context, subject: String, body: String) {
        val prefs = getEncryptedPreferences(context)
        val enabled = prefs.getBoolean(PreferencesManager.KEY_ENABLED, false)
        val gmailAddress = prefs.getString(PreferencesManager.KEY_GMAIL_ADDRESS, "") ?: ""
        val gmailPassword = prefs.getString(PreferencesManager.KEY_GMAIL_PASSWORD, "") ?: ""
        val toEmail = prefs.getString(PreferencesManager.KEY_RECIPIENT_EMAIL, "") ?: ""

        if (!enabled || gmailAddress.isEmpty() || gmailPassword.isEmpty() || toEmail.isEmpty()) {
            Logger.d(TAG, "Email ikke konfigurert eller deaktivert")
            return
        }

        // Valider e-postformat før SMTP-tilkobling
        if (!Patterns.EMAIL_ADDRESS.matcher(gmailAddress).matches() ||
            !Patterns.EMAIL_ADDRESS.matcher(toEmail).matches()) {
            Logger.w(TAG, "Ugyldig e-postformat, avbryter sending")
            return
        }

        getExecutor().execute {
            // Vent til rate-limit tillater sending (maks 2 minutter)
            var waitAttempts = 0
            while (!canSendEmail() && waitAttempts < MAX_RATE_LIMIT_WAIT_ATTEMPTS) {
                Logger.d(TAG, "Rate-limit nådd, venter... (forsøk ${waitAttempts + 1}/$MAX_RATE_LIMIT_WAIT_ATTEMPTS)")
                Thread.sleep(5000)
                waitAttempts++
            }

            if (waitAttempts >= MAX_RATE_LIMIT_WAIT_ATTEMPTS) {
                Logger.w(TAG, "Rate-limit timeout etter 2 minutter, dropper email")
                return@execute
            }

            try {
                val session = createSession(gmailAddress, gmailPassword)
                val safeSubject = sanitizeSubject(subject)
                val htmlBody = "<h3>${escapeHtml(safeSubject)}</h3><p>${escapeHtml(body)}</p>"
                val message = createMessage(session, gmailAddress, toEmail, safeSubject, htmlBody)

                sendWithRetry(session, message)

            } catch (e: Exception) {
                Logger.e(TAG, "Email feilet permanent", e)
            }
        }
    }

    /**
     * Tester email-konfigurasjonen ved å sende en test-email.
     * Leser credentials direkte fra EncryptedSharedPreferences.
     */
    fun testEmailConfig(
        context: Context,
        onResult: (Boolean, String) -> Unit
    ) {
        val prefs = getEncryptedPreferences(context)
        val gmailAddress = prefs.getString(PreferencesManager.KEY_GMAIL_ADDRESS, "") ?: ""
        val gmailPassword = prefs.getString(PreferencesManager.KEY_GMAIL_PASSWORD, "") ?: ""
        val recipientEmail = prefs.getString(PreferencesManager.KEY_RECIPIENT_EMAIL, "") ?: ""

        if (gmailAddress.isEmpty() || gmailPassword.isEmpty() || recipientEmail.isEmpty()) {
            onResult(false, "❌ Email ikke konfigurert")
            return
        }

        getExecutor().execute {
            var success = false
            var message: String

            try {
                val session = createSession(gmailAddress, gmailPassword)
                val htmlBody = "<h3>Test Email</h3><p>Email-konfigurasjonen fungerer! ✅</p>"
                val testMessage = createMessage(
                    session,
                    gmailAddress,
                    recipientEmail,
                    "Test Email - $EMAIL_DISPLAY_NAME",
                    htmlBody
                )

                Transport.send(testMessage)
                success = true
                message = "✅ Test-email sendt!"
                Logger.d(TAG, "Test-email sendt vellykket")

            } catch (e: AuthenticationFailedException) {
                message = "❌ Autentisering feilet. Sjekk Gmail-adresse og App Password."
                Logger.e(TAG, "Autentisering feilet", e)
            } catch (e: MessagingException) {
                message = "❌ Sending feilet: ${e.message}"
                Logger.e(TAG, "MessagingException", e)
            } catch (e: Exception) {
                message = "❌ Ukjent feil: ${e.message}"
                Logger.e(TAG, "Ukjent feil ved test-email", e)
            }

            // Sikker callback-håndtering
            try {
                onResult(success, message)
            } catch (e: Exception) {
                Logger.e(TAG, "Feil i onResult callback", e)
            }
        }
    }
}
