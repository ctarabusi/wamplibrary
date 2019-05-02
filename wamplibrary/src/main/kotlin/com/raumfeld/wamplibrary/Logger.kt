package com.raumfeld.wamplibrary

import android.util.Log

class Logger {

    fun v(msg: String) { Log.v(tag, msg) }

    fun d(msg: String) {  Log.d(tag, msg) }

    fun i(msg: String) {  Log.i(tag, msg) }

    fun w(msg: String) { Log.w(tag, msg) }

    fun e(msg: String) {  Log.e(tag,msg) }

     fun e(t: Throwable) =  e(t.stackTrace)

    fun e(msg: String, t: Throwable) =  e("$msg:\n${t.stackTrace}")

    companion object {
        const val tag = "WampLibrary"
    }
}
