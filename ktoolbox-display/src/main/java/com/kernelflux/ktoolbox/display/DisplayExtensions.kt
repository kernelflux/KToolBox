package com.kernelflux.ktoolbox.display

import android.util.TypedValue

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