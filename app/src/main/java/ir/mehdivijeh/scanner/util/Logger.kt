package ir.mehdivijeh.scanner.util

import android.util.Log

object Logger {
    fun log(tag: String, message: String, loggerType: LoggerType) {
        when (loggerType) {
            LoggerType.Debug -> Log.d(tag, "debug log: $message")
            LoggerType.Error -> Log.e(tag, "error log: $message")
        }
    }
}