package com.amishsxt.drawcast.core

import android.util.Log

object AppLogger {

    private const val DEFAULT_TAG = "Drawcast"

    fun v(tag: String = DEFAULT_TAG, msg: String) {
        if (Config.isLogEnabled) Log.v(tag, msg)
    }

    fun v(tag: String = DEFAULT_TAG, msg: String, tr: Throwable) {
        if (Config.isLogEnabled) Log.v(tag, msg, tr)
    }

    fun d(tag: String = DEFAULT_TAG, msg: String) {
        if (Config.isLogEnabled) Log.d(tag, msg)
    }

    fun d(tag: String = DEFAULT_TAG, msg: String, tr: Throwable) {
        if (Config.isLogEnabled) Log.d(tag, msg, tr)
    }

    fun i(tag: String = DEFAULT_TAG, msg: String) {
        if (Config.isLogEnabled) Log.i(tag, msg)
    }

    fun i(tag: String = DEFAULT_TAG, msg: String, tr: Throwable) {
        if (Config.isLogEnabled) Log.i(tag, msg, tr)
    }

    fun w(tag: String = DEFAULT_TAG, msg: String) {
        if (Config.isLogEnabled) Log.w(tag, msg)
    }

    fun w(tag: String = DEFAULT_TAG, msg: String, tr: Throwable) {
        if (Config.isLogEnabled) Log.w(tag, msg, tr)
    }

    fun e(tag: String = DEFAULT_TAG, msg: String) {
        if (Config.isLogEnabled) Log.e(tag, msg)
    }

    fun e(tag: String = DEFAULT_TAG, msg: String, tr: Throwable) {
        if (Config.isLogEnabled) Log.e(tag, msg, tr)
    }
}
