# CLAUDE.md — __DISPLAY_NAME__ Project Guide

Kotlin Multiplatform library for iOS, macOS, Android, and JVM. This file is the
contract a contributor (human or agent) reads first. Start here, then
`gradle/libs.versions.toml`, then `.claude/lessons/LESSONS.md`.

`mise.toml` is the task contract — every build/test/lint/publish action is a
`mise run <task>`. Run `mise tasks` to see them.

## 1. Scope

> **TODO (fill this in):** describe what __DISPLAY_NAME__ does — the problem it
> solves, what's *in* the shared library and what's deliberately *not*.

- **Shared (in `:src`):** the library's business logic. Headless — no UI
  dependencies. Each platform app consumes the library's `StateFlow`/`SharedFlow`
  or suspend API.
- **Not shared:** UI. Platform apps under `apps/` have their own native UI.
- **Modules:** `:src` (the library) and `:src-testing` (public test fakes +
  helpers for consumers, e.g. a `FakeX` and a `withX { … }` helper).

## 2. Decisions (load-bearing)

Record non-obvious, load-bearing decisions in `.claude/lessons/LESSONS.md` as you
make them (terse, one line each), and reference them here.

## 3. Versions

Latest stable only — no EAP/RC/Beta on `main`. K2 only. Single source of truth:
`gradle/libs.versions.toml`. **Before bumping anything, web-search the latest
stable** (training data goes stale). The Kotlin pin is bounded above by SKIE — do
not bump Kotlin past SKIE's supported range; bump SKIE first. Gradle 9.5.x,
AGP 9.2.x, JVM target 21, JDK 21. mise pins the non-Gradle tools (JDK, gradle,
xcodegen, gh); the two must agree on the Kotlin/AGP/JDK story.

## 4. Targets & module layout

- Targets: `iosArm64`, `iosSimulatorArm64`, `macosArm64`, Android (arm64-v8a),
  and **`jvm()`** (the one target the ARM-only rule doesn't touch — serves
  desktop/server/Linux/Windows). No x86, no Intel Macs, no watchOS/tvOS.
- `applyDefaultHierarchyTemplate { common { group("apple") { withIos(); withMacos() } } }` —
  iOS+macOS coalesce into a shared `appleMain` intermediate. Don't hand-roll
  source-set wiring. Code in `appleMain` must compile on **both** iOS and macOS
  (use Foundation, not UIKit).
- Module shape lives in the `template.kmp-library` convention plugin
  (`gradle/plugins/`). Framework base name and namespace are DERIVED from the
  module name (`src` → framework `Src`, namespace `com.happycodelucky.src`).
  Adding a module = apply `template.kmp-library` + `template.publish`.
- Keep the `expect`/`actual` seam tiny; push logic into `commonMain`.

## 5. Libraries — Kotlin-first

kotlinx.* family (coroutines, atomicfu, io), **Kermit for logging** (wired into
every module by the convention plugin — `Logger` is available in `commonMain`),
`kotlin.time` for `Duration`/`Instant`/`Clock` (NOT `java.time` in common —
`kotlin.time.Instant`/`Clock` are stable since 2.3.x). For HTTP, prefer
Ktor/Ktorfit. Testing: `kotlin.test` + Turbine + `kotlinx-coroutines-test` +
Kotest (property tests). Library code uses **constructor injection only** — no
Koin/service locator inside `:src`.

**Finding a library.** Before writing platform glue or pulling a JVM-only / `expect`-`actual`-heavy
dependency, look for an existing multiplatform one — in this order:

1. **Official Kotlin / JetBrains** — the kotlinx.* family (coroutines,
   serialization, datetime, io, atomicfu) and Ktor. First-party, KMP-native, and
   what the rest of this guide assumes.
2. **Google official KMP libraries** — AndroidX/Jetpack artifacts that publish
   real multiplatform targets (e.g. Room KMP, DataStore, Lifecycle, Paging,
   Collections, Annotations). Prefer these over Android-only equivalents so the
   code stays in `commonMain`.
3. **The community catalog** — [terrakok/kmp-awesome](https://github.com/terrakok/kmp-awesome),
   a curated index of KMP libraries by category. Use it to discover an existing,
   maintained multiplatform option before rolling your own.

Vet any candidate against the rules here: it must publish the targets we ship
(Apple + Android + jvm), be **stable** (no EAP/RC/Beta — §3), not pull in
CocoaPods or Compose Multiplatform (§12), and not exceed SKIE's Kotlin range.
Add it to `gradle/libs.versions.toml` only (§11), web-searching the latest stable
first. When nothing suitable exists, keep the `expect`/`actual` seam tiny (§4).

## 6. Concurrency

- `kotlinx.coroutines` only. No `GlobalScope`.
- `Flow`/`StateFlow`/`SharedFlow` over callbacks. No callback APIs in common.
- Shared mutable state across suspend boundaries → `kotlinx.coroutines.sync.Mutex`.
  Non-suspending critical sections → `kotlinx.atomicfu.locks.synchronized`. Never
  `kotlin.synchronized`, `@Synchronized`, `java.util.concurrent.locks.*`,
  `volatile`.
- **Inject `Clock` + `CoroutineScope`** into time-driven code. Never read
  wall-clock in timer logic — it breaks `runTest` virtual time.
- A client that owns work should own a `SupervisorJob` *child* of the scope it's
  given; `close()` cancels only that child, never the caller's scope.

## 7. Swift interop

SKIE mandatory (convention plugin configures it; `produceDistributableFramework()`
in `:src`). `Flow`/`StateFlow` → `AsyncSequence`. Sealed types → exhaustive Swift
enums. **`@Throws` on an `expect` must be replicated verbatim on every `actual`**,
and a `@Throws` on a `suspend fun` must list `CancellationException`. Never
`kotlin.Result<T>` at the boundary. Apple casing everywhere in prose, file names,
and types (`iOS`, `macOS`) except JetBrains spellings (`iosArm64`, `withMacos()`).

## 8. Distribution

Two channels, non-overlapping:
- **Maven Central** (`template.publish` / vanniktech): Android AAR + jvm jar +
  KMP metadata + klibs. For Gradle/KMP consumers. `mise run publish:local`
  installs to `~/.m2`. `mise run publish:maven` releases to Central — bump with
  `--major`/`--minor`/`--patch` (most significant wins) or `--version X`;
  `--dryrun` stages only. A real run updates Package.swift, publishes, tags, and
  creates the GitHub release (after a typed confirmation). See `.github/PUBLISHING.md`.
- **GitHub Releases** (KMMBridge in `src/build.gradle.kts`): the SKIE-enhanced
  `Src.xcframework` for SPM consumers. Don't redeclare `XCFramework("Src")` —
  KMMBridge auto-creates it. CI-only publishing.

Real tagged releases go through `.github/workflows/release.yml` (computes the
version, dry-run by default). See `.github/PUBLISHING.md`.

**Public-API stability.** The committed dumps under `<module>/api/` are the
reference for the public surface, across every target. `mise run check` (and CI)
runs `api:check` and fails on any unintended change — so a breaking change to a
published library is always deliberate. After an *intentional* public-API change,
run `mise run api:dump` and commit the `api/` diff alongside the code; review it
like any other change. This is the single most important guard for a library:
it's what stops an accidental rename or removed function from breaking consumers.

## 9. Platform notes

Fill in as your library's platform needs become concrete. Common gotchas:
- **iOS:** capabilities like multicast networking need entitlements (Apple gates
  some behind a request form); plain-HTTP LAN access needs an ATS exception in
  the *host app's* Info.plist. The library can't set these.
- **Android:** networking may need a `WifiManager.MulticastLock` and permissions;
  the library manifest contributes permissions to consumers via Manifest Merger.
- **macOS:** a sandboxed app needs `network.client` (outbound) and/or
  `network.server` (bind/listen) entitlements; they apply only to a *signed* app.
- **JVM:** the architecture-neutral target; serves desktop/server.

## 10. Testing

All shared logic gets `commonTest` coverage with `runTest` virtual time (never
`Thread.sleep`). Turbine for Flow assertions; Kotest for property tests (use
multiplatform arbs only — `Arb.stringPattern` is JVM-only and breaks the native
test compile). Inject a test `Clock`/`TimeSource` reading the test scheduler so
`now` and `delay` stay in lockstep. The done gate is the full
`:src:check :src-testing:check` (`mise run check`) — which compiles *test*
sources for every target and runs detekt. A JVM-only run hides native-test-compile
and detekt failures.

## 11. Task workflow (mise)

1. Read this file, then `gradle/libs.versions.toml`, then `.claude/lessons/LESSONS.md`.
2. Adding a dependency? First hunt for an existing multiplatform one (§5:
   official Kotlin/JetBrains → Google official KMP → terrakok/kmp-awesome).
   Then web-search the latest stable; add to the catalog only.
   `mise run dependencies:outdated` lists candidates; `dependencies:update`
   rewrites the catalog (review the diff); `dependencies:analyze` flags unused or
   misdeclared deps (api vs implementation).
3. Platform-specific? Keep the `expect`/`actual` seam tiny; push logic to common.
4. Public API crossing to Swift? Apply §7 at design time.
5. Changed the public API on purpose? `mise run api:dump` and commit the `api/`
   diff (§8) — otherwise `check` fails on the surface change.
6. Done when `mise run check` passes AND `:src:compileKotlinMacosArm64` /
   `compileKotlinIosSimulatorArm64` / `compileAndroidMain` build clean (common-code
   bugs often only surface on Native — the JVM compile is not a sufficient gate).
   `check` also runs the API/ABI check (§8). `mise run build:doctor` surfaces
   build-health diagnostics if a build feels slow or misconfigured.
7. Learned something non-obvious? Add it to `.claude/lessons/LESSONS.md` (terse).

## 12. Hard rules

No Compose Multiplatform in the library. No CocoaPods. No x86/Intel Macs/watchOS/
tvOS. No `GlobalScope`, no `!!` in production, no `java.time` in common, no
`kotlin.synchronized`/`@Synchronized`/`volatile`. No callback-based public APIs.
No `kotlin.Result<T>` at the Swift boundary. No EAP/RC/Beta on `main`.
