package com.kernelflux.ktoolbox.logger

/**
 * 多模块日志系统
 *
 * 适用于大型应用的多模块架构，提供：
 * 1. 模块级别的日志管理
 * 2. 自动标签生成
 * 3. 模块间日志隔离
 * 4. 统一的日志策略
 */
internal object MultiModuleLogger {

    // ==================== 日志模块定义 ====================

    /**
     * 日志模块信息
     */
    internal data class LogModule(
        val name: String,           // 模块名称，如 "UserModule"
        val packageName: String,    // 包名，如 "com.example.user"
        val version: String = "1.0.0", // 模块版本
        val description: String = "" // 模块描述
    ) {
        val tagPrefix: String get() = "[$name]"
    }

    // ==================== 日志模块注册表 ====================

    internal object LogModuleRegistry {
        private val modules = mutableMapOf<String, LogModule>()
        private val moduleLogCategories = mutableMapOf<String, MutableSet<String>>()

        /**
         * 注册日志模块
         */
        fun registerModule(module: LogModule) {
            modules[module.name] = module
            moduleLogCategories[module.name] = mutableSetOf()
            AndroidLogUtil.i("Log module registered: ${module.name} (${module.packageName})")
        }

        /**
         * 为日志模块注册日志分类
         */
        fun registerModuleLogCategories(moduleName: String, vararg logCategories: String) {
            val module = modules[moduleName]
            if (module != null) {
                val categories = moduleLogCategories[moduleName] ?: mutableSetOf()
                categories.addAll(logCategories)
                moduleLogCategories[moduleName] = categories

                // 自动注册到Logger
                Logger.registerModules(*logCategories)

                AndroidLogUtil.i(
                    "Log categories registered for $moduleName: ${
                        logCategories.joinToString(
                            ", "
                        )
                    }"
                )
            } else {
                AndroidLogUtil.e("Log module '$moduleName' not found")
            }
        }

        /**
         * 获取日志模块信息
         */
        fun getModule(moduleName: String): LogModule? {
            return modules[moduleName]
        }

        /**
         * 获取日志模块的日志分类列表
         */
        fun getModuleLogCategories(moduleName: String): Set<String> {
            return moduleLogCategories[moduleName] ?: emptySet()
        }

        /**
         * 获取所有日志模块
         */
        fun getAllModules(): List<LogModule> {
            return modules.values.toList()
        }

        /**
         * 检查日志模块是否存在
         */
        fun hasModule(moduleName: String): Boolean {
            return modules.containsKey(moduleName)
        }
    }

    // ==================== 多模块日志输出 ====================

    /**
     * 多模块Logcat输出
     */
    internal class MultiModuleLogcatOutput(
        private val enableStackTrace: Boolean = true,
        private val enableThreadInfo: Boolean = false,
        private val maxTagLength: Int = 23
    ) : LogOutput {

        private val tagCache = mutableMapOf<String, String>()

        override fun output(moduleName: String, message: String) {
            try {
                val tag = buildModuleTag(moduleName)
                val fullMessage = buildFullMessage(message)

                // 根据模块名称选择日志级别
                when {
                    moduleName.contains("ERROR", ignoreCase = true) -> android.util.Log.e(
                        tag,
                        fullMessage
                    )

                    moduleName.contains("WARN", ignoreCase = true) -> android.util.Log.w(
                        tag,
                        fullMessage
                    )

                    moduleName.contains("DEBUG", ignoreCase = true) -> android.util.Log.d(
                        tag,
                        fullMessage
                    )

                    else -> android.util.Log.i(tag, fullMessage)
                }
            } catch (e: Exception) {
                AndroidLogUtil.e("MultiModuleLogcatOutput error: ${e.message}", e)
            }
        }

        private fun buildModuleTag(moduleName: String): String {
            return tagCache.getOrPut(moduleName) {
                // 查找模块所属的日志模块
                val logModuleName = findLogModuleByCategory(moduleName)
                val module = LogModuleRegistry.getModule(logModuleName)

                val tag = if (module != null) {
                    "${module.tagPrefix}_$moduleName"
                } else {
                    "SDK_$moduleName"
                }

                if (tag.length > maxTagLength) {
                    tag.substring(0, maxTagLength)
                } else {
                    tag
                }
            }
        }

        private fun findLogModuleByCategory(logCategory: String): String {
            for ((moduleName, categories) in LogModuleRegistry.getAllModules().associate {
                it.name to LogModuleRegistry.getModuleLogCategories(it.name)
            }) {
                if (categories.contains(logCategory)) {
                    return moduleName
                }
            }
            return "Unknown"
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
                    excludeClasses = listOf("MultiModuleLogger", "LoggerCore", "AndroidLogUtil")
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

    // ==================== 多模块文件输出 ====================

    /**
     * 多模块文件输出
     */
    internal class MultiModuleFileOutput(
        private val logDir: String,
        private val maxFileSize: Long = 5 * 1024 * 1024,
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
                val logModuleName = findLogModuleByCategory(moduleName)
                val module = LogModuleRegistry.getModule(logModuleName)

                val logEntry = if (module != null) {
                    "[$timestamp] ${module.tagPrefix} [$moduleName] $message\n"
                } else {
                    "[$timestamp] [SDK] [$moduleName] $message\n"
                }

                synchronized(this) {
                    currentFileWriter?.write(logEntry)
                    currentFileWriter?.flush()

                    if (logFile.length() > maxFileSize) {
                        rotateLogFile()
                    }
                }
            } catch (e: java.io.IOException) {
                AndroidLogUtil.e("MultiModuleFileOutput error: ${e.message}", e)
            }
        }

        private fun findLogModuleByCategory(logCategory: String): String {
            for ((moduleName, categories) in LogModuleRegistry.getAllModules().associate {
                it.name to LogModuleRegistry.getModuleLogCategories(it.name)
            }) {
                if (categories.contains(logCategory)) {
                    return moduleName
                }
            }
            return "Unknown"
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
                    val fileName = "multi_module_log_${System.currentTimeMillis()}.txt"
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
                    file.name.startsWith("multi_module_log_") && file.name.endsWith(".txt")
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
                AndroidLogUtil.e("Failed to cleanup MultiModuleFileOutput: ${e.message}", e)
            }
        }
    }

    // ==================== 初始化和管理 ====================

    /**
     * 设置ApplicationContext（必须在初始化前调用）
     */
    internal fun setApplicationContext(context: android.content.Context) {
        ApplicationContextProvider.setContext(context.applicationContext)
    }

    /**
     * 初始化多模块日志系统
     */
    internal  fun initialize(
        enableLogcat: Boolean = true,
        enableFileOutput: Boolean = false,
        logDir: String? = null
    ) {
        // 初始化Logger
        Logger.initialize(enableLogcat = false) // 禁用默认Logcat

        // 添加多模块输出
        if (enableLogcat) {
            Logger.addOutput(MultiModuleLogcatOutput())
        }

        if (enableFileOutput) {
            val actualLogDir = logDir ?: LoggerUtils.getDefaultLogDir()
            Logger.addOutput(MultiModuleFileOutput(actualLogDir))
        }

        AndroidLogUtil.i("MultiModuleLogger initialized")
    }

    /**
     * 注册日志模块及其日志分类
     */
    internal fun registerLogModule(
        module: LogModule,
        vararg logCategories: String
    ) {
        LogModuleRegistry.registerModule(module)
        LogModuleRegistry.registerModuleLogCategories(module.name, *logCategories)
    }

    /**
     * 为已注册的日志模块添加日志分类
     */
    internal fun addModuleLogCategories(moduleName: String, vararg logCategories: String) {
        LogModuleRegistry.registerModuleLogCategories(moduleName, *logCategories)
    }

    /**
     * 启用日志模块的所有日志分类
     */
    internal  fun enableModuleLogCategories(moduleName: String) {
        val categories = LogModuleRegistry.getModuleLogCategories(moduleName)
        Logger.enableModules(*categories.toTypedArray())
        AndroidLogUtil.i("Enabled log categories for $moduleName: ${categories.joinToString(", ")}")
    }

    /**
     * 禁用日志模块的所有日志分类
     */
    internal fun disableModuleLogCategories(moduleName: String) {
        val categories = LogModuleRegistry.getModuleLogCategories(moduleName)
        Logger.disableModules(*categories.toTypedArray())
        AndroidLogUtil.i("Disabled log categories for $moduleName: ${categories.joinToString(", ")}")
    }

    /**
     * 启用所有日志模块的日志分类
     */
    internal fun enableAllModuleLogCategories() {
        val allCategories = LogModuleRegistry.getAllModules().flatMap { module ->
            LogModuleRegistry.getModuleLogCategories(module.name)
        }
        Logger.enableModules(*allCategories.toTypedArray())
        AndroidLogUtil.i("Enabled all log module categories")
    }

    /**
     * 获取日志模块统计信息
     */
    internal fun getModuleStats(): String {
        val modules = LogModuleRegistry.getAllModules()
        val totalCategories = modules.sumOf { module ->
            LogModuleRegistry.getModuleLogCategories(module.name).size
        }

        return buildString {
            appendLine("Log Module Statistics:")
            appendLine("Total Modules: ${modules.size}")
            appendLine("Total Log Categories: $totalCategories")
            appendLine()

            modules.forEach { module ->
                val categories = LogModuleRegistry.getModuleLogCategories(module.name)
                appendLine("${module.name} (${module.packageName}):")
                appendLine("  Version: ${module.version}")
                appendLine("  Log Categories: ${categories.joinToString(", ")}")
                appendLine()
            }
        }
    }

    // ==================== 便捷日志方法 ====================

    /**
     * 输出模块日志
     */
    internal fun log(moduleName: String, logCategory: String, message: String) {
        if (LogModuleRegistry.hasModule(moduleName)) {
            Logger.log(logCategory, message)
        } else {
            AndroidLogUtil.e("Log module '$moduleName' not found")
        }
    }

    /**
     * 格式化输出
     */
    fun log(moduleName: String, logCategory: String, format: String, vararg args: Any?) {
        if (LogModuleRegistry.hasModule(moduleName)) {
            Logger.log(logCategory, format, *args)
        } else {
            AndroidLogUtil.e("Log module '$moduleName' not found")
        }
    }

    /**
     * 异常输出
     */
    internal  fun log(
        moduleName: String,
        logCategory: String,
        throwable: Throwable,
        message: String? = null
    ) {
        if (LogModuleRegistry.hasModule(moduleName)) {
            Logger.log(logCategory, throwable, message)
        } else {
            AndroidLogUtil.e("Log module '$moduleName' not found")
        }
    }
}
