package com.kernelflux.ktoolbox.logger

import android.util.Log
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Android Logcat输出
 */
class LogcatOutput(
    private val enableStackTrace: Boolean = false,  // 默认不输出堆栈
    private val enableThreadInfo: Boolean = false,
    private val maxTagLength: Int = 23,
    private val tagPrefix: String = "KToolBoxSDK"  // 添加tag前缀
) : LogOutput {

    private val tagCache = ConcurrentHashMap<String, String>()

    override fun output(moduleName: String, message: String) {
        try {
            val tag = getOrCreateTag(moduleName)
            val fullMessage = buildFullMessage(message)

            // 根据模块名称选择日志级别
            when {
                moduleName.contains("ERROR", ignoreCase = true) -> Log.e(tag, fullMessage)
                moduleName.contains("WARN", ignoreCase = true) -> Log.w(tag, fullMessage)
                moduleName.contains("DEBUG", ignoreCase = true) -> Log.d(tag, fullMessage)
                else -> Log.i(tag, fullMessage)
            }
        } catch (e: Exception) {
            AndroidLogUtil.e("LogcatOutput error: ${e.message}", e)
        }
    }

    private fun getOrCreateTag(moduleName: String): String {
        synchronized(tagCache) {
            tagCache[moduleName]?.let { return it }

            // 构建更精准的tag
            val baseTag = if (moduleName.length > maxTagLength) {
                moduleName.substring(0, maxTagLength)
            } else {
                moduleName
            }

            // 添加前缀，便于筛选
            val tag = if (tagPrefix.isNotEmpty()) {
                "${tagPrefix}_$baseTag"
            } else {
                baseTag
            }

            tagCache[moduleName] = tag
            return tag
        }
    }

    private fun buildFullMessage(message: String): String {
        val builder = StringBuilder()

        if (enableThreadInfo) {
            val thread = Thread.currentThread()
            builder.append("[${thread.name}] ")
        }

        builder.append(message)

        // 只在明确启用时才输出堆栈信息
        if (enableStackTrace) {
            val stackTrace = LoggerUtils.getRelevantStackTrace(
                excludeClasses = listOf("LogcatOutput", "LoggerCore", "AndroidLogUtil", "SDKLogger")
            )
            if (stackTrace.isNotEmpty()) {
                builder.append("\n").append(stackTrace)
            }
        }

        return builder.toString()
    }

    override fun cleanup() {
        tagCache.clear()
    }
}

/**
 * 智能Android Logcat输出
 *
 * 支持更精准的tag控制和日志级别选择
 */
class SmartLogcatOutput(
    private val enableStackTrace: Boolean = false,
    private val enableThreadInfo: Boolean = false,
    private val maxTagLength: Int = 23,
    private val tagPrefix: String = "SDK",
    private val tagMapping: Map<String, String> = emptyMap(),  // 自定义tag映射
    private val levelMapping: Map<String, Int> = emptyMap()    // 自定义日志级别映射
) : LogOutput {

    private val tagCache = ConcurrentHashMap<String, String>()

    companion object {
        const val LEVEL_VERBOSE = 0
        const val LEVEL_DEBUG = 1
        const val LEVEL_INFO = 2
        const val LEVEL_WARN = 3
        const val LEVEL_ERROR = 4
    }

    override fun output(moduleName: String, message: String) {
        try {
            val tag = getOrCreateTag(moduleName)
            val fullMessage = buildFullMessage(message)
            val level = getLogLevel(moduleName)

            // 根据级别输出日志
            when (level) {
                LEVEL_VERBOSE -> Log.v(tag, fullMessage)
                LEVEL_DEBUG -> Log.d(tag, fullMessage)
                LEVEL_INFO -> Log.i(tag, fullMessage)
                LEVEL_WARN -> Log.w(tag, fullMessage)
                LEVEL_ERROR -> Log.e(tag, fullMessage)
                else -> Log.i(tag, fullMessage)
            }
        } catch (e: Exception) {
            AndroidLogUtil.e("SmartLogcatOutput error: ${e.message}", e)
        }
    }

    private fun getOrCreateTag(moduleName: String): String {
        synchronized(tagCache) {
            tagCache[moduleName]?.let { return it }

            // 优先使用自定义映射
            val mappedTag = tagMapping[moduleName]
            if (mappedTag != null) {
                tagCache[moduleName] = mappedTag
                return mappedTag
            }

            // 构建默认tag
            val baseTag = if (moduleName.length > maxTagLength) {
                moduleName.substring(0, maxTagLength)
            } else {
                moduleName
            }

            // 添加前缀
            val tag = if (tagPrefix.isNotEmpty()) {
                "${tagPrefix}_$baseTag"
            } else {
                baseTag
            }

            tagCache[moduleName] = tag
            return tag
        }
    }

    private fun getLogLevel(moduleName: String): Int {
        // 优先使用自定义级别映射
        levelMapping[moduleName]?.let { return it }

        // 根据模块名称智能判断级别
        return when {
            moduleName.contains("ERROR", ignoreCase = true) -> LEVEL_ERROR
            moduleName.contains("WARN", ignoreCase = true) -> LEVEL_WARN
            moduleName.contains("DEBUG", ignoreCase = true) -> LEVEL_DEBUG
            moduleName.contains("VERBOSE", ignoreCase = true) -> LEVEL_VERBOSE
            else -> LEVEL_INFO
        }
    }

    private fun buildFullMessage(message: String): String {
        val builder = StringBuilder()

        if (enableThreadInfo) {
            val thread = Thread.currentThread()
            builder.append("[${thread.name}] ")
        }

        builder.append(message)

        // 只在明确启用时才输出堆栈信息
        if (enableStackTrace) {
            val stackTrace = LoggerUtils.getRelevantStackTrace(
                excludeClasses = listOf(
                    "LogcatOutput",
                    "SmartLogcatOutput",
                    "LoggerCore",
                    "AndroidLogUtil",
                    "SDKLogger"
                )
            )
            if (stackTrace.isNotEmpty()) {
                builder.append("\n").append(stackTrace)
            }
        }

        return builder.toString()
    }

    override fun cleanup() {
        tagCache.clear()
    }
}

/**
 * 控制台输出
 */
class ConsoleOutput : LogOutput {
    override fun output(moduleName: String, message: String) {
        println("[$moduleName] $message")
    }

    override fun cleanup() {
        // 控制台输出无需清理
    }
}

/**
 * Android文件输出
 */
class AndroidFileOutput(
    private val logDir: String,
    private val maxFileSize: Long = 5 * 1024 * 1024, // 5MB
    private val maxFileCount: Int = 3
) : LogOutput {

    private var currentFile: java.io.File? = null
    private var currentFileWriter: java.io.FileWriter? = null
    private val dateFormat =
        java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", java.util.Locale.getDefault())

    init {
        createLogDirectory()
    }

    override fun output(moduleName: String, message: String) {
        try {
            val logFile = getCurrentLogFile()
            val timestamp = dateFormat.format(java.util.Date())
            val logEntry = "[$timestamp] [$moduleName] $message\n"

            synchronized(this) {
                currentFileWriter?.write(logEntry)
                currentFileWriter?.flush()

                if (logFile.length() > maxFileSize) {
                    rotateLogFile()
                }
            }
        } catch (e: java.io.IOException) {
            AndroidLogUtil.e("AndroidFileOutput error: ${e.message}", e)
        }
    }

    private fun createLogDirectory() {
        try {
            val dir = java.io.File(logDir)
            if (!dir.exists()) {
                dir.mkdirs()
            }
        } catch (e: Exception) {
            AndroidLogUtil.e("Failed to create log directory: ${e.message}", e)
        }
    }

    private fun getCurrentLogFile(): java.io.File {
        if (currentFile == null || currentFileWriter == null) {
            try {
                val fileName = "logger_${System.currentTimeMillis()}.txt"
                currentFile = java.io.File(logDir, fileName)
                currentFileWriter = java.io.FileWriter(currentFile, true)
            } catch (e: Exception) {
                AndroidLogUtil.e("Failed to create log file: ${e.message}", e)
            }
        }
        return currentFile ?: java.io.File(logDir, "fallback_log.txt")
    }

    private fun rotateLogFile() {
        try {
            currentFileWriter?.close()
            currentFileWriter = null
            currentFile = null
            cleanOldLogFiles()
        } catch (e: Exception) {
            AndroidLogUtil.e("Failed to rotate log file: ${e.message}", e)
        }
    }

    private fun cleanOldLogFiles() {
        try {
            val logDir = java.io.File(logDir)
            val logFiles = logDir.listFiles { file ->
                file.name.startsWith("logger_") && file.name.endsWith(".txt")
            }?.sortedBy { it.lastModified() }

            logFiles?.let { files ->
                if (files.size > maxFileCount) {
                    val filesToDelete = files.take(files.size - maxFileCount)
                    filesToDelete.forEach { it.delete() }
                }
            }
        } catch (e: Exception) {
            AndroidLogUtil.e("Failed to clean old log files: ${e.message}", e)
        }
    }

    override fun cleanup() {
        try {
            currentFileWriter?.close()
            currentFileWriter = null
            currentFile = null
        } catch (e: Exception) {
            AndroidLogUtil.e("Failed to cleanup AndroidFileOutput: ${e.message}", e)
        }
    }
}

/**
 * 内存优化输出
 */
class OptimizedMemoryOutput(
    private val maxBufferSize: Int = 1000,
    private val flushInterval: Long = 5000 // 5秒
) : LogOutput {

    private val logBuffer =
        java.util.Collections.newSetFromMap(java.util.concurrent.ConcurrentHashMap<String, Boolean>())
    private val lastFlushTime = AtomicInteger(0)

    override fun output(moduleName: String, message: String) {
        val logEntry = "[$moduleName] $message"

        if (logBuffer.size >= maxBufferSize) {
            flushBuffer()
        }

        logBuffer.add(logEntry)

        val currentTime = System.currentTimeMillis().toInt()
        if (currentTime - lastFlushTime.get() > flushInterval) {
            flushBuffer()
            lastFlushTime.set(currentTime)
        }
    }

    private fun flushBuffer() {
        synchronized(logBuffer) {
            logBuffer.forEach { entry ->
                println(entry)
            }
            logBuffer.clear()
        }
    }

    override fun cleanup() {
        flushBuffer()
    }
}

/**
 * 条件输出
 */
class ConditionalOutput(
    private val condition: (String, String) -> Boolean,
    private val delegate: LogOutput
) : LogOutput {

    override fun output(moduleName: String, message: String) {
        if (condition(moduleName, message)) {
            delegate.output(moduleName, message)
        }
    }

    override fun cleanup() {
        delegate.cleanup()
    }
}

/**
 * 组合输出
 */
class CompositeOutput(private val outputs: List<LogOutput>) : LogOutput {

    override fun output(moduleName: String, message: String) {
        outputs.forEach { output ->
            try {
                output.output(moduleName, message)
            } catch (e: Exception) {
                AndroidLogUtil.e("CompositeOutput error: ${e.message}", e)
            }
        }
    }

    override fun cleanup() {
        outputs.forEach { output ->
            try {
                output.cleanup()
            } catch (e: Exception) {
                AndroidLogUtil.e("CompositeOutput cleanup error: ${e.message}", e)
            }
        }
    }
}