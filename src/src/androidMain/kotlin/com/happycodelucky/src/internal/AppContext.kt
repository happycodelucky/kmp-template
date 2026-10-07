/*
 * __DISPLAY_NAME__ — the Android application Context, captured once by
 * SrcInitializer at process startup (LESSONS D-006).
 *
 * Read [androidApplicationContext] lazily, where a system service is actually
 * needed — never in a static initializer — so it's read after startup has run.
 */
package com.happycodelucky.src.internal

import android.content.Context
import kotlinx.atomicfu.atomic

// Written on the main thread during startup, read from any thread afterwards:
// an atomic gives the cross-thread visibility a plain var doesn't (CLAUDE.md §6
// rules out `volatile`).
private val captured = atomic<Context?>(null)

/**
 * The application Context captured by `SrcInitializer`.
 *
 * @throws IllegalStateException if the initializer hasn't run — only when the app
 *   disabled androidx.startup's `InitializationProvider` and didn't initialize
 *   `SrcInitializer` through `AppInitializer` itself.
 */
internal val androidApplicationContext: Context
    get() =
        captured.value
            ?: error(
                "Android Context not initialized. SrcInitializer (androidx.startup) captures it at startup; " +
                    "if you disabled InitializationProvider, call " +
                    "AppInitializer.getInstance(context).initializeComponent(SrcInitializer::class.java) first.",
            )

/** Captures [context]'s application Context. Called by `SrcInitializer`; idempotent. */
internal fun initAndroidContext(context: Context) {
    captured.value = context.applicationContext
}
