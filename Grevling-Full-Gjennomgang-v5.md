# Grevling Appen — Full Kodegjennomgang v5
> **14/14 funn bekreftet mot faktisk kildekode ✅**  
> Leste filer: MainActivity.kt · MainScreen.kt · Theme.kt · SettingsScreen.kt  
> SettingsViewModel.kt · HomeViewModel.kt · NotificationMonitorService.kt  
> PreferencesRepository.kt · EmailSender.kt · AutoReplyHelper.kt  
> ForwardingState.kt · AppRepository.kt · ViewModelUtils.kt · proguard-rules.pro  
> Commit: `ce81a7b` — Dato: 2026-02-22 — Prinsipp: KISS + DRY

---

## Gjenstående fra v4-plan

### [K-1] `gmailPassword` — fjern fra `SettingsUiState` 🔴 KRITISK ✅ Verifisert

**Prioritet:** Høy | **Fil:** `SettingsViewModel.kt` | **Estimat:** ~10 min

#### Problemstilling
`updateGmailPassword()` kopierer passordet direkte inn i `SettingsUiState` via
`.copy(gmailPassword = password)`. `testEmail()` leser `state.gmailPassword` direkte.
`pendingGmailPassword` er deklarert som felt i ViewModel men **aldri lest**.

#### Løsning
```kotlin
// SettingsViewModel.kt
fun updateGmailPassword(password: String) {
    pendingGmailPassword = password          // nå faktisk brukt
    gmailPasswordInput.tryEmit(password)
    // IKKE lenger: _uiState.value = _uiState.value.copy(gmailPassword = password)
}

fun testEmail() {
    val passwordToUse = pendingGmailPassword
        .ifEmpty { repository.getGmailPassword() }
    viewModelScope.launch {
        val result = EmailSender.testEmailConfigWithParams(
            context = getApplication(),
            gmailAddress = _uiState.value.gmailAddress,
            gmailPassword = passwordToUse,   // aldri fra state
            recipientEmail = _uiState.value.recipientEmail
        )
        ...
    }
}
```
Fjern `gmailPassword: String? = null`-feltet fra `SettingsUiState`.

---

### [N-1] `NotificationListenerService` — bytt til `onListenerConnected()` ⚠️ VIKTIG ✅ Verifisert

**Prioritet:** Høy | **Filer:** `NotificationMonitorService.kt`, `PreferencesRepository.kt`, `AppsViewModel.kt` | **Estimat:** ~20 min

#### Problemstilling
`cachedMonitoredApps` er `emptySet()` når systemet starter levering av varsler fordi
`state.collect` startes asynkront i `onCreate()`. `setMonitoredApps(context)` kaller
`stopService/startService` som er systemstyrt og oppfører seg ulikt på tvers av produsenter.

#### Løsning
```kotlin
override fun onCreate() {
    super.onCreate()
    prefsRepo = PreferencesRepository.getInstance(applicationContext)
}

override fun onListenerConnected() {
    super.onListenerConnected()
    cachedMonitoredApps = prefsRepo.getMonitoredApps()
    scope.launch {
        prefsRepo.state.collect {
            cachedMonitoredApps = prefsRepo.getMonitoredApps()
        }
    }
}
```
Fjern `context`-parameteret fra `setMonitoredApps()` og `toggleApp()` i
`PreferencesRepository.kt` og oppdater kall-stedene i `AppsViewModel.kt`.

---

### [N-2] `setupDebounceSaveWithFlush` — slett overflødig collector 🟡 MEDIUM ✅ Verifisert

**Prioritet:** Medium | **Fil:** `ViewModelUtils.kt` | **Estimat:** ~5 min

#### Problemstilling
```kotlin
// ViewModelUtils.kt — to launch-blokker bekreftet i kode:
viewModelScope.launch {
    flow.debounce(debounceTimeMs).collect { value ->
        pendingValue = value   // ← dead code: kun pendingValue oppdateres
    }
}
viewModelScope.launch {
    flow.collect { value ->
        pendingValue = value   // ← gjør nøyaktig det samme, bare uten forsinkelse
    }
}
```
`saveAction` kalles **aldri** fra disse blokkene — kun fra `flush()`.
Debounce-collectoren er identisk med den umiddelbare, bare 200ms forsinket.

#### Løsning
Slett den øverste `launch`-blokken (debounce-collectoren). Behold den umiddelbare.

---

### [N-3] ProGuard — to ugyldige `dontwarn`-linjer 🟢 LAV ✅ Verifisert

**Prioritet:** Lav | **Fil:** `proguard-rules.pro` | **Estimat:** ~1 min

#### Problemstilling
```
-dontwarn jakarta.awt.**      ← finnes ikke (AWT er alltid java.awt, aldri jakarta.awt)
-dontwarn jakarta.security.** ← Jakarta Mail bruker javax.security.sasl, ikke jakarta.security
```
Begge er allerede dekket av korrekte linjer lenger ned i samme fil:
```
-dontwarn java.awt.Image
-dontwarn java.awt.Toolkit
-dontwarn javax.security.auth.callback.NameCallback
-dontwarn javax.security.sasl.*
```

#### Løsning
Slett de to linjene. Ingen funksjonell endring.

---

### [N-4] Edge-to-edge — appen dekker statuslinjen 🔴 KRITISK ✅ Verifisert (begge filer)

**Prioritet:** Høy | **Filer:** `MainActivity.kt`, `Theme.kt` | **Estimat:** ~10 min

#### Problemstilling
`enableEdgeToEdge()` finnes ikke i `MainActivity.onCreate()` (bekreftet).
`Theme.kt` setter `window.statusBarColor` manuelt inne i en `SideEffect`-blokk (bekreftet):

```kotlin
// Theme.kt — SideEffect-blokk, slik det er nå:
SideEffect {
    val window = (view.context as Activity).window
    window.statusBarColor = colorScheme.background.toArgb()  // ← konflikt med edge-to-edge
    WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
}
```

Med `targetSdk = 35` håndhever Android 15 edge-to-edge automatisk.
`statusBarColor` etter `enableEdgeToEdge()` overstyrer den transparente statuslinjen tilbake.

#### Løsning
```kotlin
// MainActivity.kt — legg til som aller første linje i onCreate():
import androidx.activity.enableEdgeToEdge

override fun onCreate(savedInstanceState: Bundle?) {
    enableEdgeToEdge()
    super.onCreate(savedInstanceState)
    ...
}

// Theme.kt — fjern kun statusBarColor-linjen, behold resten:
SideEffect {
    val window = (view.context as Activity).window
    // Fjern: window.statusBarColor = colorScheme.background.toArgb()
    WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
}
```

---

### [N-5] Legg tilbake "Utviklet av"-linjen over navigasjonsfeltet 🟢 LAV ✅ Verifisert

**Prioritet:** Lav | **Filer:** `MainScreen.kt`, `strings.xml` | **Estimat:** ~10 min

#### Problemstilling
`MainScreen.kt` bekreftet: `bottomBar` inneholder kun `NavigationBar { }` direkte,
ingen `Column`-wrapper, ingen "Utviklet av"-tekst noe sted.

#### Løsning
```xml
<!-- strings.xml -->
<string name="developed_by">Utviklet av Almin Colakovic</string>
```
```kotlin
// MainScreen.kt — bottomBar:
bottomBar = {
    Column {
        Text(
            text = stringResource(R.string.developed_by),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(vertical = 5.dp)
        )
        NavigationBar { /* uendret */ }
    }
}
```

---

### [N-6] Tillatelse-statuslinjer — grønn individuelt 🟡 MEDIUM ✅ Verifisert

**Prioritet:** Medium | **Fil:** `SettingsScreen.kt` | **Estimat:** ~10 min

#### Problemstilling (bekreftet i kode)
`buildString` sammenfatter alle tre statuslinjer til én streng, og én felles `color`-betingelse
bestemmer fargen på **alt på en gang**:

```kotlin
// SettingsScreen.kt — slik det er nå:
val statusText = buildString {
    if (uiState.hasNotificationAccess) append("✓ Varslingstilgang aktivert\n")
    else append("✗ Varslingstilgang mangler\n")
    if (uiState.hasAllPermissions) append("✓ SMS-tillatelser OK\n")
    else append("✗ SMS-tillatelser mangler\n")
    if (uiState.isIgnoringBatteryOptimizations) append("✓ Batteri OK")
    else append("✗ Batteri-optimalisering aktiv")
}
Text(
    text = statusText,
    color = if (hasNotification && hasAllPermissions && isIgnoringBattery)
        StatusSuccess else StatusWarning   // ← ALL tekst gul om ett punkt mangler
)
```

Problemet: Hvis kun batteri-optimalisering mangler, farges også "✓ Varslingstilgang aktivert"
og "✓ SMS-tillatelser OK" i gult — selv om de er i orden.

#### Løsning
```kotlin
// Tre separate Text-komponenter med individuell farge:
Text(
    text = stringResource(
        if (uiState.hasNotificationAccess) R.string.permissions_status_notification_ok
        else R.string.permissions_status_notification_missing
    ),
    color = if (uiState.hasNotificationAccess) StatusSuccess else StatusWarning,
    style = MaterialTheme.typography.bodyMedium
)
Text(
    text = stringResource(
        if (uiState.hasAllPermissions) R.string.permissions_status_sms_ok
        else R.string.permissions_status_sms_missing
    ),
    color = if (uiState.hasAllPermissions) StatusSuccess else StatusWarning,
    style = MaterialTheme.typography.bodyMedium
)
Text(
    text = stringResource(
        if (uiState.isIgnoringBatteryOptimizations) R.string.permissions_status_battery_ok
        else R.string.permissions_status_battery_missing
    ),
    color = if (uiState.isIgnoringBatteryOptimizations) StatusSuccess else StatusWarning,
    style = MaterialTheme.typography.bodyMedium
)
```

---

### [N-7] Auto-svar meldingsfelt — bytt fra debounce til Lagre-knapp 🔴 KRITISK ✅ Verifisert

**Prioritet:** Høy | **Filer:** `HomeViewModel.kt`, `HomeScreen.kt`, `ForwardingState.kt`, `strings.xml` | **Estimat:** ~30 min

#### Problemstilling (bekreftet i kode)
`AUTO_SAVE_DELAY_MS = 1000L` og `setupDebounceSave()` bekreftet for alle tre felt.
`collect`-blokken gjør `_state.value = repoState` uten guard — race condition bekreftet.
Tre konkrete feil:

1. Default settes til `""` og autosvar sendes aldri
2. Melding lagres halvveis ved rask navigering/appstenging
3. Race condition: `repository.state.collect` overskriver pågående tekstredigering

#### Løsning — `HomeViewModel.kt`
```kotlin
// Fjern: unifiedMessageInput, smsMessageInput, callMessageInput
// Fjern: setupDebounce(), AUTO_SAVE_DELAY_MS

init {
    viewModelScope.launch {
        repository.state.collect { repoState ->
            if (_state.value == ForwardingState()) {
                _state.value = repoState   // kun ved oppstart
            } else {
                _state.value = _state.value.copy(
                    isEnabled = repoState.isEnabled,
                    hasNotificationAccess = repoState.hasNotificationAccess,
                    hasEmailConfig = repoState.hasEmailConfig,
                    recipientEmail = repoState.recipientEmail,
                    autoReplyEnabled = repoState.autoReplyEnabled,
                    useSameMessage = repoState.useSameMessage,
                    statusMessage = repoState.statusMessage
                    // meldingsfelt berøres ikke etter oppstart
                )
            }
        }
    }
}

fun updateUnifiedMessage(message: String) { _state.value = _state.value.copy(unifiedMessage = message) }
fun updateSmsMessage(message: String) { _state.value = _state.value.copy(smsMessage = message) }
fun updateCallMessage(message: String) { _state.value = _state.value.copy(callMessage = message) }

fun saveMessages() {
    val app = getApplication<Application>()
    val unified = _state.value.unifiedMessage.ifBlank { app.getString(R.string.default_unified_message) }
    val sms = _state.value.smsMessage.ifBlank { app.getString(R.string.default_sms_message) }
    val call = _state.value.callMessage.ifBlank { app.getString(R.string.default_call_message) }
    repository.setUnifiedMessage(unified)
    repository.setSmsMessage(sms)
    repository.setCallMessage(call)
    _state.value = _state.value.copy(unifiedMessage = unified, smsMessage = sms, callMessage = call)
}
```

#### Løsning — `HomeScreen.kt`
Slett `SaveStatusIndicator`. Legg til hint og Lagre-knapp:

```kotlin
if (state.unifiedMessage.isBlank()) {
    Text(
        text = stringResource(R.string.message_empty_hint),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    )
}
// Etter siste felt:
GrevlingButton(
    text = stringResource(R.string.save_messages_button),
    onClick = { viewModel.saveMessages() },
    modifier = Modifier.fillMaxWidth()
)
```

#### Løsning — `strings.xml`
```xml
<string name="save_messages_button">Lagre meldinger</string>
<string name="message_empty_hint">Tom melding gjenopprettes til standardmelding ved lagring</string>
```

#### Løsning — `ForwardingState.kt`
```kotlin
// Fjern:
val saveStatusUnified: SaveStatus = SaveStatus.NONE,
val saveStatusSms: SaveStatus = SaveStatus.NONE,
val saveStatusCall: SaveStatus = SaveStatus.NONE,
```
Sjekk om `SaveStatus`-enumen brukes andre steder etter N-7 — slett den hvis ikke.

---

## Nye funn etter full gjennomgang

---

### [O-1] Dobbel deteksjon av tapte anrop — duplikat-email mulig 🔴 KRITISK ✅ Verifisert

**Prioritet:** Høy | **Fil:** `NotificationMonitorService.kt` | **Estimat:** ~25 min

#### Problemstilling
To uavhengige kodestier kan begge sende e-post for samme tapte anrop:

- **Sti 1** — `CATEGORY_MISSED_CALL` → `handleMissedCall(postTime)` → deduper på `postTime`
- **Sti 2** — `CATEGORY_CALL` forsvinner → `checkForMissedCall()` → deduper på `callLog.time`

`postTime` (notifikasjonstidspunkt) og `callLog.time` (CallLog-tidspunkt) er to ulike tall.
`processedMissedCalls`-settet treffer aldri på tvers av stiene → duplikat-email er mulig.

#### Løsning — behold kun sti 2 (CallLog)
```kotlin
// onNotificationPosted — fjern CATEGORY_MISSED_CALL-branchen:
when (cat) {
    Notification.CATEGORY_CALL -> activeCalls[pkg] = sbn.postTime
    // Fjern: Notification.CATEGORY_MISSED_CALL -> handleMissedCall(time)
    else -> handleAppNotification(pkg, notification, sbn.postTime)
}
// Fjern handleMissedCall()-funksjonen helt
```

---

### [O-2] `ForwardingState` er en God-object — splitt i to modeller 🟢 LAV ✅ Verifisert

**Prioritet:** Lav | **Filer:** `ForwardingState.kt`, `HomeViewModel.kt` | **Estimat:** ~30 min

#### Problemstilling
`ForwardingState` deles mellom fire lag og blander domenefelt med rene UI-felt
(`statusMessage`, `gmailAddress`, `saveStatusX`). `NotificationMonitorService`
importerer klassen men bruker kun `isEnabled` og `hasEmailConfig`.

Etter N-7 er situasjonen vesentlig bedre — `saveStatus`-feltene forsvinner.
Resten er teknisk gjeld som tas når koden ellers er stabil.

#### Løsning (etter N-7)
```kotlin
// ForwardingState.kt — ren domenemodell:
data class ForwardingState(
    val isEnabled: Boolean = false,
    val hasNotificationAccess: Boolean = false,
    val hasEmailConfig: Boolean = false,
    val autoReplyEnabled: Boolean = false,
    val useSameMessage: Boolean = true,
    val unifiedMessage: String = "",
    val smsMessage: String = "",
    val callMessage: String = ""
)

// HomeUiState.kt — ny fil:
data class HomeUiState(
    val forwardingState: ForwardingState = ForwardingState(),
    val recipientEmail: String = "",
    val statusMessage: String = ""
)
```

---

### [O-3] `AutoReplyHelper` og `EmailSender` omgår repository-singletonen 🟡 MEDIUM ✅ Verifisert

**Prioritet:** Medium | **Filer:** `AutoReplyHelper.kt`, `EmailSender.kt` | **Estimat:** ~15 min

#### Problemstilling
```kotlin
// Bekreftet i begge filer:
val prefs = EncryptedPrefsFactory.get(appContext)   // omgår PreferencesRepository
```
Funksjonelt ikke en bug i dag, men ved endring av storage-lag må tre steder oppdateres.

#### Løsning
La `NotificationMonitorService` hente verdier fra `prefsRepo.state.value` og sende
dem inn som parametere til `AutoReplyHelper` og `EmailSender`.

---

### [O-4] `ViewModelUtils.kt` — slett `setupDebounceSave` etter N-7 🟡 MEDIUM ✅ Verifisert

**Prioritet:** Medium | **Fil:** `ViewModelUtils.kt` | **Estimat:** ~5 min

#### Bekreftet bruk
| Funksjon | Brukes i | Etter N-7 |
|---|---|---|
| `setupDebounceSave` | `HomeViewModel` | Fjernet av N-7 → **SLETT** |
| `setupDebounceSaveWithFlush` | `SettingsViewModel` | Fortsatt nødvendig → **BEHOLD** |

`ViewModelUtils.kt` kan **ikke** slettes i sin helhet — kun `setupDebounceSave`-funksjonen.

---

### [N-8] `MainActivity.kt` — tre kosmetiske problemer 🟢 LAV ✅ Verifisert

**Prioritet:** Lav | **Fil:** `MainActivity.kt` | **Estimat:** ~10 min

Bekreftet i kode:
1. `runOnUiThread { }` er redundant i `onCreate()` — allerede på main thread
2. Hardkodede norske strenger i `AlertDialog.Builder` — burde bruke `getString(R.string.xxx)`
3. Magic string `"app_flags"` brukes direkte to steder — burde være en konstant

#### Løsning
```kotlin
companion object {
    private const val PREFS_FLAGS = "app_flags"
    private const val KEY_PREFS_RESET = "prefs_were_reset"
}

// Fjern runOnUiThread { } — kall AlertDialog.Builder direkte
// Erstatt hardkodede strenger med getString(R.string.dialog_prefs_reset_title) etc.
```

---

## Over-engineering — samlet vurdering

### Hva som er riktig arkitektur ✅
- WorkManager for email — riktig for retry og bakgrunnskjøring
- Rate-limiting i `EmailSender` — nødvendig, godt implementert
- Cooldown i `AutoReplyHelper` — nødvendig for å unngå spam
- `EncryptedSharedPreferences` — riktig for sensitiv data
- Singleton `PreferencesRepository` — unngår dupliserte coroutine scopes
- `StateFlow` for reaktiv UI — korrekt Compose-mønster
- `@Volatile cachedMonitoredApps` — riktig for thread-safe lesing i service
- `AppRepository.kt` — riktig separasjon, kun PackageManager-ansvar

### Hva som er over-engineered

| Område | Problem | Konsekvens |
|---|---|---|
| [O-1] Dobbel missed-call-deteksjon | To stier, ulik dedup-nøkkel | Duplikat-epost — reell bug |
| [O-2] `ForwardingState` God-object | Blander domene + UI | Vokser ukontrollert |
| [O-3] Prefs-lesing i hjelpere | Omgår repository | Tre steder å oppdatere |
| [O-4] `setupDebounceSave` | Abstraksjon av 4 linjer, ubrukt etter N-7 | Slett kun denne funksjonen |

---

## Anbefalt implementeringsrekkefølge

| # | Fiks | Fil(er) | Estimat | Prioritet |
|---|---|---|---|---|
| 1 | **[N-4]** Edge-to-edge / statuslinje | `MainActivity.kt`, `Theme.kt` | ~10 min | 🔴 Kritisk |
| 2 | **[N-7]** Bytt debounce til Lagre-knapp | `HomeViewModel.kt`, `HomeScreen.kt`, `ForwardingState.kt`, `strings.xml` | ~30 min | 🔴 Kritisk |
| 3 | **[K-1]** `gmailPassword` ut av state | `SettingsViewModel.kt` | ~10 min | 🔴 Kritisk |
| 4 | **[O-1]** Fjern dobbel missed-call-deteksjon | `NotificationMonitorService.kt` | ~25 min | 🔴 Kritisk |
| 5 | **[N-1]** `onListenerConnected()` + fjern service restart | `NotificationMonitorService.kt`, `PreferencesRepository.kt`, `AppsViewModel.kt` | ~20 min | ⚠️ Viktig |
| 6 | **[N-6]** Individuelle tillatelsesfarger | `SettingsScreen.kt` | ~10 min | 🟡 Medium |
| 7 | **[O-3]** Injiser credentials i hjelpere | `AutoReplyHelper.kt`, `EmailSender.kt` | ~15 min | 🟡 Medium |
| 8 | **[N-2]** Slett overflødig debounce-collector | `ViewModelUtils.kt` | ~5 min | 🟡 Medium |
| 9 | **[O-4]** Slett `setupDebounceSave` (ikke hele filen) | `ViewModelUtils.kt` | ~5 min | 🟡 Medium |
| 10 | **[N-8]** MainActivity.kt kosmetikk | `MainActivity.kt`, `strings.xml` | ~10 min | 🟢 Lav |
| 11 | **[N-5]** "Utviklet av"-linje i nav | `MainScreen.kt`, `strings.xml` | ~10 min | 🟢 Lav |
| 12 | **[N-3]** Slett ugyldige ProGuard-linjer | `proguard-rules.pro` | ~1 min | 🟢 Lav |
| 13 | **[O-2]** Splitt `ForwardingState` i domene + UI | `ForwardingState.kt`, `HomeViewModel.kt` | ~30 min | 🟢 Lav |

**Total estimert tid: ~3 timer 1 min**

---

*Full Kodegjennomgang v5 (endelig) — **14/14 funn bekreftet mot faktisk kildekode ✅***  
*Aliman00/grevling — 2026-02-22*
