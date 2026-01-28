# Grevling

En Android-app som videresender SMS og tapte anrop til email via Gmail SMTP.

## Funksjoner

- Videresend innkommende SMS til email
- Videresend tapte anrop til email
- Automatisk SMS-svar til avsendere
- Kontaktnavnoppslag for bedre lesbarhet
- Kryptert lagring av Gmail-legitimasjon
- Test-knapp for å verifisere email-konfigurasjonen

## Sette opp Gmail App Password

For at appen skal kunne sende email via Gmail trenger du et **App Password**. Dette er et spesielt passord som Google genererer for tredjeparts-apper.

### Steg-for-steg guide:

#### 1. Aktiver 2-faktor autentisering (2FA)

**Dette er et krav fra Google for å kunne opprette App Passwords.**

1. Gå til [Google Account Security](https://myaccount.google.com/security)
2. Under "Signing in to Google", velg **2-Step Verification**
3. Følg instruksjonene for å aktivere 2FA (bruk telefonnummer eller autentiseringsapp)

#### 2. Opprett App Password

1. Gå til [App Passwords](https://myaccount.google.com/apppasswords)
   - Eller: Google Account → Security → 2-Step Verification → App passwords (nederst på siden)
2. Logg inn med Gmail-kontoen din
3. Under "Select app", velg **Mail**
4. Under "Select device", velg **Other (Custom name)**
5. Skriv inn: `SMS Forwarder` eller et annet navn du husker
6. Klikk **Generate**
7. Google viser nå et **16-tegn passord** (f.eks. `abcd efgh ijkl mnop`)
8. **Kopier dette passordet** (du trenger å fjerne mellomrom når du limer det inn i appen)

#### 3. Konfigurer appen

1. Åpne **Grevling** appen
2. Gå til **Innstillinger**-fanen
3. Trykk **🔒 Lås opp**
4. Fyll ut feltene:
   - **Mottaker email**: Email-adressen hvor du vil motta varsler (kan være samme som Gmail-adressen)
   - **Gmail-adresse**: Din `brukernavn@gmail.com`
   - **Gmail App Password**: Lim inn det 16-tegn passordet **uten mellomrom** (f.eks. `abcdefghijklmnop`)
5. Trykk **📧 Send test-email** for å teste konfigurasjonen
6. Hvis test-emailen sendes, vil du se: `✅ Test-email sendt!`

### Feilsøking

#### ❌ Autentisering feilet

**Løsning:**
- Sjekk at du har aktivert 2FA på Google-kontoen
- Verifiser at App Password er riktig kopiert (fjern alle mellomrom)
- Prøv å generere et nytt App Password

#### ❌ Sending feilet: Connection timeout

**Løsning:**
- Sjekk internettforbindelsen
- Verifiser at appen har INTERNET-tillatelse (den skal ha det automatisk)

## Distribusjon

Appen distribueres kun via GitHub:
- **Last ned:** [GitHub Releases](https://github.com/Aliman00/grevling/releases)
- **Ingen tracking eller analytics**
- **Ingen proprietære avhengigheter**
- **Open source** (Personal Use Only license)

## Tillatelser

Appen trenger følgende tillatelser:

- `RECEIVE_SMS` - Motta SMS-meldinger
- `READ_SMS` - Lese SMS-innhold
- `SEND_SMS` - Sende automatiske svar
- `READ_CALL_LOG` - Lese anropslogg for tapte anrop
- `READ_CONTACTS` - Slå opp kontaktnavn
- `INTERNET` - Sende email via SMTP
- `NOTIFICATION_LISTENER` - Lytte til tapte anrop-varsler (må aktiveres i Android-innstillinger)

## Bygging

```bash
./gradlew assembleRelease
```

APK-filen vil være i: `app/build/outputs/apk/release/`

## Sikkerhet

- Alle credentials (Gmail-adresse og App Password) lagres kryptert med **EncryptedSharedPreferences**
- Bruker AES256-GCM kryptering
- Ingen credentials sendes til tredjepart
- All kommunikasjon skjer direkte med Gmail SMTP (TLS 1.2/1.3)

## Lisens

Dette prosjektet er lisensiert under **Personal Use Only License** - se [LICENSE](LICENSE) filen for detaljer.

**Viktig:**
- ✅ Gratis for personlig bruk
- ✅ Kan modifiseres for eget bruk
- ✅ Kan deles med andre (gratis)
- ❌ Kommersiell bruk IKKE tillatt
- ❌ Kan IKKE selges
- ❌ Kan IKKE ha subscription-modeller

For kommersiell bruk, kontakt utvikleren via GitHub Issues.

## Support

For spørsmål eller problemer, opprett en [issue](https://github.com/Aliman00/grevling/issues) i dette repositoriet.
