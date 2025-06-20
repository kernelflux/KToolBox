package com.kernelflux.sdk.common.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Parcelable
import android.text.TextUtils
import com.kernelflux.ktoolbox.network.NetworkMonitor

class NetworkMonitorReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        ThreadManager.getInstance().execTask {
            if (TextUtils.equals(MSG_SYS_CONNECTIVITY_ACTION, intent.action) &&
                intent.getParcelableExtra<Parcelable?>("networkInfo") != null
            ) {
                NetworkUtils.refreshNetwork()
                NetworkMonitor.getInstance().notifyConnectivityReceived(context, intent)
            }
        }
    }

    companion object {
        const val MSG_SYS_CONNECTIVITY_ACTION = "android.net.conn.CONNECTIVITY_CHANGE"
    }
}