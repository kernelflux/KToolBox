package com.kernelflux.ktoolboxsample

import android.app.Application
import com.kernelflux.ktoolbox.logger.SDKLogger

class KToolBoxApp : Application() {

    override fun onCreate() {
        super.onCreate()

        initLoggerSDK()
    }


    private fun initLoggerSDK() {
        SDKLogger.initialize(
            this,
            enableLogcat = true
        )
        SDKLogger.registerModules("user")
        SDKLogger.enableModules("user")
    }


}