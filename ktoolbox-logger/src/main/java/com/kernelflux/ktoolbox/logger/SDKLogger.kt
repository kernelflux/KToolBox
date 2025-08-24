package com.kernelflux.ktoolbox.logger

import android.content.Context


/**
 * SDK日志系统
 */
object SDKLogger {

    // ==================== 初始化 ====================
    /**
     * 初始化日志系统
     */
    fun initialize(
        context: Context,
        enableLogcat: Boolean = true,
        enableFileOutput: Boolean = false,
        logDir: String? = null
    ) {
        ApplicationContextProvider.setContext(context.applicationContext)
        Logger.initialize(enableLogcat, enableFileOutput, logDir)
        AndroidLogUtil.i("SDKLogger initialized")
    }

    // ==================== 简单日志API ====================

    /**
     * 注册模块
     */
    fun registerModules(vararg moduleNames: String) {
        Logger.registerModules(*moduleNames)
    }

    /**
     * 启用模块
     */
    fun enableModules(vararg moduleNames: String) {
        Logger.enableModules(*moduleNames)
    }

    /**
     * 禁用模块
     */
    fun disableModules(vararg moduleNames: String) {
        Logger.disableModules(*moduleNames)
    }

    /**
     * 检查模块是否启用
     */
    fun isModuleEnabled(moduleName: String): Boolean {
        return Logger.isModuleEnabled(moduleName)
    }

    /**
     * 输出日志
     */
    fun log(moduleName: String, message: String) {
        Logger.log(moduleName, message)
    }

    /**
     * 格式化输出
     */
    fun log(moduleName: String, format: String, vararg args: Any?) {
        Logger.log(moduleName, format, *args)
    }

    /**
     * 异常输出
     */
    fun log(moduleName: String, throwable: Throwable, message: String? = null) {
        Logger.log(moduleName, throwable, message)
    }

    // ==================== 多模块日志API ====================

    /**
     * 日志模块信息
     */
    data class LogModule(
        val name: String,           // 模块名称，如 "UserModule"
        val packageName: String,    // 包名，如 "com.example.user"
        val version: String = "1.0.0", // 模块版本
        val description: String = "" // 模块描述
    ) {
        val tagPrefix: String get() = "[$name]"
    }

    /**
     * 注册多模块日志
     */
    fun registerLogModule(
        module: LogModule,
        vararg logCategories: String
    ) {
        MultiModuleLogger.registerLogModule(
            MultiModuleLogger.LogModule(
                name = module.name,
                packageName = module.packageName,
                version = module.version,
                description = module.description
            ),
            *logCategories
        )
    }

    /**
     * 启用模块的所有日志分类
     */
    fun enableModuleLogCategories(moduleName: String) {
        MultiModuleLogger.enableModuleLogCategories(moduleName)
    }

    /**
     * 禁用模块的所有日志分类
     */
    fun disableModuleLogCategories(moduleName: String) {
        MultiModuleLogger.disableModuleLogCategories(moduleName)
    }

    /**
     * 启用所有模块的日志分类
     */
    fun enableAllModuleLogCategories() {
        MultiModuleLogger.enableAllModuleLogCategories()
    }

    /**
     * 输出多模块日志
     */
    fun log(moduleName: String, logCategory: String, message: String) {
        MultiModuleLogger.log(moduleName, logCategory, message)
    }

    /**
     * 格式化输出多模块日志
     */
    fun log(moduleName: String, logCategory: String, format: String, vararg args: Any?) {
        MultiModuleLogger.log(moduleName, logCategory, format, *args)
    }

    /**
     * 异常输出多模块日志
     */
    fun log(
        moduleName: String,
        logCategory: String,
        throwable: Throwable,
        message: String? = null
    ) {
        MultiModuleLogger.log(moduleName, logCategory, throwable, message)
    }

    // ==================== 高级功能 ====================

    /**
     * 添加自定义输出
     */
    fun addOutput(output: LogOutput) {
        Logger.addOutput(output)
    }

    /**
     * 移除输出
     */
    fun removeOutput(output: LogOutput) {
        Logger.removeOutput(output)
    }

    /**
     * 清除所有输出
     */
    fun clearOutputs() {
        Logger.clearOutputs()
    }

    /**
     * 获取统计信息
     */
    fun getStats(): String {
        return Logger.getStats()
    }

    /**
     * 获取模块统计信息
     */
    fun getModuleStats(): String {
        return MultiModuleLogger.getModuleStats()
    }
}
