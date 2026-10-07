/*
 * __DISPLAY_NAME__ — androidx.startup Initializer that captures the application
 * Context (LESSONS D-006).
 *
 * Registered in the library's AndroidManifest.xml under androidx.startup's
 * InitializationProvider, a ContentProvider that runs early in process startup —
 * before Application.onCreate. It stashes the application Context in
 * internal/AppContext.kt, so the library's Android code can reach system services
 * (WifiManager, ConnectivityManager, NsdManager, …) without its public API taking
 * a Context. Same shape as reachable's, ssdp-kmp's and mdns-kmp's initializers.
 */
package com.happycodelucky.src

import android.content.Context
import androidx.startup.Initializer
import com.happycodelucky.src.internal.androidApplicationContext
import com.happycodelucky.src.internal.initAndroidContext

/**
 * Captures the application [Context] for the library at process startup.
 *
 * Runs automatically: the library manifest registers it with androidx.startup's
 * `InitializationProvider`, and the manifest merger combines that entry with any
 * other library's or the app's. An app that disables `InitializationProvider`
 * (or removes this entry) initializes it itself before using the library:
 *
 * ```kotlin
 * AppInitializer.getInstance(context).initializeComponent(SrcInitializer::class.java)
 * ```
 *
 * Public so an app's own `Initializer` can list it in [dependencies].
 */
public class SrcInitializer : Initializer<Context> {
    override fun create(context: Context): Context {
        initAndroidContext(context)
        return androidApplicationContext
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}
