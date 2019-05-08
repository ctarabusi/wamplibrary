package com.raumfeld.wamplibrary

import android.util.Log

object Logger {
    const val tag = "WampLibrary"

    fun v(msg: String) {
        Log.v(tag, msg)
    }

    fun d(msg: String) {
        Log.d(tag, msg)
    }

    fun i(msg: String) {
        Log.i(tag, msg)
    }

    fun w(msg: String) {
        Log.w(tag, msg)
    }

    fun e(msg: String) {
        Log.e(tag, msg)
    }

    fun e(t: Throwable) = Log.e(tag, t.stackTrace.toString())

    fun e(msg: String, t: Throwable) = Log.e(tag, "$msg:\n${t.stackTrace}")
}
