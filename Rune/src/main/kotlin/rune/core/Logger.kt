package rune.core

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.concurrent.locks.ReentrantLock

enum class LogLevel(val priority: Int) {
    TRACE(0),
    DEBUG(1),
    INFO(2),
    WARN(3),
    ERROR(4)
}

data class LoggerConfig(
    var logLevel: LogLevel = LogLevel.DEBUG,
    var enableFileLogging: Boolean = false,
    var logFilePath: String = "rune-engine.log",
    var dateFormat: String = "yyyy-MM-dd HH:mm:ss.SSS"
)

object Logger {
    private var config = LoggerConfig()

    private var dateFormatter = SimpleDateFormat(config.dateFormat)

    private val lock = ReentrantLock()

    private fun formatMessage(level: LogLevel, message: String): String {
        val timestamp = dateFormatter.format(Date())
        val threadName = Thread.currentThread().name
        return "[$timestamp] [$threadName][${level.name}] $message "
    }

    private fun log(level: LogLevel, message: String) {
        val formattedMessage = formatMessage(level, message)

        println(formattedMessage)

        if (config.enableFileLogging) {
            File(config.logFilePath).appendText(formattedMessage + "\n")
        }
    }

    fun trace(message: String) = log(LogLevel.TRACE, message)
    fun debug(message: String) = log(LogLevel.DEBUG, message)
    fun info(message: String) = log(LogLevel.INFO, message)
    fun warn(message: String) = log(LogLevel.WARN, message)
    fun error(message: String) {
        log(LogLevel.ERROR, message)
        // kotlin.error(message)
    }

    fun error(message: String, throwable: Throwable) {
        log(LogLevel.ERROR, "$message\n${throwable.stackTraceToString()}")
    }
}