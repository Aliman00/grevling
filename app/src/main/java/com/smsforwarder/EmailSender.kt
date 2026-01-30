package com.smsforwarder

import android.content.Context
import java.util.Properties
import java.util.concurrent.Executors
import java.util.concurrent.ThreadPoolExecutor
import javax.mail.*
import javax.mail.internet.*

object EmailSender {

    private const val TAG = "EmailSender"
    private const val SMTP_HOST = "smtp.gmail.com"
    private const val SMTP_PORT = 587
    private const val SMTP_TIMEOUT = "10000"
    private const val EMAIL_DISPLAY_NAME = "Grevling Appen"

    // Begrens til maks 3 samtidige e-poster
    private val emailExecutor = Executors.newFixedThreadPool(3) as ThreadPoolExecutor

    private fun getEncryptedPreferences(context: Context) =
        PreferencesManager.getEncryptedPreferences(context)

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
        val enabled = prefs.getBoolean("enabled", false)
        val gmailAddress = prefs.getString("gmail_address", "") ?: ""
        val gmailPassword = prefs.getString("gmail_password", "") ?: ""
        val toEmail = prefs.getString("email", "") ?: ""

        if (!enabled || gmailAddress.isEmpty() || gmailPassword.isEmpty() || toEmail.isEmpty()) {
            Logger.d(TAG, "Email ikke konfigurert eller deaktivert")
            return
        }

        emailExecutor.execute {
            try {
                val session = createSession(gmailAddress, gmailPassword)
                // Escape HTML for sikkerhet
                val htmlBody = "<h3>${escapeHtml(subject)}</h3><p>${escapeHtml(body)}</p>"
                val message = createMessage(session, gmailAddress, toEmail, subject, htmlBody)

                Transport.send(message)
                Logger.d(TAG, "Email sendt vellykket")

            } catch (e: Exception) {
                Logger.e(TAG, "Feil ved sending av email", e)
            }
        }
    }

    /**
     * Tester email-konfigurasjonen ved å sende en test-email
     */
    fun testEmailConfig(
        gmailAddress: String,
        gmailPassword: String,
        recipientEmail: String,
        onResult: (Boolean, String) -> Unit
    ) {
        emailExecutor.execute {
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
