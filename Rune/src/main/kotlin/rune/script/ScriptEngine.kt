package rune.script

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rune.scene.ScriptableEntity
import java.io.File
import kotlin.script.experimental.annotations.KotlinScript
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvm.dependenciesFromClassContext
import kotlin.script.experimental.jvm.jvm
import kotlin.script.experimental.jvmhost.BasicJvmScriptingHost

object ScriptEngine {
    private data class ScriptCacheKey(
        val filePath: String,
        val lastModified: Long
    )

    // ConcurrentHashMap() if needed for thread safety
    private val compiledScripts = mutableMapOf<ScriptCacheKey, CompiledScript>()

    private val host = BasicJvmScriptingHost()

    // TODO: find a better way than sending in a File object
    suspend fun loadScript(file: File): ScriptableEntity? = withContext(Dispatchers.IO) {
        val key = ScriptCacheKey(file.absolutePath, file.lastModified())
        val compiledScript = compiledScripts[key] ?: compileScript(file).also {
            if (it != null) compiledScripts[key] = it
        }
        compiledScript?.let { evaluateCompiledScript(it) }
    }

    private suspend fun compileScript(file: File): CompiledScript? = withContext(Dispatchers.IO) {
        val scriptSource = file.readText().toScriptSource(file.name)
        val compilationResult  = host.compiler(scriptSource, ScriptConfiguration)
        if (compilationResult  is ResultWithDiagnostics.Success) {
            compilationResult .value
        } else {
            compilationResult .reports.forEach { println("${it.severity}: ${it.message}") }
            null
        }
    }

    private suspend fun evaluateCompiledScript(compiled: CompiledScript): ScriptableEntity? = withContext(Dispatchers.IO) {
        val evalResult = host.evaluator(compiled, ScriptEvaluation)
        if (evalResult is ResultWithDiagnostics.Success) {
            val scriptInstance = evalResult.value.returnValue.scriptInstance
            if (scriptInstance is ScriptableEntity) {
                scriptInstance
            } else {
                println("Warning: Script instance is not a ScriptableEntity.")
                null
            }
        } else {
            evalResult.reports.forEach { println("${it.severity}: ${it.message}") }
            null
        }
    }
}