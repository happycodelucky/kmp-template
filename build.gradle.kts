/*
 * __PROJECT_NAME__ — root build script.
 *
 * Plugins are declared here with `apply false`; they're applied in :src and
 * :src-testing (mostly via the `__PROJECT_NAME__.kmp-library` convention
 * plugin). This keeps `gradle/libs.versions.toml` as the single source of truth
 * for versions (CLAUDE.md §3).
 */

import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask

plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.android.kotlin.multiplatform.library) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.skie) apply false
    alias(libs.plugins.kmmbridge.github) apply false
    alias(libs.plugins.maven.publish) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.detekt) apply false

    // Dokka v2: Kotlin API doc generator. Produces HTML for the public API of
    // every source set. The HTML is copied into docs/api/ for mkdocs to bundle.
    alias(libs.plugins.dokka)

    // Dependency-update tooling (mise dependencies:outdated / dependencies:update).
    // ben-manes reports updates; version-catalog-update rewrites libs.versions.toml.
    alias(libs.plugins.ben.manes.versions)
    alias(libs.plugins.version.catalog.update)

    // Build-health tooling. dependency-analysis adds the root `buildHealth` task
    // (mise dependencies:analyze) — unused/misused/transitive dependency advice.
    // gradle-doctor warns on slow config, JVM mismatches, and cache misses on
    // every build (mise build:doctor surfaces its diagnostics explicitly).
    alias(libs.plugins.dependency.analysis)
    alias(libs.plugins.gradle.doctor)
}

allprojects {
    group = "com.happycodelucky.__PROJECT_NAME__"
    // The in-tree version carries `-SNAPSHOT` and a `0` patch slot. Humans bump
    // major/minor here and commit the change; the patch slot stays `0`. CI
    // overrides this at build time via `-Pversion=...` to stamp exact `vX.Y.Z`
    // for releases without ever committing the override back.
    version = providers.gradleProperty("version").getOrElse("0.1.0-SNAPSHOT")
}

// Gradle Doctor — build-health diagnostics. mise owns the JDK (via the [tools]
// pins), so its JAVA_HOME checks are advisory here, not fatal: a fresh
// `git clone && mise run check` must never fail on a tool's environment opinion.
// The remaining checks (slow config, negative-avoidance, cache misuse) stay on.
doctor {
    javaHome {
        // mise puts the right JDK on PATH; JAVA_HOME may be unset. Warn, don't fail.
        ensureJavaHomeIsSet.set(false)
        ensureJavaHomeMatches.set(false)
        failOnError.set(false)
    }
    // Don't fail a build just because another Gradle daemon is alive (common
    // when an IDE holds one open alongside a terminal build).
    disallowMultipleDaemons.set(false)
}

subprojects {
    // ktlint + detekt wire onto the KMP plugin — i.e. onto the published
    // library modules only (CLAUDE.md §3). The sample apps (`:androidApp`,
    // `:jvm-cli`) are demo scaffolding, not shipped code, and are intentionally
    // excluded from Kotlin lint and from CI's check task.
    pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
        apply(plugin = "org.jlleitschuh.gradle.ktlint")
        apply(plugin = "io.gitlab.arturbosch.detekt")
    }

    plugins.withId("org.jlleitschuh.gradle.ktlint") {
        configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
            version.set(libs.versions.ktlint.get())
            android.set(false)
            outputToConsole.set(true)
            ignoreFailures.set(false)
            filter {
                exclude { element -> element.file.path.contains("/build/generated/") }
                exclude("**/build/**")
                exclude("**/generated/**")
            }
        }

        tasks.withType<org.jlleitschuh.gradle.ktlint.tasks.BaseKtLintCheckTask>().configureEach {
            exclude { element -> element.file.path.contains("/build/generated/") }
        }
    }

    plugins.withId("io.gitlab.arturbosch.detekt") {
        configure<io.gitlab.arturbosch.detekt.extensions.DetektExtension> {
            buildUponDefaultConfig = true
            // Project overrides layered on the defaults live in config/detekt.
            config.setFrom(rootProject.files("config/detekt/detekt.yml"))
            // detekt's default source resolution only knows JVM layouts
            // (src/main/kotlin); point it at the module root so every KMP source
            // set (commonMain, appleMain, jvmMain, androidHostTest, …) is
            // scanned. The task filters to *.kt, and build/ output is excluded.
            source.setFrom(files("src"))
        }
    }
}

// Apply Dokka to the published modules and aggregate into docs/api/.
dokka {
    moduleName.set("__DISPLAY_NAME__")
}

dependencies {
    // Aggregate Dokka HTML from the published modules into the root build
    // (Dokka v2 pattern). `:src-testing` is a public-API module too — consumers
    // writing tests want its fakes documented next to the main library.
    dokka(project(":src"))
    dokka(project(":src-testing"))
}

/**
 * Copies Dokka v2 HTML output into docs/api/, where mkdocs picks it up.
 *
 * The aggregated HTML lives at build/dokka/html after
 * dokkaGeneratePublicationHtml. mkdocs looks at docs/api/ when it builds the
 * site; CI runs Dokka before mkdocs.
 */
tasks.register<Copy>("copyDokkaToDocs") {
    group = "documentation"
    description = "Copies aggregated Dokka HTML into docs/api/ for mkdocs."

    dependsOn("dokkaGeneratePublicationHtml")
    from(layout.buildDirectory.dir("dokka/html"))
    into(layout.projectDirectory.dir("docs/api"))
}

// Stable-only dependency updates (CLAUDE.md §3: no EAP/RC/Beta on main).
//
// `-Drevision=release` only chooses which Maven metadata channel ben-manes
// reads — it does NOT reject versions whose string is a pre-release, so without
// this rule `dependencyUpdates` happily suggests 1.5.0-alpha22 over 1.4.0. The
// rejectVersionIf rule below filters every candidate whose version carries a
// pre-release qualifier. It governs BOTH mise tasks: `dependencies:outdated`
// (the report) and `dependencies:update` (version-catalog-update consumes the
// same dependencyUpdates output).
//
// A version is considered STABLE only if it has no pre-release qualifier.
// Matches: 1.2.3, 2026.06.00, 1.2.3.4. Rejects: -alpha/-beta/-rc/-eap/-m1/
// -snapshot/-dev/-preview (any case, with or without a separator).
val stableVersion = "^[0-9][0-9.]*$".toRegex()
val preReleaseQualifier =
    "(?i)[.\\-]?(alpha|beta|rc|cr|m|eap|snapshot|dev|preview|pre|b)[.\\-]?[0-9]*$|(?i)(snapshot)".toRegex()

tasks.withType<DependencyUpdatesTask>().configureEach {
    // Read the stable release channel, not integration/milestone metadata.
    revision = "release"
    rejectVersionIf {
        // Reject a candidate that isn't a clean stable version, OR carries a
        // pre-release qualifier. The current version is never rejected here —
        // ben-manes only feeds candidate upgrades through this predicate.
        !stableVersion.matches(candidate.version) ||
            preReleaseQualifier.containsMatchIn(candidate.version)
    }
}
