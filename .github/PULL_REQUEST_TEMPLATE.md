<!--
  Thanks for contributing to __DISPLAY_NAME__! This template is filled in the same
  way by a human or a coding agent:

  - Comments that start with "AI:" are fill-in instructions. They stay hidden
    in the rendered PR; keep or delete them.
  - A section holding an "Unfilled" callout (a `> [!IMPORTANT]` block) is
    required. Replace the whole callout with your content. No "Unfilled"
    callout should survive into a ready-for-review PR.
  - Plain bullet lists are choices: keep the line(s) that apply and delete the
    rest. Checkboxes appear only in the done-gate, so the PR list's "N of M
    tasks" counter tracks exactly that gate.
  - Delete any other section that doesn't apply, or write "N/A".

  Agents: CLAUDE.md §11 covers opening the PR (draft by default).
-->

## Summary

<!-- AI: What changed and why, in 1–3 sentences a reviewer can read before
     the diff. Lead with the behavior change, not the file list. Replace
     `Closes #` with the issue number, or delete the line. -->

> [!IMPORTANT]
> _Unfilled — what changed and why._

Closes #

## Type of change

<!-- AI: Keep the line(s) that apply; delete the rest. -->

- Bug fix (non-breaking)
- New feature (non-breaking)
- Breaking change (public API / behavior)
- Docs / tooling only

## Changeset

<!-- AI: The release PR builds the next version and the changelog from the
     pending .changeset/*.md files (.changeset/README.md). `mise run changeset`
     writes .changeset/<branch>.md; fill in its body. Pick `change` honestly:
     removing or renaming public API is `major`, even while 0.x. Keep the one
     line that applies, naming the file and level; delete the other. -->

- Added `.changeset/<file>.md` — `change: <major | minor | patch>`
- None — nothing here reaches consumers (docs, CI, tests, samples); labelled `no-changeset`

## Affected platforms

<!-- AI: Keep every target the change can reach and delete the rest, or
     write "None" for a docs/tooling-only change. The JVM compile hides
     Native-only bugs, so a commonMain change needs Native verification too. -->

- iOS
- macOS
- Android
- JVM
- commonMain (all targets)

## Reviewer focus

<!-- AI: Keep each area this PR touches that deserves a careful look and
     delete the rest, then name where a reviewer should start (file:line or
     symbol) and why. If nothing is risky, replace the list with
     "Routine — no hotspots." -->

- Public API — the `api/` dump diff (CLAUDE.md §8)
- Swift boundary — SKIE output, `@Throws`, sealed → enum (§7)
- Concurrency — scope ownership, `Mutex` / `synchronized`, injected `Clock` (§6)
- New or bumped dependencies (§5)
- Build logic / CI — convention plugins, `mise.toml`, workflows

**Start here:** _…_

## Open questions

<!-- AI: Anything that needs a human decision: an ambiguous requirement, a
     trade-off you couldn't settle, a check you couldn't run (and why). Write
     "None." if there are none — an empty section is not the same as "None." -->

> [!IMPORTANT]
> _Unfilled — list open questions, or write "None."_

## Decisions & trade-offs

<!-- AI: Non-obvious choices made while implementing, each with the
     alternative you rejected and why. If a decision is load-bearing beyond
     this PR, also add it to .claude/lessons/LESSONS.md and cite its ID here.
     Delete this section if there were none. -->

## How it was verified

<!-- AI: Evidence, not assertions. List each command you actually ran with
     its result (e.g. `mise run check` → BUILD SUCCESSFUL), and name the new
     or changed tests. Never list a command you did not run; if something
     couldn't be verified, say so under Open questions. "It compiles" is not
     verification. -->

> [!IMPORTANT]
> _Unfilled — commands run and their results._

## Done-gate checklist

<!-- AI: These mirror CLAUDE.md §5–§12. Tick only what you verified for this
     PR. Leave a box unticked, and explain under Open questions, rather than
     tick it on faith. -->

- [ ] `mise run check` passes (ktlint + detekt + ABI check + every test target, both modules)
- [ ] Native + Android compile clean (`:src:compileKotlinMacosArm64` / `compileKotlinIosSimulatorArm64` / `compileAndroidMain`) — the JVM compile alone is not a sufficient gate
- [ ] `mise run build:samples` passes if dependencies or the public API changed — `check` never builds the sample apps (LESSONS N-006)
- [ ] New/changed logic has `commonTest` coverage (`runTest` virtual time, no `Thread.sleep`)
- [ ] Public API changes follow the Swift-interop rules (§7): sealed → exhaustive enum, `@Throws` replicated on every `actual` incl. `CancellationException`, no `kotlin.Result<T>` at the boundary
- [ ] If the public API changed intentionally, `mise run api:dump` was run and the `api/` diff is committed and reviewed (§8)
- [ ] New dependencies were sourced per §5 (official Kotlin → Google KMP → kmp-awesome), are stable, and were added to `gradle/libs.versions.toml` only
- [ ] A changeset is committed with an honest `change` level, or the PR is labelled `no-changeset` because nothing in it reaches consumers (§8) — the Changeset check enforces it
- [ ] Docs updated (`docs/` + KDoc) for any public API or behavior change
- [ ] Anything non-obvious learned is recorded in `.claude/lessons/LESSONS.md` (§11)
- [ ] No hard-rule violations (§12): no Compose MP, CocoaPods, `GlobalScope`, `!!` in production, `java.time` in common, `@Synchronized`/`volatile`, callback public APIs, EAP/RC/Beta deps

## AI assistance

<!-- AI: Disclosure, not a gate — the checklist above applies no matter who
     wrote the code. Keep exactly one line and delete the rest. If an agent
     wrote any of it, name the tool and model; otherwise write "N/A". Human
     review isn't recorded here: an agent opens the PR as a draft, and a human
     marking it ready is the review sign-off. -->

- None
- AI-assisted — human-written, AI suggested
- AI-authored

**Tool / model:** _…_
