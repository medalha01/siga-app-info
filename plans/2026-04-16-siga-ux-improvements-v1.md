# SIGA App - UX & Best Practices Improvement Plan

## Objective

Transform the SIGA app from a minimal WebView wrapper into a polished, secure, and user-friendly Android application by addressing critical gaps in architecture, security, UX patterns, accessibility, and performance. The app currently functions as a single-Activity WebView shell with a tenant selection screen, but lacks nearly every modern Android best practice.

---

## Current State Summary

The app is a **WebView-based wrapper** for `sigastr.com.br`. Users enter a tenant name, the app calls an API to resolve the tenant ID, then loads the web app. Key observations:

- **Single file architecture**: All logic lives in `MainActivity.kt` (192 lines)
- **No ViewModel, no Repository, no DI**: Zero separation of concerns
- **Hardcoded strings & colors**: No use of resource system for theming/localization
- **Dark theme is broken**: Layout uses hardcoded `#FFFFFF`/`#000000` despite DayNight theme
- **Security concerns**: Cleartext traffic enabled, mixed content allowed, unencrypted SharedPreferences, no input sanitization
- **No offline handling**: No connectivity check, no cached state, no graceful degradation
- **No navigation framework**: Visibility toggling between views instead of proper navigation
- **Zero accessibility**: No content descriptions, no talkback support
- **No animations/transitions**: Abrupt view switching
- **No tests**: Only boilerplate example test

---

## Implementation Plan

### Phase 1: Critical UX Fixes (High Impact, Immediate User Benefit)

- [ ] **1.1 Fix dark theme support** — Replace all hardcoded colors (`#FFFFFF`, `#000000`, `#00C291`) in `activity_main.xml:8,28,31,38,39,46,59,61,80` with semantic color resources (`?attr/colorSurface`, `?attr/colorOnSurface`, `colorPrimary`, etc.) so the app renders correctly in both light and dark mode. The theme already inherits `Theme.Material3.DayNight.NoActionBar` but the layout completely overrides it.

- [ ] **1.2 Add network connectivity check before API call** — Currently `buscarIdEIniciarSistema()` at `MainActivity.kt:111` only reacts to exceptions. Add a proactive connectivity check using `ConnectivityManager` / `NetworkCapabilities` before making the API call, and show a clear, actionable offline message (not just a Toast) so users understand *why* the action failed.

- [ ] **1.3 Replace Toast messages with Snackbar** — Toasts are ephemeral and easily missed. Replace the three Toast calls (`MainActivity.kt:106,137,139,144`) with Snackbar from Material Components, which is more visible, can include actions (e.g., "Retry"), and respects the current theme.

- [ ] **1.4 Add IME action on input field** — The `TextInputEditText` at `activity_main.xml:42-48` uses default IME action. Set `android:imeOptions="actionGo"` and handle the editor action to submit the form from the keyboard, so users don't need to tap the button.

- [ ] **1.5 Add WebView loading progress** — Replace the indeterminate `ProgressBar` with a `LinearProgressIndicator` (Material) that shows actual page load progress using `WebChromeClient.onProgressChanged()`. This gives users meaningful feedback about how far a page has loaded.

- [ ] **1.6 Add confirmation before logout** — Pressing back on the WebView (`MainActivity.kt:173-180`) immediately clears SharedPreferences and returns to login with no confirmation. Add an AlertDialog confirming the user wants to disconnect, preventing accidental logouts.

- [ ] **1.7 Add smooth transitions between login and WebView** — Currently visibility is toggled abruptly with `GONE`/`VISIBLE`. Use `Fade` or `Slide` transitions (Material Motion) for a polished feel when switching between the login screen and the WebView.

### Phase 2: Security Hardening

- [ ] **2.1 Disable cleartext traffic** — Remove `android:usesCleartextTraffic="true"` from `AndroidManifest.xml:8`. The API and web app both use HTTPS (`https://api.sigastr.com.br`, `https://app.sigastr.com.br`), so cleartext is unnecessary and exposes users to MITM attacks.

- [ ] **2.2 Remove `MIXED_CONTENT_ALWAYS_ALLOW`** — At `MainActivity.kt:78`, this setting allows insecure HTTP content within an HTTPS page. Change to `MIXED_CONTENT_NEVER_ALLOW` to enforce secure content loading.

- [ ] **2.3 Use EncryptedSharedPreferences** — At `MainActivity.kt:41`, tenant credentials are stored in plain SharedPreferences. Migrate to `androidx.security:security-crypto` EncryptedSharedPreferences so the tenant name and ID are encrypted at rest.

- [ ] **2.4 Sanitize tenant input** — The input at `MainActivity.kt:101` is used directly in a URL path. Add input validation (alphanumeric only, length limits) to prevent injection or unexpected URL construction.

- [ ] **2.5 Enable R8/ProGuard minification** — At `app/build.gradle.kts:25`, `isMinifyEnabled = false` in the release build. Enable it with proper ProGuard rules to reduce APK size and obfuscate code as a basic security measure.

### Phase 3: Architecture & State Management

- [ ] **3.1 Extract a ViewModel** — Move all business logic (API call, SharedPreferences read/write, WebView URL construction) from `MainActivity` into a `ViewModel`. The Activity should only handle UI rendering and user interaction delegation. This survives configuration changes and separates concerns.

- [ ] **3.2 Create a Repository layer** — Encapsulate the API call (`MainActivity.kt:111-147`) and SharedPreferences operations behind a Repository interface. This makes the data source swappable and testable.

- [ ] **3.3 Use Kotlin coroutines properly with a proper HTTP client** — Replace `URL(apiUrl).readText()` at `MainActivity.kt:118` (which is a blocking call on IO, no timeout handling, no proper error categorization) with OkHttp or Retrofit. This provides: configurable timeouts, proper error codes, retry logic, interceptors, and typed responses.

- [ ] **3.4 Use sealed class for UI state** — Replace the ad-hoc visibility toggling with a sealed class (`Idle`, `Loading`, `WebViewLoaded`, `Error`) observed via LiveData or StateFlow. This makes state transitions explicit and impossible to get into inconsistent states.

- [ ] **3.5 Add DataStore instead of SharedPreferences** — Replace `SharedPreferences` with Jetpack DataStore (Preferences DataStore) for a coroutine-friendly, non-blocking, type-safe preference storage.

### Phase 4: WebView UX Enhancements

- [ ] **4.1 Add pull-to-refresh on WebView** — Wrap the WebView in a `SwipeRefreshLayout` so users can refresh the current page with a familiar gesture.

- [ ] **4.2 Handle file downloads in WebView** — Implement `WebViewClient`'s download handling or set a `DownloadListener` so that when the web app triggers a file download, it actually works instead of silently failing.

- [ ] **4.3 Handle file uploads in WebView** — Add a `WebChromeClient` with `onShowFileChooser()` implementation so that if the web app has file upload inputs (e.g., photo upload), they work through the native file picker.

- [ ] **4.4 Add WebView cache control for offline** — Configure `WebSettings.cacheMode` to use `LOAD_CACHE_ELSE_NETWORK` when offline, so previously loaded pages are still accessible without connectivity.

- [ ] **4.5 Handle WebView errors gracefully** — Override `onReceivedError()` and `onReceivedHttpError()` in the WebViewClient to show a custom error page (not just the default browser error), with a retry button.

- [ ] **4.6 Add JavaScript interface for native features** (optional) — If the web app supports it, add a `@JavascriptInterface` bridge to access native features like camera, notifications, or biometric auth, making the hybrid experience feel more native.

### Phase 5: Polish & Accessibility

- [ ] **5.1 Extract all hardcoded strings to strings.xml** — Move "Acesso ao Sistema", "Nome do ambiente (ex: galaxy)", "Entrar", and all error messages from `activity_main.xml:29,37,57` and `MainActivity.kt:106,137,139,144` into `res/values/strings.xml`.

- [ ] **5.2 Define semantic color resources** — Create `colors.xml` entries for brand colors (`siga_green` = `#00C291`, etc.) and reference them via the theme. This ensures consistency and makes re-theming trivial.

- [ ] **5.3 Add content descriptions for accessibility** — The `ImageView` logo at `activity_main.xml:18-23` has no `android:contentDescription`. Add one. Ensure the WebView has appropriate announcements for screen readers.

- [ ] **5.4 Add a proper splash screen** — Use the Splash Screen API (`androidx.core:core-splashscreen`) to show a branded splash while the app initializes, instead of a blank white screen.

- [ ] **5.5 Add "switch environment" option in WebView** — Instead of requiring users to press back (which logs them out), add a subtle UI element (e.g., a menu or toolbar) while in the WebView that allows switching tenants without full logout.

- [ ] **5.6 Add keyboard avoidance** — When the software keyboard appears in the WebView or login screen, ensure content scrolls or resizes properly. Set `android:windowSoftInputMode="adjustResize"` in the manifest if not already default.

### Phase 6: Build & Performance

- [ ] **6.1 Enable build parallelization** — Uncomment `org.gradle.parallel=true` in `gradle.properties:13` for faster builds.

- [ ] **6.2 Update Java compatibility to 17** — At `app/build.gradle.kts:33-34`, update from `JavaVersion.VERSION_11` to `JavaVersion.VERSION_17` for better language feature support and desugaring.

- [ ] **6.3 Add baseline profile** — Create a baseline profile module to improve startup time by pre-compiling critical code paths. This is especially valuable for a WebView app where initial load matters.

- [ ] **6.4 Configure proper backup rules** — The `backup_rules.xml` and `data_extraction_rules.xml` are still templates. Exclude `SharedPreferences` (which contains tenant data) from auto-backup to prevent credential leakage across devices.

- [ ] **6.5 Add leak detection in debug builds** — Add `leakcanary` as a `debugImplementation` dependency to catch memory leaks, especially important with WebView which is notoriously leaky.

---

## Verification Criteria

- [ ] App renders correctly in both light and dark mode with no hardcoded color artifacts
- [ ] Offline state is detected proactively and communicated clearly to the user
- [ ] All error feedback uses Snackbar (not Toast) and is actionable
- [ ] Back navigation from WebView shows a confirmation dialog before logout
- [ ] Tenant input is validated before API call
- [ ] Release build has R8 minification enabled
- [ ] No cleartext traffic is allowed
- [ ] SharedPreferences data is encrypted at rest
- [ ] ViewModel holds all UI state; Activity is a passive view
- [ ] WebView supports pull-to-refresh, file downloads, and graceful error pages
- [ ] All user-facing strings are in strings.xml
- [ ] Content descriptions are present on all interactive/icon elements
- [ ] Splash screen displays on cold start

## Potential Risks and Mitigations

1. **WebView behavior changes may break the web app flow** — Mitigation: Test each WebView enhancement incrementally; keep the `shouldOverrideUrlLoading` logic unchanged initially and add features one at a time.

2. **EncryptedSharedPreferences migration may log out existing users** — Mitigation: Implement a migration strategy that reads from plain SharedPreferences on first launch after update, writes to EncryptedSharedPreferences, then deletes the old data.

3. **R8 minification may break WebView JavaScript interfaces or reflection** — Mitigation: Add ProGuard keep rules for any JavaScript interfaces and WebView-related classes before enabling minification.

4. **Architecture refactoring is a big change for a small codebase** — Mitigation: Introduce ViewModel + Repository incrementally; start by extracting just the API call and SharedPreferences logic, then iterate.

5. **Disabling mixed content may break web app resources loaded over HTTP** — Mitigation: Test thoroughly; if the web app loads some HTTP resources, coordinate with the web team to serve everything over HTTPS rather than weakening security.

## Alternative Approaches

1. **TWA (Trusted Web Activity) instead of WebView** — If the web app is a PWA, consider replacing the entire WebView approach with a Trusted Web Activity. This gives Chrome's rendering engine (better performance, auto-updated), but loses fine-grained control over the web content and requires the site to pass digital asset link verification.

2. **Jetpack Compose migration** — Instead of improving the XML-based UI, migrate to Jetpack Compose for a modern declarative UI. Trade-off: larger initial effort but much better maintainability and animation support long-term. Given the app's simplicity, this could be done in a single sprint.

3. **Multi-module architecture** — Instead of keeping everything in `:app`, split into `:core:data`, `:core:ui`, `:feature:login`, `:feature:webview` modules. Trade-off: overkill for the current app size, but valuable if the app will grow to include native features alongside the WebView.
