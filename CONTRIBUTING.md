# Contributing to __DISPLAY_NAME__

Thanks for contributing. This repo uses [mise](https://mise.jdx.dev) as the task
contract — every action is a `mise run <task>`.

## Setup

```bash
brew install mise
mise trust
mise install            # provisions JDK, Gradle, xcodegen, gh
cp local.properties.example local.properties   # point sdk.dir at your Android SDK
```

Xcode is not managed by mise — install a recent Xcode that SKIE supports.

## Workflow

1. Read [`CLAUDE.md`](CLAUDE.md), then `gradle/libs.versions.toml`, then
   `.claude/lessons/LESSONS.md`.
2. Keep the `expect`/`actual` seam tiny; push logic into `commonMain`.
3. Public API that crosses to Swift? Apply CLAUDE.md §7 (SKIE) at design time.
4. Adding a dependency? Web-search the latest stable and add it to
   `gradle/libs.versions.toml` only. `mise run dependencies:outdated` helps.

## The done gate

```bash
mise run check
```

`check` runs ktlint + detekt + every unit-test target (iOS simulator, macOS,
Android host, JVM). A JVM-only run hides native-test-compile and detekt
failures, so `check` — not `test:jvm` — is the gate. Format first if ktlint
complains:

```bash
mise run format
```

## Commits & PRs

- Keep commits focused; explain *why* in the body when it isn't obvious.
- CI runs the same `mise run check` + `mise run build:xcframework`. Green CI is
  required to merge.
- Learned something non-obvious? Add a terse line to
  `.claude/lessons/LESSONS.md`.

## Releases

Releases are CI-driven via `.github/workflows/release.yml` (computes the version,
dry-run by default). See [`.github/PUBLISHING.md`](.github/PUBLISHING.md). Don't
hand-edit `Package.swift` — it's generated.
