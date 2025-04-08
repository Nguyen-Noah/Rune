package rune.debug

import java.io.File
import java.io.PrintWriter
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

data class ProfileResult(
    val name: String,
    val start: Double,   // start time in microseconds
    val elapsed: Long,   // elapsed time in microseconds
    val threadId: Long
)

data class InstrumentationSession(val name: String)

object Instrumentor {
    private var currentSession: InstrumentationSession? = null
    private var writer: PrintWriter? = null
    private val lock = ReentrantLock()
    private var sessionStartTime: Long = 0

    fun beginSession(name: String, filepath: String = "results.json") {
        lock.withLock {
            if (currentSession != null) {
                // End current session if one is active.
                endSession()
            }
            writer = PrintWriter(File(filepath))
            currentSession = InstrumentationSession(name)
            writeHeader()
            sessionStartTime = System.nanoTime()  // record start time of the session
        }
    }

    fun endSession() {
        lock.withLock {
            if (currentSession != null) {
                val sessionEndTime = System.nanoTime()
                val elapsed = (sessionEndTime - sessionStartTime) / 1000  // microseconds
                val startMicro = sessionStartTime / 1000.0
                // Write one profile event for the entire session.
                writeProfile(
                    ProfileResult(
                        name = "Session: ${currentSession!!.name}",
                        start = startMicro,
                        elapsed = elapsed,
                        threadId = Thread.currentThread().id
                    )
                )
                writeFooter()
                writer?.close()
                writer = null
                currentSession = null
                sessionStartTime = 0
            }
        }
    }

    fun writeProfile(result: ProfileResult) {
        val json = buildString {
            append(",{")
            append("\"cat\":\"function\",")
            append("\"dur\":${result.elapsed},")
            append("\"name\":\"${result.name}\",")
            append("\"ph\":\"X\",")
            append("\"pid\":0,")
            append("\"tid\":${result.threadId},")
            append("\"ts\":${result.start}")
            append("}")
        }
        lock.withLock {
            currentSession?.let {
                writer?.print(json)
                writer?.flush()
            }
        }
    }

    private fun writeHeader() {
        writer?.println("{\"otherData\": {},\"traceEvents\":[{}")
        writer?.flush()
    }

    private fun writeFooter() {
        writer?.println("]}")
        writer?.flush()
    }
}

/**
 * For finer-grained profiling, you can still use this timer.
 */
class InstrumentationTimer(private val name: String) : AutoCloseable {
    private val startTime = System.nanoTime()
    private var stopped = false

    fun stop() {
        if (!stopped) {
            val endTime = System.nanoTime()
            val elapsed = (endTime - startTime) / 1000  // microseconds
            val startMicro = startTime / 1000.0
            Instrumentor.writeProfile(
                ProfileResult(
                    name = name,
                    start = startMicro,
                    elapsed = elapsed,
                    threadId = Thread.currentThread().id
                )
            )
            stopped = true
        }
    }

    override fun close() {
        stop()
    }
}
