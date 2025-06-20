package com.kernelflux.sdk.common.util

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.net.Proxy
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.telephony.ServiceState
import android.telephony.TelephonyManager
import android.text.TextUtils
import android.util.Log
import com.kernelflux.ktoolbox.network.APN
import com.kernelflux.ktoolbox.network.NetInfo
import com.kernelflux.ktoolbox.network.NetworkMonitor
import java.net.NetworkInterface
import java.util.*


object NetworkUtils {
    private val TAG = NetworkUtils::class.java.simpleName
    private var isHotSpotWifi = true
    private var isNetworkActive = true
    private var sAppContext: Context? = UtilsConfig.getAppContext()
    private var netInfo: NetInfo = getNetInfo(getAppContext())

    @JvmStatic
    fun getApn(): APN {
        return getNetInfo().apn
    }

    @Synchronized
    fun getNetInfo(): NetInfo {
        var sNetInfo: NetInfo
        synchronized(NetworkUtils::class.java) {
            if (netInfo.apn == APN.UN_DETECT) {
                refreshNetwork()
            }
            sNetInfo = netInfo
        }
        return sNetInfo
    }

    private fun getAppContext(): Context? {
        if (sAppContext != null) {
            return sAppContext
        }

        val context = AndroidUtils.getCurrentApplication()
        if (context != null) {
            setAppContext(context)
        }
        return sAppContext
    }

    fun setAppContext(context: Context) {
        if (sAppContext != null) {
            return
        }
        if (context is Application) {
            sAppContext = context
            return
        }
        val applicationContext = context.applicationContext
        if (applicationContext != null) {
            sAppContext = applicationContext
        }
    }

    @JvmStatic
    fun getCacheApn(): APN {
        return netInfo.apn
    }

    @JvmStatic
    fun getGroupNetType(): Int {
        if (isWifi()) {
            return 3
        }
        if (is5G()) {
            return 6
        }
        if (is4G()) {
            return 5
        }
        if (is3G()) {
            return 2
        }
        return if (is2G()) 1 else 4
    }

    @JvmStatic
    fun getIsHotWifi(): Boolean {
        return isHotSpotWifi
    }

    @JvmStatic
    fun isWap(): Boolean {
        return !TextUtils.isEmpty(Proxy.getDefaultHost())
    }

    @JvmStatic
    fun isWifi(): Boolean {
        return getApn() == APN.WIFI
    }

    @JvmStatic
    fun is2G(): Boolean {
        val apn = getApn()
        return apn == APN.CMNET || apn == APN.CMWAP || apn == APN.UNINET || apn == APN.UNIWAP
    }

    @JvmStatic
    fun is3G(): Boolean {
        val apn = getApn()
        return apn === APN.CTWAP || apn === APN.CTNET || apn === APN.WAP3G || apn === APN.NET3G
    }

    @JvmStatic
    fun is4G(): Boolean {
        return getApn() == APN.LTE
    }

    @JvmStatic
    fun is5G(): Boolean {
        return getApn() == APN.NR
    }

    @JvmStatic
    fun isNetworkActive(): Boolean {
        if (netInfo.apn == APN.UN_DETECT || netInfo.apn == APN.NO_NETWORK) {
            refreshNetwork()
        }
        return isNetworkActive
    }

    @JvmStatic
    fun isNetworkConnected(context: Context): Boolean {
        val networkInfo: NetworkInfo? = getNetWorkInfo()
        return networkInfo != null && networkInfo.isConnected
    }

    @JvmStatic
    fun getMobileNetInfo(context: Context?, netInfo: NetInfo): NetInfo {
        val isWap = isWap()
        netInfo.isWap = isWap
        val networkOperator =
            (context?.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager)?.networkOperator
                ?: ""
        netInfo.networkOperator = networkOperator
        val networkType = getNetworkType(context)
        Log.i(TAG, "getMobileNetInfo, networkType:$networkType")
        netInfo.networkType = networkType
        if (networkType == TelephonyManager.NETWORK_TYPE_NR) {
            netInfo.apn = APN.NR
            return netInfo
        } else if (networkType == TelephonyManager.NETWORK_TYPE_LTE) {
            netInfo.apn = APN.LTE
            return netInfo
        } else {
            return when (getSimOperator(networkOperator)) {
                0 -> {
                    when (networkType) {
                        TelephonyManager.NETWORK_TYPE_GPRS, TelephonyManager.NETWORK_TYPE_EDGE -> {
                            if (isWap) {
                                netInfo.apn = APN.CMWAP
                            } else {
                                netInfo.apn = APN.CMNET
                            }
                            netInfo
                        }

                        TelephonyManager.NETWORK_TYPE_HSDPA, TelephonyManager.NETWORK_TYPE_HSUPA, TelephonyManager.NETWORK_TYPE_HSPA, TelephonyManager.NETWORK_TYPE_EHRPD, TelephonyManager.NETWORK_TYPE_HSPAP -> {
                            if (isWap) {
                                netInfo.apn = APN.WAP3G
                            } else {
                                netInfo.apn = APN.NET3G
                            }
                            netInfo
                        }

                        else -> {
                            if (isWap) {
                                netInfo.apn = APN.UNKNOWN_WAP
                            } else {
                                netInfo.apn = APN.UNKNOWN
                            }
                            netInfo
                        }
                    }
                }

                1 -> {
                    when (networkType) {
                        TelephonyManager.NETWORK_TYPE_GPRS, TelephonyManager.NETWORK_TYPE_EDGE -> {
                            if (isWap) {
                                netInfo.apn = APN.UNIWAP
                            } else {
                                netInfo.apn = APN.UNINET
                            }
                            netInfo
                        }

                        TelephonyManager.NETWORK_TYPE_UMTS, TelephonyManager.NETWORK_TYPE_HSDPA, TelephonyManager.NETWORK_TYPE_HSUPA, TelephonyManager.NETWORK_TYPE_HSPA, TelephonyManager.NETWORK_TYPE_EHRPD, TelephonyManager.NETWORK_TYPE_HSPAP -> {
                            if (isWap) {
                                netInfo.apn = APN.WAP3G
                            } else {
                                netInfo.apn = APN.NET3G
                            }
                            netInfo
                        }

                        else -> {
                            if (isWap) {
                                netInfo.apn = APN.UNKNOWN_WAP
                            } else {
                                netInfo.apn = APN.UNKNOWN
                            }
                            netInfo
                        }
                    }
                }

                2 -> {
                    if (networkType != TelephonyManager.NETWORK_TYPE_EVDO_B && networkType != TelephonyManager.NETWORK_TYPE_EVDO_0 && networkType != TelephonyManager.NETWORK_TYPE_EVDO_A) {
                        if (isWap) {
                            netInfo.apn = APN.UNIWAP
                        } else {
                            netInfo.apn = APN.UNINET
                        }
                    } else {
                        if (isWap) {
                            netInfo.apn = APN.CTWAP
                        } else {
                            netInfo.apn = APN.CTNET
                        }
                    }
                    netInfo
                }

                else -> {
                    if (isWap) {
                        netInfo.apn = APN.UNKNOWN_WAP
                    } else {
                        netInfo.apn = APN.UNKNOWN
                    }
                    netInfo
                }
            }
        }
    }

    /**
     * 获取SIM卡运营商
     *
     * 获取SIM卡的IMSI码
     * SIM卡唯一标识：IMSI 国际移动用户识别码（IMSI：International Mobile Subscriber Identification Number）是区别移动用户的标志，
     * 储存在SIM卡中，可用于区别移动用户的有效信息。IMSI由MCC、MNC、MSIN组成，其中MCC为移动国家号码，由3位数字组成，
     * 唯一地识别移动客户所属的国家，我国为460；MNC为网络id，由2位数字组成，
     * 用于识别移动客户所归属的移动网络，
     *
     *
     * 46000 中国移动 （GSM）
     *
     * 46001 中国联通 （GSM）
     *
     * 46002 中国移动 （TD-S）
     *
     * 46003 中国电信（CDMA）
     *
     * 46004 空（似乎是专门用来做测试的）
     *
     * 46005 中国电信 （CDMA）
     *
     * 46006 中国联通 （WCDMA）
     *
     * 46007 中国移动 （TD-S）
     *
     * MSIN为移动客户识别码，采用等长11位数字构成。
     * 唯一地识别国内GSM移动通信网中移动客户。所以要区分是移动还是联通，只需取得SIM卡中的MNC字段即可
     *
     * @return
     *   0  移动 /  1 联通  / 2 电信  / -1 未知
     */
    @JvmStatic
    fun getSimOperator(str: String?): Int {
        if (TextUtils.isEmpty(str)) {
            return -1
        }
        if (str.equals("46000") || str.equals("46002") || str.equals("46007")) {
            return 0
        }
        if (str.equals("46001")) {
            return 1
        }
        return if (str.equals("46003")) 2 else -1
    }

    @JvmStatic
    fun getWifiBSSID(context: Context?): String? {
        val wifiManger: WifiManager? =
            context?.applicationContext?.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        if (wifiManger != null) {
            try {
                val connectionInfo: WifiInfo = wifiManger.connectionInfo ?: return ""
                return connectionInfo.bssid
            } catch (e: Throwable) {
            }
        }
        return null
    }

    @JvmStatic
    @SuppressLint("ObsoleteSdkInt")
    fun getWifiNetStrength(context: Context?): Int {
        if (context == null) {
            return 0
        }
        return try {
            val connectionInfo: WifiInfo? =
                (context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager)?.connectionInfo
            if (connectionInfo == null) {
                0
            } else {
                val rssi = connectionInfo.rssi
                if (Build.VERSION.SDK_INT >= 14) {
                    try {
                        WifiManager.calculateSignalLevel(
                            connectionInfo.rssi,
                            101
                        )
                    } catch (th0: Throwable) {
                        0
                    }
                } else if (rssi <= -100) {
                    0
                } else {
                    if (rssi >= -55) {
                        100
                    } else {
                        ((rssi - -100).toFloat() * 100.toFloat() / 45.toFloat()).toInt()
                    }
                }
            }
        } catch (th: Throwable) {
            0
        }
    }

    @JvmStatic
    fun getWifiSSID(context: Context?): String? {
        val wifiManger: WifiManager? =
            context?.applicationContext?.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        if (wifiManger != null) {
            try {
                val connectionInfo: WifiInfo = wifiManger.connectionInfo ?: return ""
                return connectionInfo.ssid
            } catch (e: Throwable) {
            }
        }
        return null
    }

    @JvmStatic
    fun getNetWorkInfo(): NetworkInfo? {
        return getNetInfo().networkInfo
    }

    @SuppressLint("MissingPermission")
    @JvmStatic
    fun getNetworkType(context: Context?): Int {
        if (context == null) {
            return TelephonyManager.NETWORK_TYPE_UNKNOWN
        }
        val networkType: Int
        return try {
            val telephonyManager =
                context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            if (telephonyManager == null) {
                TelephonyManager.NETWORK_TYPE_UNKNOWN
            } else {
                networkType = telephonyManager.networkType
                if (networkType != TelephonyManager.NETWORK_TYPE_LTE) {
                    networkType
                } else {
                    try {
                        var invalidServeStateCase = false
                        var serviceState: ServiceState? = null
                        if (Build.VERSION.SDK_INT >= 26) {
                            serviceState = telephonyManager.serviceState
                            invalidServeStateCase = serviceState == null
                        }
                        if (Build.VERSION.SDK_INT < 29 || context.checkSelfPermission(android.Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED || invalidServeStateCase) {
                            networkType
                        } else {
                            val tempServiceState = ReflectUtil.invokeMethod(
                                "android.telephony.ServiceState",
                                "getNrState",
                                obj = serviceState,
                                arrayOfNulls<Class<*>>(0),
                                arrayOfNulls<Any>(0)
                            ) as Int
                            if (tempServiceState == ServiceState.STATE_EMERGENCY_ONLY || tempServiceState == ServiceState.STATE_POWER_OFF) {
                                //5G
                                TelephonyManager.NETWORK_TYPE_NR
                            } else {
                                networkType
                            }
                        }
                    } catch (th2: Throwable) {
                        th2.printStackTrace()
                        networkType
                    }
                }
            }
        } catch (th: Throwable) {
            th.printStackTrace()
            0
        }
    }

    @JvmStatic
    fun setIsHotWifi(flag: Boolean) {
        isHotSpotWifi = flag
    }

    @SuppressLint("MissingPermission")
    @JvmStatic
    private fun getNetInfo(context: Context?): NetInfo {
        val netInfo = NetInfo()
        var networkInfo: NetworkInfo? = null
        try {
            val connectivityManager =
                context?.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            if (connectivityManager != null) {
                networkInfo = connectivityManager.activeNetworkInfo
            }
            netInfo.networkInfo = networkInfo
            if (networkInfo == null || !networkInfo.isAvailable) {
                isNetworkActive = false
                netInfo.apn = APN.NO_NETWORK
                return netInfo
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        isNetworkActive = true
        if (networkInfo != null && networkInfo.type == ConnectivityManager.TYPE_WIFI) {
            netInfo.apn = APN.WIFI
            val wifiManager =
                context?.applicationContext?.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            if (wifiManager != null) {
                try {
                    val wifiInfo = wifiManager.connectionInfo
                    if (wifiInfo != null) {
                        netInfo.bssId = wifiInfo.bssid
                        netInfo.ssId = wifiInfo.ssid
                    }
                } catch (th: Throwable) {
                    th.printStackTrace()
                }
            }
            return netInfo
        } else if (networkInfo == null || networkInfo.type != ConnectivityManager.TYPE_ETHERNET) {
            getMobileNetInfo(context, netInfo)
            return netInfo
        } else {
            netInfo.apn = APN.ETHERNET
            return netInfo
        }
    }

    @JvmStatic
    fun refreshNetwork() {
        val apn = netInfo.apn
        val ssid: String? = netInfo.ssId
        netInfo = getNetInfo(getAppContext())
        if (apn != netInfo.apn) {
            Log.i(TAG, "refreshNetwork 1, netInfo, apn= ${netInfo.apn}, lastApn =$apn")
            when {
                apn == APN.NO_NETWORK -> {
                    NetworkMonitor.getInstance().notifyConnected(netInfo.apn)
                }

                netInfo.apn == APN.NO_NETWORK -> {
                    NetworkMonitor.getInstance().notifyDisconnected(apn)
                }

                else -> {
                    NetworkMonitor.getInstance().notifyChanged(apn, netInfo.apn)
                }
            }
        } else if (apn === APN.WIFI && netInfo.apn == APN.WIFI && !TextUtils.isEmpty(ssid) && ssid != netInfo.ssId) {
            NetworkMonitor.getInstance().notifyChanged(apn, netInfo.apn)
        }
    }


    @SuppressLint("MissingPermission")
    @JvmStatic
    fun isConfiguredTargetWifi(ssid: String?, context: Context?): WifiConfiguration? {
        val configuredNetworks =
            (context?.applicationContext?.getSystemService(Context.WIFI_SERVICE) as? WifiManager)?.configuredNetworks
        if (!TextUtils.isEmpty(ssid) && configuredNetworks != null) {
            for (wifiConfiguration in configuredNetworks) {
                val wifiConfigurationSSID = wifiConfiguration?.SSID
                if (!(wifiConfiguration == null || !wifiConfigurationSSID.equals(ssid))) {
                    return wifiConfiguration
                }
            }
        }
        return null
    }

    @JvmStatic
    fun isMobile(): Boolean {
        return isMobileNetwork(null)
    }

    @JvmStatic
    fun isMobileNetwork(context: Context?): Boolean {
        val netWorkInfo = getNetWorkInfo()
        if (netWorkInfo == null || !netWorkInfo.isConnected) {
            return false
        }
        val type = netWorkInfo.type
        if (type == TelephonyManager.NETWORK_TYPE_UNKNOWN) {
            return true
        }

        return when (type) {
            TelephonyManager.NETWORK_TYPE_EDGE, TelephonyManager.NETWORK_TYPE_UMTS, TelephonyManager.NETWORK_TYPE_CDMA, TelephonyManager.NETWORK_TYPE_EVDO_0 -> {
                true
            }

            else -> {
                false
            }
        }
    }


    @JvmStatic
    fun getLocalIpAddresses(): Map<String, String> {
        val linkedHashMap = LinkedHashMap<String, String>()
        try {
            val networkInterfaces = NetworkInterface.getNetworkInterfaces()
            if (networkInterfaces != null) {
                while (networkInterfaces.hasMoreElements()) {
                    val nextElement = networkInterfaces.nextElement()
                    val inetAddresses = nextElement.inetAddresses
                    while (inetAddresses.hasMoreElements()) {
                        val nextElement2 = inetAddresses.nextElement()
                        if (!nextElement2.isLoopbackAddress && InetAddressUtils.isIPv4Address(
                                nextElement2.hostAddress
                            )
                        ) {
                            linkedHashMap[nextElement.displayName] = nextElement2.hostAddress ?: ""
                            break
                        }
                    }
                }
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
        return linkedHashMap
    }


    @SuppressLint("MissingPermission")
    @JvmStatic
    fun getActiveNetworkInfo(context: Context?): NetworkInfo? {
        if (context == null) {
            return null
        }
        try {
            val connectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            if (connectivityManager != null) {
                return connectivityManager.activeNetworkInfo
            }
        } catch (th: Throwable) {
            th.printStackTrace()
        }
        return null
    }


    @JvmStatic
    fun getActiveNetworkType(context: Context?): Int {
        val activeNetworkInfo = getActiveNetworkInfo(context)
        return activeNetworkInfo?.type ?: -1
    }


    /**
     * 获取内网IP地址
     */
    @JvmStatic
    fun getLocalIpAddress(context: Context): String {
        val localIpAddresses = getLocalIpAddresses()
        if (localIpAddresses.size <= 1) {
            val it = localIpAddresses.values.iterator()
            return if (it.hasNext()) it.next() else "0.0.0.0"
        }
        val appContext = context.applicationContext
        val activeNetworkType: Int = getActiveNetworkType(appContext)
        if (activeNetworkType == ConnectivityManager.TYPE_WIFI) {
            val wifiManager = appContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            if (wifiManager != null) {
                val connectionInfo: WifiInfo? = wifiManager.connectionInfo
                if (wifiManager.isWifiEnabled &&
                    wifiManager.wifiState == WifiManager.WIFI_STATE_ENABLED &&
                    connectionInfo != null
                ) {
                    val ipAddress = connectionInfo.ipAddress
                    if (ipAddress != 0) {
                        return (ipAddress and 255).toString() + "." + (ipAddress shr 8 and 255) + "." + (ipAddress shr 16 and 255) + "." + (ipAddress shr 24 and 255)
                    }
                }
            }
            val str = localIpAddresses["wlan0"]
            if (!TextUtils.isEmpty(str)) {
                return str!!
            }
            for ((key, value) in localIpAddresses) {
                if (key.startsWith("wlan")) {
                    return value
                }
            }
        } else if (activeNetworkType == ConnectivityManager.TYPE_ETHERNET) {
            val str2 = localIpAddresses["eth0"]
            if (!TextUtils.isEmpty(str2)) {
                return str2!!
            }
            for ((key2, value) in localIpAddresses) {
                if (key2.startsWith("eth")) {
                    return value
                }
            }
        }
        val it2 = localIpAddresses.values.iterator()
        return if (it2.hasNext()) it2.next() else "0.0.0.0"
    }
}