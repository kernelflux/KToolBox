package com.kernelflux.ktoolbox.display

import android.util.TypedValue
import android.view.View
import androidx.annotation.DimenRes
import androidx.fragment.app.Fragment

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

fun dimenPx(@DimenRes resId: Int): Int =
    ViewContextHolder.get().resources.getDimensionPixelSize(resId)

fun dimenFloat(@DimenRes resId: Int): Float =
    ViewContextHolder.get().resources.getDimension(resId)

fun View.dimenPx(@DimenRes resId: Int): Int = dimenPx(resId)
fun Fragment.dimenPx(@DimenRes resId: Int): Int = dimenPx(resId)

fun View.dimenFloat(@DimenRes resId: Int): Float = dimenFloat(resId)
fun Fragment.dimenFloat(@DimenRes resId: Int): Float = dimenFloat(resId)