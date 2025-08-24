package com.kernelflux.ktoolbox.logger

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger


/**
 * 核心日志系统
 */
internal object Logger {

    private val enabledModules = AtomicInteger(0)
    private val isInitialized = AtomicBoolean(false)

    /**
     * 设置ApplicationContext（必须在初始化前调用）
     */
    internal fun setApplicationContext(context: android.content.Context) {
        ApplicationContextProvider.setContext(context.applicationContext)
    }

    /**
     * 初始化核心日志系统
     */
    internal fun initialize(
        enableLogcat: Boolean = true,
        enableFileOutput: Boolean = false,
        logDir: String? = null
    ) {
        if (isInitialized.compareAndSet(false, true)) {
            if (enableLogcat) {
                LogOutputManager.addOutput(LogcatOutput())
            }

            if (enableFileOutput) {
                val actualLogDir = logDir ?: LoggerUtils.getDefaultLogDir()
                LogOutputManager.addOutput(AndroidFileOutput(actualLogDir))
            }

            AndroidLogUtil.i("Logger initialized")
        }
    }

    /**
     * 注册模块
     */
    internal fun registerModules(vararg moduleNames: String) {
        moduleNames.forEach { moduleName ->
            val bit = ModuleManager.registerModule(moduleName)
            if (bit != -1) {
                AndroidLogUtil.i("Module registered: $moduleName (bit: $bit)")
            }
        }
    }

    /**
     * 启用模块
     */
    internal fun enableModules(vararg moduleNames: String) {
        moduleNames.forEach { moduleName ->
            val bit = ModuleManager.getModuleBit(moduleName)
            if (bit != null) {
                enabledModules.updateAndGetCompat { it or bit }
                AndroidLogUtil.i("Module enabled: $moduleName")
            } else {
                AndroidLogUtil.e("Module not found: $moduleName")
            }
        }
    }

    /**
     * 禁用模块
     */
    internal fun disableModules(vararg moduleNames: String) {
        moduleNames.forEach { moduleName ->
            val bit = ModuleManager.getModuleBit(moduleName)
            if (bit != null) {
                enabledModules.updateAndGetCompat { it and bit.inv() }
                AndroidLogUtil.i("Module disabled: $moduleName")
            } else {
                AndroidLogUtil.e("Module not found: $moduleName")
            }
        }
    }

    /**
     * 检查模块是否启用
     */
    internal fun isModuleEnabled(moduleName: String): Boolean {
        val bit = ModuleManager.getModuleBit(moduleName)
        return bit != null && (enabledModules.get() and bit) != 0
    }

    /**
     * 添加日志输出
     */
    internal fun addOutput(output: LogOutput) {
        LogOutputManager.addOutput(output)
    }

    /**
     * 移除日志输出
     */
    internal fun removeOutput(output: LogOutput) {
        LogOutputManager.removeOutput(output)
    }

    /**
     * 清除所有输出
     */
    internal fun clearOutputs() {
        LogOutputManager.clearOutputs()
    }

    /**
     * 输出日志
     */
    internal fun log(moduleName: String, message: String) {
        if (!isModuleEnabled(moduleName)) {
            return
        }

        LogOutputManager.outputToAll(moduleName, message)
    }

    /**
     * 格式化输出
     */
    internal fun log(moduleName: String, format: String, vararg args: Any?) {
        try {
            val message = String.format(format, *args)
            log(moduleName, message)
        } catch (e: Exception) {
            AndroidLogUtil.e("Format error: ${e.message}", e)
        }
    }

    /**
     * 异常输出
     */
    internal fun log(moduleName: String, throwable: Throwable, message: String? = null) {
        val fullMessage = LoggerUtils.formatStackTrace(throwable, message)
        log(moduleName, fullMessage)
    }

    /**
     * 获取统计信息
     */
    internal fun getStats(): String {
        return buildString {
            appendLine("Logger Statistics:")
            appendLine("Initialized: ${isInitialized.get()}")
            appendLine("Output Count: ${LogOutputManager.getOutputCount()}")
            appendLine("Enabled Modules: ${enabledModules.get()}")
            appendLine("Exception Count: ${LogOutputManager.getExceptionCount()}")
            appendLine("Registered Modules: ${ModuleManager.getModuleCount()}")
            appendLine("Module Names: ${ModuleManager.getAllModuleNames().joinToString(", ")}")
        }
    }
}
