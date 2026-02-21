# Grevling Appen — Verifisert Bugrapport

> **Generert:** 2026-02-21  
> **Analysert av:** Kilo Code (Claude Sonnet 4.5) — uavhengig gjennomgang av alle 45 kildefiler  
> **Totalt funn:** 44 verifiserte problemer (4 Kritiske · 10 Høy · 22 Medium · 8 Lav)

Denne rapporten er basert på en fullstendig, uavhengig gjennomgang av kildekoden. Hvert funn er direkte verifiserbart i den angitte filen og linjenummeret. Ingen spekulasjoner — kun reelle problemer funnet i koden.

---

## Innholdsfortegnelse

1. [🔴 Kritiske problemer (4)](#-kritiske-problemer)
2. [🟡 Høy alvorlighetsgrad (10)](#-høy-alvorlighetsgrad)
3. [🟠 Medium alvorlighetsgrad (22)](#-medium-alvorlighetsgrad)
4. [🟢 Lav alvorlighetsgrad (8)](#-lav-alvorlighetsgrad)
5. [Oppsummering og prioritert handlingsplan](#oppsummering-og-prioritert-handlingsplan)

---

## 🔴 Kritiske problemer

### S-01 — Gmail-passord lagret i klartekst i UI-tilstand

**Fil:** `app/src/main/java/com/grevlingappen/ui/screens/settings/SettingsViewModel.kt` (ca. linje 167)  
**Kategori:** Sikkerhet  

**Problem:**  
`SettingsUiState.gmailPassword: String = ""` holder Gmail App Password i ViewModelens `MutableStateFlow` i heap-minnet for hele ViewModel-levetiden. På en rootet enhet eller med et heap-dump er dette ekstraherbart.

**Løsningsforslag:**
```kotlin
// ❌ Nåværende — passord lever i UI-state
data class SettingsUiState(
    val gmailPassword: String = "",
    // ...
)

// ✅ Anbefalt — bruk en separat, kortlivet input-flow
private val _passwordInput = MutableStateFlow("")
// Tøm etter at debounce-lagring er fullført:
private fun onPasswordSaved() {
    _passwordInput.value = ""
}
```

---

### S-02 — Widget-mottakere aksepterer kringkastinger fra alle apper

**Fil:** `app/src/main/AndroidManifest.xml` (ca. linje 72–106)  
**Kategori:** Sikkerhet  

**Problem:**  
Alle tre widget-mottakere (`ForwardingWidget`, `ForwardingWidgetMini`, `StatsWidget`) er `exported="true"` med egendefinerte toggle-handlinger, men **mangler `android:permission`-attributt**. Enhver app på enheten kan sende `com.grevlingappen.ACTION_TOGGLE_FORWARDING` og slå videresending av og på.

**Løsningsforslag:**
```xml
<!-- I AndroidManifest.xml — legg til en signatur-nivå tillatelse -->
<permission
    android:name="com.grevlingappen.WIDGET_CONTROL"
    android:protectionLevel="signature" />

<!-- På hver widget-mottaker: -->
<receiver
    android:name=".widgets.ForwardingWidget"
    android:exported="true"
    android:permission="com.grevlingappen.WIDGET_CONTROL">
    ...
</receiver>
```

---

### B-01 — `SmsReceiver` oppretter uhåndtert coroutine-scope per SMS-hendelse

**Fil:** `app/src/main/java/com/grevlingappen/receivers/SmsReceiver.kt` (ca. linje 53)  
**Kategori:** Bug / Ressurslekkasje  

**Problem:**  
`CoroutineScope(Dispatchers.IO + SupervisorJob()).launch { ... }` oppretter en ny scope uten at referansen beholdes. Ved mange innkommende SMS akkumuleres mange usporate scopes. `goAsync()`-resultatet gir 10 sekunder på Android 8+ før systemet dreper prosessen.

**Løsningsforslag:**
```kotlin
// ❌ Nåværende — ny scope per SMS, ingen sporing
override fun onReceive(context: Context, intent: Intent) {
    val pendingResult = goAsync()
    CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
        // ...
        pendingResult.finish()
    }
}

// ✅ Anbefalt — bruk en delt, administrert scope
companion object {
    private val receiverScope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO
    )
}

override fun onReceive(context: Context, intent: Intent) {
    val pendingResult = goAsync()
    receiverScope.launch {
        try {
            // ...
        } finally {
            pendingResult.finish()
        }
    }
}
```

---

### B-02 — `BaseWidget` bruker delt statisk coroutine-scope som aldri kanselleres

**Fil:** `app/src/main/java/com/grevlingappen/widgets/BaseWidget.kt` (ca. linje 24)  
**Kategori:** Bug / Ressurslekkasje  

**Problem:**  
`private val widgetScope = CoroutineScope(Dispatchers.IO + SupervisorJob())` i companion object kanselleres aldri. Feil i child-coroutines svelges stille av `SupervisorJob`. Hvis en widget fjernes mens en oppdaterings-coroutine kjører, kalles `appWidgetManager.updateAppWidget()` for en ikke-eksisterende widget-ID.

**Løsningsforslag:**
```kotlin
// ✅ Overstyr onDisabled() for å kansellere scope
override fun onDisabled(context: Context) {
    super.onDisabled(context)
    widgetScope.cancel()
}

// Og sjekk om widget-ID fortsatt er gyldig:
widgetScope.launch {
    val manager = AppWidgetManager.getInstance(context)
    val validIds = manager.getAppWidgetIds(
        ComponentName(context, this@BaseWidget::class.java)
    )
    if (appWidgetId in validIds) {
        updateWidget(context, manager, appWidgetId)
    }
}
```

---

## 🟡 Høy alvorlighetsgrad

### S-03 — Stille datatap ved `EncryptedSharedPreferences`-korrupsjon

**Fil:** `app/src/main/java/com/grevlingappen/utils/EncryptedPrefsFactory.kt` (ca. linje 43)  
**Kategori:** Sikkerhet / UX  

**Problem:**  
Ved initialiseringsfeil sletter `context.deleteSharedPreferences(PreferenceKeys.PREFS_NAME)` stille alle lagrede legitimasjoner (Gmail-adresse, App Password, mottaker-e-post) uten brukervarsel.

**Løsningsforslag:**  
Logg feilen, vis en brukersynlig feilmelding/notifikasjon, og vurder å be brukeren om å legge inn legitimasjonen på nytt i stedet for å slette stille.

---

### S-04 — Videresendingsstatistikk lagret i ukryptert `SharedPreferences`

**Fil:** `app/src/main/java/com/grevlingappen/utils/ForwardingStats.kt` (ca. linje 32)  
**Kategori:** Sikkerhet  

**Problem:**  
`getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)` brukes for statistikk mens all annen data bruker `EncryptedSharedPreferences`. `last_forwarded_time`-tidsstempelet avslører atferdsmønstre på rootede enheter.

**Løsningsforslag:**  
Bruk `EncryptedPrefsFactory.get(context)` konsekvent for all sensitiv data, inkludert statistikk.

---

### S-05 / AR-01 — `SettingsViewModel` starter aktiviteter fra Application-kontekst

**Fil:** `app/src/main/java/com/grevlingappen/ui/screens/settings/SettingsViewModel.kt` (ca. linje 142–157)  
**Kategori:** Arkitektur / Sikkerhet  

**Problem:**  
`getApplication<Application>().startActivity(intent)` kalles direkte fra ViewModel. ViewModels skal ikke starte aktiviteter.

**Løsningsforslag:**
```kotlin
// ✅ Emit en UI-hendelse i stedet
sealed class SettingsUiEvent {
    data class OpenBatterySettings(val intent: Intent) : SettingsUiEvent()
}

private val _uiEvents = MutableSharedFlow<SettingsUiEvent>()
val uiEvents = _uiEvents.asSharedFlow()

// I ViewModel:
fun openBatterySettings() {
    viewModelScope.launch {
        _uiEvents.emit(SettingsUiEvent.OpenBatterySettings(intent))
    }
}

// I Composable:
LaunchedEffect(Unit) {
    viewModel.uiEvents.collect { event ->
        when (event) {
            is SettingsUiEvent.OpenBatterySettings -> 
                context.startActivity(event.intent)
        }
    }
}
```

---

### S-06 — `libs.versions.toml` er død kode med motstridende versjoner

**Fil:** `gradle/libs.versions.toml` og `app/build.gradle.kts` (linje 109)  
**Kategori:** Byggkonfigurasjon  

**Problem:**  
Versjonskataloget er definert men `build.gradle.kts` bruker hardkodede strenger og refererer aldri `libs.*`. Kataloget definerer `coreKtx = "1.17.0"` mens `build.gradle.kts` bruker `"1.12.0"`. En utvikler som oppdaterer kataloget vil ikke oppdatere de faktiske avhengighetene.

**Løsningsforslag:**  
Enten migrer `build.gradle.kts` til å bruke `libs.*`-referanser, eller slett `libs.versions.toml` for å unngå forvirring.

---

### B-04 — Repository-tilstand overskriver UI-spesifikke lagringsstatusfelt

**Fil:** `app/src/main/java/com/grevlingappen/ui/screens/home/HomeViewModel.kt` (ca. linje 66–70)  
**Kategori:** Bug / UI  

**Problem:**  
`repository.state.collect { repoState -> _state.value = repoState }` overskriver hele `ForwardingState` inkludert `saveStatusUnified/Sms/Call`-felt. Hvis brukeren skriver (debounce pågår, `saveStatusUnified = SAVING`) og en annen preferanse endres, forsvinner "Lagres..."-indikatoren for tidlig.

**Løsningsforslag:**  
Skill domene-tilstand fra UI-tilstand. Flytt `saveStatus`-felt ut av `ForwardingState` og inn i en separat `HomeUiState`.

---

### B-05 — Test-e-post bruker utdaterte legitimasjoner i debounce-vinduet

**Fil:** `app/src/main/java/com/grevlingappen/ui/screens/settings/SettingsViewModel.kt` (ca. linje 89)  
**Kategori:** Bug / UX  

**Problem:**  
`testEmail()` validerer mot `state.hasGmailPassword` (reflekterer det *lagrede* passordet) men `EmailSender.testEmailConfig()` leser fra `EncryptedSharedPreferences`. Hvis brukeren endrer passordet og klikker "Test E-post" innen 500ms debounce-vinduet, bruker testen det gamle passordet.

**Løsningsforslag:**  
Tving lagring av legitimasjoner før test-e-post sendes, eller vis en advarsel om at endringer ikke er lagret ennå.

---

### B-07 — TOCTOU race condition i `getTotalCountToday()`

**Fil:** `app/src/main/java/com/grevlingappen/utils/ForwardingStats.kt` (ca. linje 93–96)  
**Kategori:** Bug / Concurrency  

**Problem:**  
`getSmsCountToday(context) + getCallsCountToday(context)` anskaffer `synchronized(lock)` to ganger uavhengig. Mellom de to kallene kan `recordEvent()` inkrementere en teller og tilbakestille dagen ved midnatt, og returnere en verdi som blander tellinger fra to forskjellige dager.

**Løsningsforslag:**
```kotlin
fun getTotalCountToday(context: Context): Int {
    return synchronized(lock) {
        getSmsCountTodayLocked(context) + getCallsCountTodayLocked(context)
    }
}
// Lag interne *Locked()-varianter som antar at lock allerede holdes
```

---

### B-08 — Dupliserte tapte anrop-e-poster via to uavhengige kodestier

**Fil:** `app/src/main/java/com/grevlingappen/services/NotificationMonitorService.kt` (ca. linje 80–97)  
**Kategori:** Bug / Logikk  

**Problem:**  
`CATEGORY_MISSED_CALL`-notifikasjon utløser `handleMissedCall(postTime)` som dedupliserer etter notifikasjonens `postTime`. `CATEGORY_CALL`-fjerning utløser `checkForMissedCall()` som dedupliserer etter CallLog `DATE`-tidsstempel. Disse er **forskjellige verdier** for samme tapte anrop, så deduplisering fungerer ikke på tvers av de to stiene — samme tapte anrop kan generere to e-poster.

**Løsningsforslag:**  
Bruk én enkelt dedupliseringsnøkkel (f.eks. telefonnummer + avrundet tidsstempel) på tvers av begge kodestier, lagret i et felles `Set<String>` med TTL.

---

### A-01 — `NotificationListenerService` mangler `android:label`

**Fil:** `app/src/main/AndroidManifest.xml` (ca. linje 62)  
**Kategori:** Android-spesifikk  

**Problem:**  
Tjenesteerklæringen mangler `android:label`, som er beste praksis for brukervendte tjenester som vises i systeminnstillinger.

**Løsningsforslag:**
```xml
<service
    android:name=".services.NotificationMonitorService"
    android:label="@string/notification_service_label"
    android:exported="false"
    android:permission="android.permission.BIND_NOTIFICATION_LISTENER_SERVICE">
```

---

### A-03 — `getString()` kalles fra IO-tråd i `NotificationMonitorService`

**Fil:** `app/src/main/java/com/grevlingappen/services/NotificationMonitorService.kt` (ca. linje 239)  
**Kategori:** Android-spesifikk / Threading  

**Problem:**  
`sendCallEmail()` og `sendNotificationEmail()` kaller `getString()` (en `Context`-metode) fra innsiden av `scope = CoroutineScope(Dispatchers.IO + SupervisorJob())`. `Resources`-tilgang fra bakgrunnstråder bryter Androids trådmodell.

**Løsningsforslag:**
```kotlin
// ✅ Hent strenger på Main-tråden før du bytter til IO
suspend fun sendCallEmail(context: Context, ...) {
    val subject = withContext(Dispatchers.Main) {
        context.getString(R.string.email_subject_call, ...)
    }
    withContext(Dispatchers.IO) {
        EmailSender.send(subject, ...)
    }
}
```

---

## 🟠 Medium alvorlighetsgrad

| ID | Fil | Problem | Løsningsforslag |
|----|-----|---------|-----------------|
| **S-07** | `app/build.gradle.kts:136` | `security-crypto:1.1.0-alpha06` alpha-avhengighet i produksjon | Bytt til stabil `1.0.0` |
| **B-03** | `NotificationMonitorService.kt:249` | `sendNotificationEmail()` er ikke `suspend` mens `sendCallEmail()` er det; inkonsistent trådmodell | Gjør begge til `suspend`-funksjoner |
| **B-06** | `AppsViewModel.kt:89` | Optimistisk oppdatering i `toggleApp()` kan desynkronisere fra repository | Koble `AppsViewModel._state` til `repository.state` |
| **B-09** | `EmailSender.kt:38` | Race condition i `sentTimestamps` rate limiter — to samtidige kall kan begge passere `canSendAndRegister()` | Bruk `AtomicReference` eller `Mutex` for atomisk sjekk-og-sett |
| **B-10** | `PreferencesRepository.kt:61` | `prefs.registerOnSharedPreferenceChangeListener()` kalles på `Dispatchers.IO`-tråd; Android krever main-tråd | Pakk inn i `withContext(Dispatchers.Main)` |
| **B-11** | `AppRepository.kt:34` | `info.applicationInfo` er `@Nullable` på API 33+; tilgang til `.packageName` uten null-sjekk | Legg til null-sjekk: `info.applicationInfo?.packageName ?: continue` |
| **B-13** | `WidgetHelper.kt:106` | `getOrCreateToken()` har check-then-act race — to samtidige widget-oppdateringer kan generere forskjellige UUID-er | Bruk `synchronized`-blokk rundt hele lese-generer-skriv-sekvensen |
| **B-14** | `EmailWorker.kt:46` | Catch-all `Exception` returnerer alltid `Result.retry()`, forårsaker 3 WorkManager-forsøk for autentiseringsfeil | Skill mellom `AuthenticationFailedException` (→ `failure()`) og nettverksfeil (→ `retry()`) |
| **B-15** | `AutoReplyHelper.kt:47` | Telefonnumre logges via `Logger.d()` i debug-bygg | Fjern eller mask telefonnumre i logger |
| **A-04** | `BaseWidget.kt:55` | `goAsync()`-timeout-risiko hvis `EncryptedPrefsFactory.get()` initialiserer for første gang | Pre-initialiser `EncryptedPrefsFactory` ved app-oppstart |
| **A-05** | `app/build.gradle.kts:23` | `targetSdk = 34` mangler Android 15 (API 35) atferdsendringer | Oppdater til `targetSdk = 35` og test |
| **A-06** | `AndroidManifest.xml:21` | `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` krever Play Store-begrunnelse | Dokumenter brukstilfelle i Play Store-oppføring |
| **Q-01** | `SettingsViewModel.kt:57` | Tom `{}` `onStatusChange`-lambda for e-postfelt — ingen visuell lagringstilbakemelding for mottaker/Gmail-adressefelt | Implementer `onStatusChange` for alle felt |
| **Q-04** | `ContactHelperLogicTest.kt:22` | Tester dupliserer logikk inline i stedet for å kalle faktisk `ContactHelper.formatSender()` — tester testens egen kode | Refaktorer tester til å kalle produksjonskode direkte |
| **Q-05** | `ForwardingStats.kt:102` | `return@withContext` inne i `synchronized`-blokk er gyldig men uvanlig | Bruk `return@synchronized` for klarhet |
| **Q-07** | `SettingsViewModel.kt:175` | `isIgnoringBatteryOptimizations: Boolean = true` viser "✓ OK" før faktisk sjekk kjøres i `init` | Sett standardverdi til `false` |
| **Q-08** | `EmailSender.kt:187` | `safeSubject` HTML-escapes to ganger; `footer` med `<b>` HTML escapes ikke — inkonsistens | Standardiser HTML-escaping-strategi |
| **Q-09** | `SettingsViewModel.kt:59` | Passord-lagrings-callback leser `repository.getGmailPassword()` med `SaveStatus.SAVING` (før lagring fullføres) | Les passord-status etter at lagring er bekreftet |
| **AR-02** | `PreferencesRepository.kt:83` | Datalag kaller `PermissionsHelper.isNotificationServiceEnabled()` — krysslagsavhengighet | Flytt tillatelsessjekk til ViewModel-laget |
| **AR-03** | Flere filer | Ingen DI-rammeverk; alle singletons manuelt administrert, begrenser testbarhet | Vurder Hilt/Koin for dependency injection |
| **B-12** | `ContactHelper.kt:39` | URI-spørring bruker rå `phoneNumber` mens cache-nøkkel bruker `normalized`; kanttilfelle med URI-usikre tegn | Normaliser telefonnummer før URI-konstruksjon |
| **Q-06** | `AppRepository.kt:48` | Legacy API-sti bruker `map` uten per-element unntakshåndtering, ulikt API 33+-stien | Legg til `mapNotNull` med try/catch for konsistens |

---

## 🟢 Lav alvorlighetsgrad

| ID | Fil | Problem | Løsningsforslag |
|----|-----|---------|-----------------|
| **Q-10** | `gradle/libs.versions.toml` | Hele filen er død kode | Slett filen eller migrer `build.gradle.kts` til å bruke den |
| **Q-12** | `PermissionsHelper.kt:54` | `try/catch` pakker inn `Intent()`-konstruktør som ikke kan kaste unntak | Flytt try/catch til der `startActivity()` kalles |
| **Q-13** | `NotificationMonitorService.kt:257` | `getApplicationInfo(pkg, 0)` er deprecated på API 33+ | Legg til `@Suppress("DEPRECATION")` eller bruk ny API |
| **Q-14** | `SettingsScreen.kt:147` | Hardkodet `"SettingsScreen"` logg-tag-streng | Bruk `private const val TAG = "SettingsScreen"` |
| **Q-03** | `PermissionsHelperTest.kt:172` | Test asserterer strengverdien av `Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS` i stedet for konstanten | Bruk konstanten direkte i assertion |
| **Q-02** | `HomeViewModel.kt:47` | To motstridende tilstandskilder (repository-samler + direkte `_state.value`-mutasjoner) i samme ViewModel | Konsolider til én enkelt tilstandskilde |
| **AR-04** | `ForwardingState.kt:27` | `saveStatusUnified/Sms/Call` er UI-spesifikke felt i en domenemodellklasse | Flytt til en separat `HomeUiState`-klasse |
| **A-02** | `AndroidManifest.xml:56` | SMS-mottaker prioritet 999 (designbekymring, ikke en bug) | Vurder om høy prioritet er nødvendig og dokumenter begrunnelsen |

---

## Oppsummering og prioritert handlingsplan

### Fase 1 — Kritisk (gjør umiddelbart)

1. **[S-02]** Legg til signatur-nivå tillatelse på widget-mottakere i `AndroidManifest.xml`
2. **[S-01]** Fjern Gmail-passord fra `SettingsUiState` — bruk kortlivet input-flow
3. **[B-08]** Fiks duplisert tapte anrop-e-post ved å bruke én felles dedupliseringsnøkkel
4. **[B-01]** Erstatt per-SMS coroutine-scope med delt administrert scope i `SmsReceiver`

### Fase 2 — Høy (neste sprint)

5. **[B-02]** Kanseller `BaseWidget`-scope i `onDisabled()` og valider widget-ID-er
6. **[S-03]** Varsle bruker ved `EncryptedSharedPreferences`-korrupsjon i stedet for stille sletting
7. **[B-04]** Skill domene-tilstand fra UI-tilstand — flytt `saveStatus` ut av `ForwardingState`
8. **[A-03]** Flytt `getString()`-kall til Main-tråd i `NotificationMonitorService`
9. **[B-07]** Fiks TOCTOU race condition i `ForwardingStats.getTotalCountToday()`
10. **[S-05]** Erstatt `startActivity()` fra ViewModel med `SharedFlow<UiEvent>`-mønster

### Fase 3 — Medium (teknisk gjeld)

11. **[S-06]** Migrer `build.gradle.kts` til å bruke `libs.versions.toml` eller slett kataloget
12. **[B-09]** Fiks race condition i `EmailSender` rate limiter med `Mutex`
13. **[B-10]** Flytt `registerOnSharedPreferenceChangeListener()` til Main-tråd
14. **[B-14]** Skill mellom autentiseringsfeil og nettverksfeil i `EmailWorker`
15. **[S-07]** Oppgrader `security-crypto` fra alpha til stabil versjon
16. **[AR-03]** Vurder å introdusere Hilt for dependency injection

### Fase 4 — Lav (kodekvalitet)

17. Rydd opp i dead code (`libs.versions.toml`)
18. Standardiser logg-tags med `const val TAG`
19. Flytt UI-felt ut av domenemodeller
20. Fiks tester som tester seg selv i stedet for produksjonskode

---

*Rapport generert av Kilo Code — alle funn er verifisert direkte i kildekoden.*
