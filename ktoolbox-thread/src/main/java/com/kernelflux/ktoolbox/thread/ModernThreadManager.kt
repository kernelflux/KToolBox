package com.kernelflux.ktoolbox.thread

import com.kernelflux.ktoolbox.logger.SDKLogger
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger


/**
 * 现代化线程/协程交互管理器
 */
class ModernThreadManager private constructor() {

    companion object {
        private const val TAG = "ModernThreadManager"

        @Volatile
        private var instance: ModernThreadManager? = null

        @JvmStatic
        fun getInstance(): ModernThreadManager = instance ?: synchronized(this) {
            instance ?: ModernThreadManager().also { instance = it }
        }
    }

    private val isInitialized = AtomicBoolean(false)
    private var config: ThreadManagerConfig = ThreadManagerConfig()

    // 线程池
    private lateinit var ioExecutor: ExecutorService
    private lateinit var computeExecutor: ExecutorService
    private lateinit var taskExecutor: ExecutorService

    // Scope管理 - 支持多个Scope和自动重建
    private var globalScope: CoroutineScope = createGlobalScope()
    private val scopeMap = ConcurrentHashMap<String, CoroutineScope>()

    private fun createGlobalScope(): CoroutineScope {
        return CoroutineScope(
            Dispatchers.IO +
                    SupervisorJob() +
                    CoroutineExceptionHandler { _, throwable ->
                        handleGlobalException(throwable)
                    }
        )
    }

    private fun getOrCreateScope(scopeName: String = "default"): CoroutineScope {
        return if (scopeName == "default") {
            if (!globalScope.isActive) {
                globalScope = createGlobalScope()
            }
            globalScope
        } else {
            synchronized(scopeMap) {
                scopeMap[scopeName]?.takeIf { it.isActive } ?: createGlobalScope().also { scopeMap[scopeName] = it }
            }
        }
    }

    // 监控统计
    private val taskCount = AtomicInteger(0)
    private val completedTaskCount = AtomicInteger(0)
    private val failedTaskCount = AtomicInteger(0)

    fun initialize(config: ThreadManagerConfig) {
        if (isInitialized.compareAndSet(false, true)) {
            this.config = config

            ioExecutor = Executors.newFixedThreadPool(config.ioThreadPoolSize)
            computeExecutor = Executors.newFixedThreadPool(config.computeThreadPoolSize)
            taskExecutor = Executors.newFixedThreadPool(config.taskThreadPoolSize)

            if (config.enableLogging) {
                SDKLogger.info(TAG).msg("ModernThreadManager initialized")
            }
        }
    }

    fun execute(
        taskName: String,
        priority: TaskPriority = TaskPriority.NORMAL,
        executionMode: ExecutionMode = ExecutionMode.AUTO,
        block: suspend CoroutineScope.() -> Unit
    ): Job {
        ensureInitialized()

        return when (executionMode) {
            ExecutionMode.COROUTINE -> executeCoroutine(taskName, Dispatchers.IO, block)
            ExecutionMode.THREAD -> {
                executeThread(taskName, priority) { runBlocking { block() } }
                return Job()
            }
            ExecutionMode.AUTO -> {
                if (config.preferCoroutines) {
                    executeCoroutine(taskName, Dispatchers.IO, block)
                } else {
                    executeThread(taskName, priority) { runBlocking { block() } }
                    return Job()
                }
            }
        }
    }

    fun executeThread(
        taskName: String,
        priority: TaskPriority = TaskPriority.NORMAL,
        runnable: Runnable
    ) {
        ensureInitialized()

        taskCount.incrementAndGet()

        val executor = when (priority) {
            TaskPriority.HIGH -> taskExecutor
            TaskPriority.NORMAL -> ioExecutor
            TaskPriority.LOW -> computeExecutor
        }

        executor.submit {
            try {
                runnable.run()
                completedTaskCount.incrementAndGet()
                if (config.enableLogging) {
                    SDKLogger.info(TAG).msg("Thread task completed: $taskName")
                }
            } catch (e: Exception) {
                failedTaskCount.incrementAndGet()
                if (config.enableLogging) {
                    SDKLogger.error(TAG).exception(e, "Thread task failed: $taskName")
                }
            }
        }
    }

    fun executeCoroutine(
        taskName: String,
        dispatcher: CoroutineDispatcher = Dispatchers.IO,
        block: suspend CoroutineScope.() -> Unit
    ): Job {
        ensureInitialized()

        val scope = getOrCreateScope()
        taskCount.incrementAndGet()

        return scope.launch(dispatcher) {
            try {
                block()
                completedTaskCount.incrementAndGet()
                if (config.enableLogging) {
                    SDKLogger.info(TAG).msg("Coroutine task completed: $taskName")
                }
            } catch (e: Exception) {
                failedTaskCount.incrementAndGet()
                if (config.enableLogging) {
                    SDKLogger.error(TAG).exception(e, "Coroutine task failed: $taskName")
                }
            }
        }
    }

    /**
     * 使用指定Scope执行协程任务
     */
    fun executeCoroutineWithScope(
        scopeName: String,
        taskName: String,
        dispatcher: CoroutineDispatcher = Dispatchers.IO,
        block: suspend CoroutineScope.() -> Unit
    ): Job {
        ensureInitialized()

        val scope = getOrCreateScope(scopeName)
        taskCount.incrementAndGet()

        return scope.launch(dispatcher) {
            try {
                block()
                completedTaskCount.incrementAndGet()
                if (config.enableLogging) {
                    SDKLogger.info(TAG).msg("Coroutine task completed: $taskName in scope: $scopeName")
                }
            } catch (e: Exception) {
                failedTaskCount.incrementAndGet()
                if (config.enableLogging) {
                    SDKLogger.error(TAG).exception(e, "Coroutine task failed: $taskName in scope: $scopeName")
                }
            }
        }
    }

    fun updateConfig(newConfig: ThreadManagerConfig) {
        config = newConfig
        if (config.enableLogging) {
            SDKLogger.info(TAG).msg("Configuration updated")
        }
    }

    fun getConfig(): ThreadManagerConfig = config

    fun resetConfig() {
        config = ThreadManagerConfig()
        if (config.enableLogging) {
            SDKLogger.info(TAG).msg("Configuration reset to default")
        }
    }

    fun getPerformanceStats(): String {
        return buildString {
            appendLine("Performance Statistics:")
            appendLine("Total Tasks: ${taskCount.get()}")
            appendLine("Completed Tasks: ${completedTaskCount.get()}")
            appendLine("Failed Tasks: ${failedTaskCount.get()}")
            appendLine("Success Rate: ${if (taskCount.get() > 0) (completedTaskCount.get() * 100.0 / taskCount.get()) else 0.0}%")
        }
    }

    fun getTaskStats(): String {
        return buildString {
            appendLine("Task Statistics:")
            appendLine("Total Tasks: ${taskCount.get()}")
            appendLine("Completed Tasks: ${completedTaskCount.get()}")
            appendLine("Failed Tasks: ${failedTaskCount.get()}")
        }
    }

    fun getThreadPoolStats(): String {
        return buildString {
            appendLine("Thread Pool Statistics:")
            appendLine("IO Executor Active: ${if (::ioExecutor.isInitialized) !ioExecutor.isShutdown else "Not initialized"}")
            appendLine("Compute Executor Active: ${if (::computeExecutor.isInitialized) !computeExecutor.isShutdown else "Not initialized"}")
            appendLine("Task Executor Active: ${if (::taskExecutor.isInitialized) !taskExecutor.isShutdown else "Not initialized"}")
        }
    }

    fun getDispatcherStats(): String {
        return buildString {
            appendLine("Dispatcher Statistics:")
            appendLine("Global Scope Active: ${globalScope.isActive}")
            appendLine("Prefer Coroutines: ${config.preferCoroutines}")
        }
    }

    fun getIOExecutor(): ExecutorService {
        ensureInitialized()
        return ioExecutor
    }

    fun getComputeExecutor(): ExecutorService {
        ensureInitialized()
        return computeExecutor
    }

    fun getTaskExecutor(): ExecutorService {
        ensureInitialized()
        return taskExecutor
    }

    fun getCustomExecutor(): ExecutorService {
        ensureInitialized()
        return taskExecutor
    }

    fun shutdown() {
        if (isInitialized.get()) {
            try {
                globalScope.cancel()
                ioExecutor.shutdown()
                computeExecutor.shutdown()
                taskExecutor.shutdown()

                if (config.enableLogging) {
                    SDKLogger.info(TAG).msg("All thread pools shutdown")
                }
            } catch (e: Exception) {
                if (config.enableLogging) {
                    SDKLogger.error(TAG).exception(e, "Error during shutdown")
                }
            }
        }
    }

    private fun ensureInitialized() {
        if (!isInitialized.get()) {
            throw IllegalStateException("ModernThreadManager not initialized. Call initialize() first.")
        }
    }

    private fun handleGlobalException(throwable: Throwable) {
        failedTaskCount.incrementAndGet()
        if (config.enableLogging) {
            SDKLogger.error(TAG).exception(throwable, "Global scope exception")
        }
    }
}
