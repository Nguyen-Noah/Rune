package rune.scene

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.FixtureDef
import com.github.quillraven.fleks.*
import glm_.mat4x4.Mat4
import glm_.vec2.Vec2
import glm_.vec3.Vec3
import glm_.vec4.Vec4
import kotlinx.serialization.json.Json
import ktx.box2d.*
import rune.components.*
import rune.core.UUID
import rune.renderer.EditorCamera
import rune.renderer.Renderer2D
import rune.renderer.RuneCamera
import rune.scene.systems.ScriptSystem

import rune.core.Logger
import rune.scene.copyComponentsToEntity
import kotlin.reflect.KClass

typealias PhysicsWorld = com.badlogic.gdx.physics.box2d.World

class Scene {
    var viewportWidth = 0
    var viewportHeight = 0
    var world: World = configureWorld {
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

        return entity
    }

    fun duplicateEntity(src: Entity): Entity = with(world) {
        val name = src[TagComponent].tag
        val newEntity = createEntity("$name (copy)")

        val components = world.snapshotOf(src).components

        world.copyComponentsToEntity(newEntity, components)
        newEntity
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
                    if (it.has(CircleCollider2DComponent)) {
                        val cc2d = it[CircleCollider2DComponent]

                        circle(
                            radius = transform.scale.x * cc2d.radius,
                            position = Vector2(cc2d.offset.x, cc2d.offset.y)
                        ) {
                            density = cc2d.density
                            friction = cc2d.friction
                            restitution = cc2d.restitution
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

            world.family { all(SpriteRendererComponent, TransformComponent) }.forEach {
                Renderer2D.drawSprite(it[TransformComponent].getTransform(), it[SpriteRendererComponent], it.id)
            }
            world.family { all(CircleRendererComponent, TransformComponent) }.forEach {
                val circle = it[CircleRendererComponent]
                Renderer2D.drawCircle(it[TransformComponent].getTransform(), circle.color, circle.thickness, circle.fade, it.id)
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
        world.family { all(CircleRendererComponent, TransformComponent) }.forEach {
            val circle = it[CircleRendererComponent]
            Renderer2D.drawCircle(it[TransformComponent].getTransform(), circle.color, circle.thickness, circle.fade, it.id)
        }

        Renderer2D.drawRect(Vec3(0f), Vec2(1f), Vec4(1f, 1f, 1f, 1f))
        Renderer2D.drawLine(Vec3(0f), Vec3(5f), Vec4(1f, 0f, 1f, 1f), -1)

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

    companion object {
        fun copy(other: Scene): Scene {
            val newScene = Scene()

            newScene.viewportWidth = other.viewportWidth
            newScene.viewportHeight = other.viewportHeight

            // TODO: figure out a way to make this dynamic
            val newWorld = configureWorld {
                injectables {
                    add(newScene)
                }

                systems {
                    add(ScriptSystem())
                }
            }

            newScene.world = newWorld

            // a whole lot of code duplication here -> probably find a better way in the future
            other.world.family { all(TagComponent) }.forEach { src ->
                val id = src[IDComponent].id
                val tag = src[TagComponent].tag
                val newEntity = newScene.createEntityWithUUID(id, tag)

                val components = other.world.snapshotOf(src).components
                Logger.warn("Copying entity $id with components ${components.map { it::class.simpleName }}")

                newWorld.copyComponentsToEntity(newEntity, components)
            }

            Logger.warn("Scene copied")
            return newScene
        }
    }
}

fun World.copyComponentsToEntity(entity: Entity, components: List<Component<*>>) {
    with(this) {
        entity.configure {
            components.forEach { comp ->
                when (comp) {
                    is TransformComponent       -> entity += comp.copy()
                    is SpriteRendererComponent  -> entity += comp.copy()
                    is CircleRendererComponent  -> entity += comp.copy()
                    is RigidBody2DComponent     -> entity += comp.copy()
                    is BoxCollider2DComponent   -> entity += comp.copy()
                    is CircleCollider2DComponent   -> entity += comp.copy()
                    is CameraComponent          -> entity += comp.copy()
                    else -> Logger.warn("Unmatched component: ${comp::class.simpleName}")
                    // TODO: add ScriptComponent
                }
            }
        }
    }
}