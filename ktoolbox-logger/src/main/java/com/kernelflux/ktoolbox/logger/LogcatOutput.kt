package com.kernelflux.ktoolbox.logger

import android.util.Log
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Android Logcat输出
 */
class LogcatOutput(
    private val enableStackTrace: Boolean = true,
    private val enableThreadInfo: Boolean = false,
    private val maxTagLength: Int = 23
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

            val tag = if (moduleName.length > maxTagLength) {
                moduleName.substring(0, maxTagLength)
            } else {
                moduleName
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

        if (enableStackTrace) {
            val stackTrace = LoggerUtils.getRelevantStackTrace(
                excludeClasses = listOf("LogcatOutput", "LoggerCore", "AndroidLogUtil")
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
    private val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", java.util.Locale.getDefault())

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

    private val logBuffer = java.util.Collections.newSetFromMap(java.util.concurrent.ConcurrentHashMap<String, Boolean>())
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
