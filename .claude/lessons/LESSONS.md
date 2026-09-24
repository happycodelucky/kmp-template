# LESSONS — __DISPLAY_NAME__

Terse, append-only log of non-obvious things learned while building this library.
One line each. Prefix with a category + number so CLAUDE.md and code comments can
reference them.

- **D-NNN** — Decisions (load-bearing architecture choices).
- **B-NNN** — Bugs / gotchas (the thing that bit, and the fix).
- **N-NNN** — Notes (build-system / toolchain quirks).

_Empty to start. Add entries as you learn them._

- **D-001** — Line length: detekt `MaxLineLength` 140 is the only limit; ktlint's `max_line_length = off` and its parameter-count forced-multiline class/function signature rules are `unset` in the root `.editorconfig` (ktlint_official otherwise wraps a 1-param constructor).
