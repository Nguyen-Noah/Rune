package rune.scene

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.FixtureDef
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World
import com.github.quillraven.fleks.configureWorld
import glm_.mat4x4.Mat4
import ktx.box2d.createWorld
import rune.components.*
import rune.core.UUID
import rune.renderer.EditorCamera
import rune.renderer.Renderer2D
import rune.renderer.RuneCamera
import rune.scene.systems.ScriptSystem

import ktx.box2d.BodyDefinition
import ktx.box2d.body
import ktx.box2d.box

typealias PhysicsWorld = com.badlogic.gdx.physics.box2d.World

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

    var physicsWorld: PhysicsWorld? = null

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

    fun onRuntimeStart() {
        physicsWorld = createWorld(Vector2(0f, -9.8f))

        // creating physics bodies
        with (world) {
            family { all(RigidBody2DComponent) }.forEach {
                val transform = it[TransformComponent]
                val rb2d = it[RigidBody2DComponent]

                val body = physicsWorld!!.body(type = rb2d.type.toBox2d()) {
                    position.set(transform.translation.x, transform.translation.y)
                    angle = transform.rotation.z
                    fixedRotation = rb2d.fixedRotation

                    if (it.has(BoxCollider2DComponent)) {
                        val bc2d = it[BoxCollider2DComponent]

                        box(
                            width = bc2d.size.x * transform.scale.x,
                            height = bc2d.size.y * transform.scale.y
                        ) {
                            density = bc2d.density
                            friction = bc2d.friction
                            restitution = bc2d.restitution
                            // TODO: figure out how to set restitutionThreshold (not avail in ktx-box2d:1.13.3-rc1
                        }
                    }
                }

                rb2d.runtimeBody = body
            }
        }
    }

    fun onRuntimeStop() {
        physicsWorld = null
    }

    fun onUpdateRuntime(dt: Float) {
        // camera
        var mainCamera: RuneCamera? = null
        var transform: Mat4? = null

        world.update(dt)

        // physics
        val velocityIterations = 6      // how often is it doing calculations
        val positionIterations = 2      // TODO: expose these to the editor
        physicsWorld!!.step(dt, velocityIterations, positionIterations)

        // retrieve transform from box2d
        world.family { all(RigidBody2DComponent) }.forEach {
            val physicsTransform = it[TransformComponent]
            val rb2d = it[RigidBody2DComponent]

            rb2d.runtimeBody?.let { body ->
                val position = body.position
                physicsTransform.translation.x = position.x
                physicsTransform.translation.y = position.y
                physicsTransform.rotation.z = body.angle
            }

        }

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
                //Renderer2D.drawQuad(it[TransformComponent].getTransform(), it[SpriteRendererComponent].color)
                Renderer2D.drawSprite(it[TransformComponent].getTransform(), it[SpriteRendererComponent], it.id)
            }

            Renderer2D.endScene()
        }
    }

    fun onUpdateEditor(dt: Float, camera: EditorCamera) {
        Renderer2D.beginScene(camera)

        val renderers = world.family { all(SpriteRendererComponent, TransformComponent) }

        renderers.forEach {
            Renderer2D.drawSprite(it[TransformComponent].getTransform(), it[SpriteRendererComponent], it.id)
        }

        Renderer2D.endScene()
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
