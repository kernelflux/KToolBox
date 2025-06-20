package com.kernelflux.ktoolbox

import android.os.Handler
import android.os.Looper
import android.os.Message

object HandlerUtils {
    private val mUiHandler: Handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            (msg.obj as? Runnable)?.run()
            super.handleMessage(msg)
        }
    }

    @JvmStatic
    fun post(runnable: Runnable?) {
        runnable?.also {
            mUiHandler.post(it)
        }
    }

    @JvmStatic
    fun post(runnable: Runnable?, delay: Long) {
        runnable?.also {
            mUiHandler.postDelayed(it, delay)
        }
    }

    @JvmStatic
    fun remove(runnable: Runnable?) {
        runnable?.also {
            mUiHandler.removeCallbacks(it)
        }
    }

    @JvmStatic
    fun clearMsg() {
        mUiHandler.removeCallbacksAndMessages(null)
    }
}