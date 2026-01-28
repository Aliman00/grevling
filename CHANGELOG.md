# Changelog - Grevling

Alle viktige endringer i Grevling vil bli dokumentert her.

Formatet baseres på [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
og følger [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [1.0] - 2026-01-28

### Lagt til
- SMS forwarding til email via Gmail SMTP
- Missed call forwarding til email via Gmail SMTP
- Automatisk SMS-svar for innkommende SMS
- Automatisk SMS-svar for tapte anrop
- Kontaktnavnoppslag for bedre lesbarhet
- Test-knapp for å verifisere email-konfigurering
- EncryptedSharedPreferences for sikker lagring av credentials
- Gmail App Password støtte
- In-app guide for å få Gmail App Password
- Notifikasjonstilgang for å fange tapte anrop
- Status-indikator (Aktiv/Pauset/Mangler konfig)
- Separate eller unified auto-svar meldinger
- Låsefunksjon for auto-svar meldinger
- Norsk brukergrensesnitt

### Sikkerhet
- AES256-GCM kryptering av Gmail credentials
- GDPR-compliant logging (ingen sensitiv data i logs)
- HTML-escaping i emails
- Input validation for email-adresser
- Runtime permission checks
- TLS 1.2/1.3 for SMTP kommunikasjon

### Teknisk
- Målplattform: Android 8.0+ (API 26+)
- Target SDK: 34 (Android 14)
- Språk: Kotlin
- Dependencies: JavaMail, AndroidX, Kotlin Coroutines
- Ingen proprietary dependencies
- Thread-safe operasjoner
- Memory leak-free architecture
- Modern Android APIs (Activity Result API, etc)

### Lisens
- Personal Use Only License (non-commercial)
- Distribueres kun via GitHub Releases

---

## Template for fremtidige versjoner:

## [Unreleased]

### Lagt til
- Nye features

### Endret
- Endringer i eksisterende funksjonalitet

### Deprecated
- Features som snart fjernes

### Fjernet
- Fjernede features

### Fikset
- Bug fixes

### Sikkerhet
- Sikkerhetsforbedringer

---

**Versjonsnummerering:**
- **MAJOR.MINOR.PATCH** (f.eks. 1.0.0)
- **MAJOR:** Breaking changes (inkompatibel API)
- **MINOR:** Nye features (backwards compatible)
- **PATCH:** Bug fixes (backwards compatible)
