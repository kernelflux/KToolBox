package com.kernelflux.ktoolbox.thread

import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.Log
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.Callable
import java.util.concurrent.ExecutionException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Future
import java.util.concurrent.SynchronousQueue
import java.util.concurrent.ThreadFactory
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.min

class ThreadManager private constructor() {


    class WrapperExecutorService(private val mOriginExecutorService: ExecutorService) : ExecutorService {
        private var mRealExecutorService: ExecutorService

        init {
            this.mRealExecutorService = mOriginExecutorService
        }

        @Throws(InterruptedException::class)
        override fun awaitTermination(j2: Long, timeUnit: TimeUnit): Boolean {
            return mRealExecutorService.awaitTermination(j2, timeUnit)
        }

        override fun execute(runnable: Runnable) {
            mRealExecutorService.execute(runnable)
        }

        @Throws(InterruptedException::class)
        override fun <T> invokeAll(collection: Collection<Callable<T>?>): List<Future<T>> {
            return mRealExecutorService.invokeAll(collection)
        }

        @Throws(InterruptedException::class, ExecutionException::class)
        override fun <T> invokeAny(collection: Collection<Callable<T>?>): T {
            return mRealExecutorService.invokeAny(collection) as T
        }

        override fun isShutdown(): Boolean {
            return mRealExecutorService.isShutdown
        }

        override fun isTerminated(): Boolean {
            return mRealExecutorService.isTerminated
        }

        fun setRealExecutorService(executorService: ExecutorService) {
            val executorService2 = this.mRealExecutorService
            val executorService3 = this.mOriginExecutorService
            if (executorService2 === executorService3) {
                executorService3.shutdown()
            }
            this.mRealExecutorService = executorService
        }

        override fun shutdown() {
            mRealExecutorService.shutdown()
        }

        override fun shutdownNow(): List<Runnable> {
            return mRealExecutorService.shutdownNow()
        }

        override fun <T> submit(callable: Callable<T>): Future<T> {
            return mRealExecutorService.submit(callable)
        }

        @Throws(InterruptedException::class)
        override fun <T> invokeAll(
            collection: Collection<Callable<T>?>,
            j2: Long,
            timeUnit: TimeUnit
        ): List<Future<T>> {
            return mRealExecutorService.invokeAll(collection, j2, timeUnit)
        }

        @Throws(
            InterruptedException::class,
            ExecutionException::class,
            TimeoutException::class
        )
        override fun <T> invokeAny(
            collection: Collection<Callable<T>?>,
            j2: Long,
            timeUnit: TimeUnit
        ): T {
            return mRealExecutorService.invokeAny(collection, j2, timeUnit) as T
        }

        override fun <T> submit(runnable: Runnable, t2: T): Future<T> {
            return mRealExecutorService.submit(runnable, t2)
        }

        override fun submit(runnable: Runnable): Future<*> {
            return mRealExecutorService.submit(runnable)
        }
    }

    private fun ensureHandlerCreated() {
        ensureHandlerThread()
        if (globalThreadHandler == null) {
            synchronized(ThreadManager::class.java) {
                if (globalThreadHandler == null) {
                    handlerThread?.also {
                        globalThreadHandler = Handler(it.looper)
                    }
                }
            }
        }
    }

    private fun ensureHandlerThread() {
        if (handlerThread == null) {
            synchronized(ThreadManager::class.java) {
                if (handlerThread == null) {
                    handlerThread = HandlerThread(GLOBAL_HANDLER_THREAD)
                    handlerThread?.start()
                }
            }
        }
    }

    fun execIo(runnable: Runnable) {
        try {
            ioExecutor?.execute(runnable)
        } catch (unused: OutOfMemoryError) {
            System.gc()
        }
    }

    fun execTask(runnable: Runnable) {
        try {
            taskExecutor?.execute(runnable)
        } catch (unused: OutOfMemoryError) {
            System.gc()
        }
    }

    fun getHandlerThread():Thread?{
        ensureHandlerThread()
        return handlerThread
    }

    fun getHandlerThreadLooper():Looper?{
        ensureHandlerThread()
        return handlerThread?.looper
    }

    fun getIOExecutor():WrapperExecutorService?{
        return ioExecutor
    }

    fun getTaskExecutor():WrapperExecutorService?{
        return taskExecutor
    }

    fun post(runnable: Runnable) {
        try {
            ensureHandlerCreated()
            globalThreadHandler?.post(runnable)
        } catch (th: Throwable) {
            Log.e(TAG, Log.getStackTraceString(th))
        }
    }

    fun postDelayed(runnable: Runnable, delayMillis: Long) {
        ensureHandlerCreated()
        globalThreadHandler?.postDelayed(runnable, delayMillis)
    }

    init {
        val availableProcessors = Runtime.getRuntime().availableProcessors()
        taskExecutor = WrapperExecutorService(
            ThreadPoolExecutor(
                min(availableProcessors.toDouble(), 8.0).toInt(),
                50,
                10,
                TimeUnit.SECONDS,
                ArrayBlockingQueue(50),
                object : ThreadFactory {
                    val counter: AtomicInteger = AtomicInteger(1)

                    override fun newThread(runnable: Runnable): Thread {
                        return Thread(
                            null,
                            runnable,
                            "Task-Thread-" + counter.getAndIncrement(),
                            (64 * 1024).toLong()
                        )
                    }
                }
            ) { runnable, _ ->
                Log.i(
                    TAG,
                    "rejectedExecution:$runnable"
                )
            }
        )
        ioExecutor = WrapperExecutorService(
            ThreadPoolExecutor(
                0,
                Int.MAX_VALUE,
                10,
                TimeUnit.SECONDS,
                SynchronousQueue(),
                object : ThreadFactory {
                    val counter: AtomicInteger = AtomicInteger(1)

                    override fun newThread(runnable: Runnable): Thread {
                        return Thread(
                            null,
                            runnable,
                            "IO-Thread-" + counter.getAndIncrement(),
                            (64 * 1024).toLong()
                        )
                    }
                }
            )
        )
    }

    companion object {
        const val GLOBAL_HANDLER_THREAD: String = "ThreadManager-Handler-Thread"
        const val TAG: String = "ThreadManager"

        @Volatile
        private var sInstance: ThreadManager? = null

        @Volatile
        var globalThreadHandler: Handler? = null

        @Volatile
        var handlerThread: HandlerThread? = null
        var ioExecutor: WrapperExecutorService? = null
        var taskExecutor: WrapperExecutorService? = null


        fun setIoExecutor(executorService: ExecutorService?) {
            if (executorService == null) {
                Log.e(TAG, "method setIoExecutor >>> ioExecutor is null")
            } else {
                ioExecutor?.setRealExecutorService(executorService)
            }
        }

        fun setTaskExecutor(executorService: ExecutorService?) {
            if (executorService == null) {
                Log.e(TAG, "method setTaskExecutor >>> taskExecutor is null")
            } else {
                taskExecutor?.setRealExecutorService(executorService)
            }
        }

        @JvmStatic
        fun getInstance(): ThreadManager = sInstance ?: synchronized(this) {
            sInstance ?: ThreadManager().also { sInstance = it }
        }

    }
}
