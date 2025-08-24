package com.kernelflux.ktoolbox.thread

import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import com.kernelflux.ktoolbox.logger.SDKLogger
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.util.concurrent.ExecutorService
import java.util.concurrent.atomic.AtomicBoolean


/**
 * SDK线程管理器
 */
object SDKThreadManager {

    private val isInitialized = AtomicBoolean(false)

    // ==================== 核心组件 ====================

    // 内部实现委托给ModernThreadManager
    private lateinit var modernThreadManager: ModernThreadManager

    // HandlerThread管理 - 借鉴传统ThreadManager
    @Volatile
    private var handlerThread: HandlerThread? = null
    @Volatile
    private var globalThreadHandler: Handler? = null

    // ==================== 初始化 ====================

    /**
     * 初始化线程管理器
     */
    fun initialize(
        preferCoroutines: Boolean = true,
        enableMonitoring: Boolean = true,
        enableLogging: Boolean = true
    ) {
        if (isInitialized.compareAndSet(false, true)) {
            modernThreadManager = ModernThreadManager.getInstance()
            modernThreadManager.initialize(
                ThreadManagerConfig(
                    preferCoroutines = preferCoroutines,
                    enableMonitoring = enableMonitoring,
                    enableLogging = enableLogging
                )
            )
            ensureHandlerThread()
            if (enableLogging) {
                SDKLogger.info("SDKThreadManager").msg("SDKThreadManager initialized with preferCoroutines: $preferCoroutines")
            }
        }
    }

    // ==================== 核心API ====================

    /**
     * 执行任务（自动选择最优方式）
     */
    fun execute(
        taskName: String,
        priority: TaskPriority = TaskPriority.NORMAL,
        executionMode: ExecutionMode = ExecutionMode.AUTO,
        block: suspend CoroutineScope.() -> Unit
    ): Job {
        ensureInitialized()
        return modernThreadManager.execute(taskName, priority, executionMode, block)
    }

    /**
     * 执行任务（简化版本）
     */
    fun execute(
        taskName: String,
        block: suspend CoroutineScope.() -> Unit
    ): Job {
        return execute(taskName, TaskPriority.NORMAL, ExecutionMode.AUTO, block)
    }

    /**
     * 执行线程任务
     */
    fun executeThread(
        taskName: String,
        priority: TaskPriority = TaskPriority.NORMAL,
        runnable: Runnable
    ) {
        ensureInitialized()
        modernThreadManager.executeThread(taskName, priority, runnable)
    }

    /**
     * 执行协程任务
     */
    fun executeCoroutine(
        taskName: String,
        dispatcher: CoroutineDispatcher = Dispatchers.IO,
        block: suspend CoroutineScope.() -> Unit
    ): Job {
        ensureInitialized()
        return modernThreadManager.executeCoroutine(taskName, dispatcher, block)
    }

    // ==================== 便捷方法 ====================

    /**
     * 在IO线程执行
     */
    fun executeIO(
        taskName: String,
        block: suspend CoroutineScope.() -> Unit
    ): Job {
        return executeCoroutine(taskName, Dispatchers.IO, block)
    }

    /**
     * 在计算线程执行
     */
    fun executeCompute(
        taskName: String,
        block: suspend CoroutineScope.() -> Unit
    ): Job {
        return executeCoroutine(taskName, Dispatchers.Default, block)
    }

    /**
     * 在主线程执行
     */
    fun executeMain(
        taskName: String,
        block: suspend CoroutineScope.() -> Unit
    ): Job {
        return executeCoroutine(taskName, Dispatchers.Main, block)
    }

    /**
     * 执行高优先级任务
     */
    fun executeHighPriority(
        taskName: String,
        block: suspend CoroutineScope.() -> Unit
    ): Job {
        return execute(taskName, TaskPriority.HIGH, ExecutionMode.AUTO, block)
    }

    /**
     * 执行低优先级任务
     */
    fun executeLowPriority(
        taskName: String,
        block: suspend CoroutineScope.() -> Unit
    ): Job {
        return execute(taskName, TaskPriority.LOW, ExecutionMode.AUTO, block)
    }

    /**
     * 强制使用协程执行
     */
    fun executeWithCoroutine(
        taskName: String,
        block: suspend CoroutineScope.() -> Unit
    ): Job {
        return execute(taskName, TaskPriority.NORMAL, ExecutionMode.COROUTINE, block)
    }

    /**
     * 强制使用线程执行
     */
    fun executeWithThread(
        taskName: String,
        runnable: Runnable
    ) {
        executeThread(taskName, TaskPriority.NORMAL, runnable)
    }

    // ==================== Kotlin语法糖方法 ====================

    /**
     * IO线程执行（语法糖版本）
     */
    fun io(
        taskName: String,
        block: suspend CoroutineScope.() -> Unit
    ): Job = executeIO(taskName, block)

    /**
     * 计算线程执行（语法糖版本）
     */
    fun compute(
        taskName: String,
        block: suspend CoroutineScope.() -> Unit
    ): Job = executeCompute(taskName, block)

    /**
     * 主线程执行（语法糖版本）
     */
    fun main(
        taskName: String,
        block: suspend CoroutineScope.() -> Unit
    ): Job = executeMain(taskName, block)

    /**
     * 高优先级执行（语法糖版本）
     */
    fun high(
        taskName: String,
        block: suspend CoroutineScope.() -> Unit
    ): Job = executeHighPriority(taskName, block)

    /**
     * 低优先级执行（语法糖版本）
     */
    fun low(
        taskName: String,
        block: suspend CoroutineScope.() -> Unit
    ): Job = executeLowPriority(taskName, block)

    /**
     * 强制使用协程执行
     */
    fun coroutine(
        taskName: String,
        block: suspend CoroutineScope.() -> Unit
    ): Job = executeWithCoroutine(taskName, block)

    /**
     * 使用指定Scope执行协程
     */
    fun coroutineWithScope(
        scopeName: String,
        taskName: String,
        block: suspend CoroutineScope.() -> Unit
    ): Job {
        ensureInitialized()
        return modernThreadManager.executeCoroutineWithScope(scopeName, taskName, Dispatchers.IO, block)
    }

    /**
     * 线程执行（语法糖版本）
     */
    fun thread(
        taskName: String,
        runnable: Runnable
    ) = executeWithThread(taskName, runnable)

    /**
     * 异步执行并返回结果
     */
    suspend fun <T> async(
        taskName: String,
        block: suspend CoroutineScope.() -> T
    ): T = withContext(Dispatchers.IO) {
        execute(taskName) { block() }
        block()
    }

    /**
     * 延迟执行
     */
    fun delay(
        taskName: String,
        delayMillis: Long,
        block: suspend CoroutineScope.() -> Unit
    ): Job = execute(taskName) {
        kotlinx.coroutines.delay(delayMillis)
        block()
    }

    /**
     * 重复执行
     */
    fun repeat(
        taskName: String,
        times: Int,
        block: suspend CoroutineScope.(Int) -> Unit
    ): Job = execute(taskName) {
        repeat(times) { index ->
            block(index)
        }
    }

    /**
     * 超时执行
     */
    fun timeout(
        taskName: String,
        timeoutMillis: Long,
        block: suspend CoroutineScope.() -> Unit
    ): Job = execute(taskName) {
        withTimeout(timeoutMillis) {
            block()
        }
    }

    /**
     * 重试执行
     */
    suspend fun <T> retry(
        taskName: String,
        maxAttempts: Int = 3,
        delayMillis: Long = 1000,
        block: suspend CoroutineScope.() -> T
    ): T {
        var lastException: Exception? = null

        repeat(maxAttempts) { attempt ->
            try {
                return withContext(Dispatchers.IO) {
                    execute(taskName) { block() }
                    block()
                }
            } catch (e: Exception) {
                lastException = e
                if (attempt < maxAttempts - 1) {
                    kotlinx.coroutines.delay(delayMillis)
                }
            }
        }

        throw lastException ?: Exception("Retry failed after $maxAttempts attempts")
    }

    // ==================== 资源管理 ====================

    /**
     * 获取IO线程池
     */
    fun getIOExecutor(): ExecutorService {
        ensureInitialized()
        return modernThreadManager.getIOExecutor()
    }

    /**
     * 获取计算线程池
     */
    fun getComputeExecutor(): ExecutorService {
        ensureInitialized()
        return modernThreadManager.getComputeExecutor()
    }

    /**
     * 获取任务线程池
     */
    fun getTaskExecutor(): ExecutorService {
        ensureInitialized()
        return modernThreadManager.getTaskExecutor()
    }

    /**
     * 获取自定义线程池
     */
    fun getCustomExecutor(): ExecutorService {
        ensureInitialized()
        return modernThreadManager.getCustomExecutor()
    }

    // ==================== 监控和统计 ====================

    /**
     * 获取性能统计
     */
    fun getPerformanceStats(): String {
        ensureInitialized()
        return modernThreadManager.getPerformanceStats()
    }

    /**
     * 获取任务统计
     */
    fun getTaskStats(): String {
        ensureInitialized()
        return modernThreadManager.getTaskStats()
    }

    /**
     * 获取线程池统计
     */
    fun getThreadPoolStats(): String {
        ensureInitialized()
        return modernThreadManager.getThreadPoolStats()
    }

    /**
     * 获取调度器统计
     */
    fun getDispatcherStats(): String {
        ensureInitialized()
        return modernThreadManager.getDispatcherStats()
    }

    /**
     * 获取配置
     */
    fun getConfig(): ThreadManagerConfig {
        ensureInitialized()
        return modernThreadManager.getConfig()
    }

    /**
     * 更新配置
     */
    fun updateConfig(newConfig: ThreadManagerConfig) {
        ensureInitialized()
        modernThreadManager.updateConfig(newConfig)
    }

    /**
     * 重置配置
     */
    fun resetConfig() {
        ensureInitialized()
        modernThreadManager.resetConfig()
    }

    // ==================== 生命周期管理 ====================

    /**
     * 关闭线程管理器
     */
    fun shutdown() {
        if (isInitialized.get()) {
            modernThreadManager.shutdown()

            // 清理HandlerThread
            try {
                handlerThread?.quit()
                handlerThread = null
                globalThreadHandler = null
            } catch (e: Exception) {
                SDKLogger.error("SDKThreadManager").exception(e, "Error during HandlerThread shutdown")
            }

            isInitialized.set(false)
        }
    }

    // ==================== HandlerThread管理 ====================

    private fun ensureHandlerThread() {
        if (handlerThread == null) {
            synchronized(SDKThreadManager::class.java) {
                if (handlerThread == null) {
                    handlerThread = HandlerThread("SDKThreadManager-Handler-Thread")
                    handlerThread?.start()
                }
            }
        }
    }

    private fun ensureHandlerCreated() {
        ensureHandlerThread()
        if (globalThreadHandler == null) {
            synchronized(SDKThreadManager::class.java) {
                if (globalThreadHandler == null) {
                    handlerThread?.also {
                        globalThreadHandler = Handler(it.looper)
                    }
                }
            }
        }
    }

    /**
     * 获取HandlerThread
     */
    fun getHandlerThread(): Thread? {
        ensureHandlerThread()
        return handlerThread
    }

    /**
     * 获取HandlerThread的Looper
     */
    fun getHandlerThreadLooper(): Looper? {
        ensureHandlerThread()
        return handlerThread?.looper
    }

    /**
     * 在主线程HandlerThread上执行任务
     */
    fun post(runnable: Runnable) {
        try {
            ensureHandlerCreated()
            globalThreadHandler?.post(runnable)
        } catch (th: Throwable) {
            SDKLogger.error("SDKThreadManager").exception(th, "Post task failed")
        }
    }

    /**
     * 在主线程HandlerThread上延迟执行任务
     */
    fun postDelayed(runnable: Runnable, delayMillis: Long) {
        ensureHandlerCreated()
        globalThreadHandler?.postDelayed(runnable, delayMillis)
    }

    // ==================== 内部方法 ====================

    private fun ensureInitialized() {
        if (!isInitialized.get()) {
            throw IllegalStateException("SDKThreadManager not initialized. Call initialize() first.")
        }
    }
}