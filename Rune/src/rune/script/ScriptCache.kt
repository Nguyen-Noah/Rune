package rune.script

import rune.scene.ScriptableEntity
import java.io.File
import kotlin.reflect.KClass
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvm.dependenciesFromClassContext
import kotlin.script.experimental.jvm.jvm
import kotlin.script.experimental.jvmhost.BasicJvmScriptingHost

object ScriptCache {
    // We’ll store the KClass of the evaluated script (must have a no-arg constructor),
    // plus the last-modified time so we know if we need to recompile.
    private data class CachedScript(
        val scriptClass: KClass<out ScriptableEntity>,
        val lastModified: Long
    )

    private val compiledScripts = mutableMapOf<String, CachedScript>()

    private val scriptCompilationConfiguration = ScriptCompilationConfiguration {
        baseClass(ScriptableEntity::class)

        jvm {
            dependenciesFromClassContext(ScriptableEntity::class, wholeClasspath = true)
        }

        defaultImports(
            // Import your relevant engine classes so the .kts can see them
            "rune.scene.ScriptableEntity",
            "rune.scene.Scene",
            "com.github.quillraven.fleks.Entity",
            "rune.components.TransformComponent",
            "glm_.mat4x4.Mat4",
            "glm_.vec3.Vec3"
        )
    }

    private val scriptEvaluationConfiguration = ScriptEvaluationConfiguration()

    /**
     * Returns a fresh instance of the script for [scriptPath].
     *
     * - If [scriptPath] is unchanged on disk, we re-instantiate from the cached [KClass].
     * - If it changed, we recompile and evaluate, then store the new [KClass].
     * - IMPORTANT: The .kts script must have a no-arg constructor if we want to call constructor again.
     */
    suspend fun getScriptInstance(scriptPath: String): ScriptableEntity? {
        val scriptFile = File(scriptPath)
        val currentModified = scriptFile.lastModified()
        if (!scriptFile.exists()) {
            println("Script file does not exist: $scriptPath")
            return null
        }

        // 1) If the file hasn’t changed, try returning a fresh instance from the cached KClass
        compiledScripts[scriptPath]?.let { cached ->
            if (cached.lastModified == currentModified) {
                // Attempt to call a no-arg constructor so we get a new instance each time
                val noArgCtor = cached.scriptClass.constructors.firstOrNull { it.parameters.isEmpty() }
                return if (noArgCtor != null) {
                    noArgCtor.call()
                } else {
                    println("No no-arg constructor found for script `$scriptPath`. Returning null.")
                    null
                }
            }
        }

        // 2) Otherwise, we must recompile
        val scriptText = scriptFile.readText()
        val scriptSource = scriptText.toScriptSource(scriptPath)
        val host = BasicJvmScriptingHost()

        val compilationResult = host.compiler(scriptSource, scriptCompilationConfiguration)
        if (compilationResult is ResultWithDiagnostics.Success) {
            // We have a compiled script => now evaluate it
            val evaluationResult = host.evaluator(compilationResult.value, scriptEvaluationConfiguration)
            if (evaluationResult is ResultWithDiagnostics.Success) {
                // The last evaluated expression should yield an instance of ScriptableEntity
                val scriptInstance = evaluationResult.value.returnValue.scriptInstance
                if (scriptInstance is ScriptableEntity) {
                    // Cache the compiled script's KClass
                    @Suppress("UNCHECKED_CAST")
                    val kScript = scriptInstance::class as KClass<out ScriptableEntity>
                    compiledScripts[scriptPath] = CachedScript(kScript, currentModified)

                    return scriptInstance
                } else {
                    println("Evaluation returned something not a ScriptableEntity. Check your .kts script.")
                }
            } else {
                evaluationResult.reports.forEach { println("Script evaluation error: ${it.message}") }
            }
        } else {
            compilationResult.reports.forEach { println("Script compilation error: ${it.message}") }
        }
        return null
    }
}
