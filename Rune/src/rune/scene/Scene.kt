package rune.scene

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World
import com.github.quillraven.fleks.configureWorld
import glm_.mat4x4.Mat4
import kotlinx.coroutines.*
import rune.components.*
import rune.core.Coroutine
import rune.core.UUID
import rune.renderer.Renderer2D
import rune.renderer.RuneCamera
import rune.script.ScriptEngine
import java.io.File

class Scene {
    private val entityMap = hashMapOf<UUID, Entity>()
    private var viewportWidth = 0
    private var viewportHeight = 0
    val world: World = configureWorld {
        onAddEntity {

        }

        onRemoveEntity {

        }


    }

    fun createEntity(name: String? = null): Entity {
        return createEntityWithUUID(UUID(), name)
    }

    fun createEntityWithUUID(uuid: UUID, name: String?): Entity {
        val entity = world.entity {
            it += IDComponent(uuid)
            it += TransformComponent()
            it += TagComponent(name ?: "Entity")
        }
        entityMap[uuid] = entity

        return entity
    }

    fun getEntityByUUID(uuid: UUID): Entity? {
        return entityMap[uuid]
    }

    fun onViewportResize(width: Int, height: Int) {
        viewportWidth = width
        viewportHeight = height

        // resize our non-aspectRatio fixed cameras
        val cameras = world.family { all(CameraComponent) }
        cameras.forEach {comp ->
            if (!comp[CameraComponent].fixedAspectRatio) {
                comp[CameraComponent].camera.setViewportSize(width, height)
            }
        }
    }

    fun onUpdate(dt: Float) {
        // camera
        var mainCamera: RuneCamera? = null
        var transform: Mat4? = null

        // update scripts
        world.family { all(ScriptComponent) }
            .forEach {entity ->
                val scriptComp = entity[ScriptComponent]
                if (scriptComp.isBound) {
                    scriptComp.instance.onUpdate(dt)
                }
            }

        // -------------------------

        val family = world.family { all(TransformComponent, CameraComponent) }
        family.forEach {
            val camera = it[CameraComponent]
            if (camera.primary) {
                mainCamera = camera.camera
                transform = it[TransformComponent].transform
                return@forEach      // same as break but for forEach
            }
        }

        if (mainCamera != null) {
            Renderer2D.beginScene(mainCamera!!, transform!!)

            val renderers = world.family { all(SpriteRendererComponent, TransformComponent) }

            renderers.forEach {
                //println("Actual: ${System.identityHashCode(it)}")
                Renderer2D.drawQuad(it[TransformComponent].transform, it[SpriteRendererComponent].color)
            }

            Renderer2D.endScene()
        }
    }
}
