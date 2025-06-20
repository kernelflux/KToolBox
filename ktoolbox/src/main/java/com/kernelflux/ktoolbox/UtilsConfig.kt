package com.kernelflux.ktoolbox

import android.app.Application


object UtilsConfig {
    private var isUserAgreePrivateProtocol = false
    private var sApplication: Application? = null
    private var sVersionCode: Int = 0
    private var sVersionName: String = ""

    @JvmStatic
    fun init(
        application: Application,
        versionCode: Int,
        versionName: String
    ) {
        sApplication = application
        sVersionCode = versionCode
        sVersionName = versionName

    }

    @JvmStatic
    fun getAppContext(): Application? = sApplication

    @JvmStatic
    fun isIsUserAgreePrivateProtocol(): Boolean = isUserAgreePrivateProtocol

    @JvmStatic
    fun setIsUserAgreePrivateProtocol(flag: Boolean) {
        isUserAgreePrivateProtocol = flag
    }

    @JvmStatic
    fun getVersionCode(): Int = sVersionCode

    @JvmStatic
    fun getVersionName(): String = sVersionName

}