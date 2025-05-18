package rune.script

import rune.scene.ScriptableEntity
import kotlin.script.experimental.api.*
import kotlin.script.experimental.jvm.dependenciesFromClassContext
import kotlin.script.experimental.jvm.jvm

object ScriptConfiguration : ScriptCompilationConfiguration(
    {
        baseClass(ScriptableEntity::class)

        jvm {
            compilerOptions("-jvm-target", "21")    // TODO: maybe dont hard code
            dependenciesFromClassContext(ScriptableEntity::class, wholeClasspath = true)
        }

        ide {
            acceptedLocations(ScriptAcceptedLocation.Everywhere)
        }

        defaultImports(
            "rune.scene.ScriptableEntity",
            "rune.scene.Scene",
            "rune.components.*",
            "rune.core.Input",
            "rune.core.Key",
            "glm_.glm"                  // TODO: wrap in Rune primitives
        )
    }
) {
    private fun readResolve(): Any = ScriptConfiguration
}