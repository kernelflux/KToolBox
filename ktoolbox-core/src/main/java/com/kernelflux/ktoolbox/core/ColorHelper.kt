package com.kernelflux.ktoolbox.core

import androidx.core.graphics.toColorInt


object ColorHelper {

    @JvmStatic
    fun parseColor(colorStr: String?, defaultColor: Int): Int {
        return colorStr?.let {
            return try {
                it.trim().toColorInt()
            } catch (th: Throwable) {
                defaultColor
            }
        } ?: defaultColor
    }

    @JvmStatic
    fun parseColor(colorStr: String?): Int {
        return parseColor(colorStr, 0)
    }


}