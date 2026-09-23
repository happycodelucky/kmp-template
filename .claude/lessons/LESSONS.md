# LESSONS — __DISPLAY_NAME__

Terse, append-only log of non-obvious things learned while building this library.
One line each. Prefix with a category + number so CLAUDE.md and code comments can
reference them.

- **D-NNN** — Decisions (load-bearing architecture choices).
- **B-NNN** — Bugs / gotchas (the thing that bit, and the fix).
- **N-NNN** — Notes (build-system / toolchain quirks).

- **N-001** — version-catalog-update ≥ 1.0 does NOT read the ben-manes report; it resolves versions itself with its own (different) stability rule. The root build passes it the shared `stableVersion` predicate, `pin`s `kotlin`, `keep`s findVersion-only keys, and disables `sortByKey`. It still strips blank lines and end-of-line comments.
- **N-002** — Build JDK stays 21: detekt 1.23.8's embedded Kotlin 2.0.21 compiler crashes on JDK 25 (`IllegalArgumentException: 25`, detekt/detekt#8714; fixed only in 2.x alphas). Revisit when detekt 2.0 is stable.
- **N-003** — KGP 2.4.10 is only *fully* supported up to Gradle 9.5.0 / AGP 9.1.0; the build runs ahead (Gradle 9.7.1, AGP 9.4.1 — which itself requires Gradle ≥ 9.6.0), verified by the full check. Closes when SKIE unlocks Kotlin 2.4.20.
- **N-004** — A git worktree doesn't carry the gitignored `local.properties`; AGP fails at task-graph time ("SDK location not found") — copy it from `local.properties.example`.
- **N-005** — AGP's KMP Android target (`KotlinMultiplatformAndroidLibraryTargetImpl`) is a `DecoratedExternalKotlinTarget`, NOT a `KotlinJvmTarget` — `targets.withType<KotlinJvmTarget>()` never reaches it, and unset its `jvmTarget` follows the JDK running the build. Set `jvmTarget` explicitly on `android { compilerOptions {} }` AND `jvm { compilerOptions {} }` (convention plugin reads the catalog's `jvm-target`).
- **N-006** — AndroidX AARs carry `minCompileSdk` in `META-INF/com/android/build/gradle/aar-metadata.properties`; AGP's `checkAarMetadata` enforces it. A library bump can force a compileSdk bump (Compose 1.12 / Lifecycle 2.11 → 37). `mise run check` never builds `:androidApp` — `mise run build:samples` does, and CI's fast leg runs it (a broken sample reached `main` before that).
- **N-007** — Kotlin 2.4 idioms verified against the WHOLE toolchain (compiler on jvm/android/iOS/macOS with allWarningsAsErrors, ktlint 1.8.0, detekt 1.23.8): explicit backing fields, context parameters, `Uuid.random()` (no opt-in), `when` guards, `$$` strings. An explicit-backing-field `StateFlow` exports to ObjC as a read-only `StateFlow` and via SKIE as `SkieKotlinStateFlow` — the mutable field is invisible.
- **N-008** — A custom `applyDefaultHierarchyTemplate { common { group("apple") { withIos(); withMacos() } } }` puts targets DIRECTLY under appleMain — there is no iosMain/macosMain. The implicit default template has them (native → apple → ios/macos). Its lambda form is still `@ExperimentalKotlinGradlePluginApi` in 2.4.x; `compilerOptions {}` and the AGP `android {}` block need no opt-in.
- **N-009** — Gradle 10 blockers are plugin-side. gradle-doctor 0.12.1 (`Project.getProperties`, runningcode/gradle-doctor#493; no release since 2025-08) was REMOVED — its JDK checks were disabled anyway (mise + gradle-daemon-jvm.properties own the JDK); `mise run build:profile` replaces `build:doctor`. Remaining: detekt 1.23.8 (`ReportingExtension.file(String)`), fixed in detekt 2.x. Check `build/reports/problems/` after Gradle bumps.
