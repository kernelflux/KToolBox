package com.kernelflux.ktoolbox.logger


/**
 * SDK日志系统
 */
object SDKLogger {


    @JvmStatic
    fun initialize(
        context: android.content.Context,
        enableLogcat: Boolean = true,
        enableFileOutput: Boolean = false,
        logDir: String? = null,
        tagPrefix: String = "KToolBoxSDK",
        enableStackTrace: Boolean = false
    ) {
        // 设置ApplicationContext
        ApplicationContextProvider.setContext(context.applicationContext)
        // 初始化Logger
        Logger.initialize(enableLogcat = false, enableFileOutput, logDir)
        if (enableLogcat) {
            val smartLogcat = SmartLogcatOutput(
                enableStackTrace = enableStackTrace,
                tagPrefix = tagPrefix
            )
            Logger.addOutput(smartLogcat)
        }
        AndroidLogUtil.i("SDKLogger initialized with tag prefix: $tagPrefix")
    }

    @JvmStatic
    fun registerModules(vararg moduleNames: String) {
        Logger.registerModules(*moduleNames)
    }

    @JvmStatic
    fun enableModules(vararg moduleNames: String) {
        Logger.enableModules(*moduleNames)
    }

    @JvmStatic
    fun disableModules(vararg moduleNames: String) {
        Logger.disableModules(*moduleNames)
    }

    @JvmStatic
    fun isModuleEnabled(moduleName: String): Boolean {
        return Logger.isModuleEnabled(moduleName)
    }

    @JvmStatic
    fun log(moduleName: String, message: String) {
        Logger.log(moduleName, message)
    }

    @JvmStatic
    fun log(moduleName: String, format: String, vararg args: Any?) {
        Logger.log(moduleName, format, *args)
    }

    @JvmStatic
    fun log(moduleName: String, throwable: Throwable, message: String? = null) {
        Logger.log(moduleName, throwable, message)
    }

    data class LogModule(
        val name: String,
        val packageName: String,
        val version: String = "1.0.0",
        val description: String = ""
    ) {
        val tagPrefix: String get() = "[$name]"
    }

    @JvmStatic
    fun registerLogModule(
        module: LogModule,
        vararg logCategories: String
    ) {
        val internalModule = MultiModuleLogger.LogModule(
            name = module.name,
            packageName = module.packageName,
            version = module.version,
            description = module.description
        )
        MultiModuleLogger.registerLogModule(internalModule, *logCategories)
    }

    @JvmStatic
    fun enableModuleLogCategories(moduleName: String) {
        MultiModuleLogger.enableModuleLogCategories(moduleName)
    }

    @JvmStatic
    fun disableModuleLogCategories(moduleName: String) {
        MultiModuleLogger.disableModuleLogCategories(moduleName)
    }

    @JvmStatic
    fun enableAllModuleLogCategories() {
        MultiModuleLogger.enableAllModuleLogCategories()
    }

    @JvmStatic
    fun log(moduleName: String, logCategory: String, message: String) {
        MultiModuleLogger.log(moduleName, logCategory, message)
    }

    @JvmStatic
    fun log(moduleName: String, logCategory: String, format: String, vararg args: Any?) {
        MultiModuleLogger.log(moduleName, logCategory, format, *args)
    }

    @JvmStatic
    fun log(
        moduleName: String,
        logCategory: String,
        throwable: Throwable,
        message: String? = null
    ) {
        MultiModuleLogger.log(moduleName, logCategory, throwable, message)
    }

    @JvmStatic
    fun addOutput(output: LogOutput) {
        Logger.addOutput(output)
    }

    @JvmStatic
    fun removeOutput(output: LogOutput) {
        Logger.removeOutput(output)
    }

    @JvmStatic
    fun clearOutputs() {
        Logger.clearOutputs()
    }

    @JvmStatic
    fun getStats(): String {
        return Logger.getStats()
    }

    @JvmStatic
    fun getModuleStats(): String {
        return MultiModuleLogger.getModuleStats()
    }

    // 便捷方法
    @JvmStatic
    fun debug(moduleName: String, message: String) {
        log("${moduleName}_DEBUG", message)
    }

    @JvmStatic
    fun info(moduleName: String, message: String) {
        log("${moduleName}_INFO", message)
    }

    @JvmStatic
    fun warn(moduleName: String, message: String) {
        log("${moduleName}_WARN", message)
    }

    @JvmStatic
    fun error(moduleName: String, message: String) {
        log("${moduleName}_ERROR", message)
    }

    @JvmStatic
    fun error(moduleName: String, throwable: Throwable, message: String? = null) {
        log("${moduleName}_ERROR", throwable, message)
    }

    // 链式调用支持
    fun debug(moduleName: String): LoggerChain {
        return LoggerChain(moduleName, "DEBUG")
    }

    fun info(moduleName: String): LoggerChain {
        return LoggerChain(moduleName, "INFO")
    }

    fun warn(moduleName: String): LoggerChain {
        return LoggerChain(moduleName, "WARN")
    }

    fun error(moduleName: String): LoggerChain {
        return LoggerChain(moduleName, "ERROR")
    }


    /**
     * 链式调用支持类
     */
    class LoggerChain(
        private val moduleName: String,
        private val level: String
    ) {
        fun msg(message: String) {
            log("${moduleName}_$level", message)
        }

        fun msg(format: String, vararg args: Any?) {
            log("${moduleName}_$level", format, *args)
        }

        fun exception(throwable: Throwable, message: String? = null) {
            log("${moduleName}_$level", throwable, message)
        }
    }

}
