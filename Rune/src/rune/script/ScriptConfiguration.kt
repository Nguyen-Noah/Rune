package rune.script

import rune.scene.ScriptableEntity
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.baseClass
import kotlin.script.experimental.api.compilerOptions
import kotlin.script.experimental.api.defaultImports
import kotlin.script.experimental.jvm.dependenciesFromClassContext
import kotlin.script.experimental.jvm.jvm

object ScriptConfiguration : ScriptCompilationConfiguration(
    {
        baseClass(ScriptableEntity::class)

        jvm {
            compilerOptions("-jvm-target", "21")    // TODO: maybe dont hard code
            dependenciesFromClassContext(ScriptableEntity::class, wholeClasspath = true)
        }

        defaultImports(
            "rune.scene.ScriptableEntity",
            "rune.components.TransformComponent",
            "rune.scene.Scene",
            "rune.components.*",
            "rune.core.Input",
            "rune.core.Key"
        )
    }
) {
    private fun readResolve(): Any = ScriptConfiguration
}