package ir.mehdivijeh.scanner.util

sealed class LoggerType {
    object Debug : LoggerType()
    object Error : LoggerType()
}