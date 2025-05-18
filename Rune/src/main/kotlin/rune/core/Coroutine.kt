package rune.core

import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

class Coroutine(
    dispatcher: CoroutineDispatcher = Dispatchers.Default
) : CoroutineScope {

    // allows children to fail independently
    private val job = SupervisorJob()

    // composing the context with the provided dispatcher and job
    override val coroutineContext: CoroutineContext = dispatcher + job

    /**
     * Launches a new coroutine within the wrapper's scope
     *
     * @param block The suspendable block to execute
     * @return The Job representing the coroutine
     */
    fun launchTask(block: suspend CoroutineScope.() -> Unit): Job {
        return launch {
            try {
                block()
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    /**
     * Executes a suspending task and returns its result
     *
     * @param block The suspendable block producing a result
     * @return The result of the block
     */
    suspend fun <T> runTask(block: suspend CoroutineScope.() -> T): T {
        return withContext(coroutineContext) {
            block()
        }
    }

    /**
     * Launches an asynchronous task that returns a Deferred result
     *
     * @param block The suspendable block producing a result
     * @return A Deferred representing the pending result
     */
    fun <T> asyncTask(block: suspend CoroutineScope.() -> T): Deferred<T> {
        return async {
            try {
                block()
            } catch (ex: Exception) {
                ex.printStackTrace()
                throw ex
            }
        }
    }

    /**
     * Cancels all currently running child coroutines
     */
    fun cancelAll() {
        job.cancelChildren()
    }
}