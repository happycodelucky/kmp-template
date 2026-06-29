#!/bin/sh
# version.sh — compute the next release version from the latest git tag.
#
#   scripts/version.sh [--major] [--minor] [--patch] [--version X.Y.Z]
#
# Prints the computed version (no leading "v") to stdout; nothing else goes to
# stdout, so callers can capture it with $(...). Diagnostics go to stderr.
#
# Semantics:
#   --version X.Y.Z   explicit override; wins over everything. Validated as semver.
#   --major/--minor/--patch
#                     bump that component from the latest released tag. If more
#                     than one is given, the MOST SIGNIFICANT wins (major >
#                     minor > patch) and every lower component resets to zero.
#   (none)            defaults to --patch.
#
# "Latest released tag" is the newest vX.Y.Z (or X.Y.Z) tag reachable in the
# repo, matching how .github/workflows/release.yml derives the base. If there
# are no tags yet, the base is 0.0.0 so:
#   --patch -> 0.0.1   --minor -> 0.1.0   --major -> 1.0.0
#
# Sourced by mise's publish:maven task; usable standalone for scripting.

set -eu

BUMP=""        # major | minor | patch
EXPLICIT=""

while [ $# -gt 0 ]; do
    case "$1" in
        --major) [ "$BUMP" != "major" ] && BUMP="major"; shift ;;
        --minor) [ "$BUMP" = "major" ] || BUMP="minor"; shift ;;
        --patch) [ -z "$BUMP" ] && BUMP="patch"; shift ;;
        --version) EXPLICIT="${2:-}"; shift 2 ;;
        --version=*) EXPLICIT="${1#--version=}"; shift ;;
        *) echo "version.sh: unknown argument '$1'" >&2; exit 2 ;;
    esac
done

# Explicit override wins. Validate it's a sane semver core (+ optional -suffix).
if [ -n "$EXPLICIT" ]; then
    if ! printf '%s' "$EXPLICIT" | grep -Eq '^[0-9]+\.[0-9]+\.[0-9]+(-[0-9A-Za-z.-]+)?$'; then
        echo "version.sh: --version '$EXPLICIT' is not a valid semver (X.Y.Z or X.Y.Z-suffix)." >&2
        exit 2
    fi
    printf '%s\n' "$EXPLICIT"
    exit 0
fi

[ -z "$BUMP" ] && BUMP="patch"

# Latest released tag: newest vX.Y.Z by version sort. Strip a leading v and any
# pre-release suffix before parsing — we always bump from the numeric core.
LATEST=$(git tag --list 'v*' --sort=-v:refname 2>/dev/null | head -1)
[ -z "$LATEST" ] && LATEST=$(git tag --list --sort=-v:refname 2>/dev/null | grep -E '^[0-9]+\.[0-9]+\.[0-9]+' | head -1)

if [ -n "$LATEST" ]; then
    BASE=${LATEST#v}
    BASE=${BASE%%-*}
else
    echo "version.sh: no existing tags; starting from 0.0.0." >&2
    BASE="0.0.0"
fi

MAJOR=${BASE%%.*}
REST=${BASE#*.}
MINOR=${REST%%.*}
PATCH=${REST#*.}

# Guard against a malformed base.
case "$MAJOR$MINOR$PATCH" in
    *[!0-9]*) echo "version.sh: could not parse '$BASE' as X.Y.Z." >&2; exit 2 ;;
esac

case "$BUMP" in
    major) MAJOR=$((MAJOR + 1)); MINOR=0; PATCH=0 ;;
    minor) MINOR=$((MINOR + 1)); PATCH=0 ;;
    patch) PATCH=$((PATCH + 1)) ;;
esac

printf '%s.%s.%s\n' "$MAJOR" "$MINOR" "$PATCH"
