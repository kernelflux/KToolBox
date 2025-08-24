package com.kernelflux.ktoolbox.thread

/**
 * 任务优先级
 */
enum class TaskPriority {
    HIGH,      // 高优先级
    NORMAL,    // 普通优先级
    LOW        // 低优先级
}

/**
 * 执行模式
 */
enum class ExecutionMode {
    AUTO,      // 自动选择
    COROUTINE, // 强制使用协程
    THREAD     // 强制使用线程
}

/**
 * 线程管理器配置
 */
data class ThreadManagerConfig(
    val preferCoroutines: Boolean = true,
    val enableMonitoring: Boolean = true,
    val enableLogging: Boolean = true,
    val ioThreadPoolSize: Int = 4,
    val computeThreadPoolSize: Int = 2,
    val taskThreadPoolSize: Int = 2
)
