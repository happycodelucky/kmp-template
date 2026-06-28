<!--
  Thanks for contributing to __DISPLAY_NAME__! Fill in the sections below.
  The checklist mirrors CLAUDE.md's done-gate — it's meant to be self-verified,
  by a human or a coding agent, before requesting review.
-->

## Summary

<!-- What does this change and why? Link the issue it closes. -->

Closes #

## Type of change

- [ ] Bug fix (non-breaking)
- [ ] New feature (non-breaking)
- [ ] Breaking change (public API / behavior)
- [ ] Docs / tooling only

## Affected platforms

<!-- Check what this touches. Native bugs often hide on the JVM, so verify there. -->

- [ ] iOS
- [ ] macOS
- [ ] Android
- [ ] JVM
- [ ] commonMain (all targets)

## How it was verified

<!-- Describe the testing. Paste the relevant `mise run check` result or the
     new/changed tests. "It compiles" is not verification. -->

## Done-gate checklist

<!-- These mirror CLAUDE.md §10–§11. A change isn't done until they pass. -->

- [ ] `mise run check` passes (ktlint + detekt + every test target)
- [ ] New/changed logic has `commonTest` coverage (`runTest` virtual time, no `Thread.sleep`)
- [ ] Native + Android compile clean (`:src:compileKotlinMacosArm64` / `compileKotlinIosSimulatorArm64` / `compileAndroidMain`) — the JVM compile alone is not a sufficient gate
- [ ] Public API changes follow the Swift-interop rules (§7): sealed → exhaustive enum, `@Throws` replicated on every `actual` incl. `CancellationException`, no `kotlin.Result<T>` at the boundary
- [ ] New dependencies were sourced per §5 (official Kotlin → Google KMP → kmp-awesome), are stable, and were added to `gradle/libs.versions.toml` only
- [ ] Docs updated (`docs/` + KDoc) for any public API or behavior change
- [ ] No hard-rule violations (§12): no Compose MP, CocoaPods, `GlobalScope`, `!!` in production, `java.time` in common, `@Synchronized`/`volatile`, callback public APIs, EAP/RC/Beta deps

## AI assistance

<!-- Disclosure, not a gate. If a coding agent (Claude Code, etc.) authored or
     co-authored this change, note it so reviewers can calibrate. The checklist
     above still applies regardless of who wrote the code. -->

- [ ] This change was authored or co-authored by an AI agent
