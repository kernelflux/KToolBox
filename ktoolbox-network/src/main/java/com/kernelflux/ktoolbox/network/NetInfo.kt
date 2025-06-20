package com.kernelflux.ktoolbox.network

import android.net.NetworkInfo


enum class APN {
    UN_DETECT,
    WIFI,
    CMWAP,
    CMNET,
    UNIWAP,
    UNINET,
    WAP3G,
    NET3G,
    CTWAP,
    CTNET,
    UNKNOWN,
    UNKNOWN_WAP,
    NO_NETWORK,
    LTE,
    ETHERNET,
    NR;
}


data class NetInfo(
    var apn: APN = APN.UN_DETECT,
    //返回MCC+MNC代码 (SIM卡运营商国家代码和运营商网络代码)(IMSI)
    var IMSI: String = "",
    //手机网络类型
    var networkType: Int = -1,
    //是否有代理
    var isWap: Boolean = false,
    var networkOperator: String = "",
    //通常，手机WLAN中，BSSID其实就是无线路由的MAC地址。
    var bssId: String = "",
    //用于标识无线局域网，网络名
    var ssId: String? = "",
    var networkInfo: NetworkInfo? = null
)

