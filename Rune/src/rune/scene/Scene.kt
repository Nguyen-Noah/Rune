package rune.scene

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World
import com.github.quillraven.fleks.configureWorld
import glm_.mat4x4.Mat4
import rune.components.*
import rune.core.UUID
import rune.renderer.Renderer2D
import rune.renderer.RuneCamera
import rune.scene.systems.ScriptSystem

class Scene {
    private val entityMap = hashMapOf<UUID, Entity>()
    var viewportWidth = 0
    var viewportHeight = 0
    val world: World = configureWorld {
        injectables {
            add(this@Scene)
        }
        onAddEntity {

        }

        onRemoveEntity {

        }

        systems {
            add(ScriptSystem())
        }
    }

    fun destroyEntity(entity: Entity) {
        with(world) {
            entityMap.remove(entity[IDComponent].id)
            entity.remove()
        }
    }

    fun createEntity(name: String? = null): Entity {
        return createEntityWithUUID(UUID(), name)
    }

    fun createEntityWithUUID(uuid: UUID, name: String?): Entity {
        val entity = world.entity {
            it += IDComponent(uuid)
            it += TransformComponent()
            if (name != null) {
                it += TagComponent(name)
            }
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

        world.update(dt)

        val family = world.family { all(TransformComponent, CameraComponent) }
        family.forEach {
            val camera = it[CameraComponent]
            if (camera.primary) {
                mainCamera = camera.camera
                transform = it[TransformComponent].getTransform()
                return@forEach      // same as break but for forEach
            }
        }

        if (mainCamera != null) {
            Renderer2D.beginScene(mainCamera!!, transform!!)

            val renderers = world.family { all(SpriteRendererComponent, TransformComponent) }

            renderers.forEach {
                //println("Actual: ${System.identityHashCode(it)}")
                Renderer2D.drawQuad(it[TransformComponent].getTransform(), it[SpriteRendererComponent].color)
            }

            Renderer2D.endScene()
        }
    }

    fun getPrimaryCameraEntity(): Entity {
        var primaryCamera: Entity = world.entity()  // default to an empty entity
        world.family { all(CameraComponent) }.forEach {
            val comp = it[CameraComponent]

            if (comp.primary) {
                primaryCamera = it
                return@forEach
            }
        }
        return primaryCamera
    }
}
