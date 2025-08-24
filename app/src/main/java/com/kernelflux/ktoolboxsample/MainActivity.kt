package com.kernelflux.ktoolboxsample

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.kernelflux.ktoolbox.logger.SDKLogger


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        SDKLogger.log("user", "user enter home page...")
    }
}