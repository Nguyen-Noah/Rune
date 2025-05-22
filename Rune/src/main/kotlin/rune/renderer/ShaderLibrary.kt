package rune.renderer

import rune.renderer.gpu.Shader


class ShaderLibrary {
        private val shaders = mutableMapOf<String, Shader>()

        fun add(name: String, shader: Shader) {
            require(!exists(name)) { "Shader already exists!" }
            shaders[name] = shader
        }
        fun add(shader: Shader) {
            add(shader.getName(), shader)
        }

        fun load(filepath: String): Shader {
            val shader = Shader.create(filepath)
            add(shader)
            return shader
        }
        fun load(name: String, filepath: String): Shader {
            val shader = Shader.create(filepath)
            add(name, shader)
            return shader
        }

        fun get(name: String): Shader {
            require(exists(name)) { "Shader not found!" }
            return shaders[name]!!
        }

        private fun exists(name: String): Boolean = shaders.containsKey(name)
}