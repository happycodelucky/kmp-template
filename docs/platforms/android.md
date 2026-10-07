---
title: Android
---

# Android

The library registers `SrcInitializer` with
[androidx.startup](https://developer.android.com/topic/libraries/app-startup),
which captures the application `Context` as your app starts — so nothing in the
API asks you for one. It runs automatically through androidx.startup's
`InitializationProvider`, shared with any other library that uses it.

If your app disables `InitializationProvider`, initialize the library yourself
before using it:

```kotlin
import androidx.startup.AppInitializer
import com.happycodelucky.src.SrcInitializer

AppInitializer.getInstance(context)
    .initializeComponent(SrcInitializer::class.java)
```

TODO: document anything Android consumers need beyond the dependency.

If the library does networking, note any required permissions (e.g.
`INTERNET`, `ACCESS_NETWORK_STATE`) and — for multicast/SSDP-style discovery —
acquiring a `WifiManager.MulticastLock` so the OS doesn't filter multicast
packets. Mention the minimum supported `minSdk` and any runtime permissions the
host app must request.
