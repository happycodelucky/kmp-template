# LESSONS — __DISPLAY_NAME__

Terse, append-only log of non-obvious things learned while building this library.
One line each. Prefix with a category + number so CLAUDE.md and code comments can
reference them.

- **D-NNN** — Decisions (load-bearing architecture choices).
- **B-NNN** — Bugs / gotchas (the thing that bit, and the fix).
- **N-NNN** — Notes (build-system / toolchain quirks).

- **N-001** — version-catalog-update ≥ 1.0 does NOT read the ben-manes report; it resolves versions itself with its own (different) stability rule. The root build passes it the shared `stableVersion` predicate, `pin`s `kotlin`, `keep`s findVersion-only keys, and disables `sortByKey`. It still strips blank lines and end-of-line comments.
