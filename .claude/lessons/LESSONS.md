# LESSONS — __DISPLAY_NAME__

Terse, append-only log of non-obvious things learned while building this library.
One line each. Prefix with a category + number so CLAUDE.md and code comments can
reference them.

- **D-NNN** — Decisions (load-bearing architecture choices).
- **B-NNN** — Bugs / gotchas (the thing that bit, and the fix).
- **N-NNN** — Notes (build-system / toolchain quirks).

The entries below are inherited from the template — they describe the shared
toolchain every project built from it gets, so they apply here too. Add your
project's own lessons underneath. The reasoning behind them, plus a repeatable
audit procedure, is in [`toolchain-audit.md`](toolchain-audit.md).

## Toolchain (inherited)

- **D-001** — dependency-analysis excludes only what it structurally *cannot* see (deliberate `api` re-exports, convention-plugin-injected baselines); genuinely-unused scaffolding stays reported, because that advice is correct and self-clears.
- **D-002** — The compiler bump ships in its own commit: `allWarningsAsErrors` turns any new Kotlin warning into a build failure, so bundling it makes a bisect useless.
- **B-001** — Renovate removed `matchPackagePrefixes` in v38; worse, prefix matching has no boundary, so `org.jetbrains.kotlin` also silences `org.jetbrains.kotlinx` — and the auto-migration preserves the over-match. Use an anchored regex with a `[.:]` boundary.
- **B-002** — Gradle Doctor reports nothing under `--dry-run`: it instruments task *execution*, so a no-op build reads identically to a clean bill of health.
- **B-003** — A `rejectVersionIf` whitelist of `^[0-9][0-9.]*$` already excludes every pre-release qualifier; a second "looks like a pre-release?" regex after it is unreachable, and invites someone to loosen the wrong half later.
- **N-001** — SKIE bounds the Kotlin pin. Confirm SKIE's changelog *names* the target Kotlin version before the paired bump; its per-Kotlin runtime artifact naming is not a reliable support signal.
- **N-002** — `check` really does depend on `checkKotlinAbi`, `detekt`, and all four test targets — verified from the `--dry-run` task graph, not assumed. Re-verify rather than trusting the comment.
- **N-003** — dependency-analysis is structurally noisy on hierarchical KMP source sets (`usedTransitiveDependencies` always misfires), but its other categories do get fixed upstream — re-test them periodically instead of leaving blanket ignores.
- **N-004** — DAGP's `AGP_MAX` lags the AGP release train, so current AGP prints "proceed at your own risk". Advisory, not broken — verify output equivalence and record the choice.
- **N-005** — Bump Gradle with `./gradlew wrapper --gradle-version X`, never by editing the properties file; the `mise.toml` pin and the wrapper must move in the same commit (Renovate groups them).
- **N-006** — The ben-manes versions plugin id is `io.github.ben-manes.versions`; the old `com.github.*` id still resolves but warns on every configuration. The Java package is unchanged, so imports stay put.
- **N-007** — `check` does not cover framework linking. After a Kotlin or SKIE bump, also run `build:xcframework` — Swift-facing output like `.swiftinterface` emission can regress on its own.
