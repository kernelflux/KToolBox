package com.kernelflux.ktoolbox.display

import android.content.Context

object AppContextProvider {
    private lateinit var appContext: Context

    @JvmStatic
    fun init(context: Context) {
        appContext = context.applicationContext
    }

    @JvmStatic
    fun get(): Context {
        check(::appContext.isInitialized) {
            "AppContextProvider is not initialized. Make sure it's set via AppInitProvider or manually in Application."
        }
        return appContext
    }
}