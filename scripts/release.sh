#!/bin/sh
# release.sh — local Maven Central release for this KMP library.
#
#   scripts/release.sh [--major|--minor|--patch] [--version X.Y.Z] [--dryrun]
#
# Invoked by `mise run publish:maven`. Computes the next version (see
# scripts/version.sh), then:
#
#   --dryrun : compute version, then `publishToMavenCentral` — uploads to the
#              Central Portal STAGING area and stops. Nothing is committed,
#              tagged, or released. Review at https://central.sonatype.com/.
#
#   (real)   : the full release, after a typed confirmation:
#              1. update Package.swift to the released remote-binary form
#                 (URL + checksum of the XCFramework asset for this tag);
#              2. publishAndReleaseToMavenCentral  (IRREVERSIBLE);
#              3. commit Package.swift + the version, create + push the vX.Y.Z
#                 git tag;
#              4. gh release create vX.Y.Z, uploading the XCFramework zip asset.
#
# NOTE: this is a convenience for solo / local releases. The canonical path is
# .github/workflows/release.yml (runs the same steps in CI with org secrets).
# Prefer the workflow for team releases; use this when you're releasing by hand.
#
# Required for a REAL release: a clean git tree on the default branch, `gh`
# authenticated, and the Maven Central credentials exported as the
# ORG_GRADLE_PROJECT_* env vars vanniktech reads (see .github/PUBLISHING.md).

set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)

DRYRUN=""
PASS_ARGS=""
while [ $# -gt 0 ]; do
    case "$1" in
        --dryrun) DRYRUN="true"; shift ;;
        --major|--minor|--patch) PASS_ARGS="$PASS_ARGS $1"; shift ;;
        --version) PASS_ARGS="$PASS_ARGS --version ${2:-}"; shift 2 ;;
        --version=*) PASS_ARGS="$PASS_ARGS $1"; shift ;;
        *) echo "release.sh: unknown argument '$1'" >&2; exit 2 ;;
    esac
done

# Compute the target version (handles --major/--minor/--patch most-significant-
# wins, --version override, latest-tag base).
# shellcheck disable=SC2086
VERSION=$(sh "$SCRIPT_DIR/version.sh" $PASS_ARGS)
TAG="v$VERSION"
FRAMEWORK="__FRAMEWORK__"   # init.sh rewrites this to the framework module name.

# Fail fast if the Maven Central credentials vanniktech needs aren't available.
# A Gradle property `foo` resolves from an ORG_GRADLE_PROJECT_foo env var or from
# ~/.gradle/gradle.properties — check both so either setup passes. This runs for
# dry runs too: Central validates GPG signatures even in staging. See
# .github/PUBLISHING.md for what these are and where to get them.
require_credentials() {
    home_props="$HOME/.gradle/gradle.properties"
    missing=""
    for prop in mavenCentralUsername mavenCentralPassword signingInMemoryKey signingInMemoryKeyPassword; do
        env_name=$(printf 'ORG_GRADLE_PROJECT_%s' "$prop")
        eval "env_val=\${$env_name:-}"
        if [ -n "$env_val" ]; then
            continue
        fi
        if [ -f "$home_props" ] && grep -q "^[[:space:]]*$prop[[:space:]]*=" "$home_props"; then
            continue
        fi
        missing="$missing $prop"
    done
    if [ -n "$missing" ]; then
        echo "error: missing Maven Central credentials:$missing" >&2
        echo "       Set them in ~/.gradle/gradle.properties or as ORG_GRADLE_PROJECT_* env vars." >&2
        echo "       See .github/PUBLISHING.md → Credentials." >&2
        exit 1
    fi
}

# --- Dry run: stage only ----------------------------------------------------
if [ "$DRYRUN" = "true" ]; then
    require_credentials
    echo "Dry run — computed version: $VERSION"
    echo "Uploading to Maven Central STAGING only (no release, no tag, no commit)."
    ./gradlew publishToMavenCentral -Pversion="$VERSION"
    echo ""
    echo "Staged. Review at https://central.sonatype.com/ and Publish or Drop there."
    exit 0
fi

# --- Real release: preflight ------------------------------------------------
require_credentials
if [ -n "$(git status --porcelain)" ]; then
    echo "error: working tree is not clean. Commit or stash before releasing." >&2
    exit 1
fi
if git rev-parse -q --verify "refs/tags/$TAG" >/dev/null 2>&1; then
    echo "error: tag $TAG already exists. Pick a different bump." >&2
    exit 1
fi
if ! command -v gh >/dev/null 2>&1; then
    echo "error: gh is required for the GitHub release step." >&2
    exit 1
fi

REPO=$(gh repo view --json nameWithOwner -q .nameWithOwner 2>/dev/null || true)
echo "About to PUBLISH AND RELEASE — this is IRREVERSIBLE:"
echo "  version : $VERSION"
echo "  tag     : $TAG"
echo "  repo    : ${REPO:-<unknown>}"
echo "  Maven Central: publishAndReleaseToMavenCentral (cannot be undone)"
echo "  git     : commit + tag $TAG + push"
echo "  GitHub  : create release $TAG with the $FRAMEWORK.xcframework zip asset"
echo ""
printf 'Type the version (%s) to confirm: ' "$VERSION"
read -r CONFIRM
if [ "$CONFIRM" != "$VERSION" ]; then
    echo "Aborted — confirmation did not match." >&2
    exit 1
fi

# 1. Build the release XCFramework + zip it (the SPM asset).
echo "==> Building release XCFramework"
./gradlew ":src:assemble${FRAMEWORK}XCFramework" -Pversion="$VERSION"
XCF_DIR="src/build/XCFrameworks/release"
ZIP="$XCF_DIR/$FRAMEWORK.xcframework.zip"
( cd "$XCF_DIR" && rm -f "$FRAMEWORK.xcframework.zip" && zip -qry "$FRAMEWORK.xcframework.zip" "$FRAMEWORK.xcframework" )

# 2. Rewrite Package.swift to the released remote-binary form (URL + checksum).
ASSET_URL="https://github.com/$REPO/releases/download/$TAG/$FRAMEWORK.xcframework.zip"
CHECKSUM=$(swift package compute-checksum "$ZIP")
echo "==> Pointing Package.swift at $ASSET_URL"
cat > Package.swift <<EOF
// swift-tools-version:6.0
import PackageDescription

let packageName = "$FRAMEWORK"

let package = Package(
    name: packageName,
    platforms: [
        .iOS(.v18),
        .macOS(.v15),
    ],
    products: [
        .library(
            name: packageName,
            targets: [packageName]
        ),
    ],
    targets: [
        .binaryTarget(
            name: packageName,
            url: "$ASSET_URL",
            checksum: "$CHECKSUM"
        ),
    ]
)
EOF
swift package dump-package > /dev/null   # prove the manifest still parses.

# 3. Publish + release to Maven Central (IRREVERSIBLE).
echo "==> Publishing to Maven Central"
./gradlew publishAndReleaseToMavenCentral -Pversion="$VERSION"

# 4. Commit Package.swift, tag, push.
echo "==> Committing, tagging, pushing"
git add Package.swift
git commit -m "release: $TAG"
git tag -a "$TAG" -m "Release $TAG"
git push origin HEAD
git push origin "refs/tags/$TAG"

# 5. GitHub release with the XCFramework asset.
echo "==> Creating GitHub release $TAG"
gh release create "$TAG" "$ZIP" --title "$TAG" --generate-notes --latest

echo ""
echo "Released $VERSION."
echo "  Maven Central: https://central.sonatype.com/artifact/com.happycodelucky.__PROJECT_NAME__/__PROJECT_NAME__/$VERSION"
echo "  GitHub:        https://github.com/$REPO/releases/tag/$TAG"
