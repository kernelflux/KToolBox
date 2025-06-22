package com.kernelflux.ktoolbox.display

import android.content.Context
import android.util.TypedValue
import android.view.View
import androidx.annotation.DimenRes

val Float.dp: Int
    get() = toPx(this, isSp = false)

val Float.sp: Int
    get() = toPx(this, isSp = true)

val Int.dp: Int
    get() = toPx(this.toFloat(), isSp = false)

val Int.sp: Int
    get() = toPx(this.toFloat(), isSp = true)

private fun toPx(value: Float, isSp: Boolean): Int {
    val context = ViewContextHolder.get()
    val metrics = context.resources.displayMetrics
    val unit = if (isSp) TypedValue.COMPLEX_UNIT_SP else TypedValue.COMPLEX_UNIT_DIP
    return TypedValue.applyDimension(unit, value, metrics).toInt()
}

fun Context.dimenPx(@DimenRes resId: Int): Int =
    resources.getDimensionPixelSize(resId)

fun Context.dimenFloat(@DimenRes resId: Int): Float =
    resources.getDimension(resId)

fun View.dimenPx(@DimenRes resId: Int): Int =
    context.dimenPx(resId)

fun View.dimenFloat(@DimenRes resId: Int): Float =
    context.dimenFloat(resId)