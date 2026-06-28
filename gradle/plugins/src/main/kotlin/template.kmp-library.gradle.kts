/*
 * Convention plugin: the shared module shape for this library's published KMP
 * modules (`:src`, `:src-testing`).
 *
 * Owns everything the modules would otherwise duplicate (CLAUDE.md §4, §5): the
 * target matrix, the apple intermediate source set, the Android library block,
 * the jvm() target, compiler options, JVM target wiring, the Kermit logging
 * baseline, and the SKIE settings that must match across modules. Per-module
 * identity (framework base name, bundle id, Android namespace) is DERIVED from
 * the project name, so adding a module means applying this plugin and nothing
 * else:
 *
 *   src          → framework "Src",        namespace com.happycodelucky.src
 *   src-testing  → framework "SrcTesting",  namespace com.happycodelucky.src.testing
 *
 * `mise run init <name>` renames the module directories (src → <name>,
 * src-testing → <name>-testing); the derivations below then produce the right
 * framework name and namespace with zero token replacement. The group prefix
 * `com.happycodelucky` is the at-rest default — init.sh rewrites it only when
 * `--group` differs.
 *
 * Module build scripts keep only what genuinely differs: dependencies, the
 * KMMBridge SPM distribution config (`:src` only), and POM name/description.
 */

import org.gradle.api.artifacts.VersionCatalogsExtension
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget

plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.kotlin.multiplatform.library")
    id("co.touchlab.skie")
    id("org.jetbrains.dokka")
}

// Typed `libs` accessors aren't generated inside precompiled script plugins;
// the named-lookup API reads the same catalog the main build uses.
val libs = the<VersionCatalogsExtension>().named("libs")

// src → "Src"; src-testing → "SrcTesting".
val frameworkBaseName = name.split("-").joinToString("") { part -> part.replaceFirstChar(Char::uppercase) }

// src → com.happycodelucky.src; src-testing → ….src.testing.
// Doubles as the framework bundle id, pinned so SKIE doesn't fall back to the
// framework name. The "com.happycodelucky" prefix is the group default; init.sh
// rewrites it when `--group` differs.
val moduleNamespace = "com.happycodelucky." + name.replace("-", ".")

kotlin {
    // CLAUDE.md §4: applyDefaultHierarchyTemplate. Don't hand-roll source set
    // wiring. iosMain + macosMain coalesce into a shared "appleMain"
    // intermediate. Adding jvm() gives a jvmMain/jvmTest sibling automatically.
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    applyDefaultHierarchyTemplate {
        common {
            group("apple") {
                withIos()
                withMacos()
            }
        }
    }

    // --- Apple targets (CLAUDE.md §4) ---------------------------------------
    // Static framework binaries with a stable bundle id. In `:src`, KMMBridge
    // aggregates these into `Src.xcframework` at config time (no explicit
    // XCFramework declaration — see src/build.gradle.kts).
    listOf(iosArm64(), iosSimulatorArm64(), macosArm64()).forEach { target ->
        target.binaries.framework {
            baseName = frameworkBaseName
            isStatic = true
            binaryOption("bundleId", moduleNamespace)
        }
    }

    // --- Android target (CLAUDE.md §4) --------------------------------------
    // The new com.android.kotlin.multiplatform.library plugin's android {} block.
    // arm64-v8a only: consumers' app modules pin the ABI splits; we test
    // arm64-v8a only (documented in README).
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    android {
        namespace = moduleNamespace
        compileSdk =
            libs
                .findVersion("android-compile-sdk")
                .get()
                .requiredVersion
                .toInt()
        minSdk =
            libs
                .findVersion("android-min-sdk")
                .get()
                .requiredVersion
                .toInt()

        withHostTestBuilder { /* enables the androidHostTest source set */ }
    }

    // --- JVM target (desktop / server / Linux / Windows) --------------------
    // Architecture-neutral bytecode — the one target the ARM-only rule doesn't
    // touch. No SKIE, no KMMBridge — the JVM ships through Maven Central only,
    // like Android.
    jvm()

    // --- Compiler options (CLAUDE.md §3) ------------------------------------
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    compilerOptions {
        // K2 stable APIs only.
        languageVersion.set(KotlinVersion.KOTLIN_2_3)
        apiVersion.set(KotlinVersion.KOTLIN_2_3)
        allWarningsAsErrors.set(true)
    }

    // Per-target JVM toolchain knobs — both the Android target's JVM
    // compilation and the desktop jvm() target need bytecode level 21.
    targets.withType<KotlinJvmTarget>().configureEach {
        compilations.configureEach {
            compileTaskProvider.configure {
                compilerOptions {
                    jvmTarget.set(JvmTarget.JVM_21)
                }
            }
        }
    }

    // --- Logging baseline (CLAUDE.md §5) ------------------------------------
    // Kermit in commonMain so every module gets multiplatform logging without
    // repeating the dependency. `api` so consumers' Swift/Android code can reach
    // the same Logger surface the library logs through.
    sourceSets.commonMain.dependencies {
        api(libs.findLibrary("kermit").get())
    }
}

skie {
    // SKIE handles the Kotlin → Swift bridge enhancements (CLAUDE.md §7):
    // exhaustive sealed switching, suspend → async/await, Flow → AsyncSequence,
    // default-arg overloads. All feature defaults stay on; tighten only when
    // something bites.
    analytics {
        // Disable opt-in analytics; revisit if useful.
        disableUpload.set(true)
    }
    // Prevent SKIE from copying bundled Swift sources into the klib. A module
    // may ship hand-written Swift sweeteners whose `extension` is only valid
    // inside the module where the type keeps its short swift_name; if bundled
    // into the klib, SKIE unpacks and recompiles them in downstream modules
    // where the type is module-prefixed, causing a compile error. With bundling
    // disabled, SKIE still compiles the Swift sources into each framework binary
    // via its own compile task.
    swiftBundling {
        enabled.set(false)
    }
}
