package com.kernelflux.ktoolbox.core

import android.content.Context
import android.util.TypedValue
import kotlin.math.roundToInt

/**
 * 常用单位转换的 Kotlin 扩展方法
 */
val Context.density: Float
    get() = resources.displayMetrics.density

val Context.scaledDensity: Float
    get() = resources.displayMetrics.scaledDensity

val Context.screenWidthPx: Int
    get() = resources.displayMetrics.widthPixels

val Context.screenHeightPx: Int
    get() = resources.displayMetrics.heightPixels

fun Context.dp2px(dp: Float): Int =
    (dp * density).roundToInt()

fun Context.px2dp(px: Float): Float =
    px / density

fun Context.sp2px(sp: Float): Int =
    (sp * scaledDensity).roundToInt()

fun Context.px2sp(px: Float): Float =
    px / scaledDensity

fun Context.dp(dp: Number): Int =
    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(), resources.displayMetrics)
        .roundToInt()

fun Context.sp(sp: Number): Int =
    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp.toFloat(), resources.displayMetrics)
        .roundToInt()