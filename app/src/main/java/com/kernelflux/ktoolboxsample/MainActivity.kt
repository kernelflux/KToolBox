package com.kernelflux.ktoolboxsample

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.kernelflux.ktoolbox.logger.SDKLogger
import com.kernelflux.ktoolbox.thread.SDKThreadManager


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        SDKLogger.log("user", "user enter home page...")

        testCoroutineFeatures()
    }


    private fun testCoroutineFeatures() {
        SDKThreadManager.execute("NetworkRequest") {
            SDKLogger.log("main_page", "NetworkRequest...")
        }


        SDKLogger.enableModules()
        SDKLogger.log("hello", "1234...")
    }


}