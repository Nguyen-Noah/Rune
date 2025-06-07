package rune.scene

import com.badlogic.gdx.math.Vector2
import com.github.quillraven.fleks.*
import ktx.box2d.*
import rune.components.*
import rune.core.Logger
import rune.core.UUID
import rune.renderer.EditorCamera
import rune.renderer.Renderer
import rune.renderer.renderer2d.Renderer2D
import rune.scene.systems.ScriptSystem

typealias PhysicsWorld = com.badlogic.gdx.physics.box2d.World

class Scene {
    var viewportWidth = 0
        private set
    var viewportHeight = 0
        private set

    val lightEnvironment: SceneLights = SceneLights()

    var world: World = configureWorld {
        injectables { add(this@Scene) }
        systems { add(ScriptSystem()) }
    }

    private var physicsWorld: PhysicsWorld2D = PhysicsWorld2D()

    fun destroyEntity(entity: Entity) = with(world) { entity.remove() }

    fun createEntity(name: String? = null, uuid: UUID = UUID()): Entity {
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
        val newEntity = createEntity("${src[TagComponent].tag} (copy)")
        world.copyComponentsToEntity(newEntity, world.snapshotOf(src).components)
        newEntity
    }

    /* ------------------------------------------------------------------ */
    /*  Window resize                                                     */
    /* ------------------------------------------------------------------ */

    fun onViewportResize(width: Int, height: Int) {
        viewportWidth = width
        viewportHeight = height

        // resize our non-aspectRatio fixed cameras
        world.family { all(CameraComponent) }.forEach {comp ->
            if (!comp[CameraComponent].fixedAspectRatio) {
                comp[CameraComponent].camera.setViewportSize(width, height)
            }
        }
    }

    /* ------------------------------------------------------------------ */
    /*  Physics                                                           */
    /* ------------------------------------------------------------------ */

    fun onSimulationStart() = onPhysics2DStart()
    fun onSimulationStop()  = onPhysics2DStop()
    fun onRuntimeStart()    = onPhysics2DStart()
    fun onRuntimeStop()     = onPhysics2DStop()

    private fun onPhysics2DStart() {
        physicsWorld.onStart()

        world.family { all(RigidBody2DComponent) }.forEach {
            val transform = it[TransformComponent]
            val rb2d = it[RigidBody2DComponent]

            val body = physicsWorld.body(type = rb2d.type.toBox2d()) {
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

    private fun onPhysics2DStop() { /* TODO */}

    /* ------------------------------------------------------------------ */
    /*  Update loops                                                      */
    /* ------------------------------------------------------------------ */

    fun onUpdateSimulation(dt: Float, camera: EditorCamera) {
        syncPhysicsToTransforms(dt)

        // render
        renderScene(camera)
    }

    fun onUpdateRuntime(dt: Float) {
        world.update(dt)
        syncPhysicsToTransforms(dt)

        val pair = with(world) {
            family { all(TransformComponent, CameraComponent) }
                .firstOrNull { it[CameraComponent].primary }
                ?.let { it[CameraComponent].camera to it[TransformComponent].getTransform() }
                ?: return
        }

        val (mainCamera, transform) = pair

        Renderer.beginScene(mainCamera, transform)
        drawRenderables()
        Renderer.endScene()
    }

    fun onUpdateEditor(dt: Float, camera: EditorCamera) {
        renderScene(camera)
    }

    fun onRenderEditor(renderer: SceneRenderer, dt: Float, camera: EditorCamera) {

    }

    /* ------------------------------------------------------------------ */
    /*  Helpers                                                           */
    /* ------------------------------------------------------------------ */

    private fun syncPhysicsToTransforms(dt: Float) {
        physicsWorld.onUpdate(dt)

        world.family { all(RigidBody2DComponent) }.forEach {
            val transform = it[TransformComponent]
            it[RigidBody2DComponent].runtimeBody?.let { body ->
                val position = body.position
                transform.translation.x = position.x
                transform.translation.y = position.y
                transform.rotation.z = body.angle
            }
        }
    }

    // TODO: SceneRenderer.kt
    private fun renderScene(camera: EditorCamera) {
        Renderer.beginScene(camera)

        //! LIGHTS
        world.family { all(DirectionalLightComponent, TransformComponent) }.forEach {
            // only supports a single light rn
            val dLight = it[DirectionalLightComponent]

            lightEnvironment.light = DirectionalLight(
                dLight.color,
                dLight.diffuseIntensity,
                dLight.direction
            )
        }

        lightEnvironment.bake()
        //drawRenderables()

        Renderer.endScene()
    }

    private fun drawRenderables() {
        world.family { all(SpriteRendererComponent, TransformComponent) }.forEach {
            Renderer2D.drawSprite(it[TransformComponent].getTransform(), it[SpriteRendererComponent], it.id)
        }
        world.family { all(CircleRendererComponent, TransformComponent) }.forEach {
            Renderer2D.drawCircle(it[TransformComponent].getTransform(), it[CircleRendererComponent], it.id)
        }
    }

    fun getPrimaryCameraEntity(): Entity? = with(world) {
        family { all(CameraComponent) }
            .firstOrNull { it[CameraComponent].primary }
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
                val newEntity = newScene.createEntity(tag, id)

                val components = other.world.snapshotOf(src).components
                //Logger.warn("Copying entity $id with components ${components.map { it::class.simpleName }}")

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
                    is TransformComponent           -> entity += comp.copy()

                    // 2D rendering
                    is SpriteRendererComponent      -> entity += comp.copy()
                    is CircleRendererComponent      -> entity += comp.copy()

                    // physics 2D
                    is RigidBody2DComponent         -> entity += comp.copy()
                    is BoxCollider2DComponent       -> entity += comp.copy()
                    is CircleCollider2DComponent    -> entity += comp.copy()

                    is CameraComponent              -> entity += comp.copy()

                    // 3d
                    is StaticMeshComponent          -> entity += comp.copy()

                    // lights
                    is DirectionalLightComponent    -> entity += comp.copy()

                    else -> {
                        if (comp !is IDComponent && comp !is TagComponent)
                            Logger.warn("Unmatched component: ${comp::class.simpleName}")
                    }
                    // TODO: add ScriptComponent
                }
            }
        }
    }
}