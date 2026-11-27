---
title: Bump Kotlin, AGP, Gradle and the build toolchain to latest stable
change: patch
description: "Dependency and toolchain refresh: no public API change."
---

Routine toolchain refresh — no public API change, no required consumer action.

- Kotlin 2.4.10 → 2.4.21 (highest version SKIE 0.10.16 supports; Kotlin 2.5.0 is
  still RC-only and held per CLAUDE.md §3)
- SKIE 0.10.14 → 0.10.16, AGP 9.4.1 → 9.5.3, Gradle 9.7.1 → 9.9.2
- Dokka 2.2.0 → 2.3.0 (fixes CVE-2026-84939, a path-traversal vulnerability)
- dependency-analysis 3.19.2 → 3.20.4, ben-manes-versions 0.64.0 → 0.67.0,
  version-catalog-update 1.1.1 → 1.1.2, Kotest 6.2.5 → 6.2.7 (restores Gradle
  9.9.x compatibility), androidx-lifecycle-runtime-compose 2.11.0 → 2.11.1
- `api:check` now also covers the Android target's public API (Kotlin 2.4.20,
  KT-85950) — closes a previously review-only gap; no surface changed
- CI: `jdx/mise-action` v4 → v5 (v5.0.1 fixes a supply-chain issue where the
  action could run a cached binary before verifying it)
- Docs toolchain: mkdocs-material 9.7.7 → 9.8.2 (fixes GHSA-wxw8-4jgj-q85j),
  pymdown-extensions 12.1 → 13.1.2, mkdocs-llmstxt 0.5.0 → 0.5.1

Held: Kotlin 2.5.0 (RC-only), the Compose BOM (2026.11.01 would force a
compileSdk 37.1 floor for the Android sample app), detekt 1.23.8 (2.x is still
alpha), ktlint/ktlint-gradle, KMMBridge, vanniktech maven-publish (already
latest stable).
