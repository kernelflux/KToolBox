package com.kernelflux.ktoolbox.display

import android.app.Activity
import android.content.Context
import android.graphics.Point
import android.graphics.Rect
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.*
import java.lang.ref.WeakReference


interface WindowInsetsProvider {
    fun getRealSize(): Point
    fun getUsableSize(): Point
    fun getDisplayCutout(): Rect
    fun getSafeInsets(): Rect

    fun isInMultiWindowMode(): Boolean
    fun isInPictureInPictureMode(): Boolean
    fun invalidate()
    fun applyToViewPadding(view: View, types: Int = WindowInsetsCompat.Type.systemBars())
    fun applyToViewMargin(view: View, types: Int = WindowInsetsCompat.Type.systemBars())

    fun onAttach(owner: LifecycleOwner) {}
    fun onDetach() {}

    companion object {
        @JvmStatic
        fun from(activity: AppCompatActivity): WindowInsetsProvider = Impl(activity)

        @JvmStatic
        fun from(fragment: Fragment): WindowInsetsProvider =
            fragment.activity?.let { Impl(it) } ?: EmptyProvider

        @JvmStatic
        fun from(view: View): WindowInsetsProvider =
            (view.context as? AppCompatActivity)?.let { Impl(it) } ?: EmptyProvider

        @JvmStatic
        fun from(context: Context): WindowInsetsProvider =
            (context as? AppCompatActivity)?.let { Impl(it) } ?: EmptyProvider
    }

    object EmptyProvider : WindowInsetsProvider {
        override fun getRealSize() = Point(0, 0)
        override fun getUsableSize() = Point(0, 0)
        override fun getDisplayCutout() = Rect(0, 0, 0, 0)
        override fun getSafeInsets() = Rect(0, 0, 0, 0)
        override fun isInMultiWindowMode(): Boolean = false

        override fun isInPictureInPictureMode(): Boolean = false

        override fun invalidate() {
        }

        override fun applyToViewPadding(view: View, types: Int) {
        }

        override fun applyToViewMargin(view: View, types: Int) {
        }
    }

    class Impl(activity: Activity) : WindowInsetsProvider, DefaultLifecycleObserver {
        private val activityRef = WeakReference(activity)

        @Volatile
        private var realSize: Point? = null

        @Volatile
        private var usableSize: Point? = null

        @Volatile
        private var cutout: Rect? = null

        override fun getRealSize(): Point {
            return realSize ?: calculateRealSize().also { realSize = it }
        }

        override fun getUsableSize(): Point {
            return usableSize ?: calculateUsableSize().also { usableSize = it }
        }

        override fun getDisplayCutout(): Rect {
            return cutout ?: calculateCutout().also { cutout = it }
        }

        override fun getSafeInsets(): Rect {
            val activity = activityRef.get() ?: return Rect()
            val rootInsets = ViewCompat.getRootWindowInsets(activity.window.decorView)
                ?: return Rect()
            val insets = rootInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            return Rect(insets.left, insets.top, insets.right, insets.bottom)
        }

        override fun isInMultiWindowMode(): Boolean {
            val activity = activityRef.get() ?: return false
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                activity.isInMultiWindowMode
            } else false
        }

        override fun isInPictureInPictureMode(): Boolean {
            val activity = activityRef.get() ?: return false
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                activity.isInPictureInPictureMode
            } else false
        }

        override fun invalidate() {
            realSize = null
            usableSize = null
            cutout = null
        }

        override fun onResume(owner: LifecycleOwner) {
            super.onResume(owner)
            activityRef.get()?.window?.decorView?.setOnApplyWindowInsetsListener { v, insets ->
                invalidate()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    WindowInsets.CONSUMED
                } else {
                    @Suppress("DEPRECATION")
                    insets.consumeSystemWindowInsets()
                }
            }
        }

        @Suppress("DEPRECATION")
        private fun calculateRealSize(): Point {
            val activity = activityRef.get() ?: return Point()
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                activity.display?.let {
                    val bounds = activity.windowManager.currentWindowMetrics.bounds
                    Point(bounds.width(), bounds.height())
                } ?: Point()
            } else {
                val display = activity.windowManager.defaultDisplay
                Point().also { display.getRealSize(it) }
            }
        }

        @Suppress("DEPRECATION")
        private fun calculateUsableSize(): Point {
            val activity = activityRef.get() ?: return Point()
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val bounds = activity.windowManager.currentWindowMetrics.bounds
                Point(bounds.width(), bounds.height())
            } else {
                Rect().also {
                    val display = activity.windowManager.defaultDisplay
                    display.getRectSize(it)
                }.let { Point(it.width(), it.height()) }
            }
        }

        private fun calculateCutout(): Rect {
            val activity = activityRef.get() ?: return Rect()
            val insets = ViewCompat.getRootWindowInsets(activity.window.decorView)
            val cut = insets?.displayCutout
            return cut?.let {
                Rect(it.safeInsetLeft, it.safeInsetTop, it.safeInsetRight, it.safeInsetBottom)
            } ?: Rect()
        }

        override fun applyToViewPadding(view: View, types: Int) {
            ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
                val systemInsets = insets.getInsets(types)
                v.setPadding(
                    systemInsets.left,
                    systemInsets.top,
                    systemInsets.right,
                    systemInsets.bottom
                )
                insets
            }
            ViewCompat.requestApplyInsets(view)
        }

        override fun applyToViewMargin(view: View, types: Int) {
            ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
                val systemInsets = insets.getInsets(types)
                val lp = v.layoutParams
                if (lp is ViewGroup.MarginLayoutParams) {
                    lp.setMargins(
                        systemInsets.left,
                        systemInsets.top,
                        systemInsets.right,
                        systemInsets.bottom
                    )
                    v.layoutParams = lp
                }
                insets
            }
            ViewCompat.requestApplyInsets(view)
        }

    }
}

class WindowInsetsLifecycleBinder(
    private val provider: WindowInsetsProvider
) : DefaultLifecycleObserver {
    override fun onCreate(owner: LifecycleOwner) {
        provider.onAttach(owner)
    }

    override fun onDestroy(owner: LifecycleOwner) {
        provider.onDetach()
    }
}

fun AppCompatActivity.bindInsetsProvider() {
    lifecycle.addObserver(WindowInsetsLifecycleBinder(WindowInsetsProvider.from(this)))
}

fun Fragment.bindInsetsProvider() {
    viewLifecycleOwner.lifecycle.addObserver(
        WindowInsetsLifecycleBinder(
            WindowInsetsProvider.from(
                this
            )
        )
    )
}

fun View.bindInsetsProvider(lifecycleOwner: LifecycleOwner? = findViewTreeLifecycleOwner()) {
    val owner = lifecycleOwner ?: return
    owner.lifecycle.addObserver(WindowInsetsLifecycleBinder(WindowInsetsProvider.from(this)))
}




