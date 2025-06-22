package com.kernelflux.ktoolbox.display

import android.content.Context
import java.lang.ref.WeakReference

object ViewContextHolder {
    private var contextRef: WeakReference<Context>? = null

    @JvmStatic
    fun attach(context: Context) {
        contextRef = WeakReference(context)
    }

    @JvmStatic
    fun get(): Context {
        return contextRef?.get() ?: AppContextProvider.get()
    }

    @JvmStatic
    fun clear() {
        contextRef?.clear()
        contextRef = null
    }
}