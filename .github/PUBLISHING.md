# Publishing

__DISPLAY_NAME__ ships via two independent channels from `.github/workflows/release.yml`:

- **Maven Central** — Android AAR, `kotlinMultiplatform` metadata, per-target klibs. For Gradle/KMP consumers.
- **GitHub Releases** (via KMMBridge) — the SKIE-enhanced `__FRAMEWORK__.xcframework` zip. For pure-Swift SPM consumers.

## Maven Central

**Coordinates:** `com.happycodelucky.__PROJECT_NAME__:__PROJECT_NAME__`

One Gradle invocation publishes:

- The Android AAR.
- The `kotlinMultiplatform` metadata module (`.module` file) that ties every target together.
- Per-target klibs: `__PROJECT_NAME__-iosarm64`, `__PROJECT_NAME__-iossimulatorarm64`, `__PROJECT_NAME__-macosarm64`, `__PROJECT_NAME__-android`.
- Sources / javadoc jars next to each, with detached GPG signatures.

The test-fakes module publishes alongside it under `com.happycodelucky.__PROJECT_NAME__:__PROJECT_NAME__-testing`.

## Release pipeline

Releases are triggered via `workflow_dispatch` on `release.yml`. No one types a version number — the workflow computes it.

1. Choose `bumpType` (patch / minor / major). The version is derived from the latest GitHub Release; the chosen component increments and everything below it resets to zero. An optional `versionSuffix` adds a SemVer pre-release identifier.
2. **`dryRun=true` (default)** — runs `publishToMavenCentral`: uploads to Central Portal staging only. The deployment sits in "validated" state; review it at https://central.sonatype.com/ and click Publish (or Drop). Nothing is tagged and no XCFramework is published. Safe to run frequently. The local equivalent is `mise run publish:maven --dryrun`.
3. **`dryRun=false`** — runs `publishAndReleaseToMavenCentral` (irreversible), then the SPM steps: `:src:kmmBridgePublish` builds and uploads `__FRAMEWORK__.xcframework.zip` to the `vX.Y.Z` GitHub Release and regenerates `Package.swift`; the workflow rewrites the asset URL to the public form, commits `Package.swift` to `main`, and force-moves the tag onto that commit.

The `automaticRelease = false` flag in `src/build.gradle.kts` is what makes dry-run behaviour correct. Do not flip it without reading the comment there.

## Releasing by hand (`mise run publish:maven`)

The CI workflow above is the canonical path. For a solo or local release, `mise run publish:maven` (→ `scripts/release.sh`) runs the same steps from your machine. The version is computed from the latest git tag (`scripts/version.sh`):

```bash
mise run publish:maven --minor --dryrun   # compute next version, stage to Central only
mise run publish:maven --patch            # full release of the next patch (prompts to confirm)
mise run publish:maven --version 1.4.0     # release an exact version
```

- **Bump flags** — `--major` / `--minor` / `--patch` bump that component from the latest `vX.Y.Z` tag (lower components reset to zero). If more than one is passed, the **most significant wins**. `--version X.Y.Z` overrides the computed value. Default is `--patch`.
- **`--dryrun`** — computes the version and runs `publishToMavenCentral` (Central staging only). Nothing is committed, tagged, or released. Safe to run repeatedly.
- **Real release** — after a typed confirmation (it echoes the plan first; Maven Central is irreversible), it: rewrites `Package.swift` to the released remote-binary form (URL + checksum), runs `publishAndReleaseToMavenCentral`, commits, creates and pushes the `vX.Y.Z` tag, and runs `gh release create` with the XCFramework zip asset.

Requires a clean tree, an authenticated `gh`, and the Maven Central credentials configured (next section).

## Credentials

The vanniktech plugin reads **four Gradle properties**. It doesn't care where they come from — Gradle resolves a property `foo` from a `-Pfoo=` flag, an `ORG_GRADLE_PROJECT_foo` env var, or a `gradle.properties` file (CLI → env → `~/.gradle` → project). That's why the same setup works in CI and locally.

| Property | What it is | Where to get it |
|---|---|---|
| `mavenCentralUsername` | Central Portal **token** username (not your login) | [central.sonatype.com](https://central.sonatype.com/) → Account → Generate User Token |
| `mavenCentralPassword` | Central Portal token password | same token |
| `signingInMemoryKey` | ASCII-armored **GPG private key** block | `gpg --armor --export-secret-keys <KEY_ID>` (the whole `-----BEGIN…END PGP PRIVATE KEY BLOCK-----`) |
| `signingInMemoryKeyPassword` | The GPG key's passphrase | what you set when creating the key |

Your GPG public key must be published to a keyserver Central checks (e.g. `gpg --keyserver keyserver.ubuntu.com --send-keys <KEY_ID>`), or Central rejects the signatures.

### CI

Set four secrets on the **`continuous-deployment` GitHub environment** (repo Settings → Environments → `continuous-deployment` → Secrets — *not* repository-scoped secrets; `release.yml` binds to that environment):

| GitHub secret | Maps to property |
|---|---|
| `MAVEN_CENTRAL_USERNAME` | `mavenCentralUsername` |
| `MAVEN_CENTRAL_PASSWORD` | `mavenCentralPassword` |
| `MAVEN_CENTRAL_SIGNING_KEY` | `signingInMemoryKey` |
| `MAVEN_CENTRAL_SIGNING_KEY_PASSWORD` | `signingInMemoryKeyPassword` |

`release.yml` exports them as `ORG_GRADLE_PROJECT_*` env vars, which Gradle maps onto the property names above.

### Local (`mise run publish:maven`)

Put them in **`~/.gradle/gradle.properties`** (your home directory — **never** the repo, and never a committed `gradle.properties`; these are secrets):

```properties
mavenCentralUsername=<token-username>
mavenCentralPassword=<token-password>
signingInMemoryKeyPassword=<gpg-passphrase>
# Paste the armored key as a single line with literal \n between lines, or use
# the file-based form: signingInMemoryKeyFile=/absolute/path/to/secret-key.asc
signingInMemoryKey=-----BEGIN PGP PRIVATE KEY BLOCK-----\n…\n-----END PGP PRIVATE KEY BLOCK-----
```

Or export the matching `ORG_GRADLE_PROJECT_*` env vars in your shell instead. `mise run publish:maven` checks they're present before it builds and fails fast with this guidance if not. A `--dryrun` still needs the signing key (Central validates signatures even in staging).

## SPM distribution — KMMBridge → GitHub Releases

Touchlab's KMMBridge publishes the Apple framework to pure-Swift SPM consumers. The pipeline (real publishes only, not dry-run):

1. Gradle builds an `XCFramework` with `iosArm64` + `iosSimulatorArm64` + `macosArm64` slices. No x86. SKIE-enhanced (`produceDistributableFramework()` emits `.swiftinterface` files required by Xcode 26).
2. KMMBridge zips the XCFramework and uploads it as a GitHub Release asset. GitHub *Releases*, not GitHub *Packages* — Packages requires a PAT to download even from public repos; Release assets are public and unauthenticated.
3. KMMBridge regenerates the root `Package.swift` referencing the asset by URL + sha256 checksum. The workflow rewrites KMMBridge's API asset URL to the public `releases/download/…` form, commits `Package.swift` to `main`, and force-moves the version tag onto that commit so the tagged manifest matches the uploaded binary.
4. Swift consumers add this repo's URL as an SPM dependency pinned to a version tag; the tagged `Package.swift` hands them the prebuilt binary.

### Rules

- KMMBridge config lives in the `kmmbridge { }` block in `src/build.gradle.kts`; the version pin lives in `gradle/libs.versions.toml`. Only `:src` gets KMMBridge — `:src-testing` ships klibs via Maven Central only.
- Do **not** redeclare `XCFramework("__FRAMEWORK__")` in the `kotlin { }` block: KMMBridge auto-creates the aggregator tasks (`assemble__FRAMEWORK__{Debug,Release}XCFramework`) at config time; a second declaration collides.
- Versioning: the release workflow computes the version and passes `-Pversion=X.Y.Z`; KMMBridge tags `v${version}`. KMMBridge's own timestamp versioning is not used.
- Publishing is CI-only: the `kmmBridgePublish` task only exists when `-PENABLE_PUBLISHING=true` is passed (the release workflow does this).
- Don't vendor `XCFramework` zips into the repo. Everything flows through GitHub Release assets + the committed `Package.swift`.
- `Package.swift` is generated — `kmmBridgePublish` writes the released form, `spmDevBuild` the local-dev form. Don't hand-edit it, and never commit the local-dev form.

## Local XCFramework development

The sample apps under `/apps/ios` and `/apps/macos` consume the root `Package.swift` as a local package.

```bash
mise run spm:dev        # rebuild debug XCFramework + flip Package.swift to local path
mise run spm:restore    # restore the committed (remote-binary) Package.swift
mise run build:xcframework  # rebuild release XCFramework without touching Package.swift
mise run publish:local  # publish the artifacts to the local Maven repository for consumption
```

Until the first `dryRun=false` release runs, the committed `Package.swift` still points at the local build path — the first real release flips it to the remote-binary form.
