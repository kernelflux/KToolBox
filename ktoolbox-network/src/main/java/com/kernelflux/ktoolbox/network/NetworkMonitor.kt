package com.kernelflux.ktoolbox.network

import android.content.Context
import android.content.Intent


class NetworkMonitor private constructor() {
    private var mWeakListenerMgr: WeakListenerMgr<ConnectivityChangeListener> = WeakListenerMgr()
    private var mReceiverWeakListenerMgr: WeakListenerMgr<ConnectivityBroadcastReceivedListener> =
        WeakListenerMgr()

    interface ConnectivityBroadcastReceivedListener {
        fun onConnectivityBroadcastReceived(context: Context, intent: Intent)
    }

    interface ConnectivityChangeListener {
        fun onConnected(apn: APN)
        fun onConnectivityChanged(apn: APN, apn2: APN)
        fun onDisconnected(apn: APN)
    }

    fun register(listener: ConnectivityChangeListener?) {
        mWeakListenerMgr.register(listener)
    }

    fun unregister(listener: ConnectivityChangeListener?) {
        mWeakListenerMgr.unregister(listener)
    }

    fun register(listener: ConnectivityBroadcastReceivedListener?) {
        mReceiverWeakListenerMgr.register(listener)
    }

    fun unregister(listener: ConnectivityBroadcastReceivedListener?) {
        mReceiverWeakListenerMgr.unregister(listener)
    }

    fun notifyConnected(apn: APN) {
        mWeakListenerMgr.startNotify(object : WeakListenerMgr.INotifyCallback<ConnectivityChangeListener> {
            override fun onNotify(listener: ConnectivityChangeListener) {
                listener.onConnected(apn)
            }
        })
    }

    fun notifyDisconnected(apn: APN) {
        mWeakListenerMgr.startNotify(object : WeakListenerMgr.INotifyCallback<ConnectivityChangeListener> {
            override fun onNotify(listener: ConnectivityChangeListener) {
                listener.onDisconnected(apn)
            }
        })
    }

    fun notifyChanged(lastApn: APN, nowApn: APN) {
        mWeakListenerMgr.startNotify(object : WeakListenerMgr.INotifyCallback<ConnectivityChangeListener> {
            override fun onNotify(listener: ConnectivityChangeListener) {
                listener.onConnectivityChanged(lastApn, nowApn)
            }
        })
    }

    fun notifyConnectivityReceived(context: Context, intent: Intent) {
        mReceiverWeakListenerMgr.startNotify(object :
            WeakListenerMgr.INotifyCallback<ConnectivityBroadcastReceivedListener> {
            override fun onNotify(listener: ConnectivityBroadcastReceivedListener) {
                listener.onConnectivityBroadcastReceived(context, intent)
            }
        })
    }

    companion object {
        @Volatile
        private var instance: NetworkMonitor? = null

        fun getInstance(): NetworkMonitor = instance ?: synchronized(this) {
            instance ?: NetworkMonitor().also { instance = it }
        }
    }
}