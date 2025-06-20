package com.kernelflux.ktoolbox.core

import android.os.Handler
import android.os.Looper
import android.util.Log
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.RuntimeException


class WeakListenerMgr<T>() {
    private val mListenerQueue = ConcurrentLinkedQueue<WeakReference<T>>()

    interface INotifyCallback<T> {
        fun onNotify(listener: T)
    }

    fun clear() {
        synchronized(mListenerQueue) { mListenerQueue.clear() }
    }

    fun register(listener: T?) {
        if (listener != null) {
            synchronized(mListenerQueue) {
                var z: Boolean = false
                val it: MutableIterator<WeakReference<T>?> = mListenerQueue.iterator()
                while (it.hasNext()) {
                    val t3: T? = it.next()!!.get()
                    if (t3 == null) {
                        it.remove()
                    } else if (t3 === listener) {
                        z = true
                    }
                }
                if (!z) {
                    mListenerQueue.add(WeakReference(listener))
                }
            }
        }
    }

    fun size(): Int {
        var size: Int
        synchronized(mListenerQueue) { size = mListenerQueue.size }
        return size
    }


    fun startNotify(iNotifyCallback: INotifyCallback<T>) {
        var concurrentLinkedQueue: ConcurrentLinkedQueue<WeakReference<T>>?
        synchronized(mListenerQueue) {
            concurrentLinkedQueue = if (mListenerQueue.size > 0) ConcurrentLinkedQueue<WeakReference<T>>(
                mListenerQueue
            ) else null
        }
        if (concurrentLinkedQueue != null) {
            try {
                val listeners: Iterator<WeakReference<T>> = concurrentLinkedQueue!!.iterator()
                while (listeners.hasNext()) {
                    val obj = listeners.next().get()
                    if (obj != null) {
                        try {
                            iNotifyCallback.onNotify(obj)
                        } catch (th: Throwable) {
                            th.printStackTrace()
                            Log.e("crash", th.toString(), th)
                            if (isDebug) {
                                Handler(Looper.getMainLooper()).post {
                                    throw RuntimeException(th)
                                }
                            }
                        }
                    }
                }
            } catch (unused: Throwable) {
            }
        }
    }

    fun unregister(listener: T?) {
        if (listener != null) {
            synchronized(mListenerQueue) {
                val it: MutableIterator<WeakReference<T>> = mListenerQueue.iterator()
                while (it.hasNext()) {
                    if (it.next().get() === listener) {
                        it.remove()
                        return
                    }
                }
            }
        }
    }

    companion object {
        private var isDebug = false

        fun setDebug(z: Boolean) {
            isDebug = z
        }
    }
}