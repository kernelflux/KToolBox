package com.kernelflux.ktoolbox.logger

import android.annotation.SuppressLint
import android.util.Log
import java.io.PrintWriter
import java.io.StringWriter
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Android兼容的日志工具类
 */
object AndroidLogUtil {
    private const val TAG = "LoggerCore"

    fun e(message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.e(TAG, message, throwable)
        } else {
            Log.e(TAG, message)
        }
    }

    fun w(message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.w(TAG, message, throwable)
        } else {
            Log.w(TAG, message)
        }
    }

    fun i(message: String) {
        Log.i(TAG, message)
    }

    fun d(message: String) {
        Log.d(TAG, message)
    }
}

/**
 * ApplicationContext提供者
 */
@SuppressLint("StaticFieldLeak")
object ApplicationContextProvider {
    @Volatile
    private var context: android.content.Context? = null

    fun setContext(context: android.content.Context) {
        this.context = context.applicationContext
    }

    fun getContext(): android.content.Context {
        return context ?: throw IllegalStateException("ApplicationContext not set")
    }
}

/**
 * 日志输出接口
 */
interface LogOutput {
    /**
     * 输出日志
     * @param moduleName 模块名称
     * @param message 日志消息
     */
    fun output(moduleName: String, message: String)

    /**
     * 清理资源
     */
    fun cleanup()
}

/**
 * 模块管理器 - 支持自定义模块名称
 */
object ModuleManager {
    private val modules = ConcurrentHashMap<String, Int>()
    private val moduleNames = ConcurrentHashMap<Int, String>()
    private val nextBit = AtomicInteger(0)

    // 位运算限制：最多支持31个模块（避免溢出）
    private const val MAX_MODULES = 31

    /**
     * 注册模块
     * @param moduleName 模块名称
     * @return 模块位标识，如果注册失败返回-1
     */
    fun registerModule(moduleName: String): Int {
        if (moduleName.isBlank()) {
            AndroidLogUtil.e("Module name cannot be blank")
            return -1
        }

        synchronized(modules) {
            // 检查是否已存在
            modules[moduleName]?.let { return it }

            val currentBit = nextBit.get()
            if (currentBit >= MAX_MODULES) {
                AndroidLogUtil.e("Maximum modules reached ($MAX_MODULES)")
                return -1
            }

            val bit = 1 shl currentBit
            modules[moduleName] = bit
            moduleNames[bit] = moduleName
            nextBit.incrementAndGet()
            return bit
        }
    }

    /**
     * 获取模块位标识
     */
    fun getModuleBit(moduleName: String): Int? {
        return modules[moduleName]
    }

    /**
     * 获取模块名称
     */
    fun getModuleName(bit: Int): String? {
        return moduleNames[bit]
    }

    /**
     * 检查模块是否已注册
     */
    fun isModuleRegistered(moduleName: String): Boolean {
        return modules.containsKey(moduleName)
    }

    /**
     * 获取所有已注册的模块名称
     */
    fun getAllModuleNames(): Set<String> {
        return modules.keys.toSet()
    }

    /**
     * 获取已注册的模块数量
     */
    fun getModuleCount(): Int {
        return modules.size
    }

    /**
     * 清除所有模块（谨慎使用）
     */
    fun clearAllModules() {
        synchronized(modules) {
            modules.clear()
            moduleNames.clear()
            nextBit.set(0)
        }
    }
}

/**
 * 日志输出管理器
 */
object LogOutputManager {
    private val outputs = Collections.newSetFromMap(ConcurrentHashMap<LogOutput, Boolean>())
    private val outputCount = AtomicInteger(0)
    private val exceptionCount = AtomicInteger(0)

    private const val MAX_OUTPUTS = 10
    private const val MAX_EXCEPTIONS = 100

    /**
     * 添加日志输出
     */
    fun addOutput(output: LogOutput) {
        if (outputCount.get() >= MAX_OUTPUTS) {
            AndroidLogUtil.e("Maximum outputs reached ($MAX_OUTPUTS)")
            return
        }

        try {
            outputs.add(output)
            outputCount.incrementAndGet()
            AndroidLogUtil.i("Log output added: ${output::class.java.simpleName}")
        } catch (e: Exception) {
            AndroidLogUtil.e("Failed to add output: ${e.message}", e)
        }
    }

    /**
     * 移除日志输出
     */
    fun removeOutput(output: LogOutput) {
        try {
            if (outputs.remove(output)) {
                output.cleanup()
                outputCount.decrementAndGet()
                AndroidLogUtil.i("Log output removed: ${output::class.java.simpleName}")
            }
        } catch (e: Exception) {
            AndroidLogUtil.e("Failed to remove output: ${e.message}", e)
        }
    }

    /**
     * 清除所有输出
     */
    fun clearOutputs() {
        try {
            outputs.forEach { it.cleanup() }
            outputs.clear()
            outputCount.set(0)
            AndroidLogUtil.i("All log outputs cleared")
        } catch (e: Exception) {
            AndroidLogUtil.e("Failed to clear outputs: ${e.message}", e)
        }
    }

    /**
     * 输出日志到所有输出
     */
    fun outputToAll(moduleName: String, message: String) {
        if (exceptionCount.get() >= MAX_EXCEPTIONS) {
            return
        }

        try {
            outputs.forEach { output ->
                try {
                    output.output(moduleName, message)
                } catch (e: Exception) {
                    exceptionCount.incrementAndGet()
                    AndroidLogUtil.e("Output error: ${e.message}", e)
                }
            }
        } catch (e: Exception) {
            exceptionCount.incrementAndGet()
            AndroidLogUtil.e("Log error: ${e.message}", e)
        }
    }

    /**
     * 获取输出数量
     */
    fun getOutputCount(): Int = outputCount.get()

    /**
     * 获取异常计数
     */
    fun getExceptionCount(): Int = exceptionCount.get()
}

/**
 * 工具类
 */
object LoggerUtils {
    /**
     * 获取默认日志目录
     */
    fun getDefaultLogDir(): String {
        return try {
            // 尝试获取应用内部存储目录
            val context = ApplicationContextProvider.getContext()
            val logDir = java.io.File(context.filesDir, "logs")
            if (!logDir.exists()) {
                logDir.mkdirs()
            }
            logDir.absolutePath
        } catch (e: Exception) {
            // 如果无法获取Context，使用临时目录
            val tempDir = java.io.File(System.getProperty("java.io.tmpdir"), "logger_logs")
            if (!tempDir.exists()) {
                tempDir.mkdirs()
            }
            tempDir.absolutePath
        }
    }

    /**
     * 格式化异常堆栈
     */
    fun formatStackTrace(throwable: Throwable, message: String? = null): String {
        return try {
            val stringWriter = StringWriter()
            val printWriter = PrintWriter(stringWriter)
            throwable.printStackTrace(printWriter)

            val stackTrace = stringWriter.toString()
            if (message != null) {
                "$message\n$stackTrace"
            } else {
                stackTrace
            }
        } catch (e: Exception) {
            AndroidLogUtil.e("Exception format error: ${e.message}", e)
            "Failed to format exception: ${e.message}"
        }
    }

    /**
     * 获取相关堆栈帧
     */
    fun getRelevantStackTrace(excludeClasses: List<String> = emptyList(), maxFrames: Int = 3): String {
        return try {
            val stackTrace = Thread.currentThread().stackTrace
            val relevantFrames = stackTrace.dropWhile { frame ->
                excludeClasses.any { frame.className.contains(it) }
            }.take(maxFrames)

            if (relevantFrames.isNotEmpty()) {
                relevantFrames.joinToString("\n") { frame ->
                    "    at ${frame.className}.${frame.methodName}(${frame.fileName}:${frame.lineNumber})"
                }
            } else {
                ""
            }
        } catch (e: Exception) {
            ""
        }
    }
}



fun AtomicInteger.updateAndGetCompat(update: (Int) -> Int): Int {
    var prev: Int
    var next: Int
    do {
        prev = get()
        next = update(prev)
    } while (!compareAndSet(prev, next))
    return next
}

fun AtomicInteger.getAndUpdateCompat(update: (Int) -> Int): Int {
    var prev: Int
    var next: Int
    do {
        prev = get()
        next = update(prev)
    } while (!compareAndSet(prev, next))
    return prev
}

fun AtomicInteger.accumulateAndGetCompat(x: Int, accumulator: (Int, Int) -> Int): Int {
    var prev: Int
    var next: Int
    do {
        prev = get()
        next = accumulator(prev, x)
    } while (!compareAndSet(prev, next))
    return next
}

fun AtomicInteger.getAndAccumulateCompat(x: Int, accumulator: (Int, Int) -> Int): Int {
    var prev: Int
    var next: Int
    do {
        prev = get()
        next = accumulator(prev, x)
    } while (!compareAndSet(prev, next))
    return prev
}
