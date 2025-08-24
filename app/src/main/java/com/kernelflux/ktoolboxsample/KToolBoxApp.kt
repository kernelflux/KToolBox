package com.kernelflux.ktoolboxsample

import android.app.Application
import com.kernelflux.ktoolbox.logger.SDKLogger
import com.kernelflux.ktoolbox.thread.SDKThreadManager

class KToolBoxApp : Application() {

    override fun onCreate() {
        super.onCreate()

        initLoggerSDK()
        initThreadSDK()
    }


    private fun initLoggerSDK() {
        SDKLogger.initialize(
            this,
            enableLogcat = true
        )
        SDKLogger.registerLogModule(
            SDKLogger.LogModule(
                "KToolBox",
                "com.kernelflux.ktoolbox"
            ),
            "user",
            "main_page"
        )
    }


    private fun initThreadSDK() {
        SDKThreadManager.initialize()
    }

}