package com.kernelflux.ktoolbox

import android.util.Log
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue


/**
 * * 通用接口回调管理类
 **/
internal class CommonListenerMgr<T> {
    private val mListenerQueue = ConcurrentLinkedQueue<T>()

    interface IBoolNotifyCallback<T> {
        fun onNotify(listener: T): Boolean
    }

    interface INotifyCallback<T> {
        fun onNotify(listener: T)
    }

    fun addAll(listeners: Collection<T>) {
        mListenerQueue.addAll(listeners)
    }

    fun clear() {
        synchronized(mListenerQueue) { mListenerQueue.clear() }
    }

    fun copy(): LinkedList<T> {
        var linkedList: LinkedList<T>
        synchronized(mListenerQueue) { linkedList = LinkedList(mListenerQueue) }
        return linkedList
    }

    fun hasContain(listener: T?): Boolean {
        if (listener == null) {
            return false
        }
        synchronized(mListenerQueue) {
            val it: Iterator<T> = mListenerQueue.iterator()
            while (it.hasNext()) {
                if (it.next() === listener) {
                    return true
                }
            }
            return false
        }
    }

    fun register(listener: T?) {
        if (listener != null) {
            synchronized(mListenerQueue) {
                var z = false
                val it = mListenerQueue.iterator()
                while (it.hasNext()) {
                    val next = it.next()
                    if (next == null) {
                        it.remove()
                    } else if (next === listener) {
                        z = true
                    }
                }
                if (!z) {
                    mListenerQueue.add(listener)
                }
            }
        }
    }

    fun reverseNotify(boolNotifyCallback: IBoolNotifyCallback<T>): Boolean {
        val listeners = copy()
        if (listeners.isEmpty()) {
            return false
        }
        for (size in listeners.indices.reversed()) {
            val listener: T? = listeners[size]
            if (listener != null) {
                try {
                    if (boolNotifyCallback.onNotify(listener)) {
                        return true
                    }
                } catch (th: Throwable) {
                    th.printStackTrace()
                    Log.e("ListenerMgr", "reverseNotify:", th)
                }
            }
        }
        return false
    }

    fun size(): Int {
        var size: Int
        synchronized(mListenerQueue) { size = mListenerQueue.size }
        return size
    }

    fun startNotify(iNotifyCallback: INotifyCallback<T>) {
        val listeners = copy()
        for (listener in listeners) {
            if (listener != null) {
                try {
                    iNotifyCallback.onNotify(listener)
                } catch (th: Throwable) {
                    th.printStackTrace()
                    Log.e("ListenerMgr", "startNotify:", th)
                }
            }
        }
    }

    fun unregister(listener: T?) {
        if (listener != null) {
            synchronized(mListenerQueue) {
                val it = mListenerQueue.iterator()
                while (it.hasNext()) {
                    if (it.next() === listener) {
                        it.remove()
                        return
                    }
                }
            }
        }
    }
}