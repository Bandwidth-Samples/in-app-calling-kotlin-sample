package com.bandwidth.sample

import android.util.Log
import com.bandwidth.webrtc.log.Logger

/**
 * A sample logger implementation that uses Android's Log class.
 */
class SampleLogger : Logger {

    // Tag for identifying log messages.
    companion object {
        private const val TAG = "SampleLogger"
    }

    /**
     * Logs a message with the given log level.
     *
     * @param level The log level (verbose, debug, info, warning, error).
     * @param tag The log tag.
     * @param msg The log message.
     * @param tr The throwable to log (optional).
     * @return Always returns 0.
     */
    private fun log(level: Int, tag: String?, msg: String?, tr: Throwable? = null): Int {
        Log.println(level, tag ?: TAG, msg.orEmpty())
        tr?.let { Log.println(level, tag ?: TAG, Log.getStackTraceString(it)) }
        return 0
    }

    override fun v(tag: String?, msg: String?): Int = log(Log.VERBOSE, tag, msg)
    override fun v(tag: String?, msg: String?, tr: Throwable?): Int = log(Log.VERBOSE, tag, msg, tr)

    override fun d(tag: String?, msg: String?): Int = log(Log.DEBUG, tag, msg)
    override fun d(tag: String?, msg: String?, tr: Throwable?): Int = log(Log.DEBUG, tag, msg, tr)

    override fun i(tag: String?, msg: String?): Int = log(Log.INFO, tag, msg)
    override fun i(tag: String?, msg: String?, tr: Throwable?): Int = log(Log.INFO, tag, msg, tr)

    override fun w(tag: String?, msg: String?): Int = log(Log.WARN, tag, msg)
    override fun w(tag: String?, msg: String?, tr: Throwable?): Int = log(Log.WARN, tag, msg, tr)

    override fun e(tag: String?, msg: String?): Int = log(Log.ERROR, tag, msg)
    override fun e(tag: String?, msg: String?, tr: Throwable?): Int = log(Log.ERROR, tag, msg, tr)
}
