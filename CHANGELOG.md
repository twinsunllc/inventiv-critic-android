# Changelog

All notable changes to the Inventiv Critic Android SDK are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [2.0.0] - 2026-03-30

### Breaking Changes

- **Kotlin rewrite**: The entire SDK has been rewritten from Java to Kotlin. Java interop is
  preserved but the public API surface is now idiomatic Kotlin.
- **v3 API migration**: All network calls now target the Critic v3 REST API. The v2 API is no
  longer supported.
- **`baseUrl` renamed to `host`**: `CriticConfig` (and related initialization helpers) now use
  `host` instead of `baseUrl`. Pass only the hostname (e.g. `"critic.example.com"`) — the SDK
  constructs the full URL automatically.
- **GET endpoints removed**: `listBugReports`, `getBugReport`, and `listDevices` have been
  removed from the client SDK. The Critic v3 API surfaces these through the server-side dashboard
  only.
- **Build system modernized**: The project now requires Gradle 8+ and Java 17. Older toolchains
  are no longer supported.

### Added

- Full Kotlin rewrite of core library targeting the Critic v3 API (`CriticApi`, `ApiClient`,
  `CriticInterceptor`, and all model classes).
- `memory_active` field added to `DeviceStatus`, populated from `/proc/meminfo` on the device.
- Example Android application (`example/`) demonstrating SDK initialization, shake-to-report,
  bug report submission, and screenshot attachment.
- Comprehensive unit and integration test suite using JUnit 4, OkHttp `MockWebServer`, and
  `kotlinx-coroutines-test`.
- Nightly security CI: OWASP Dependency-Check, GitHub Actions security audit, and a 7-day
  package-quarantine check that blocks newly published Maven Central dependencies.
- AndroidX migration — all `android.support` imports replaced with `androidx` equivalents.
- ProGuard consumer rules (`proguard-rules.pro`) bundled with the AAR.
- `sourcesJar` and `javadocJar` Gradle tasks for publishing source and Javadoc artifacts.
- Maven publishing block in `library/build.gradle.kts` with Sonatype OSSRH support for
  Maven Central distribution.
- GitHub Actions release workflow (`.github/workflows/release.yml`) triggered on `v*.*.*` tag
  push or manual dispatch: builds, tests, creates a GitHub Release with changelog notes,
  publishes to Maven Central, and attaches AAR artifacts.

### Changed

- Log filename standardized to `console-logs.txt` across all log capture paths (was previously
  inconsistent, e.g. `logcat.txt`, `logs.txt`).
- Log format normalized: each line now has a consistent timestamp prefix.
- `BugReport` attachment model: the `url` field name corrected to match the v3 API response.
- `AppInstall`, `Device`, `DeviceStatus`, `PingRequest` models updated for v3 API field names
  and serialization.
- Metadata JSON parsing updated to handle v3 response envelope format.
- Example app: `HIGH_SAMPLING_RATE_SENSORS` permission added; shake detection initialization
  guarded to prevent crashes on devices without accelerometers.

### Fixed

- Remaining `kotlin.assert()` calls replaced with JUnit `assertEquals`/`assertNotNull` in all
  test classes (Kotlin's built-in `assert` is a no-op in release builds).
- `parseProcMeminfoValue` partial-key guard: prevented false positive matches where a key like
  `MemFree` would match queries for `Mem`.
- Base URL normalization: host-only strings (without scheme) are now correctly expanded to
  `https://` before being passed to Retrofit.
- Status area in example app uses `TextView` instead of `EditText` to prevent keyboard focus
  stealing on tap.

### Removed

- Java source files — the library is now 100% Kotlin.
- `listBugReports`, `getBugReport`, `listDevices` endpoints (v2 API, client-side GET access).
- Hard-coded test credentials and localhost URLs removed from all committed files.
- Build artifacts removed from tracking (added to `.gitignore`).

## [1.0.4] - 2019-04-01

### Changed

- Dependency updates and minor maintenance.

## [1.0.3] - 2019-04-01

### Changed

- Dependency updates and minor maintenance.

## [1.0.2] - 2018-11-08

### Changed

- Dependency updates.

## [1.0.1] - 2018-03-22

### Added

- Initial stable release of the Inventiv Critic Android SDK (Java).
- `ReportCreator` builder pattern for simplified bug report creation.
- Shake-to-report trigger via the Seismic library.
- Distribution via `repo.inventiv.io` Maven repository.

## [1.0.0] - 2018-03-01

### Added

- Initial release of the Inventiv Critic Android SDK.
- Basic bug report submission against the Critic v2 API.
- Retrofit 2 networking with Gson serialization.

[Unreleased]: https://github.com/twinsunllc/inventiv-critic-android/compare/v2.0.0...HEAD
[2.0.0]: https://github.com/twinsunllc/inventiv-critic-android/compare/1.0.4...v2.0.0
[1.0.4]: https://github.com/twinsunllc/inventiv-critic-android/compare/1.0.3...1.0.4
[1.0.3]: https://github.com/twinsunllc/inventiv-critic-android/compare/1.0.2...1.0.3
[1.0.2]: https://github.com/twinsunllc/inventiv-critic-android/compare/1.0.1...1.0.2
[1.0.1]: https://github.com/twinsunllc/inventiv-critic-android/compare/1.0.0...1.0.1
[1.0.0]: https://github.com/twinsunllc/inventiv-critic-android/releases/tag/1.0.0
