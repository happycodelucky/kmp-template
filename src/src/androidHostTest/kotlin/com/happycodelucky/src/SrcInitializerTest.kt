package com.happycodelucky.src

import android.content.Context
import android.content.ContextWrapper
import com.happycodelucky.src.internal.androidApplicationContext
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class SrcInitializerTest {
    /** A Context whose application Context is a distinct object, so the test sees which one is stored. */
    private class ActivityLikeContext(private val app: Context) : ContextWrapper(null) {
        override fun getApplicationContext(): Context = app
    }

    private class AppLikeContext : ContextWrapper(null) {
        override fun getApplicationContext(): Context = this
    }

    // One test, in order: the holder is process-global, so the "not yet
    // initialized" state can only be observed before anything initializes it.
    @Test
    fun capturesTheApplicationContext() {
        assertFailsWith<IllegalStateException> { androidApplicationContext }

        val app = AppLikeContext()
        val returned = SrcInitializer().create(ActivityLikeContext(app))

        assertSame(app, returned)
        assertSame(app, androidApplicationContext)
    }
}
