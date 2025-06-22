package com.kernelflux.ktoolbox.core

import android.util.Log
import java.lang.reflect.Field
import java.lang.reflect.Method
import kotlin.jvm.Throws


object ReflectUtil {
    private val TAG: String = ReflectUtil::class.java.simpleName


    @JvmStatic
    fun getDeclaredField(cls: Class<*>?, filedName: String, obj: Any?): Any? {
        return try {
            val declaredField: Field? = cls?.getDeclaredField(filedName)
            declaredField?.isAccessible = true
            declaredField?.get(obj)
        } catch (e: Exception) {
            Log.w(TAG, e)
            null
        }
    }

    @Throws(NoSuchMethodException::class)
    @JvmStatic
    fun getDeclaredMethod(obj: Any?, methodName: String, clsArr: Array<Class<*>>? = null): Method {
        val method = getDeclaredMethod(obj?.javaClass, methodName, clsArr)
        method.isAccessible = true
        return method
    }

    @JvmStatic
    fun getDeclaredField(cls: Class<*>?, filedName: String): Any? {
        return try {
            val field = cls?.getDeclaredField(filedName)
            field?.isAccessible = true
            return field?.get(cls)
        } catch (e: Exception) {
            Log.w(TAG, e)
            null
        }
    }

    @JvmStatic
    fun invokeDeclaredMethod(
        cls: Class<*>?,
        methodName: String,
        obj: Any?,
        clsArr: Array<Class<*>>? = null,
        objArr: Array<*>? = null
    ): Any? {
        return try {
            val declaredMethod = if (clsArr != null) {
                cls?.getDeclaredMethod(methodName, *clsArr)
            } else {
                cls?.getDeclaredMethod(methodName)
            }
            declaredMethod?.isAccessible = true
            if (objArr != null) {
                declaredMethod?.invoke(obj, *objArr)
            } else {
                declaredMethod?.invoke(obj)
            }
        } catch (e: Exception) {
            Log.w(TAG, e)
            null
        }
    }


    @Throws(NoSuchMethodException::class)
    @JvmStatic
    fun getDeclaredMethod(
        cls: Class<*>?,
        methodName: String,
        clsArr: Array<Class<*>>? = null
    ): Method {
        return if (cls != null) {
            try {
                if (clsArr != null) {
                    cls.getDeclaredMethod(methodName, *clsArr)
                } else {
                    cls.getDeclaredMethod(methodName)
                }
            } catch (unused: NoSuchMethodException) {
                getDeclaredMethod(cls.superclass, methodName, clsArr)
            }
        } else {
            throw NoSuchMethodException("Error method !")
        }
    }

    @JvmStatic
    fun setDeclaredField(cls: Class<*>?, fieldName: String, obj: Any?, param: Any?) {
        try {
            val field = cls?.getDeclaredField(fieldName)
            field?.isAccessible = true
            field?.set(obj, param)
        } catch (e: Exception) {
            Log.w(TAG, e)
        }
    }

    @JvmStatic
    fun getDeclaredField(clsName: String, fieldName: String, obj: Any): Any? {
        return try {
            getDeclaredField(Class.forName(clsName), fieldName, obj)
        } catch (e2: java.lang.Exception) {
            Log.w("ReflectUtil", e2)
            null
        }
    }

    @JvmStatic
    fun setDeclaredField(clsName: String, fieldName: String, obj: Any?, param: Any?) {
        try {
            setDeclaredField(Class.forName(clsName), fieldName, obj, param)
        } catch (e: Exception) {
            Log.w(TAG, e)
        }
    }

    @JvmStatic
    fun invokeDeclaredMethod(
        clsName: String,
        methodName: String,
        obj: Any?,
        clsArr: Array<Class<*>>? = null,
        objArr: Array<Any?>? = null
    ): Any? {
        return try {
            invokeDeclaredMethod(Class.forName(clsName), methodName, obj, clsArr, objArr)
        } catch (e2: java.lang.Exception) {
            Log.w("ReflectUtil", e2)
            null
        }
    }


    @JvmStatic
    fun getField(cls: Class<*>?, filedName: String, obj: Any?): Any? {
        return try {
            val field: Field? = cls?.getField(filedName)
            field?.isAccessible = true
            field?.get(obj)
        } catch (e: Exception) {
            Log.w(TAG, e)
            null
        }
    }

    @Throws(NoSuchMethodException::class)
    @JvmStatic
    fun getMethod(obj: Any?, methodName: String, clsArr: Array<Class<*>>? = null): Method {
        val method = getMethod(obj?.javaClass, methodName, clsArr)
        method.isAccessible = true
        return method
    }

    @JvmStatic
    fun getField(cls: Class<*>?, filedName: String): Any? {
        return try {
            val field = cls?.getField(filedName)
            field?.isAccessible = true
            return field?.get(cls)
        } catch (e: Exception) {
            Log.w(TAG, e)
            null
        }
    }

    @JvmStatic
    fun invokeMethod(
        cls: Class<*>?,
        methodName: String,
        obj: Any?,
        clsArr: Array<Class<*>?>? = null,
        objArr: Array<*>? = null
    ): Any? {
        return try {
            val method = if (clsArr != null) {
                cls?.getMethod(methodName, *clsArr)
            } else {
                cls?.getMethod(methodName)
            }
            method?.isAccessible = true
            if (objArr != null) {
                method?.invoke(obj, *objArr)
            } else {
                method?.invoke(obj)
            }
        } catch (e: Exception) {
            Log.w(TAG, e)
            null
        }
    }


    @JvmStatic
    fun invokeMethod(
        method: Method?,
        obj: Any?,
        objArr: Array<*>? = null
    ): Any? {
        return try {
            if (objArr != null) {
                method?.invoke(obj, *objArr)
            } else {
                method?.invoke(obj)
            }
        } catch (e: Exception) {
            Log.w(TAG, e)
            null
        }
    }


    @Throws(NoSuchMethodException::class)
    @JvmStatic
    fun getMethod(cls: Class<*>?, methodName: String, clsArr: Array<Class<*>>? = null): Method {
        return if (cls != null) {
            try {
                if (clsArr != null) {
                    cls.getMethod(methodName, *clsArr)
                } else {
                    cls.getMethod(methodName)
                }
            } catch (unused: NoSuchMethodException) {
                getMethod(cls.superclass, methodName, clsArr)
            }
        } else {
            throw NoSuchMethodException("Error method !")
        }
    }

    @JvmStatic
    fun setField(cls: Class<*>?, fieldName: String, obj: Any?, param: Any?) {
        try {
            val field = cls?.getField(fieldName)
            field?.isAccessible = true
            field?.set(obj, param)
        } catch (e: Exception) {
            Log.w(TAG, e)
        }
    }

    @JvmStatic
    fun getField(clsName: String, fieldName: String, obj: Any): Any? {
        return try {
            getField(Class.forName(clsName), fieldName, obj)
        } catch (e2: java.lang.Exception) {
            Log.w("ReflectUtil", e2)
            null
        }
    }

    @JvmStatic
    fun setField(clsName: String, fieldName: String, obj: Any?, param: Any?) {
        try {
            setField(Class.forName(clsName), fieldName, obj, param)
        } catch (e: Exception) {
            Log.w(TAG, e)
        }
    }

    @JvmStatic
    fun invokeMethod(
        clsName: String,
        methodName: String,
        obj: Any?,
        clsArr: Array<Class<*>?>? = null,
        vararg objArr: Any?
    ): Any? {
        return try {
            invokeMethod(Class.forName(clsName), methodName, obj, clsArr, objArr)
        } catch (e2: java.lang.Exception) {
            Log.w("ReflectUtil", e2)
            null
        }
    }
}